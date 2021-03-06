package pl.consdata.ico.sqcompanion.members;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.consdata.ico.sqcompanion.config.AppConfig;
import pl.consdata.ico.sqcompanion.config.model.GroupLightModel;
import pl.consdata.ico.sqcompanion.config.model.Member;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final AppConfig appConfig;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final MembersIntegrations membersIntegrations;

    public void syncMembers() {
        log.info("> Sync members");
        syncLocalMembers();
        syncRemoteMembers();
        log.info("< Synced  members");
    }

    private void syncLocalMembers() {
        log.info("> Sync local members");
        memberRepository.saveAll(appConfig.getMembers().getLocal()
                .stream().map(definition -> MemberEntryEntity.builder()
                        .firstName(definition.getFirstName())
                        .lastName(definition.getLastName())
                        .id(definition.getUuid())
                        .aliases(definition.getAliases())
                        .mail(definition.getMail())
                        .remote(false)
                        .build()
                )
                .collect(Collectors.toList()));
        appConfig.getMembers().getLocal().forEach(this::detachMemberFromGroups);
        appConfig.getMembers().getLocal().forEach(this::attachMemberToGroups);
    }

    private void syncRemoteMembers() {
        log.info("> Sync remote members");
        List<Member> remoteMembers = membersIntegrations.getMembers();
        memberRepository.saveAll(remoteMembers
                .stream().map(definition -> MemberEntryEntity.builder()
                        .firstName(definition.getFirstName())
                        .lastName(definition.getLastName())
                        .id(definition.getUuid())
                        .aliases(definition.getAliases())
                        .mail(definition.getMail())
                        .remote(definition.isRemote())
                        .remoteType(definition.getRemoteType())
                        .build()
                )
                .collect(Collectors.toList()));
        remoteMembers.forEach(this::detachMemberFromGroups);
        remoteMembers.forEach(this::attachMemberToGroups);
    }

    private void attachMemberToGroups(Member member) {
        log.info("> Sync attached events");

        final MemberEntryEntity memberEntryEntity = memberRepository.getOne(member.getUuid());
        member.getGroups().forEach(groupId -> attachMemberToGroup(memberEntryEntity, groupId));

        log.info("< Synced attached events");
    }

    private void attachMemberToGroup(MemberEntryEntity memberEntryEntity, String groupId) {
        Optional<MembershipEntryEntity> latestEvent = membershipRepository.findFirstByMemberIdAndGroupIdOrderByDateDesc(memberEntryEntity.getId(), groupId);

        if (latestEvent.isPresent() && !latestEvent.get().getDate().isBefore(LocalDate.now())) {
            latestEvent.get().setEvent(MembershipEntryEntity.Event.ATTACHED);
            membershipRepository.save(latestEvent.get());
        } else if (!latestEvent.isPresent() || isDetached(latestEvent.get())) {
            membershipRepository.save(MembershipEntryEntity.builder()
                    .date(LocalDate.now())
                    .event(MembershipEntryEntity.Event.ATTACHED)
                    .member(memberEntryEntity)
                    .groupId(groupId)
                    .build());

        }
    }

    private void detachMemberFromGroups(Member member) {
        log.info("> Sync detached events");
        final MemberEntryEntity memberEntryEntity = memberRepository.getOne(member.getUuid());
        membershipRepository.findByMemberId(member.getUuid()).stream()
                .map(GroupsOnlyProjection::getGroupId)
                .filter(groupId -> !member.getGroups().contains(groupId))
                .forEach(groupId -> detachMemberFromGroup(memberEntryEntity, groupId));
        log.info("< Synced detached events");
    }

    private void detachMemberFromGroup(MemberEntryEntity memberEntryEntity, String groupId) {
        Optional<MembershipEntryEntity> latestEvent = membershipRepository.findFirstByMemberIdAndGroupIdOrderByDateDesc(memberEntryEntity.getId(), groupId);
        if (latestEvent.isPresent() && !latestEvent.get().getDate().isBefore(LocalDate.now())) {
            latestEvent.get().setEvent(MembershipEntryEntity.Event.DETACHED);
            membershipRepository.save(latestEvent.get());
        } else if (latestEvent.isPresent() && isAttached(latestEvent.get())) {
            membershipRepository.save(MembershipEntryEntity.builder()
                    .date(LocalDate.now())
                    .event(MembershipEntryEntity.Event.DETACHED)
                    .member(memberEntryEntity)
                    .groupId(groupId)
                    .build());

        }
    }

    public List<Member> groupMembers(String groupId) {
        return getAttachedMembers(membershipRepository.findByGroupIdAndDateIsLessThanEqualOrderByDateDesc(groupId, LocalDate.now().minusDays(1)));
    }

    public List<Member> groupMembers(String groupId, LocalDate form, LocalDate to) {
        return getAttachedMembers(membershipRepository.findByGroupIdAndDateIsBetweenOrderByDateDesc(groupId, form, to));
    }

    public List<GroupLightModel> memberGroups(String memberId) {
        return getMemberGroups(membershipRepository.findByMemberIdAndDateIsLessThanEqualOrderByDateDesc(memberId, LocalDate.now()));
    }

    public List<GroupLightModel> memberGroups(String memberId, LocalDate form, LocalDate to) {
        return getMemberGroups(membershipRepository.findByMemberIdAndDateIsBetweenOrderByDateDesc(memberId, form, to));
    }

    private void putIfLatestOrSkip(Map<String, MembershipEntryEntity> map, MembershipEntryEntity entryEntity, String key) {
        if (!map.containsKey(key) || entryEntity.getDate().isAfter(map.get(key).getDate())) {
            map.put(key, entryEntity);
        }
    }

    private List<GroupLightModel> getMemberGroups(Set<MembershipEntryEntity> entries) {
        return entries.stream()
                .collect(
                        HashMap<String, MembershipEntryEntity>::new,
                        (map, e) -> putIfLatestOrSkip(map, e, e.getGroupId()),
                        HashMap::putAll
                ).values().stream()
                .filter(this::isAttached)
                .map(entry -> GroupLightModel.builder()
                        .uuid(entry.getGroupId())
                        .name(appConfig.getGroup(entry.getGroupId()).getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<Member> getAttachedMembers(Set<MembershipEntryEntity> entries) {
        return entries.stream()
                .collect(
                        HashMap<String, MembershipEntryEntity>::new,
                        (map, e) -> putIfLatestOrSkip(map, e, e.getMember().getId()),
                        HashMap::putAll
                ).values().stream()
                .filter(this::isAttached)
                .map(entry -> Member.builder()
                        .uuid(entry.getMember().getId())
                        .firstName(entry.getMember().getFirstName())
                        .lastName(entry.getMember().getLastName())
                        .build()).collect(Collectors.toList());
    }

    private boolean isAttached(MembershipEntryEntity entryEntity) {
        return MembershipEntryEntity.Event.ATTACHED.equals(entryEntity.getEvent());
    }

    private boolean isDetached(MembershipEntryEntity entryEntity) {
        return MembershipEntryEntity.Event.DETACHED.equals(entryEntity.getEvent());
    }

    public Map<String, Long> getIntegrationsSummary() {
        return membersIntegrations.getSummary();
    }
}
