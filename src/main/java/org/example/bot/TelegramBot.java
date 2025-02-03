package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;

import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.DateTimeException;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;


public class TelegramBot extends TelegramLongPollingBot {
    private String botUsername;
    private String botToken;
    private InlineKeyboardMarkup inlineKeyboard;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, TriConsumer<String, Long, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private SQLiteManager sqliteManager;
    private SQLiteLocationManager locationManager;


    public TelegramBot(SQLiteManager sqliteManager, SQLiteLocationManager sqliteLocationManager) {
        this.sqliteManager = sqliteManager;
        this.sqliteManager.createTable();
        this.locationManager = sqliteLocationManager;
        this.locationManager.createChatLocationsTable();
        loadConfig();
        registerDefaultCommands(); // тут регистрация команд по умолчанию
    }

    public TelegramBot() {
        this(new SQLiteManager(), new SQLiteLocationManager());
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

    private void printAllTasks(Long chatId, StringBuilder builder){
        if (sqliteManager.hasTasks(chatId)) {
            builder.append("Текущие задачи:\n");
            List<Task> tasks = sqliteManager.getAllTasks(chatId);
            for(int i = 0; i < tasks.size(); i++){
                builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
            }
        } else {
            builder.append("У вас нет задач.");
        }
    }

    private LocalDate getCurrentDate(Long chatId){
        String location = locationManager.getChatLocation(chatId);
        LocalDate date;
        if (location != null) {
            ZoneId zoneId = ZoneId.of(location);
            ZonedDateTime zonedDateTime = Instant.now().atZone(zoneId);
            String strDate = zonedDateTime.toString().substring(0, 10);
            date = LocalDate.parse(strDate);
        } else {
            date = LocalDate.now();
        }
        return date;
    }

    public void registerDefaultCommands() {
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

        registerCommand("/setloc", "установить (изменить) местоположение\n(по умолчанию Asia/Yekaterinburg)", (message, chatId, builder) -> {
            String location = message.substring("/setloc".length()).trim();
            try {
                ZoneId.of(location); // если местоположение некорректное, то выбрасывается исключение
                locationManager.setChatLocation(chatId, location);
                builder.append("Местоположение сохранено.");
            }
            catch (DateTimeException exception) {
                builder.append("Некорректное местоположение.\nПожалуйста, введите после команды /setloc регион и населённый пункт, например, Europe/Moscow.");
            }
        });

        registerCommand("/addtask", "добавить задачу,\nможно указать дату (дэдлайн) в любой части задачи в формате YYYY-MM-DD", (message, chatId, builder) -> {
            LocalDate date;
            try {
                int index = message.indexOf("20");
                String strDate = message.substring(index, index + 10);
                date = LocalDate.parse(strDate);
            }
            catch (StringIndexOutOfBoundsException | DateTimeParseException exception) {
                date = getCurrentDate(chatId);
            }
            String taskDescription = message.substring("/addtask".length()).trim();
            if (taskDescription.isEmpty()) {
                builder.append("Пожалуйста, введите описание задачи после команды /addtask.");
                return;
            }
            Task newTask = new Task(taskDescription, chatId, date);
            sqliteManager.addTask(newTask);
            builder.append("Задача добавлена: ").append(newTask.getDescription()).append("\n");
            printAllTasks(chatId, builder);
        });

        registerCommand("/alltasks", "показать все задачи", (message, chatId, builder) -> {
            printAllTasks(chatId, builder);
        });

        registerCommand("/datetasks", "показать задачи на определённый день или несколько дней (формат: с YYYY-MM-DD по YYYY-MM-DD)", (message, chatId, builder) -> {
            // на одну дату
            if (!message.contains("с") || !message.contains("по")) {
                LocalDate date;
                String strDay;
                try {
                    int index = message.indexOf("20");
                    strDay = message.substring(index, index + 10);
                    date = LocalDate.parse(strDay);
                }
                catch (StringIndexOutOfBoundsException | DateTimeParseException exception) {
                    date = getCurrentDate(chatId);
                    strDay = "сегодня";
                }
                List<Task> allTasks = sqliteManager.getAllTasks(chatId);

                builder.append("Задачи на ").append(strDay).append(":\n");
                int num = 1;
                for (Task task : allTasks) {
                    if (task.getDate().equals(date)) {
                        builder.append(num).append(". ").append(task).append("\n");
                        num++;
                    }
                } if (num == 1) {
                    builder.append("< задач нет >");
                }
            }
            // на диапазон дат
            else {
                try {
                    int index = message.indexOf("20");
                    String strDate1 = message.substring(index, index + 10);
                    LocalDate date1 = LocalDate.parse(strDate1);

                    index = message.lastIndexOf("20");
                    String strDate2 = message.substring(index, index + 10);
                    LocalDate date2 = LocalDate.parse(strDate2);

                    List<Task> allTasks = sqliteManager.getAllTasks(chatId);

                    builder.append("Задачи на период с ").append(strDate1).append(" до ").append(strDate2).append(":\n");
                    int num = 1;
                    LocalDate taskDate;
                    for (Task task : allTasks) {
                        taskDate = task.getDate();
                        if ((taskDate.isAfter(date1) || taskDate.equals(date1)) && (taskDate.isBefore(date2) || taskDate.equals(date2))) {
                            builder.append(num).append(". ").append(task).append("\n");
                            num++;
                        }
                    } if (num == 1) {
                        builder.append("< задач нет >");
                    }
                }
                catch(StringIndexOutOfBoundsException | DateTimeParseException exception) {
                    builder.append("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.");
                }
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
                sqliteManager.markTaskAsCompleted(taskId, chatId);
                builder.append("Задача ").append(index).append(" отмечена как выполненная").append("\n");
                printAllTasks(chatId, builder);
            } catch (NumberFormatException e) {
                builder.append("Неверный формат индекса.");
            }
        });

        registerCommand("/remove", "удалить задачу", (message, chatId, builder) -> {
            String indexStr = message.substring("/remove".length()).trim();
            try {
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index, chatId);
                if (taskId == -1) {
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                sqliteManager.removeTask(taskId, chatId);
                builder.append("Задача ").append(index).append(" удалена").append("\n");
                printAllTasks(chatId, builder);
            } catch (NumberFormatException e) {
                builder.append("Неверный формат индекса.");
            }
        });

        registerCommand("/removeall", "удалить все задачи", (message, chatId, builder) -> {
            final int index = 1;
            int taskId = sqliteManager.getTaskId(index, chatId);
            while (taskId != -1) {
                sqliteManager.removeTask(taskId, chatId);
                taskId = sqliteManager.getTaskId(index, chatId);
            }
            builder.append("Все задачи удалены.");
        });

        registerCommand("/edit", "отредактировать задачу", (message, chatId, builder) -> {
            String[] parsMsg = message.split(" ");
            String[] clParsMsg = new String[parsMsg.length];
            int j = 0;
            for (int i = 0; i < parsMsg.length; i++) {
                if (!parsMsg[i].isEmpty()) {
                    clParsMsg[j] = parsMsg[i];
                    j++;
                }
            }
            if (clParsMsg.length < 3) {
                builder.append("Пожалуйста, укажите после команды /edit номер задачи и новое описание через пробел.");
                return;
            }
            int index = Integer.parseInt(clParsMsg[1]);
            int taskId = sqliteManager.getTaskId(index, chatId);
            if(taskId == -1) {
                builder.append("Задачи с таким индексом не существует.");
                return;
            }
            StringBuilder newDesc = new StringBuilder();
            for(int i = 2; i < j; i++) {
                newDesc.append(clParsMsg[i]);
                if (i != j - 1) {
                    newDesc.append(" ");
                }
            }
            LocalDate date;
            try {
                int idx = newDesc.indexOf("2");
                String strDate = newDesc.substring(idx, idx + 10);
                date = LocalDate.parse(strDate);
            }
            catch (StringIndexOutOfBoundsException | DateTimeParseException exception) {
                date = LocalDate.parse("2010-01-01");
            }
            sqliteManager.editTask(taskId, chatId, newDesc.toString(), date);
            builder.append("Задача ").append(index).append(" обновлена").append("\n");
            printAllTasks(chatId, builder);
        });

        registerCommand("/show", "показать описание задачи", (message, chatId, builder) -> {
            String indexStr = message.substring("/show".length()).trim();
            try {
                int index = Integer.parseInt(indexStr);
                int taskId = sqliteManager.getTaskId(index, chatId);
                if (taskId == -1) {
                    builder.append("Задачи с таким индексом не существует.");
                    return;
                }
                Task task = sqliteManager.getTaskById(taskId, chatId);
                if (task != null) {
                    String text = "Задача " + index + ": " + task;
                    sendMsgWithInlineKeyboard(String.valueOf(chatId), text, index, task.isCompleted());
                } else {
                    builder.append("Задача не найдена.");
                }
            } catch (NumberFormatException e) {
                builder.append("Неверный формат индекса.");
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
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            StringBuilder responseBuilder = new StringBuilder();

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboard = new ArrayList<>();
            KeyboardRow row = new KeyboardRow();

            KeyboardButton allTasksButton = new KeyboardButton();
            allTasksButton.setText("Все задачи");

            KeyboardButton dateTasksButton = new KeyboardButton();
            dateTasksButton.setText("Задачи на сегодня");

            KeyboardButton helpButton = new KeyboardButton();
            helpButton.setText("Помощь");

            row.add(allTasksButton);
            row.add(dateTasksButton);
            row.add(helpButton);
            keyboard.add(row);
            keyboardMarkup.setKeyboard(keyboard);
            keyboardMarkup.setResizeKeyboard(true);

            boolean commandFound = false;
            if (message.equals("Все задачи")) {
                commandMap.get("/alltasks").accept(message, chatId, responseBuilder);
                commandFound = true;
            } else if (message.equals("Задачи на сегодня")) {
                commandMap.get("/datetasks").accept(message, chatId, responseBuilder);
                commandFound = true;
            } else if (message.equals("Помощь")) {
                commandMap.get("/help").accept(message, chatId, responseBuilder);
                commandFound = true;
            } else {
                for (Map.Entry<String, TriConsumer<String, Long, StringBuilder>> entry : commandMap.entrySet()) {
                    String commandPrefix = entry.getKey();
                    if (message.startsWith(commandPrefix)) {
                        entry.getValue().accept(message, chatId, responseBuilder);
                        commandFound = true;
                        break;
                    }
                }
            }

            if (!commandFound) {
                responseBuilder.append("Неизвестная команда, используйте /help для просмотра списка команд");
            }
            sendMsg(String.valueOf(chatId), responseBuilder.toString(), keyboardMarkup);
        }
    }


    // Метод для отправки сообщений (с клавиатурой)
    public void sendMsg(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        if (text.isEmpty()) {return;}
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboardMarkup != null) {
            message.setReplyMarkup(keyboardMarkup);
        }
        try {
            execute(message); // Отправляем сообщение
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    } // Отправка сообщений без аргумента клавиатуры (перегрузка)
    public void sendMsg(String chatId, String text) {
        sendMsg(chatId, text, null);
    }

    public Map<String, TriConsumer<String, Long, StringBuilder>> getCommandMap() {
        return commandMap;
    }

    private void sendMsgWithInlineKeyboard(String chatId, String text, int taskIndex, boolean isComp) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        this.inlineKeyboard = markupInline;
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        // Кнопка OK
        InlineKeyboardButton okButton = new InlineKeyboardButton();
        okButton.setText("OK");
        okButton.setCallbackData("ok_" + taskIndex);
        rowInline.add(okButton);

        // Кнопка "Выполнить"
        InlineKeyboardButton doneButton = new InlineKeyboardButton();
        doneButton.setText("Выполнить");
        doneButton.setCallbackData("done_" + taskIndex);
        if (!isComp) { rowInline.add(doneButton); }

        // Кнопка "Удалить"
        InlineKeyboardButton removeButton = new InlineKeyboardButton();
        removeButton.setText("Удалить");
        removeButton.setCallbackData("remove_" + taskIndex);
        rowInline.add(removeButton);

        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        String[] parts = callbackData.split("_");
        String action = parts[0];
        int taskIndex;
        try {
            taskIndex = Integer.parseInt(parts[1]);
        }
        catch(NumberFormatException exception){return;}

        switch (action) {
            case "ok":
                deleteInlineKeyboard(chatId, messageId);
                break;
            case "done", "remove":
                StringBuilder responseBuilder = new StringBuilder();
                String command = "/" + action;
                String message = command + " " + taskIndex;
                commandMap.get(command).accept(message, chatId, responseBuilder);
                sendMsg(String.valueOf(chatId), responseBuilder.toString());
                if (action.equals("remove")) {
                    deleteInlineKeyboard(chatId, messageId);
                }
                else {
                    editInlineKeyboard(chatId, messageId, 1);
                }
                break;
        }
    }

    public void deleteInlineKeyboard(long chatId, int messageId) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(String.valueOf(chatId));
        editMessageReplyMarkup.setMessageId(messageId);
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void editInlineKeyboard(long chatId, int messageId, int buttonToRemoveIndex) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(String.valueOf(chatId));
        editMessageReplyMarkup.setMessageId(messageId);

        // Получаем текущую клавиатуру, если она есть
        InlineKeyboardMarkup oldKeyboard = this.inlineKeyboard;

        if (oldKeyboard != null && oldKeyboard.getKeyboard() != null) {
            List<List<InlineKeyboardButton>> keyboardRows = oldKeyboard.getKeyboard();
            List<List<InlineKeyboardButton>> newKeyboardRows = new ArrayList<>();

            for (int i=0; i< keyboardRows.size(); i++) {
                List<InlineKeyboardButton> row = keyboardRows.get(i);
                List<InlineKeyboardButton> newRow = new ArrayList<>();
                for (int j=0; j<row.size(); j++){
                    if (i == 0 && j != buttonToRemoveIndex){
                        newRow.add(row.get(j));
                    }
                    if(i !=0)
                        newRow.add(row.get(j));

                }
                if(!newRow.isEmpty())
                    newKeyboardRows.add(newRow);
            }
            InlineKeyboardMarkup newKeyboard = new InlineKeyboardMarkup();
            newKeyboard.setKeyboard(newKeyboardRows);
            editMessageReplyMarkup.setReplyMarkup(newKeyboard);

        } else {
            editMessageReplyMarkup.setReplyMarkup(new InlineKeyboardMarkup()); // Удаляем все кнопки
        }
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
