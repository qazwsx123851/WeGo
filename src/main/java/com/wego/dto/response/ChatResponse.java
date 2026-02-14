package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for chat endpoint.
 *
 * @contract
 *   - reply: The AI-generated reply text
 *   - sources: Optional list of search sources (null when no search was performed)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private String reply;

    private List<SearchSource> sources;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSource {
        private String title;
        private String url;
    }
}
