package com.wego.service;

import com.wego.domain.TripConstants;
import com.wego.entity.GhostMember;
import com.wego.entity.User;
import com.wego.repository.GhostMemberRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves participant UUIDs to display information from either User or GhostMember tables.
 *
 * This is the single point of abstraction for the dual-source UUID lookup pattern.
 * All code that needs to display participant names/avatars should use this resolver
 * instead of directly querying UserRepository.
 *
 * @contract
 *   - pre: participantIds may contain UUIDs from User.id or GhostMember.id
 *   - post: Returns a map with entries for all found participants
 *   - post: Unknown UUIDs get a fallback ParticipantInfo with UNKNOWN_USER_NAME
 *   - performance: Maximum 2 DB queries regardless of input size (batch User + batch Ghost)
 *   - calledBy: ExpenseService, SettlementService
 */
@Component
@RequiredArgsConstructor
public class ParticipantResolver {

    private final UserRepository userRepository;
    private final GhostMemberRepository ghostMemberRepository;

    /**
     * Display information for a participant (real user or ghost member).
     */
    public record ParticipantInfo(
            UUID id,
            String nickname,
            String avatarUrl,
            boolean isGhost
    ) {}

    /**
     * Batch-resolves a set of participant UUIDs to display information.
     *
     * @contract
     *   - pre: participantIds != null
     *   - post: Returns map with ParticipantInfo for each found UUID
     *   - post: UUIDs not found in either table get a fallback entry
     *   - performance: Exactly 1 User query + 1 GhostMember query (if needed)
     *
     * @param participantIds Set of UUIDs to resolve
     * @return Map of UUID to ParticipantInfo
     */
    public Map<UUID, ParticipantInfo> resolveAll(Set<UUID> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ParticipantInfo> result = new HashMap<>();
        Set<UUID> foundIds = new HashSet<>();

        // 1st query: batch load users
        List<User> users = userRepository.findAllById(participantIds);
        for (User user : users) {
            result.put(user.getId(), new ParticipantInfo(
                    user.getId(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    false
            ));
            foundIds.add(user.getId());
        }

        // Remaining IDs are potentially ghost members
        Set<UUID> remainingIds = new HashSet<>(participantIds);
        remainingIds.removeAll(foundIds);

        if (!remainingIds.isEmpty()) {
            // 2nd query: batch load ghost members
            List<GhostMember> ghosts = ghostMemberRepository.findAllById(remainingIds);
            for (GhostMember ghost : ghosts) {
                result.put(ghost.getId(), new ParticipantInfo(
                        ghost.getId(),
                        ghost.getDisplayName(),
                        null,
                        true
                ));
                foundIds.add(ghost.getId());
            }
        }

        // Fallback for any remaining unknown UUIDs
        Set<UUID> unknownIds = new HashSet<>(participantIds);
        unknownIds.removeAll(foundIds);
        for (UUID unknownId : unknownIds) {
            result.put(unknownId, new ParticipantInfo(
                    unknownId,
                    TripConstants.UNKNOWN_USER_NAME,
                    null,
                    false
            ));
        }

        return result;
    }

    /**
     * Resolves a single participant UUID to display information.
     *
     * @contract
     *   - pre: participantId != null
     *   - post: Returns ParticipantInfo (never null)
     *
     * @param participantId The UUID to resolve
     * @return ParticipantInfo for the participant
     */
    public ParticipantInfo resolve(UUID participantId) {
        return resolveAll(Set.of(participantId)).get(participantId);
    }
}
