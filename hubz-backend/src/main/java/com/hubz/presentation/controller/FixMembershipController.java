package com.hubz.presentation.controller;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.MemberRole;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/fix")
@RequiredArgsConstructor
public class FixMembershipController {

    private final OrganizationRepositoryPort organizationRepository;
    private final OrganizationMemberRepositoryPort memberRepository;
    private final UserRepositoryPort userRepository;
    private final JwtService jwtService;

    @PostMapping("/membership")
    public ResponseEntity<?> fixMembership(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from token
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return fixMembershipForUser(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/membership/{email}")
    public ResponseEntity<?> fixMembershipByEmail(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            return fixMembershipForUser(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private ResponseEntity<?> fixMembershipForUser(User user) {
        try {

            // Get all organizations
            List<Organization> orgs = organizationRepository.findAll();

            Map<String, Object> result = new HashMap<>();
            List<String> fixed = new ArrayList<>();

            // Fix membership for each organization where user is owner but not member
            for (Organization org : orgs) {
                if (org.getOwnerId().equals(user.getId())) {
                    boolean isMember = memberRepository.findByOrganizationIdAndUserId(org.getId(), user.getId()).isPresent();

                    if (!isMember) {
                        OrganizationMember member = OrganizationMember.builder()
                                .id(UUID.randomUUID())
                                .organizationId(org.getId())
                                .userId(user.getId())
                                .role(MemberRole.OWNER)
                                .joinedAt(LocalDateTime.now())
                                .build();

                        memberRepository.save(member);
                        fixed.add(org.getName() + " (" + org.getId() + ")");
                    }
                }
            }

            result.put("userId", user.getId());
            result.put("email", user.getEmail());
            result.put("fixedOrganizations", fixed);
            result.put("message", fixed.isEmpty() ? "Aucune organisation à fixer" : "Relations créées avec succès!");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
