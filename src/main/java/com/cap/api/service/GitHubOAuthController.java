package com.cap.api.service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class GitHubOAuthController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GitHubOAuthController.class);

    @Autowired
    private TokenRevokeService tokenRevokeService;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @GetMapping("/token")
    @ResponseBody
    public String getToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName());
        if (client != null && client.getAccessToken() != null) {
            // In production, never expose the access token directly
            // Instead, use it server-side for API calls, and store securely in session if needed
            return "Token is available (not exposed for security)";
        }
        return "No token";
    }

@GetMapping("/")
@ResponseBody
public String home(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken authentication) {
    if (principal == null || authentication == null) {
        return "<a href=\"/oauth2/authorization/github\">Login with GitHub</a><br/>"
             + "<a href=\"/oauth2/authorization/jira\">Login with Jira</a>";
    }
    String provider = authentication.getAuthorizedClientRegistrationId();
    String username = null;
    if ("github".equals(provider)) {
        username = principal.getAttribute("login");
    } else if ("jira".equals(provider)) {
        username = principal.getAttribute("email"); // or "sub" or another attribute
    }
    log.info("Provider: {}", provider);
    log.info("Username: {}", username);
    return "Hello, " + (username != null ? username : "user") + "!<br/>"
         + "<a href=\"/logout\">Logout</a>";
}

    // Endpoint to check Jira token
    @GetMapping("/jira/token")
    @ResponseBody
    public String getJiraToken(OAuth2AuthenticationToken authentication) {
        if (!"jira".equals(authentication.getAuthorizedClientRegistrationId())) {
            return "Not authenticated with Jira. <a href=\"/oauth2/authorization/jira\">Login with Jira</a>";
        }
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            "jira",
            authentication.getName());
        if (client != null && client.getAccessToken() != null) {
            // In production, never expose the access token directly
            // Instead, use it server-side for API calls, and store securely in session if needed
            return "Jira token is available (not exposed for security)";
        }
        return "No Jira token";
    }

    // Add a logout endpoint to ensure session and tokens are invalidated
    @GetMapping("/secure-logout")
    public String secureLogout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Attempt to revoke tokens before logout
        Object authObj = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authObj instanceof OAuth2AuthenticationToken authentication) {
            String provider = authentication.getAuthorizedClientRegistrationId();
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(provider, authentication.getName());
            if (client != null && client.getAccessToken() != null) {
                String token = client.getAccessToken().getTokenValue();
                tokenRevokeService.revokeToken(provider, token);
            }
        }
        // Invalidate session and clear authentication
        request.getSession().invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // Endpoint to refresh OAuth2 token
    @GetMapping("/refresh-token")
    @ResponseBody
    public String refreshToken(@RequestParam String provider, OAuth2AuthenticationToken authentication) {
        return tokenRefreshService.refreshAccessToken(provider, authentication);
    }
}