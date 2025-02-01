package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class EditTaskCommandTest {

    @Mock
    private SQLiteManager sqliteManager;

    private TriConsumer<String, Long, StringBuilder> editCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        editCommand = getEditTaskCommand(sqliteManager);
    }
    private TriConsumer<String, Long, StringBuilder> getEditTaskCommand(SQLiteManager sqliteManager){
        return (message, chatId, builder) -> {
            String[] parsMsg = message.split(" ");
            String[] clParsMsg = new String[parsMsg.length];
            int j = 0;
            for (int i = 0; i < parsMsg.length; i++)
            {
                if (!parsMsg[i].isEmpty())
                {
                    clParsMsg[j] = parsMsg[i];
                    j++;
                }
            }
            if (clParsMsg.length < 3)
            {
                builder.append("Пожалуйста, укажите после команды /edit номер задачи и новое описание через пробел");
                return;
            }
            int index = Integer.parseInt(clParsMsg[1]);
            int taskId = sqliteManager.getTaskId(index, chatId);
            if(taskId == -1){
                builder.append("Задачи с таким индексом не существует.");
                return;
            }
            StringBuilder newDesc = new StringBuilder();
            for(int i = 2; i < j; i++)
            {
                newDesc.append(clParsMsg[i]);
                if (i != j - 1)
                {
                    newDesc.append(" ");
                }
            }
            LocalDate date;
            try {
                int idx = newDesc.indexOf("2");
                String strDate = newDesc.substring(idx, idx + 10);
                date = LocalDate.parse(strDate);
            }
            catch (StringIndexOutOfBoundsException | DateTimeParseException exception)
            {
                date = LocalDate.parse("2010-01-01");
            }


            sqliteManager.editTask(taskId, chatId, newDesc.toString(), date);
            builder.append("Задача ").append(index).append(" обновлена").append("\n");
            if (sqliteManager.hasTasks(chatId)) {
                builder.append("Текущие задачи:\n");
                List<Task> tasks = sqliteManager.getAllTasks(chatId);
                for(int i = 0; i < tasks.size(); i++){
                    builder.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
                }
            }
        };
    }

    @Test
    void testSuccessfulEditTask() {
        long chatId = 12345;
        int taskIndex = 1;
        int taskId = 10;
        String newMessage = "/edit 1 Новое описание 2024-07-20";
        String expectedResponse = "Задача 1 обновлена\n";


        when(sqliteManager.getTaskId(taskIndex, chatId)).thenReturn(taskId);
        when(sqliteManager.hasTasks(chatId)).thenReturn(false);
        StringBuilder builder = new StringBuilder();
        editCommand.accept(newMessage, chatId, builder);


        verify(sqliteManager).editTask(taskId, chatId, "Новое описание 2024-07-20", LocalDate.parse("2024-07-20"));
        assertEquals(expectedResponse, builder.toString());
    }
    @Test
    void testInvalidCommandFormat() {
        long chatId = 12345;
        String newMessage = "/edit 1";
        String expectedResponse = "Пожалуйста, укажите после команды /edit номер задачи и новое описание через пробел";

        StringBuilder builder = new StringBuilder();
        editCommand.accept(newMessage, chatId, builder);
        assertEquals(expectedResponse, builder.toString());
    }
    @Test
    void testTaskNotFound() {
        long chatId = 12345;
        int taskIndex = 1;
        String newMessage = "/edit 1 Новое описание";
        String expectedResponse = "Задачи с таким индексом не существует.";

        when(sqliteManager.getTaskId(taskIndex, chatId)).thenReturn(-1);
        StringBuilder builder = new StringBuilder();
        editCommand.accept(newMessage, chatId, builder);
        assertEquals(expectedResponse, builder.toString());
    }

    @Test
    void testEditTaskWithDefaultDate() {
        long chatId = 12345;
        int taskIndex = 1;
        int taskId = 10;
        String newMessage = "/edit 1 Новое описание без даты";
        String expectedResponse = "Задача 1 обновлена\n";


        when(sqliteManager.getTaskId(taskIndex, chatId)).thenReturn(taskId);
        when(sqliteManager.hasTasks(chatId)).thenReturn(false);
        StringBuilder builder = new StringBuilder();
        editCommand.accept(newMessage, chatId, builder);


        verify(sqliteManager).editTask(taskId, chatId, "Новое описание без даты", LocalDate.parse("2010-01-01"));
        assertEquals(expectedResponse, builder.toString());
    }

    @Test
    void testEditTaskWithDate() {
        long chatId = 12345;
        int taskIndex = 1;
        int taskId = 10;
        String newMessage = "/edit 1 Новое описание с датой 2024-10-20";
        String expectedResponse = "Задача 1 обновлена\n";

        when(sqliteManager.getTaskId(taskIndex, chatId)).thenReturn(taskId);
        when(sqliteManager.hasTasks(chatId)).thenReturn(false);
        StringBuilder builder = new StringBuilder();
        editCommand.accept(newMessage, chatId, builder);


        verify(sqliteManager).editTask(taskId, chatId, "Новое описание с датой 2024-10-20", LocalDate.parse("2024-10-20"));
        assertEquals(expectedResponse, builder.toString());
    }

}

