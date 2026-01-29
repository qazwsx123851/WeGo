package com.wego.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration.
 *
 * Configures view controllers and static resource handling.
 *
 * @contract
 *   - pre: Spring MVC is enabled
 *   - post: Static resources and simple view controllers are configured
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Adds simple automated view controllers.
     *
     * @contract
     *   - pre: registry is not null
     *   - post: View controllers for login page are registered
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }

    /**
     * Configures static resource handlers.
     *
     * @contract
     *   - pre: registry is not null
     *   - post: Static resources (css, js, images) are mapped
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/");
        registry
            .addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/");
        registry
            .addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/");
    }
}
