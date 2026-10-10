
package com.example.secure.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/public/hello")
    public String publicHello() {
        return "Publiczny endpoint - dostep dla wszystkich";
    }

    @GetMapping("/api/user/hello")
    public String userHello() {
        return "Witaj USER!";
    }

    @GetMapping("/api/admin/hello")
    public String adminHello() {
        return "Witaj ADMIN!";
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Map.of(
                "id", jwt.getSubject(),
                "username", authentication.getName(),
                "roles", roles
        );
    }
}
