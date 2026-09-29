package com.asfaw.review_ai.web.api.dto;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public record CurrentUserResponse(String username, List<String> roles) {

    public static CurrentUserResponse from(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }
}
