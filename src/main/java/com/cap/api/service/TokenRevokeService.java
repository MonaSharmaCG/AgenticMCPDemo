package com.cap.api.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenRevokeService {
    public boolean revokeToken(String provider, String token) {
        String revokeUri = getRevokeUri(provider);
        if (revokeUri == null) return false;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> params = new HashMap<>();
        if ("github".equals(provider)) {
            params.put("access_token", token);
        } else if ("jira".equals(provider)) {
            params.put("token", token);
            headers.setBearerAuth(token);
        }
        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.exchange(revokeUri, HttpMethod.POST, request, String.class);
        return response.getStatusCode().is2xxSuccessful();
    }
    private String getRevokeUri(String provider) {
        if ("github".equals(provider)) {
            return "https://github.com/settings/connections/applications"; // GitHub does not provide a direct revoke endpoint, must be done via UI or API call to delete grant
        } else if ("jira".equals(provider)) {
            return "https://auth.atlassian.com/oauth/token/revoke";
        }
        return null;
    }
}
