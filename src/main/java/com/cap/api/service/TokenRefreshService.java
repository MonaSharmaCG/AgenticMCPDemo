package com.cap.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenRefreshService {
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    public String refreshAccessToken(String provider, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                provider,
                authentication.getName());
        if (client == null || client.getRefreshToken() == null) {
            return "No refresh token available.";
        }
        String tokenUri = getTokenUri(provider);
        if (tokenUri == null) {
            return "Token endpoint not configured.";
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", client.getRefreshToken().getTokenValue());
        params.put("client_id", getClientId(provider));
        params.put("client_secret", getClientSecret(provider));
        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(tokenUri, HttpMethod.POST, request, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().get("access_token") != null) {
            return (String) response.getBody().get("access_token");
        }
        return "Failed to refresh token.";
    }

    private String getTokenUri(String provider) {
        if ("github".equals(provider)) {
            return "https://github.com/login/oauth/access_token";
        } else if ("jira".equals(provider)) {
            return "https://auth.atlassian.com/oauth/token";
        }
        return null;
    }

    private String getClientId(String provider) {
        if ("github".equals(provider)) {
            return "Ov23liwbwl9y20J3xIbe";
        } else if ("jira".equals(provider)) {
            return "TExm9NeMet4GpeUdwxqkpF3A5z6eRozc";
        }
        return null;
    }

    private String getClientSecret(String provider) {
        if ("github".equals(provider)) {
            return "01f63b39a8c1e905612373d540afa1c297b8dc10";
        } else if ("jira".equals(provider)) {
            return "ATOAh_WGYPkVat_WLLVA-ZrQLCsQbvbkiOUTd_HROydBdWW-iBGDZh9Mynews80WS9hl08F183C2";
        }
        return null;
    }
}
