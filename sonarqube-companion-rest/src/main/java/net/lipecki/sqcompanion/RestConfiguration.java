package net.lipecki.sqcompanion;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

/**
 * @author gregorry
 */
@Configuration
public class RestConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ErrorViewResolver customErrorViewResolver() {
        final ModelAndView redirectToIndexHtml = new ModelAndView("forward:/index.html", Collections.emptyMap(), HttpStatus.OK);
        return (request, status, model) -> status == HttpStatus.NOT_FOUND ? redirectToIndexHtml : null;
    }

}
