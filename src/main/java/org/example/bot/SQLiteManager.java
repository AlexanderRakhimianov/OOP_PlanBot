package org.example.bot;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class SQLiteManager {
    private static String DATABASE_URL = "jdbc:sqlite:tasks.db";

    // Метод для создания подключения к БД
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " description TEXT NOT NULL,\n"
                + " completed INTEGER NOT NULL,\n"
                + " chatId INTEGER NOT NULL\n" // Добавили поле chatId
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public SQLiteManager(String databaseURL){
        DATABASE_URL = databaseURL;
        createTable();
        System.out.println("Таблица создана успешно.");
    }
    public SQLiteManager(){
    }


    // метод для добавления задачи
    public void addTask(Task task) {
        String sql = "INSERT INTO tasks(description, completed, chatId) VALUES(?,?,?)";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, task.getDescription());
            pstmt.setInt(2, task.isCompleted() ? 1: 0);
            pstmt.setLong(3, task.getChatId());
            pstmt.executeUpdate();
            System.out.println("Задача добавлена");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // метод для получения всех задач из бд
    public List<Task> getAllTasks(long chatId) {
        String sql = "SELECT id, description, completed FROM tasks WHERE chatId = ?"; // добавили фильтр по chatId
        List<Task> taskList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                Task task = new Task(rs.getString("description"), chatId);
                task.setCompleted(rs.getInt("completed") == 1);
                taskList.add(task);
            }
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
        return taskList;
    }

    // метод для изменения статуса задачи
    // метод для изменения статуса задачи
    public void markTaskAsCompleted(int index, long chatId) {
        String sql = "UPDATE tasks SET completed = ? WHERE id = ? AND chatId = ?";
        try(Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, 1);
            pstmt.setInt(2, index);
            pstmt.setLong(3, chatId);
            pstmt.executeUpdate();
            System.out.println("Задача " + index + " отмечена как выполненная");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    //Метод для удаления задачи
    //Метод для удаления задачи
    public void removeTask(int index, long chatId){
        String sql = "DELETE FROM tasks WHERE id = ? AND chatId = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, index);
            pstmt.setLong(2, chatId);
            pstmt.executeUpdate();
            System.out.println("Задача " + index + " удалена");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    //метод для получения id задачи
    public int getTaskId(int index, long chatId){
        String sql = "SELECT id FROM tasks WHERE chatId = ? LIMIT ?,1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setInt(2, index - 1);
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
    // метод для получения задачи по id
    public Task getTaskById(int id, long chatId) {
        String sql = "SELECT description, completed FROM tasks WHERE id = ? AND chatId = ?";
        Task task = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, id);
            pstmt.setLong(2, chatId);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                task = new Task(rs.getString("description"), chatId);
                task.setCompleted(rs.getInt("completed") == 1);
            }
        }  catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return task;
    }


    public boolean hasTasks(long chatId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE chatId = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();
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
