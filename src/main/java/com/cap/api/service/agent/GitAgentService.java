package com.cap.api.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GitAgentService {

    private static final Logger log = LoggerFactory.getLogger(GitAgentService.class);

    @Value("${github.token:}")
    private String githubToken;

    private int runCommand(File dir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (dir != null) pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                log.info(line);
            }
        }
        return p.waitFor();
    }

    /**
     * Commit all changes, create a branch, push to remote and create PR via GitHub API.
     * The method expects GITHUB_TOKEN in environment for authentication.
     */
    public String commitPushAndCreatePr(String repoPath, String branchName, String commitMessage, String baseBranch, String prTitle, String prBody, String reviewersCsv) throws Exception {
        File repoDir = new File(repoPath);
        // Ensure we have latest from origin for the base branch
        try {
            runCommand(repoDir, "git", "fetch", "origin");
            // checkout base branch
            runCommand(repoDir, "git", "checkout", baseBranch);
            runCommand(repoDir, "git", "pull", "origin", baseBranch);
        } catch (Exception ex) {
            log.warn("Failed to update base branch '{}', continuing: {}", baseBranch, ex.getMessage());
        }
        // configure commit author to be the bot identity so commits are authored by agenticBot (GitHub will still show the pusher based on token)
        try {
            runCommand(repoDir, "git", "config", "user.name", "agenticBot");
            runCommand(repoDir, "git", "config", "user.email", "agenticbot+actions@users.noreply.github.com");
        } catch (Exception ex) {
            log.warn("Failed to set git author config: {}", ex.getMessage());
        }
    // create and switch to new branch
    int rc = runCommand(repoDir, "git", "checkout", "-b", branchName);
        if (rc != 0) throw new RuntimeException("Failed to create branch: " + branchName);
        // git add .
        runCommand(repoDir, "git", "add", ".");
        // git commit -m commitMessage
        runCommand(repoDir, "git", "commit", "-m", commitMessage, "--allow-empty");
        // git push origin branchName
    // precedence: application property (injected), then system property, then environment variable
    String token = this.githubToken;
    if (token == null || token.isEmpty()) token = System.getProperty("github.token");
    if (token == null || token.isEmpty()) token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("GITHUB_TOKEN is required to push and create PR");
        }
    // update remote url to include token temporarily
        // derive remote url
        ProcessBuilder rb = new ProcessBuilder("git", "config", "--get", "remote.origin.url");
        rb.directory(repoDir);
        Process pr = rb.start();
        String remoteUrl;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            remoteUrl = r.readLine();
        }
        pr.waitFor();
        if (remoteUrl == null) throw new RuntimeException("Could not determine remote.origin.url");
        String authRemote = remoteUrl;
        if (remoteUrl.startsWith("https://")) {
            authRemote = remoteUrl.replaceFirst("https://", "https://" + token + "@");
            runCommand(repoDir, "git", "remote", "set-url", "origin", authRemote);
        }
        runCommand(repoDir, "git", "push", "-u", "origin", branchName);
        // restore original remote if we changed it
        if (!authRemote.equals(remoteUrl) && remoteUrl.startsWith("https://")) {
            runCommand(repoDir, "git", "remote", "set-url", "origin", remoteUrl);
        }

        // create PR via GitHub API; infer owner/repo from remote url
        // remote formats: https://github.com/owner/repo.git or git@github.com:owner/repo.git
        String owner = null, repo = null;
        if (remoteUrl.contains("github.com")) {
            String path = remoteUrl;
            if (path.endsWith(".git")) path = path.substring(0, path.length()-4);
            if (path.startsWith("https://")) {
                String[] parts = path.split("github.com/");
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            } else if (path.startsWith("git@")) {
                String[] parts = path.split(":" );
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            }
        }
        if (owner == null || repo == null) throw new RuntimeException("Could not parse owner/repo from remote url");

        String prApi = String.format("https://api.github.com/repos/%s/%s/pulls", owner, repo);
        String json = String.format("{\"title\":\"%s\",\"head\":\"%s\",\"base\":\"%s\",\"body\":\"%s\"}", escape(prTitle), escape(branchName), escape(baseBranch), escape(prBody));
        URL url = new URL(prApi);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        StringBuilder resp = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
            String ln;
            while ((ln = r.readLine()) != null) resp.append(ln).append('\n');
        }
        ObjectMapper om = new ObjectMapper();
        // optionally request reviewers
    if (reviewersCsv != null && !reviewersCsv.isBlank()) {
            // try to parse PR number via JSON
            try {
                JsonNode root = om.readTree(resp.toString());
                if (root.has("number")) {
                    String prNum = root.get("number").asText();
                    String reviewersApi = String.format("https://api.github.com/repos/%s/%s/pulls/%s/requested_reviewers", owner, repo, prNum);
                    String revJson = String.format("{\"reviewers\":[%s]}", joinReviewers(reviewersCsv));
                    URL rurl = new URL(reviewersApi);
                    HttpURLConnection rc2 = (HttpURLConnection) rurl.openConnection();
                    rc2.setRequestMethod("POST");
                    rc2.setRequestProperty("Authorization", "token " + token);
                    rc2.setRequestProperty("Accept", "application/vnd.github+json");
                    rc2.setDoOutput(true);
                    rc2.getOutputStream().write(revJson.getBytes(StandardCharsets.UTF_8));
                    int rcCode = rc2.getResponseCode();
                    log.info("Requested reviewers; HTTP code: {}", rcCode);
                } else {
                    log.warn("PR created but could not find PR number in response: {}", resp.toString());
                }
            } catch (Exception ex) {
                log.error("Failed to request reviewers: {}", ex.getMessage(), ex);
            }
        }
        return resp.toString();
    }

    /**
     * Commits LLM-generated code fix to the repository.
     * @param repoPath Path to repo
     * @param filePath Path to file to update
     * @param codeFix  Code to write
     * @param commitMessage Commit message
     */
    public void commitLLMCodeFix(String repoPath, String filePath, String codeFix, String commitMessage) throws Exception {
        File file = new File(repoPath, filePath);
        java.nio.file.Files.write(file.toPath(), codeFix.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        runCommand(new File(repoPath), "git", "add", filePath);
        runCommand(new File(repoPath), "git", "commit", "-m", commitMessage);
        // Optionally push, create PR, etc.
    }

    private String joinReviewers(String csv) {
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<parts.length;i++) {
            if (i>0) sb.append(',');
            sb.append('"').append(parts[i].trim()).append('"');
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}
