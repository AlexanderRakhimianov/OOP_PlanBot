package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TelegramBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;

    public TelegramBot() {
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            this.botToken = properties.getProperty("bot.token");
            this.botUsername = properties.getProperty("bot.username");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что обновление содержит сообщение с текстом
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText(); // Получаем текст сообщения
            String chatId = message.getChatId().toString(); // Получаем идентификатор чата

            switch (text) {
                case "/start":
                    sendMsg(chatId, "Добро пожаловать! Используйте /help для списка команд.");
                    break;
                case "/authors":
                    sendMsg(chatId, "Бота разработали студенты матмеха УрФУ:\nРахимянов Александр - @rakhiimianov;\nВладимир Ершов - @normVovan4ik;\nНикита Витров - @militory.");
                    break;
                case "/help":
                    sendMsg(chatId, "Доступные команды:\n/authors - информация об авторах;\n/info - сведения о боте;\n/help - помощь по командам.");
                    break;
                case "/info":
                    sendMsg(chatId, "Инструмент для планирования и управления задачами:\nБот может анализировать ваш календарь, список дел и напоминать о важных событиях, помогать планировать встречи, ставить задачи и отслеживать прогресс.");
                    break;
                default:
                    sendMsg(chatId, "Неизвестная команда. Используйте /help для списка команд."); // Ответ на неизвестную команду
            }
        }
    }
    // Метод для отправки сообщений
    private void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
