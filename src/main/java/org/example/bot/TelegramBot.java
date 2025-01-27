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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private TaskStorage taskStorage = new TaskStorage(); // список задач

    public TelegramBot() {
        loadConfig();
        registerDefaultCommands(); // тут регистрация команд по умолчанию
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


    /*
    // метод для регистрации команды
    public void registerCommand(String command, String description, BiConsumer<String, StringBuilder> action) {
        commandMap.put(command, action);
        helpText.append(command).append(" - ").append(description).append("\n");
    }
    */


    // метод для регистрации команды
    public void registerCommand(String commandPrefix, String description, BiConsumer<String, StringBuilder> action) {
        commandMap.put(commandPrefix, action);
        helpText.append(commandPrefix).append(" - ").append(description).append("\n");
    }

    private void registerDefaultCommands() {
        registerCommand("/start", "начало работы с ботом", (message, builder) -> {
            builder.append("Добро пожаловать! Используйте /help для списка команд.");
        });

        registerCommand("/authors", "информация об авторах", (message, builder) -> {
            builder.append("Бота разработали студенты матмеха УрФУ:\n" +
                    "Рахимянов Александр - @rakhiimianov\n" +
                    "Владимир Ершов - @normVovan4ik\n" +
                    "Никита Витров - @militory");
        });

        registerCommand("/help", "список команд", (message, builder) -> {
            builder.append("Доступные команды:\n").append(helpText.toString());
        });

        registerCommand("/info", "информация о боте", (message, builder) -> {
            builder.append("Инструмент для планирования и управления задачами:\n" +
                    "Бот может анализировать ваш календарь, список дел и напоминать о важных событиях, помогать планировать встречи, ставить задачи и отслеживать прогресс.");
        });

        registerCommand("/addtask", "добавить задачу", (message, builder) -> {
            String taskDescription = message.substring("/addtask".length()).trim();
            System.out.println(taskDescription);

            if (taskDescription.isEmpty()) {
                builder.append("Пожалуйста, введите описание задачи после команды /addtask.");
                return;
            }
            Task newTask = new Task(taskDescription);
            taskStorage.addTask(newTask);
            builder.append("Задача добавлена: ").append(newTask.getDescription()).append("\n");

            if (taskStorage.hasTasks()) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = taskStorage.getAllTasks();
                for (int i = 0; i < tasks.size(); i++) {
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            }

        });
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }


    /*
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            String chatId = message.getChatId().toString();

            // Выполняем команду
            BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(text, (id, builder) -> {
                builder.append("Неизвестная команда. Используйте /help для списка команд.");
            });

            StringBuilder responseBuilder = new StringBuilder();
            action.accept(chatId, responseBuilder);

            // Отправка сообщения пользователю
            sendMsg(chatId, responseBuilder.toString());
        }
    }
     */


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId_l = update.getMessage().getChatId();
            StringBuilder responseBuilder = new StringBuilder();

            boolean commandFound = false;
            for(Map.Entry<String, BiConsumer<String, StringBuilder>> entry : commandMap.entrySet()){
                String commandPrefix = entry.getKey();
                if(message.startsWith(commandPrefix)){
                    entry.getValue().accept(message, responseBuilder);
                    commandFound = true;
                    break;
                }
            }

            if (!commandFound) {
                responseBuilder.append("Неизвестная команда, используйте /help для просмотра списка команд");
            }
            Long chatId = chatId_l;
            sendMsg(chatId.toString(), responseBuilder.toString());
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
    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
