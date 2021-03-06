package pl.consdata.ico.sqcompanion.repository;

import pl.consdata.ico.sqcompanion.config.model.ProjectLink;

import java.util.List;

/**
 * @author gregorry
 */
@FunctionalInterface
public interface ProjectLinkResolver {

    List<Project> resolveProjectLink(final ProjectLink projectLink);

}
