
package com.cap.api.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MCPServerApp {
    public static void main(String[] args) {
        SpringApplication.run(MCPServerApp.class, args);
    }
}
