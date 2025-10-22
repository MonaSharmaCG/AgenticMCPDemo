package com.cap.api.service.agent;

import java.util.ArrayList;
import java.util.List;

public class AgentChecklist {
    public static class Task {
        private String description;
        private boolean completed;
        private boolean userInputPending;

        public Task(String description) {
            this.description = description;
            this.completed = false;
            this.userInputPending = false;
        }

        public String getDescription() { return description; }
        public boolean isCompleted() { return completed; }
        public boolean isUserInputPending() { return userInputPending; }

        public void markCompleted() { this.completed = true; this.userInputPending = false; }
        public void markUserInputPending() { this.userInputPending = true; }
    }

    private List<Task> tasks = new ArrayList<>();

    public void addTask(String description) {
        tasks.add(new Task(description));
    }

    public List<Task> getTasks() { return tasks; }

    public void markTaskCompleted(int index) {
        if (index >= 0 && index < tasks.size()) {
            tasks.get(index).markCompleted();
        }
    }

    public void markTaskUserInputPending(int index) {
        if (index >= 0 && index < tasks.size()) {
            tasks.get(index).markUserInputPending();
        }
    }
}
