package net.lipecki.sqcompanion.group;

import net.lipecki.sqcompanion.SQCompanionException;
import net.lipecki.sqcompanion.health.HealthStatus;
import net.lipecki.sqcompanion.project.ProjectSummary;
import net.lipecki.sqcompanion.project.ProjectSummaryService;
import net.lipecki.sqcompanion.project.ProjectViolations;
import net.lipecki.sqcompanion.repository.Group;
import net.lipecki.sqcompanion.repository.RepositoryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * @author gregorry
 */
@RestController
@RequestMapping("/groups")
public class GroupController {

	private final RepositoryService repositoryService;
	private final ProjectSummaryService projectSummaryService;

	public GroupController(
			final RepositoryService repositoryService,
			final ProjectSummaryService projectSummaryService) {
		this.repositoryService = repositoryService;
		this.projectSummaryService = projectSummaryService;
	}

	@RequestMapping({"", "/"})
	public GroupWithSubGroupsSummary getAllGrups() {
		return asGroupWithSubGroupsSummary(repositoryService.getRootGroup());
	}

	@RequestMapping("/root")
	public GroupDetails getRootGroup() {
		return asGroupDetails(repositoryService.getRootGroup());
	}

	@RequestMapping("/{uuid}")
	public GroupDetails getGroup(@PathVariable final String uuid) {
		final Optional<Group> group = repositoryService.getGroup(uuid);
		if (group.isPresent()) {
			return asGroupDetails(group.get());
		} else {
			throw new SQCompanionException("Can't find requested group uuid: " + uuid);
		}
	}

	private GroupDetails asGroupDetails(final Group group) {
		final List<ProjectSummary> projectSummaries = projectSummaryService.getProjectSummaries(group.getAllProjects());
		final HealthStatus healthStatus = getProjectsHealthStatus(projectSummaries);

		return GroupDetails
				.builder()
				.groups(group.getGroups().stream().map(this::asGroupSummary).collect(Collectors.toList()))
				.uuid(group.getUuid())
				.name(group.getName())
				.projects(projectSummaries)
				.healthStatus(healthStatus)
				.blockers(getProjectViolationsSum(projectSummaries, ProjectViolations::getBlockers))
				.criticals(getProjectViolationsSum(projectSummaries, ProjectViolations::getCriticals))
				.majors(getProjectViolationsSum(projectSummaries, ProjectViolations::getMajors))
				.minors(getProjectViolationsSum(projectSummaries, ProjectViolations::getMinors))
				.infos(getProjectViolationsSum(projectSummaries, ProjectViolations::getInfos))
				.build();
	}

	private GroupSummary asGroupSummary(final Group group) {
		final List<ProjectSummary> projectSummaries = projectSummaryService.getProjectSummaries(group.getAllProjects());
		final HealthStatus healthStatus = getProjectsHealthStatus(projectSummaries);
		return GroupSummary
				.builder()
				.healthStatus(healthStatus)
				.uuid(group.getUuid())
				.name(group.getName())
				.build();
	}

	private GroupWithSubGroupsSummary asGroupWithSubGroupsSummary(final Group group) {
		final List<ProjectSummary> projectSummaries = projectSummaryService.getProjectSummaries(group.getAllProjects());
		final HealthStatus healthStatus = getProjectsHealthStatus(projectSummaries);
		return GroupWithSubGroupsSummary
				.builder()
				.healthStatus(healthStatus)
				.uuid(group.getUuid())
				.name(group.getName())
				.groups(group.getGroups().stream().map(this::asGroupWithSubGroupsSummary).collect(Collectors.toList()))
				.build();
	}

	private int getProjectViolationsSum(final List<ProjectSummary> projectSummaries, final ToIntFunction<ProjectViolations> violationsExtractor) {
		return projectSummaries.stream().map(ProjectSummary::getViolations).mapToInt(violationsExtractor).sum();
	}

	private HealthStatus getProjectsHealthStatus(final List<ProjectSummary> projectSummaries) {
		return projectSummaries
				.stream()
				.map(ProjectSummary::getHealth)
				.reduce((a, b) -> a.getPriority() > b.getPriority() ? a : b)
				.orElseThrow(() -> new SQCompanionException("Can't calculate group status"));
	}

}
