package com.wego.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new trip.
 *
 * @contract
 *   - title: 1-100 characters, not blank
 *   - description: max 500 characters, optional
 *   - startDate: required, must be in the future
 *   - endDate: required, must be >= startDate
 *   - baseCurrency: 3-letter currency code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotBlank(message = "行程名稱不可為空")
    @Size(max = 100, message = "行程名稱不可超過100字")
    private String title;

    @Size(max = 500, message = "描述不可超過500字")
    private String description;

    @NotNull(message = "開始日期不可為空")
    private LocalDate startDate;

    @NotNull(message = "結束日期不可為空")
    private LocalDate endDate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "幣別格式錯誤")
    @Builder.Default
    private String baseCurrency = "TWD";

    private String coverImageUrl;
}
