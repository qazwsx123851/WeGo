package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.response.ExchangeRateResponse;
import com.wego.service.ExchangeRateService;
import com.wego.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * REST API controller for exchange rate operations.
 *
 * @contract
 *   - All endpoints require valid currency codes (3-letter ISO 4217)
 *   - Responses include cache metadata
 *   - Rate limiting: 30 requests per minute
 *
 * @see ExchangeRateService
 */
@Slf4j
@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateApiController {

    private static final int RATE_LIMIT_EXCHANGE = 30;

    private final ExchangeRateService exchangeRateService;
    private final RateLimitService rateLimitService;

    /**
     * Gets the exchange rate between two currencies.
     *
     * @contract
     *   - pre: from != null and matches ^[A-Z]{3}$
     *   - pre: to != null and matches ^[A-Z]{3}$
     *   - post: returns 200 with ExchangeRateResponse
     *   - throws: 400 if currency codes are invalid
     *   - throws: 502 if API is unavailable and no cache
     *   - calledBy: Frontend currency converter, ExpenseService
     *
     * @param from Source currency code (e.g., "USD")
     * @param to Target currency code (e.g., "TWD")
     * @return Exchange rate response
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getRate(
            @RequestParam String from,
            @RequestParam String to) {

        log.debug("GET /api/exchange-rates?from={}&to={}", from, to);

        if (!rateLimitService.isAllowed("exchange-rates", RATE_LIMIT_EXCHANGE)) {
            log.warn("Rate limit exceeded for exchange rates");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Normalize to uppercase
        String fromNormalized = from != null ? from.toUpperCase().trim() : null;
        String toNormalized = to != null ? to.toUpperCase().trim() : null;

        ExchangeRateResponse response = exchangeRateService.getRate(fromNormalized, toNormalized);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets all exchange rates from a base currency.
     *
     * @contract
     *   - pre: base != null and matches ^[A-Z]{3}$
     *   - post: returns 200 with map of currency codes to rates
     *   - throws: 400 if base currency is invalid
     *   - throws: 502 if API is unavailable and no cache
     *   - calledBy: Frontend currency selector
     *
     * @param base Base currency code (e.g., "USD")
     * @return Map of currency codes to rates
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getAllRates(
            @RequestParam String base) {

        log.debug("GET /api/exchange-rates/latest?base={}", base);

        if (!rateLimitService.isAllowed("exchange-rates", RATE_LIMIT_EXCHANGE)) {
            log.warn("Rate limit exceeded for exchange rates");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Normalize to uppercase
        String baseNormalized = base != null ? base.toUpperCase().trim() : null;

        Map<String, BigDecimal> rates = exchangeRateService.getAllRates(baseNormalized);

        return ResponseEntity.ok(ApiResponse.success(rates));
    }

    /**
     * Converts an amount from one currency to another.
     *
     * @contract
     *   - pre: from != null and matches ^[A-Z]{3}$
     *   - pre: to != null and matches ^[A-Z]{3}$
     *   - pre: amount != null and amount >= 0
     *   - post: returns 200 with converted amount
     *   - throws: 400 if parameters are invalid
     *   - calledBy: Frontend real-time conversion preview
     *
     * @param from Source currency code
     * @param to Target currency code
     * @param amount Amount to convert
     * @return Converted amount
     */
    @GetMapping("/convert")
    public ResponseEntity<ApiResponse<ConversionResult>> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {

        log.debug("GET /api/exchange-rates/convert?from={}&to={}&amount={}", from, to, amount);

        if (!rateLimitService.isAllowed("exchange-rates", RATE_LIMIT_EXCHANGE)) {
            log.warn("Rate limit exceeded for exchange rates");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        // Normalize to uppercase
        String fromNormalized = from != null ? from.toUpperCase().trim() : null;
        String toNormalized = to != null ? to.toUpperCase().trim() : null;

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }

        ExchangeRateResponse rateResponse = exchangeRateService.getRate(fromNormalized, toNormalized);
        BigDecimal convertedAmount = exchangeRateService.convert(amount, fromNormalized, toNormalized);

        ConversionResult result = new ConversionResult(
                fromNormalized,
                toNormalized,
                amount,
                convertedAmount,
                rateResponse.getRate(),
                rateResponse.isCached()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Gets the list of supported currencies.
     *
     * @contract
     *   - post: returns 200 with set of currency codes
     *   - calledBy: Frontend currency selector dropdown
     *
     * @return Set of supported currency codes
     */
    @GetMapping("/currencies")
    public ResponseEntity<ApiResponse<Set<String>>> getSupportedCurrencies() {
        log.debug("GET /api/exchange-rates/currencies");

        if (!rateLimitService.isAllowed("exchange-rates", RATE_LIMIT_EXCHANGE)) {
            log.warn("Rate limit exceeded for exchange rates");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED", "Too many requests. Please try again later."));
        }

        Set<String> currencies = exchangeRateService.getSupportedCurrencies();

        return ResponseEntity.ok(ApiResponse.success(currencies));
    }

    /**
     * Result DTO for currency conversion.
     */
    public record ConversionResult(
            String from,
            String to,
            BigDecimal originalAmount,
            BigDecimal convertedAmount,
            BigDecimal rate,
            boolean cached
    ) {}
}
