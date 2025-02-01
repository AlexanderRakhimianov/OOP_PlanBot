package org.example.bot;

import java.sql.*;

public class SQLiteLocationManager {
    private static String DATABASE_URL = "jdbc:sqlite:tasks.db";

    public SQLiteLocationManager(String databaseURL) {
        DATABASE_URL = databaseURL;
        createChatLocationsTable();
        System.out.println("Таблица chat_locations создана успешно.");
    }
    public SQLiteLocationManager() {
    }


    // Метод для создания подключения к БД
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    // Метод для создания таблицы для chat_locations
    public void createChatLocationsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_locations (\n"
                + " chat_id INTEGER PRIMARY KEY,\n"
                + " location TEXT\n"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Метод для сохранения соответствия chatId и location
    public void setChatLocation(long chatId, String location) {
        String sql = "INSERT OR REPLACE INTO chat_locations(chat_id, location) VALUES(?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, location);
            pstmt.executeUpdate();
            System.out.println("Location for chat " + chatId + " was set successfully");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Метод для получения location по chatId
    public String getChatLocation(long chatId) {
        String sql = "SELECT location FROM chat_locations WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("location");
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    //метод для удаления location по chat_id
    public void removeLocation(long chatId) {
        String sql = "DELETE FROM chat_locations WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.executeUpdate();
            System.out.println("Location for chat " + chatId + " was removed");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
