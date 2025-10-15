package com.cap.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.*;

@RestController
public class GitHubApiController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping({"/github/repos", "/mcp/github/repos"})
    @ResponseBody
    public ResponseEntity<String> getRepos(OAuth2AuthenticationToken authentication) throws Exception {
        System.out.println("[GitHubApiController] /github/repos called");
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            System.out.println("[GitHubApiController] No OAuth2 token found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No OAuth2 token found.");
        }
        String githubToken = client.getAccessToken().getTokenValue();
        System.out.println("[GitHubApiController] Using OAuth2 token: " + githubToken);

        String githubApiUrl = "https://api.github.com/user/repos";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> githubApiResponse = restTemplate.exchange(githubApiUrl, HttpMethod.GET, entity, String.class);

        System.out.println("[GitHubApiController] GitHub API status: " + githubApiResponse.getStatusCode());
        System.out.println("[GitHubApiController] GitHub API response: " + githubApiResponse.getBody());

        String githubApiResponseBody = githubApiResponse.getBody();
        StringBuilder formatted = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode repos = mapper.readTree(githubApiResponseBody);
        if (repos.isArray()) {
            for (JsonNode repo : repos) {
                String name = repo.path("name").asText("");
                String desc = repo.path("description").asText("");
                formatted.append("Name: ").append(name);
                if (!desc.isEmpty()) {
                    formatted.append(" | Description: ").append(desc);
                }
                formatted.append("\n");
            }
        }
        return ResponseEntity.ok(formatted.toString());
    }

    // New endpoint to fetch Jira user info
    @GetMapping("/jira/me")
    @ResponseBody
    public ResponseEntity<String> getJiraMe(OAuth2AuthenticationToken authentication) throws Exception {
        System.out.println("[GitHubApiController] /jira/me called");
        if (!"jira".equals(authentication.getAuthorizedClientRegistrationId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Not authenticated with Jira. <a href=\"/oauth2/authorization/jira\">Login with Jira</a>");
        }
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            "jira",
            authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            System.out.println("[GitHubApiController] No Jira OAuth2 token found.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No Jira OAuth2 token found.");
        }
        String jiraToken = client.getAccessToken().getTokenValue();
        System.out.println("[GitHubApiController] Using Jira OAuth2 token: " + jiraToken);

        String jiraApiUrl = "https://api.atlassian.com/me";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jiraToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> jiraApiResponse = restTemplate.exchange(jiraApiUrl, HttpMethod.GET, entity, String.class);

        System.out.println("[GitHubApiController] Jira API status: " + jiraApiResponse.getStatusCode());
        System.out.println("[GitHubApiController] Jira API response: " + jiraApiResponse.getBody());

        return ResponseEntity.ok(jiraApiResponse.getBody());
    }
}