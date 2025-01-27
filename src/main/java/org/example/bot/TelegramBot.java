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

    // private TaskStorage taskStorage = new TaskStorage(); // список задач
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
            if (taskDescription.isEmpty()) {
                builder.append("Пожалуйста, введите описание задачи после команды /addtask.");
                return;
            }
            Task newTask = new Task(taskDescription);
            sqliteManager.addTask(newTask);
            builder.append("Задача добавлена: ").append(newTask.getDescription()).append("\n");

            if (sqliteManager.hasTasks()) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = sqliteManager.getAllTasks();
                for(int i = 0; i < tasks.size(); i++){
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            }
        });

        registerCommand("/viewtasks", "показать все задачи", (message, builder) -> {
            if(sqliteManager.hasTasks()) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = sqliteManager.getAllTasks();
                for (int i = 0; i < tasks.size(); i++){
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            } else {
                builder.append("У вас нет задач.");
            }
        });

        registerCommand("/done", "отметить задачу как выполненную", (message, builder) -> {
            String indexStr = message.substring("/done".length()).trim();
            try{
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index);
                if(taskId == -1){
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }

                if (sqliteManager.getTaskById(taskId).isCompleted())
                {
                    builder.append("Задача ").append(index).append(" и так выполнена").append("\n");
                }
                else {
                    sqliteManager.markTaskAsCompleted(taskId);
                    builder.append("Задача ").append(index).append(" отмечена как выполненная").append("\n");
                }

                if (sqliteManager.hasTasks()) {
                    builder.append("Текущие задачи:\n");
                    List<Task> tasks = sqliteManager.getAllTasks();
                    for(int i = 0; i < tasks.size(); i++){
                        builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                    }
                }

            } catch (NumberFormatException e) {
                if (message.equals("/done"))
                {
                    builder.append("Пожалуйтса, введите номер задачи после команды /done.");
                }
                else {
                    builder.append("Неверный формат индекса.");
                }
            }
        });

        registerCommand("/remove", "удалить задачу", (message, builder) -> {
            String indexStr = message.substring("/remove".length()).trim();
            try {
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index);
                if(taskId == -1){
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                sqliteManager.removeTask(taskId);
                builder.append("Задача ").append(index).append(" удалена").append("\n");
                if (sqliteManager.hasTasks()) {
                    builder.append("Текущие задачи:\n");
                    List<Task> tasks = sqliteManager.getAllTasks();
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
