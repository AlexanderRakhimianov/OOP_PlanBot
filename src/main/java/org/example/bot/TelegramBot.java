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

import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, TriConsumer<String, Long, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private SQLiteManager sqliteManager = new SQLiteManager();


    public TelegramBot() {
        sqliteManager.createTable();
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


    // метод для регистрации команды
    public void registerCommand(String commandPrefix, String description, TriConsumer<String, Long, StringBuilder> action) {
        commandMap.put(commandPrefix, action);
        helpText.append(commandPrefix).append(" - ").append(description).append("\n");
    }


    private void registerDefaultCommands() {
        registerCommand("/start", "начало работы с ботом", (message, chatId, builder) -> {
            builder.append("Добро пожаловать! Используйте /help для списка команд.");
        });

        registerCommand("/info", "информация о боте", (message, chatId, builder) -> {
            builder.append("Инструмент для планирования и управления задачами:\n" +
                    "Бот может анализировать ваш календарь, список дел и напоминать о важных событиях, помогать планировать встречи, ставить задачи и отслеживать прогресс.");
        });

        registerCommand("/authors", "информация об авторах", (message, chatId, builder) -> {
            builder.append("Бота разработали студенты матмеха УрФУ:\n" +
                    "Рахимянов Александр - @rakhiimianov\n" +
                    "Владимир Ершов - @normVovan4ik\n" +
                    "Никита Витров - @militory");
        });

        registerCommand("/help", "показать список команд", (message, chatId, builder) -> {
            builder.append(helpText);
        });

        registerCommand("/addtask", "добавить задачу", (message, chatId, builder) -> {
            String taskDescription = message.substring("/addtask".length()).trim();
            if (taskDescription.isEmpty()) {
                builder.append("Пожалуйста, введите описание задачи после команды /addtask.");
                return;
            }
            Task newTask = new Task(taskDescription, chatId);
            sqliteManager.addTask(newTask);
            builder.append("Задача добавлена: ").append(newTask.getDescription()).append("\n");

            if (sqliteManager.hasTasks(chatId)) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = sqliteManager.getAllTasks(chatId);
                for(int i = 0; i < tasks.size(); i++){
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            }
        });

        registerCommand("/alltasks", "показать все задачи", (message, chatId, builder) -> {
            if(sqliteManager.hasTasks(chatId)) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = sqliteManager.getAllTasks(chatId);
                for (int i = 0; i < tasks.size(); i++){
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            } else {
                builder.append("У вас нет задач.");
            }
        });

        registerCommand("/done", "отметить задачу как выполненную", (message, chatId, builder) -> {
            String indexStr = message.substring("/done".length()).trim();
            try{
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index, chatId);
                if(taskId == -1){
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                if (sqliteManager.getTaskById(taskId, chatId).isCompleted())
                {
                    builder.append("Задача ").append(index).append(" и так отмечена как выполненная").append("\n");
                }
                else {
                    sqliteManager.markTaskAsCompleted(taskId, chatId);
                    builder.append("Задача ").append(index).append(" отмечена как выполненная").append("\n");
                }
                if (sqliteManager.hasTasks(chatId)) {
                    builder.append("Текущие задачи:\n");
                    List<Task> tasks = sqliteManager.getAllTasks(chatId);
                    for(int i = 0; i < tasks.size(); i++){
                        builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                    }
                }
            } catch (NumberFormatException e) {
                if (message.equals("/done")) {
                    builder.append("Пожалуйтса, введите номер задачи после команды /done.");
                } else {
                    builder.append("Неверный формат индекса.");
                }
            }
        });

        registerCommand("/remove", "удалить задачу", (message, chatId, builder) -> {
            String indexStr = message.substring("/remove".length()).trim();
            try {
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index, chatId);
                if(taskId == -1){
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                sqliteManager.removeTask(taskId, chatId);
                builder.append("Задача ").append(index).append(" удалена").append("\n");
                if (sqliteManager.hasTasks(chatId)) {
                    builder.append("Текущие задачи:\n");
                    List<Task> tasks = sqliteManager.getAllTasks(chatId);
                    for(int i = 0; i < tasks.size(); i++){
                        builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                    }
                }
            } catch (NumberFormatException e) {
                if (message.equals("/remove"))
                {
                    builder.append("Пожалуйтса, введите номер задачи после команды /remove.");
                }
                else {
                    builder.append("Неверный формат индекса.");
                }
            }
        });
        /*
        registerCommand("/show ", "показать описание задачи", (message, chatId, builder) -> {
            String indexStr = message.substring("/show ".length()).trim();
            try{
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index, chatId);
                if(taskId == -1){
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                Task task = sqliteManager.getTaskById(taskId, chatId);
                if(task != null){
                    builder.append("Задача ").append(index).append(": ").append(task.getDescription());
                } else{
                    builder.append("Задача не найдена.");
                }
            } catch (NumberFormatException e){
                builder.append("Неверный формат индекса.");
            }
        });
         */
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId_l = update.getMessage().getChatId();
            StringBuilder responseBuilder = new StringBuilder();

            boolean commandFound = false;
            for (Map.Entry<String, TriConsumer<String, Long, StringBuilder>> entry : commandMap.entrySet()) {
                String commandPrefix = entry.getKey();
                if (message.startsWith(commandPrefix)) {
                    entry.getValue().accept(message, chatId_l, responseBuilder); // Передаем chatId
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
    // Long chatId = chatId_l;

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
    public Map<String, TriConsumer<String, Long, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
