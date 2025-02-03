package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import org.mockito.Spy;
import static org.mockito.Mockito.verify;

public class ReplyKeyboardTest {

    private TelegramBot bot;

    @Spy
    private Map<String, TriConsumer<String, Long, StringBuilder>> commandMap = new HashMap<>();


    @BeforeEach
    public void setUp() {
        SQLiteManager mockSQLiteManager = Mockito.mock(SQLiteManager.class);
        SQLiteLocationManager mockSQLiteLocationManager = Mockito.mock(SQLiteLocationManager.class);
        bot = new TelegramBot(mockSQLiteManager, mockSQLiteLocationManager);
        //Мокаем все TriConsumer'ы для кнопок, чтобы можно было вызывать на них verify()
        TriConsumer<String, Long, StringBuilder> mockAllTasksConsumer = Mockito.mock(TriConsumer.class);
        TriConsumer<String, Long, StringBuilder> mockDateTasksConsumer = Mockito.mock(TriConsumer.class);
        TriConsumer<String, Long, StringBuilder> mockHelpConsumer = Mockito.mock(TriConsumer.class);
        TriConsumer<String, Long, StringBuilder> mockDoneConsumer = Mockito.mock(TriConsumer.class);


        commandMap.put("/alltasks", mockAllTasksConsumer);
        commandMap.put("/datetasks", mockDateTasksConsumer);
        commandMap.put("/help", mockHelpConsumer);
        commandMap.put("/done", mockDoneConsumer);

        //Заменяем реальную карту комманд на мокнутую
        bot.getCommandMap().clear();
        bot.getCommandMap().putAll(commandMap);

    }


    @Test
    public void testAllTasksButton() {
        Update update = createUpdate("Все задачи");
        bot.onUpdateReceived(update);
        verify(commandMap.get("/alltasks"), times(1)).accept(anyString(), anyLong(), any(StringBuilder.class));
    }

    @Test
    public void testDateTasksButton() {
        Update update = createUpdate("Задачи на сегодня");
        bot.onUpdateReceived(update);
        verify(commandMap.get("/datetasks"), times(1)).accept(anyString(), anyLong(), any(StringBuilder.class));
    }

    @Test
    public void testHelpButton() {
        Update update = createUpdate("Помощь");
        bot.onUpdateReceived(update);
        verify(commandMap.get("/help"), times(1)).accept(anyString(), anyLong(), any(StringBuilder.class));
    }

    @Test
    public void testUnknownCommand() {
        Update update = createUpdate("some unknown command");
        bot.onUpdateReceived(update);
        verify(commandMap.get("/alltasks"), never()).accept(anyString(), anyLong(), any(StringBuilder.class));
        verify(commandMap.get("/datetasks"), never()).accept(anyString(), anyLong(), any(StringBuilder.class));
        verify(commandMap.get("/help"), never()).accept(anyString(), anyLong(), any(StringBuilder.class));
    }

    private Update createUpdate(String text) {
        Update update = new Update();
        Message message = new Message();
        message.setText(text);
        Chat chat = new Chat();
        chat.setId(12345L);
        message.setChat(chat);
        update.setMessage(message);
        return update;
    }
}