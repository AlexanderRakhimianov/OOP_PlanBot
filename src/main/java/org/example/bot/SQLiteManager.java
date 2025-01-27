package org.example.bot;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class SQLiteManager {
    private static final String DATABASE_URL = "jdbc:sqlite:tasks.db";

    // Метод для создания подключения к БД
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " description TEXT NOT NULL,\n"
                + " completed INTEGER NOT NULL\n"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // метод для добавления задачи
    public void addTask(Task task) {
        String sql = "INSERT INTO tasks(description, completed) VALUES(?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, task.getDescription());
            pstmt.setInt(2, task.isCompleted() ? 1: 0);
            pstmt.executeUpdate();
            System.out.println("Задача добавлена");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // метод для получения всех задач из бд
    public List<Task> getAllTasks() {
        String sql = "SELECT id, description, completed FROM tasks";
        List<Task> taskList = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)){
            while(rs.next()){
                Task task = new Task(rs.getString("description"));
                task.setCompleted(rs.getInt("completed") == 1);
                taskList.add(task);
            }
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
        return taskList;
    }

    // метод для изменения статуса задачи
    public void markTaskAsCompleted(int index) {
        String sql = "UPDATE tasks SET completed = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, 1);
            pstmt.setInt(2, index);
            pstmt.executeUpdate();
            System.out.println("Задача " + index + " отмечена как выполненная");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    //Метод для удаления задачи
    public void removeTask(int index){
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, index);
            pstmt.executeUpdate();
            System.out.println("Задача " + index + " удалена");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    //метод для получения id задачи
    public int getTaskId(int index){
        String sql = "SELECT id FROM tasks LIMIT ?,1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, index - 1);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return rs.getInt("id");
            } else{
                return -1;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    // метод для получения задачи по id
    public Task getTaskById(int id) {
        String sql = "SELECT description, completed FROM tasks WHERE id = ?";
        Task task = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                task = new Task(rs.getString("description"));
                task.setCompleted(rs.getInt("completed") == 1);
            }
        }  catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return task;
    }


    public boolean hasTasks() {
        String sql = "SELECT COUNT(*) FROM tasks";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)){
            if(rs.next()){
                return rs.getInt(1) > 0;
            } else{
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
