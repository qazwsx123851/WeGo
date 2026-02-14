package com.wego.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

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
        CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

        registry
            .addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/")
            .setCacheControl(cacheControl);
        registry
            .addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/")
            .setCacheControl(cacheControl);
        registry
            .addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/")
            .setCacheControl(cacheControl);
    }
}
