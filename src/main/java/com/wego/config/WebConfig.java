package com.wego.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.util.concurrent.TimeUnit;

/**
 * Web MVC Configuration.
 *
 * Configures view controllers and static resource handling.
 * Uses content-hash versioning for long-term browser caching.
 *
 * @contract
 *   - pre: Spring MVC is enabled
 *   - post: Static resources and simple view controllers are configured
 *   - post: Static resources use content-hash versioning for cache busting
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Enables automatic URL rewriting in Thymeleaf templates.
     * Transforms @{/css/output.css} → /css/output-<hash>.css
     */
    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

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
     * Configures static resource handlers with content-hash versioning.
     *
     * Content-hash versioning appends a hash to the URL (e.g., output-abc123.css).
     * This allows long-term caching (365 days) because the URL changes when the file changes.
     *
     * @contract
     *   - pre: registry is not null
     *   - post: Static resources (css, js, images) are mapped with versioned URLs
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl longTermCache = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();

        VersionResourceResolver versionResolver = new VersionResourceResolver()
                .addContentVersionStrategy("/**");

        registry
            .addResourceHandler("/css/**")
            .addResourceLocations("classpath:/static/css/")
            .setCacheControl(longTermCache)
            .resourceChain(true)
            .addResolver(versionResolver);
        registry
            .addResourceHandler("/js/**")
            .addResourceLocations("classpath:/static/js/")
            .setCacheControl(longTermCache)
            .resourceChain(true)
            .addResolver(versionResolver);
        registry
            .addResourceHandler("/images/**")
            .addResourceLocations("classpath:/static/images/")
            .setCacheControl(longTermCache)
            .resourceChain(true)
            .addResolver(versionResolver);
    }
}
