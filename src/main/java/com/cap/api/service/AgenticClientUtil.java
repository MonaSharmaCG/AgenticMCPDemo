package com.cap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;

public class AgenticClientUtil {
    // Helper method to extract only the user stories from the Jira response JSON
    public static String extractStories(String jiraJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jiraJson);
            JsonNode issues = root.path("issues");
            if (issues.isMissingNode() || !issues.isArray()) {
                return "";
            }
            // Collect stories into a list for sorting
            java.util.List<String> storyList = new java.util.ArrayList<>();
            for (JsonNode issue : issues) {
                String key = issue.path("key").asText("");
                String summary = issue.path("fields").path("summary").asText("");
                String status = issue.path("fields").path("status").path("name").asText("");
                String description = issue.path("fields").path("description").asText("");
                // Confluence Content (custom field, adjust field name as needed)
                String confluenceContent = issue.path("fields").path("customfield_confluence").asText("");
                // Collect all comments bodies
                StringBuilder allComments = new StringBuilder();
                JsonNode commentsNode = issue.path("fields").path("comment").path("comments");
                if (commentsNode.isArray()) {
                    Iterator<JsonNode> it = commentsNode.elements();
                    while (it.hasNext()) {
                        JsonNode comment = it.next();
                        String body = comment.path("body").asText("");
                        allComments.append(body).append("||");
                    }
                }
                // Compose a string with all relevant fields
                storyList.add(key + "|" + summary + "|" + status + "|" + description + "|" + confluenceContent + "|" + allComments.toString());
            }
            // Sort by key for stable comparison
            java.util.Collections.sort(storyList);
            StringBuilder sb = new StringBuilder();
            for (String story : storyList) {
                sb.append(story).append("\n");
            }
            return sb.toString();
        } catch (Exception ex) {
            return jiraJson; // fallback: compare raw JSON
        }
    }
}
