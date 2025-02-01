package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class DateTasksCommandTest {

    @Mock
    private SQLiteManager sqliteManager;

    private TriConsumer<String, Long, StringBuilder> dateTasksCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dateTasksCommand = getDateTasksCommand(sqliteManager);
    }

    private TriConsumer<String, Long, StringBuilder> getDateTasksCommand(SQLiteManager sqliteManager) {
        return (message, chatId, builder) -> {
            String[] splMsg = message.split(" ");
            String clMsg = "", word;
            int splMsgLen = splMsg.length;
            for (int i = 0; i < splMsgLen; i++) {
                word = splMsg[i];
                clMsg += word;
                if (!word.isEmpty() && i != splMsgLen - 1) {
                    clMsg += " ";
                }
            }
            if (clMsg.equals("/datetasks")) {
                LocalDate date = LocalDate.now();
                List<Task> allTasks = sqliteManager.getAllTasks(chatId);
                int count = 0;
                for (Task task : allTasks) {
                    if (task.getDate().equals(date)) {
                        count++;
                    }
                }
                if (count == 0) {
                    builder.append("На сегодня нет задач.");
                } else {
                    builder.append("Задачи на сегодня:\n");
                    int num = 1;
                    for (int i = 0; i < allTasks.size(); i++) {
                        Task task = allTasks.get(i);
                        if (task.getDate().equals(date)) {
                            builder.append(num).append(". ").append(task).append("\n");
                            num++;
                        }
                    }
                }
                return;
            }
            if ((clMsg.contains("с") && !clMsg.contains("по")) || (!clMsg.contains("с") && clMsg.contains("по"))) {
                builder.append("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.");
                return;
            }
            // если не содержит "с", то автоматически не содержит "по"
            if (!clMsg.contains("с")) {
                try {
                    String strDate = clMsg.substring(11, 21);
                    LocalDate date = LocalDate.parse(strDate);
                    List<Task> allTasks = sqliteManager.getAllTasks(chatId);
                    int count = 0;
                    for (Task task : allTasks) {
                        if (task.getDate().equals(date)) {
                            count++;
                        }
                    }
                    if (count == 0) {
                        builder.append("На ").append(strDate).append(" нет задач.");
                    } else {
                        builder.append("Задачи на ").append(strDate).append(":\n");
                        int num = 1;
                        for (int i = 0; i < allTasks.size(); i++) {
                            Task task = allTasks.get(i);
                            if (task.getDate().equals(date)) {
                                builder.append(num).append(". ").append(task).append("\n");
                                num++;
                            }
                        }
                    }
                } catch (StringIndexOutOfBoundsException | DateTimeParseException exception) {
                    builder.append("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.");
                }
            } else {
                try {
                    String strDate1 = clMsg.substring(13, 23);
                    LocalDate date1 = LocalDate.parse(strDate1);
                    String strDate2 = clMsg.substring(27, 37);
                    LocalDate date2 = LocalDate.parse(strDate2);
                    List<Task> allTasks = sqliteManager.getAllTasks(chatId);
                    int count = 0;
                    LocalDate taskDate;
                    for (Task task : allTasks) {
                        taskDate = task.getDate();
                        if ((taskDate.isAfter(date1) || taskDate.equals(date1)) && (taskDate.isBefore(date2) || taskDate.equals(date2))) {
                            count++;
                        }
                    }
                    if (count == 0) {
                        builder.append("На период с ").append(strDate1).append(" до ").append(strDate2).append(" задач нет.");
                    } else {
                        builder.append("Задачи на период с ").append(strDate1).append(" до ").append(strDate2).append(":\n");
                        int num = 1;
                        for (int i = 0; i < allTasks.size(); i++) {
                            Task task = allTasks.get(i);
                            taskDate = task.getDate();
                            if ((taskDate.isAfter(date1) || taskDate.equals(date1)) && (taskDate.isBefore(date2) || taskDate.equals(date2))) {
                                builder.append(num).append(". ").append(task).append("\n");
                                num++;
                            }
                        }
                    }
                } catch (StringIndexOutOfBoundsException | DateTimeParseException exception) {
                    builder.append("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.");
                }
            }
        };
    }

    @Test
    void testNoArguments() {
        long chatId = 12345;
        LocalDate today = LocalDate.now();
        List<Task> tasks = Arrays.asList(new Task("Задача 1", chatId, today), new Task("Задача 2", chatId, today));
        when(sqliteManager.getAllTasks(chatId)).thenReturn(tasks);
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks", chatId, builder);
        String expected = "Задачи на сегодня:\n1. [ ] Задача 1\n2. [ ] Задача 2\n";
        assertEquals(expected, builder.toString());
    }

    @Test
    void testNoArgumentsNoTasks() {
        long chatId = 12345;
        when(sqliteManager.getAllTasks(chatId)).thenReturn(new ArrayList<>());
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks", chatId, builder);
        assertEquals("На сегодня нет задач.", builder.toString());
    }


    @Test
    void testSpecificDate() {
        long chatId = 12345;
        LocalDate specificDate = LocalDate.of(2024, 7, 20);
        List<Task> tasks = Arrays.asList(new Task("Задача 1", chatId, specificDate));
        when(sqliteManager.getAllTasks(chatId)).thenReturn(tasks);
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks 2024-07-20", chatId, builder);
        String expected = "Задачи на 2024-07-20:\n1. [ ] Задача 1\n";
        assertEquals(expected, builder.toString());
    }

    @Test
    void testSpecificDateNoTasks() {
        long chatId = 12345;
        when(sqliteManager.getAllTasks(chatId)).thenReturn(new ArrayList<>());
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks 2024-07-20", chatId, builder);
        assertEquals("На 2024-07-20 нет задач.", builder.toString());
    }


    @Test
    void testDateRange() {
        long chatId = 12345;
        LocalDate startDate = LocalDate.of(2024, 7, 15);
        LocalDate endDate = LocalDate.of(2024, 7, 20);
        List<Task> tasks = Arrays.asList(new Task("Задача 1", chatId, startDate), new Task("Задача 2", chatId, endDate));
        when(sqliteManager.getAllTasks(chatId)).thenReturn(tasks);
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks с 2024-07-15 по 2024-07-20", chatId, builder);
        String expected = "Задачи на период с 2024-07-15 до 2024-07-20:\n1. [ ] Задача 1\n2. [ ] Задача 2\n";
        assertEquals(expected, builder.toString());
    }

    @Test
    void testDateRangeNoTasks() {
        long chatId = 12345;
        when(sqliteManager.getAllTasks(chatId)).thenReturn(new ArrayList<>());
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks с 2024-07-15 по 2024-07-20", chatId, builder);
        assertEquals("На период с 2024-07-15 до 2024-07-20 задач нет.", builder.toString());
    }

    @Test
    void testInvalidFormat() {
        long chatId = 12345;
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks с 2024-07-15", chatId, builder);
        assertEquals("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.", builder.toString());
    }

    @Test
    void testInvalidDate() {
        long chatId = 12345;
        StringBuilder builder = new StringBuilder();
        dateTasksCommand.accept("/datetasks 2024-13-20", chatId, builder); //неправильный месяц
        assertEquals("Пожалуйста, введите дату или диапазон дат (формат: с YYYY-MM-DD по YYYY-MM-DD) после команды /datetasks.", builder.toString());
    }

}