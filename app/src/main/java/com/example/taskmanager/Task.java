package com.example.taskmanager;

public class Task {
    private String taskId;
    private String taskName;
    private long dueDateTime;

    public Task(String taskId, String taskName, long dueDateTime) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.dueDateTime = dueDateTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public long getDueDateTime() {
        return dueDateTime;
    }

    public boolean isDueWithinHour() {
        long currentTime = System.currentTimeMillis();
        return dueDateTime - currentTime <= 3600000;
    }
}