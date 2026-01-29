package com.wego.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a trip.
 * All fields are optional - only non-null fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRequest {

    @Size(min = 1, max = 100, message = "行程名稱不可為空且不可超過100字")
    private String title;

    @Size(max = 500, message = "描述不可超過500字")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "幣別格式錯誤")
    private String baseCurrency;

    private String coverImageUrl;
}
