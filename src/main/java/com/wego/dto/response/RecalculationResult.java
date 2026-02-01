package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for batch transport recalculation results.
 *
 * @contract
 *   - totalActivities: total number of activities in the trip
 *   - recalculatedCount: number of activities that were recalculated
 *   - apiSuccessCount: successful Google API calls
 *   - fallbackCount: activities that fell back to Haversine estimation
 *   - skippedCount: activities that were skipped (e.g., first activity has no transport)
 *   - manualCount: activities with manual transport input (FLIGHT/HIGH_SPEED_RAIL)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalculationResult {

    private int totalActivities;
    private int recalculatedCount;
    private int apiSuccessCount;
    private int fallbackCount;
    private int skippedCount;
    private int manualCount;
    private boolean rateLimitReached;
    private String message;

    /**
     * Creates a success result message in Traditional Chinese.
     *
     * @return Human-readable result message
     */
    public String getSuccessMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("已完成重新計算！");

        if (apiSuccessCount > 0) {
            sb.append(String.format("成功使用 Google API 計算 %d 個景點。", apiSuccessCount));
        }

        if (fallbackCount > 0) {
            sb.append(String.format("有 %d 個景點使用估算值。", fallbackCount));
        }

        if (manualCount > 0) {
            sb.append(String.format("有 %d 個景點使用手動輸入時間。", manualCount));
        }

        if (skippedCount > 0) {
            sb.append(String.format("跳過 %d 個景點（首個景點無需計算交通）。", skippedCount));
        }

        if (rateLimitReached) {
            sb.append("已達到 API 呼叫上限。");
        }

        return sb.toString();
    }
}
