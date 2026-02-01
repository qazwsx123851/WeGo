package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight trip summary for filter dropdowns.
 *
 * @contract
 *   - id: trip ID
 *   - title: trip title for display
 *   - documentCount: number of documents in trip
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSummary {

    private UUID id;
    private String title;
    private long documentCount;
}
