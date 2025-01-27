package org.example.bot;

import java.util.ArrayList;
import java.util.List;

public class TaskStorage {
    private List<Task> tasks;

    public TaskStorage() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public List<Task> getAllTasks() {
        return tasks;
    }

    // Метод для изменения статуса задачи
    public void markTaskAsCompleted(int index) {
        if (index >= 0 && index < tasks.size()) {
            tasks.get(index).setCompleted(true);
        }
    }
    //Метод для удаления задачи
    public void removeTask(int index){
        if (index >= 0 && index < tasks.size()){
            tasks.remove(index);
        }
    }

    // Метод для проверки, есть ли задачи
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }
}
