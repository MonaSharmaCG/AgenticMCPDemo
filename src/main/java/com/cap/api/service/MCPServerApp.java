
package com.cap.api.service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import com.cap.api.service.agent.DefectProcessingAgent;

@SpringBootApplication
public class MCPServerApp {
    @Autowired
    private DefectProcessingAgent defectProcessingAgent;

    public static void main(String[] args) {
        SpringApplication.run(MCPServerApp.class, args);
    }

    @PostConstruct
    public void startJiraPolling() {
        // Poll JIRA every 2 minutes using the agent's logic
        new Thread(() -> {
            while (true) {
                try {
                    defectProcessingAgent.processDefects();
                    Thread.sleep(120000); // 2 minutes
                } catch (Exception e) {
                    System.out.println("Error in JIRA polling: " + e.getMessage());
                }
            }
        }).start();
    }
}
