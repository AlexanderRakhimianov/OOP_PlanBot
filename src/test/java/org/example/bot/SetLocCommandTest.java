package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SetLocCommandTest {

    @Mock
    private SQLiteManager sqliteManager;
    @Mock
    private SQLiteLocationManager locationManager;
    @InjectMocks
    private TelegramBot bot;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bot.registerDefaultCommands(); // регистрация команд по умолчанию
    }

    @Test
    void testSetValidLocation() {
        long chatId = 123456789L;
        String validLocation = "Europe/Moscow";
        StringBuilder responseBuilder = new StringBuilder();

        // Вызываем команду /setloc
        bot.getCommandMap().get("/setloc").accept("/setloc " + validLocation, chatId, responseBuilder);


        // Проверяем, что метод setChatLocation был вызван с верными аргументами
        verify(locationManager, times(1)).setChatLocation(chatId, validLocation);

        // Проверяем, что бот отправил сообщение об успехе
        assertEquals("Местоположение сохранено.", responseBuilder.toString());
    }

    @Test
    void testSetInvalidLocation() {
        long chatId = 123456789L;
        String invalidLocation = "invalid/location";
        StringBuilder responseBuilder = new StringBuilder();

        // Вызываем команду /setloc
        bot.getCommandMap().get("/setloc").accept("/setloc " + invalidLocation, chatId, responseBuilder);


        // Проверяем, что метод setChatLocation не был вызван, так как местоположение некорректное
        verify(locationManager, never()).setChatLocation(anyLong(), anyString());

        // Проверяем, что бот отправил сообщение об ошибке
        assertEquals("Некорректное местоположение.\nПожалуйста, введите после команды /setloc регион и населённый пункт, например, Europe/Moscow.", responseBuilder.toString());
    }

    @Test
    void testSetLocationWithNoLocation(){
        long chatId = 123456789L;
        StringBuilder responseBuilder = new StringBuilder();

        // Вызываем команду /setloc без местоположения
        bot.getCommandMap().get("/setloc").accept("/setloc", chatId, responseBuilder);


        // Проверяем, что метод setChatLocation не был вызван, так как местоположение не указано
        verify(locationManager, never()).setChatLocation(anyLong(), anyString());

        // Проверяем, что бот отправил сообщение об ошибке, что местоположение некорректное
        assertEquals("Некорректное местоположение.\nПожалуйста, введите после команды /setloc регион и населённый пункт, например, Europe/Moscow.", responseBuilder.toString());
    }

    @Test
    void testSetLocationFromUpdate(){
        long chatId = 123456789L;
        String validLocation = "Europe/Moscow";
        String text = "/setloc " + validLocation;

        org.telegram.telegrambots.meta.api.objects.Message message = mock(org.telegram.telegrambots.meta.api.objects.Message.class);
        org.telegram.telegrambots.meta.api.objects.Chat chat = mock(org.telegram.telegrambots.meta.api.objects.Chat.class);
        org.telegram.telegrambots.meta.api.objects.Update update = mock(org.telegram.telegrambots.meta.api.objects.Update.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(chat.getId()).thenReturn(chatId);
        try {
            bot.onUpdateReceived(update);
        }
        catch(Exception exception){
            fail("Unexpected exception: " + exception.getMessage());
        }

        verify(locationManager, times(1)).setChatLocation(chatId, validLocation);
    }

    @Test
    void testSetLocationFromUpdateWithInvalidLocation(){
        long chatId = 123456789L;
        String invalidLocation = "invalid/location";
        String text = "/setloc " + invalidLocation;

        org.telegram.telegrambots.meta.api.objects.Message message = mock(org.telegram.telegrambots.meta.api.objects.Message.class);
        org.telegram.telegrambots.meta.api.objects.Chat chat = mock(org.telegram.telegrambots.meta.api.objects.Chat.class);
        org.telegram.telegrambots.meta.api.objects.Update update = mock(org.telegram.telegrambots.meta.api.objects.Update.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(chat.getId()).thenReturn(chatId);
        try {
            bot.onUpdateReceived(update);
        }
        catch(Exception exception){
            fail("Unexpected exception: " + exception.getMessage());
        }

        verify(locationManager, never()).setChatLocation(anyLong(), anyString());
    }
}