package pl.consdata.ico.sqcompanion.config.service.general.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.consdata.ico.sqcompanion.config.AppConfig;
import pl.consdata.ico.sqcompanion.config.model.ServerDefinition;
import pl.consdata.ico.sqcompanion.config.service.SettingsService;
import pl.consdata.ico.sqcompanion.config.validation.ValidationResult;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerConfigService {

    private final ServerConfigValidator validator;
    private final SettingsService settingsService;
    private final AppConfig appConfig;

    public ValidationResult update(final ServerDefinition serverDefinition) {
        ValidationResult validationResult = validator.validate(serverDefinition, false);
        if (validationResult.isValid()) {
            appConfig.getServers().stream().filter(server -> server.getId().equals(serverDefinition.getId()))
                    .forEach(server -> {
                        server.setId(serverDefinition.getId());
                        server.setUrl(serverDefinition.getUrl());
                        server.setAuthentication(serverDefinition.getAuthentication());
                        server.setBlacklistUsers(serverDefinition.getBlacklistUsers());
                    });
            return settingsService.save();
        } else {
            log.info("Invalid server definition {} reason: {}", serverDefinition, validationResult);
        }
        return validationResult;
    }

    public ValidationResult create(final ServerDefinition serverDefinition) {
        ValidationResult validationResult = validator.validate(serverDefinition, true);
        if (validationResult.isValid()) {
            appConfig.getServers().add(serverDefinition);
            return settingsService.save();
        } else {
            log.info("Invalid server definition {} reason: {}", serverDefinition, validationResult);
        }
        return validationResult;
    }


    public ValidationResult delete(final String uuid) {
        log.info("Delete server with id {}", uuid);
        appConfig.getServers().removeIf(server -> server.getId().equals(uuid));
        ValidationResult result = settingsService.save();
        if (result.isValid()) {
            log.info("Server {} removed permanently");
        }
        return result;
    }

    public List<ServerDefinition> get() {
        log.info("Get servers list");
        return appConfig.getServers();
    }

}

