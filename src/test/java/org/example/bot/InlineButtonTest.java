package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class InlineButtonTest {

    @Spy
    private TelegramBot bot;
    @Mock
    private SQLiteManager sqliteManager;
    @Mock
    private Update update;
    @Mock
    private CallbackQuery callbackQuery;
    @Mock
    private Message message;
    @Mock
    private EditMessageReplyMarkup editMessageReplyMarkup;

    private Map<String, TriConsumer<String, Long, StringBuilder>> commandMap;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        commandMap = new HashMap<>();
        // Имитируем обработчики команд /done и /remove (для упрощения)
        commandMap.put("/done", (msg, chatId, builder) -> builder.append("Done!"));
        commandMap.put("/remove", (msg, chatId, builder) -> builder.append("Removed!"));
        when(bot.getCommandMap()).thenReturn(commandMap);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1447190022L);
        when(message.getMessageId()).thenReturn(1);

    }


    @Test
    void testDoneButton() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("done_1");
        doNothing().when(bot).editInlineKeyboard(anyLong(), anyInt(), anyInt());
        doNothing().when(bot).sendMsg(anyString(), anyString());

        bot.onUpdateReceived(update);

        verify(bot).editInlineKeyboard(1447190022L,1, 1);

        verify(bot, times(1)).sendMsg(eq("1447190022"), anyString());
    }

    @Test
    void testRemoveButton() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("remove_2");
        doNothing().when(bot).deleteInlineKeyboard(anyLong(), anyInt());
        doNothing().when(bot).sendMsg(anyString(), anyString());

        bot.onUpdateReceived(update);

        verify(bot).deleteInlineKeyboard(1447190022L,1);
        verify(bot, times(1)).sendMsg(eq("1447190022"), anyString());
    }

    @Test
    void testOkButton() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("ok_3");
        doNothing().when(bot).deleteInlineKeyboard(anyLong(), anyInt());

        bot.onUpdateReceived(update);

        verify(bot).deleteInlineKeyboard(1447190022L,1);
        verify(bot, never()).sendMsg(anyString(), anyString());
    }

    @Test
    void testInvalidCallbackData() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("invalid_data");
        doNothing().when(bot).deleteInlineKeyboard(anyLong(), anyInt());

        bot.onUpdateReceived(update);

        verify(bot, never()).sendMsg(anyString(), anyString());
    }
}
