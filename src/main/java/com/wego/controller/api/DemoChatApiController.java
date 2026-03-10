package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.ChatRequest;
import com.wego.dto.response.ChatResponse;
import com.wego.exception.BusinessException;
import com.wego.service.ChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for demo AI chat (unauthenticated).
 *
 * @contract
 *   - pre: No authentication required
 *   - post: All responses follow ApiResponse format
 *   - calls: ChatService#demoChat
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoChatApiController {

    private final ChatService chatService;

    /**
     * Sends a demo chat message and gets an AI reply.
     *
     * @contract
     *   - pre: Request body validated (message not blank, ≤500 chars)
     *   - post: Returns 200 with AI reply, or 429 if rate limited
     *   - calls: ChatService#demoChat
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> demoChat(
            @Valid @RequestBody ChatRequest request,
            HttpSession session) {
        try {
            ChatResponse response = chatService.demoChat(
                    request.getMessage(), session.getId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            if ("RATE_LIMITED".equals(e.getErrorCode())) {
                return ResponseEntity.status(429)
                        .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        }
    }
}
