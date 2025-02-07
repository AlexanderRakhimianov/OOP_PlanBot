package org.example.bot;

import java.time.LocalDate;

public class Task {
    private String description;
    private boolean completed;
    private long chatId; // Добавили chatId пользователя
    private LocalDate date;

    public Task(String description, long chatId, LocalDate date) {
        this.description = description;
        this.completed = false;
        this.chatId = chatId;
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public LocalDate getDate(){
        return this.date;
    }

    @Override
    public String toString() {
        return (completed ? "[x] " : "[ ] ") + description;
    }
}