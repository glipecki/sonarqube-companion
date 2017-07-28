package pl.consdata.ico.sqcompanion.repository;

import lombok.extern.slf4j.Slf4j;
import pl.consdata.ico.sqcompanion.config.ProjectLink;
import pl.consdata.ico.sqcompanion.config.RegexProjectLink;
import pl.consdata.ico.sqcompanion.sonarqube.SonarQubeFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gregorry
 */
@Slf4j
@Service
public class RegexProjectLinkResolver implements ProjectLinkResolver {

    private final SonarQubeFacade sonarQubeFacade;

    public RegexProjectLinkResolver(final SonarQubeFacade sonarQubeFacade) {
        this.sonarQubeFacade = sonarQubeFacade;
    }

    @Override
    public List<Project> resolveProjectLink(final ProjectLink projectLink) {
        final RegexProjectLink regexProjectLink = RegexProjectLink.of(projectLink);
        return sonarQubeFacade.getProjects(projectLink.getServerId())
                .stream()
                .filter(project -> regexProjectLink.includes(project.getKey()))
                .filter(project -> !regexProjectLink.excludes(project.getKey()))
                .map(
                        project -> Project.builder()
                                .key(project.getKey())
                                .name(project.getName())
                                .serverId(projectLink.getServerId())
                                .build()
                )
                .collect(Collectors.toList());
    }

}
