package pl.consdata.ico.sqcompanion.members.integrations.ldap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import pl.consdata.ico.sqcompanion.config.AppConfig;
import pl.consdata.ico.sqcompanion.config.model.Member;
import pl.consdata.ico.sqcompanion.integrations.ldap.LdapService;
import pl.consdata.ico.sqcompanion.integrations.ldap.configuration.LdapGroupsConfig;
import pl.consdata.ico.sqcompanion.integrations.ldap.configuration.LdapMembersConfig;
import pl.consdata.ico.sqcompanion.members.MembersProvider;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapMembersIntegration implements MembersProvider {
    public static final String TYPE = "LDAP";
    private final LdapService ldapService;
    private final AppConfig appConfig;

    @Override
    public List<Member> getMembers() {
        Map<String, Member> membersMap = new HashMap<>();
        LdapGroupsConfig config = appConfig.getIntegrations().getLdap().getGroups();
        config.getGroupMappings().keySet()
                .stream()
                .map(this::getLdapGroups)
                .flatMap(List::stream)
                .filter(ldapGroup -> StringUtils.isNotEmpty(ldapGroup.getSqcUuid()))
                .forEach(ldapGroup -> ldapGroup.getMembers().forEach(member -> {
                    if (membersMap.containsKey(member.getUuid())) {
                        membersMap.get(member.getUuid()).getGroups().add(ldapGroup.getSqcUuid());
                    } else {
                        member.getGroups().add(ldapGroup.getSqcUuid());
                        membersMap.put(member.getUuid(), member);
                    }
                }));

        return membersMap.values().stream()
                .map(this::getMemberInfo)
                .flatMap(List::stream)
                .filter(m -> StringUtils.isNotEmpty(m.getUuid()))
                .collect(Collectors.toList());
    }

    private List<Member> getMemberInfo(Member member) {
        LdapMembersConfig config = appConfig.getIntegrations().getLdap().getMembers();
        return ldapService.search(query().where("objectClass").is(config.getMemberObjectClass()).and(query().where(config.getIdAttribute()).is(member.getUuid())), attr -> asMember(member, attr));
    }

    private Member asMember(Member member, Attributes attributes) {
        LdapMembersConfig config = appConfig.getIntegrations().getLdap().getMembers();
        try {
            return Member.builder()
                    .uuid(member.getUuid())
                    .firstName(getAttributeOrDefault(config.getFirstNameAttribute(), StringUtils.EMPTY, attributes))
                    .lastName(getAttributeOrDefault(config.getLastNameAttribute(), StringUtils.EMPTY, attributes))
                    .mail(getAttributeOrDefault(config.getMailAttribute(), StringUtils.EMPTY, attributes))
                    .groups(member.getGroups())
                    .aliases(getAttributeOrDefault(config.getAliasesAttributes(), new HashSet<>(), attributes))
                    .remote(true)
                    .remoteType(TYPE)
                    .build();
        } catch (Exception e) {
            log.warn("Unable to fetch member info for {}", member.getUuid(), e);
            return Member.builder().build();
        }
    }

    private String getAttributeOrDefault(String key, String defaultValue, Attributes attributes) {
        try {
            return attributes.get(key).get().toString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Set<String> getAttributeOrDefault(List<String> keys, Set<String> defaultValue, Attributes attributes) {
        try {
            return keys.stream().map(k -> getAttributeOrDefault(k, StringUtils.EMPTY, attributes))
                    .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private List<LdapGroup> getLdapGroups(String name) {
        log.info("[LDAP] >Get members for group {}", name);
        LdapGroupsConfig config = appConfig.getIntegrations().getLdap().getGroups();
        return ldapService.search(query().where("objectClass").is(config.getGroupObjectClass()).and(query().where(config.getGroupNameAttribute()).is(name)), attr -> asLdapGroup(name, attr));
    }

    private LdapGroup asLdapGroup(String name, Attributes attributes) {
        LdapGroupsConfig config = appConfig.getIntegrations().getLdap().getGroups();
        try {
            return LdapGroup.builder()
                    .name(name)
                    .sqcUuid(config.getGroupMappings().get(name))
                    .members(memberUidEnumerationAsStream(attributes.get(config.getMembershipAttribute()).getAll()))
                    .build();
        } catch (Exception e) {
            log.warn("Unable to fetch members for {}", name, e);
            return LdapGroup.builder().build();
        }
    }

    private List<Member> memberUidEnumerationAsStream(NamingEnumeration<?> enumeration) throws NamingException {
        List<Member> output = new ArrayList<>();
        while (enumeration.hasMore()) {
            Object next = enumeration.next();
            if (next instanceof String) {
                output.add(Member.builder().uuid(((String) next)).groups(new HashSet<>()).build());
            }
        }
        return output;
    }

}
