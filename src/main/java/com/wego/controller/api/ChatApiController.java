package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.ChatRequest;
import com.wego.dto.response.ChatResponse;
import com.wego.exception.UnauthorizedException;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for AI chat operations.
 *
 * @contract
 *   - pre: User must be authenticated
 *   - post: All responses follow ApiResponse format
 *   - calls: ChatService
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    /**
     * Sends a chat message and gets an AI reply.
     *
     * @contract
     *   - pre: User authenticated, is member of trip
     *   - pre: Request body validated (message not blank, ≤500 chars)
     *   - post: Returns 200 with AI reply
     *   - calls: ChatService#chat
     */
    @PostMapping("/trips/{tripId}/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @PathVariable UUID tripId,
            @Valid @RequestBody ChatRequest request,
            @CurrentUser UserPrincipal principal) {

        log.debug("POST /api/trips/{}/chat - Chat request", tripId);

        UUID userId = requireUserId(principal);
        ChatResponse response = chatService.chat(tripId, userId, request.getMessage(), request.getTimezone());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("認證已過期，請重新登入");
        }
        return principal.getId();
    }
}
