package com.wego.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Supabase integration.
 *
 * @contract
 *   - url: Supabase project URL (e.g., https://xxx.supabase.co)
 *   - serviceKey: Supabase service role key for server-side operations
 */
@Configuration
@ConfigurationProperties(prefix = "wego.supabase")
@Getter
@Setter
public class SupabaseProperties {

    /**
     * Supabase project URL.
     */
    private String url;

    /**
     * Supabase service role key (server-side only).
     */
    private String serviceKey;

    /**
     * Storage bucket name for documents.
     */
    private String storageBucket = "documents";

    /**
     * Signed URL expiry time in seconds (default: 1 hour).
     */
    private int signedUrlExpiry = 3600;
}
