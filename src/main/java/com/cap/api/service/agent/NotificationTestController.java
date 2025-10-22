package com.cap.api.service.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationTestController {
    @Autowired
    private NotificationAgent notificationAgent;

    @GetMapping("/test-email-notification")
    public String triggerEmail() {
        notificationAgent.notifyDLs("Live test: This is a test notification from AgenticMCPDemo.");
        return "Email notification triggered.";
    }
}
