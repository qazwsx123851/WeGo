package com.wego.controller.api;

import com.wego.dto.response.ExchangeRateResponse;
import com.wego.entity.User;
import com.wego.security.UserPrincipal;
import com.wego.service.ExchangeRateService;
import com.wego.service.RateLimitService;
import com.wego.service.external.ExchangeRateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ExchangeRateApiController.
 *
 * @contract
 *   - Tests all 4 exchange rate API endpoints
 *   - Verifies input validation and normalization
 *   - Tests rate limiting
 *   - Tests error handling
 *   - Verifies authentication requirements
 */
@WebMvcTest(ExchangeRateApiController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ExchangeRateApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private RateLimitService rateLimitService;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test User")
                .provider("test")
                .providerId("test-id")
                .build();
        userPrincipal = new UserPrincipal(testUser);

        when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(true);
    }

    @Nested
    @DisplayName("GET /api/exchange-rates")
    class GetRateTests {

        @Test
        @DisplayName("should return exchange rate with 200")
        void getRate_validParams_shouldReturn200() throws Exception {
            ExchangeRateResponse response = ExchangeRateResponse.fresh(
                    "USD", "TWD", new BigDecimal("31.5"), Instant.now());

            when(exchangeRateService.getRate("USD", "TWD")).thenReturn(response);

            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.from").value("USD"))
                    .andExpect(jsonPath("$.data.to").value("TWD"))
                    .andExpect(jsonPath("$.data.rate").value(31.5));
        }

        @Test
        @DisplayName("should normalize currency codes to uppercase")
        void getRate_lowercaseInput_shouldNormalize() throws Exception {
            ExchangeRateResponse response = ExchangeRateResponse.fresh(
                    "USD", "TWD", new BigDecimal("31.5"), Instant.now());

            when(exchangeRateService.getRate("USD", "TWD")).thenReturn(response);

            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "usd")
                            .param("to", "twd")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 when from is missing")
        void getRate_missingFrom_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/exchange-rates")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when to is missing")
        void getRate_missingTo_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "USD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid currency code")
        void getRate_invalidCurrency_shouldReturn400() throws Exception {
            when(exchangeRateService.getRate(anyString(), anyString()))
                    .thenThrow(ExchangeRateException.invalidCurrency("XX"));

            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "XX")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CURRENCY"));
        }

        @Test
        @DisplayName("should return 429 when rate limited")
        void getRate_rateLimited_shouldReturn429() throws Exception {
            when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(false);

            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("should return 502 when API unavailable")
        void getRate_apiUnavailable_shouldReturn502() throws Exception {
            when(exchangeRateService.getRate(anyString(), anyString()))
                    .thenThrow(ExchangeRateException.apiError("API down"));

            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.errorCode").value("API_ERROR"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getRate_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/exchange-rates")
                            .param("from", "USD")
                            .param("to", "TWD"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/latest")
    class GetAllRatesTests {

        @Test
        @DisplayName("should return all rates with 200")
        void getAllRates_validBase_shouldReturn200() throws Exception {
            Map<String, BigDecimal> rates = Map.of(
                    "USD", BigDecimal.ONE,
                    "TWD", new BigDecimal("31.5"),
                    "JPY", new BigDecimal("149.5")
            );
            when(exchangeRateService.getAllRates("USD")).thenReturn(rates);

            mockMvc.perform(get("/api/exchange-rates/latest")
                            .param("base", "USD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.TWD").value(31.5))
                    .andExpect(jsonPath("$.data.JPY").value(149.5));
        }

        @Test
        @DisplayName("should return 400 when base is missing")
        void getAllRates_missingBase_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/latest")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid base currency")
        void getAllRates_invalidBase_shouldReturn400() throws Exception {
            when(exchangeRateService.getAllRates(anyString()))
                    .thenThrow(ExchangeRateException.invalidCurrency("XX"));

            mockMvc.perform(get("/api/exchange-rates/latest")
                            .param("base", "XX")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CURRENCY"));
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getAllRates_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/latest")
                            .param("base", "USD"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/convert")
    class ConvertTests {

        @Test
        @DisplayName("should convert amount with 200")
        void convert_validParams_shouldReturn200() throws Exception {
            ExchangeRateResponse rateResponse = ExchangeRateResponse.fresh(
                    "USD", "TWD", new BigDecimal("31.5"), Instant.now());

            when(exchangeRateService.getRate("USD", "TWD")).thenReturn(rateResponse);
            when(exchangeRateService.convert(any(BigDecimal.class), eq("USD"), eq("TWD")))
                    .thenReturn(new BigDecimal("3150.00"));

            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .param("amount", "100")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.from").value("USD"))
                    .andExpect(jsonPath("$.data.to").value("TWD"))
                    .andExpect(jsonPath("$.data.originalAmount").value(100))
                    .andExpect(jsonPath("$.data.convertedAmount").value(3150.00))
                    .andExpect(jsonPath("$.data.rate").value(31.5));
        }

        @Test
        @DisplayName("should return 400 when amount is missing")
        void convert_missingAmount_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void convert_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/convert")
                            .param("from", "USD")
                            .param("to", "TWD")
                            .param("amount", "100"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/exchange-rates/currencies")
    class GetSupportedCurrenciesTests {

        @Test
        @DisplayName("should return supported currencies with 200")
        void getSupportedCurrencies_shouldReturn200() throws Exception {
            Set<String> currencies = Set.of("USD", "TWD", "JPY", "EUR");
            when(exchangeRateService.getSupportedCurrencies()).thenReturn(currencies);

            mockMvc.perform(get("/api/exchange-rates/currencies")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("should return 429 when rate limited")
        void getSupportedCurrencies_rateLimited_shouldReturn429() throws Exception {
            when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(false);

            mockMvc.perform(get("/api/exchange-rates/currencies")
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("should return 403 when not authenticated")
        void getSupportedCurrencies_notAuthenticated_shouldReturn403() throws Exception {
            mockMvc.perform(get("/api/exchange-rates/currencies"))
                    .andExpect(status().isForbidden());
        }
    }
}
