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


class AddTaskCommandTest {

    @Mock
    private SQLiteManager sqliteManager;

    private  TriConsumer<String, Long, StringBuilder> addCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addCommand = getAddTaskCommand(sqliteManager);
    }
    private TriConsumer<String, Long, StringBuilder> getAddTaskCommand(SQLiteManager sqliteManager){
        return (message, chatId, builder) -> {
            LocalDate date;
            try {
                int index = message.indexOf("2");
                String strDate = message.substring(index, index + 10);
                date = LocalDate.parse(strDate);
            }
            catch (StringIndexOutOfBoundsException | DateTimeParseException exception)
            {
                date = LocalDate.parse("2010-01-01");
            }

            String taskDescription = message.substring("/addtask".length()).trim();
            if (taskDescription.isEmpty()) {
                builder.append("Пожалуйста, введите описание задачи после команды /addtask.");
                return;
            }
            Task newTask = new Task(taskDescription, chatId, date);
            sqliteManager.addTask(newTask);
            builder.append("Задача добавлена: ").append(newTask.getDescription()).append("\n");

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
    void testAddTaskWithDate() {
        long chatId = 12345;
        String newMessage = "/addtask Купить молоко 2024-08-15";
        String expectedResponse = "Задача добавлена: Купить молоко 2024-08-15\n";
        StringBuilder builder = new StringBuilder();

        addCommand.accept(newMessage, chatId, builder);
        verify(sqliteManager).addTask(argThat(task ->
                task.getDescription().equals("Купить молоко 2024-08-15") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2024-08-15"))));
        assertEquals(expectedResponse, builder.toString());
    }
    @Test
    void testAddTaskWithoutDate() {
        long chatId = 12345;
        String newMessage = "/addtask Купить молоко";
        String expectedResponse = "Задача добавлена: Купить молоко\n";

        StringBuilder builder = new StringBuilder();
        addCommand.accept(newMessage, chatId, builder);

        verify(sqliteManager).addTask(argThat(task ->
                task.getDescription().equals("Купить молоко") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2010-01-01"))));
        assertEquals(expectedResponse, builder.toString());
    }
    @Test
    void testAddTaskWithIncorrectDate() {
        long chatId = 12345;
        String newMessage = "/addtask Купить молоко 2024/08/15";
        String expectedResponse = "Задача добавлена: Купить молоко 2024/08/15\n";

        StringBuilder builder = new StringBuilder();
        addCommand.accept(newMessage, chatId, builder);

        verify(sqliteManager).addTask(argThat(task ->
                task.getDescription().equals("Купить молоко 2024/08/15") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2010-01-01"))));
        assertEquals(expectedResponse, builder.toString());
    }
    @Test
    void testAddTaskWithDateInDifferentParts() {
        long chatId = 12345;
        //Дата в начале
        String newMessage1 = "/addtask 2024-10-20 Купить молоко";
        String expectedResponse1 = "Задача добавлена: 2024-10-20 Купить молоко\n";

        StringBuilder builder1 = new StringBuilder();
        addCommand.accept(newMessage1, chatId, builder1);

        verify(sqliteManager, times(1)).addTask(argThat(task ->
                task.getDescription().equals("2024-10-20 Купить молоко") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2024-10-20"))));
        assertEquals(expectedResponse1, builder1.toString());

        //Дата в середине
        String newMessage2 = "/addtask Купить 2024-10-21 молоко";
        String expectedResponse2 = "Задача добавлена: Купить 2024-10-21 молоко\n";

        StringBuilder builder2 = new StringBuilder();
        addCommand.accept(newMessage2, chatId, builder2);
        verify(sqliteManager, times(1)).addTask(argThat(task ->
                task.getDescription().equals("Купить 2024-10-21 молоко") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2024-10-21"))));
        assertEquals(expectedResponse2, builder2.toString());

        //Дата в конце
        String newMessage3 = "/addtask Купить молоко 2024-10-22";
        String expectedResponse3 = "Задача добавлена: Купить молоко 2024-10-22\n";

        StringBuilder builder3 = new StringBuilder();
        addCommand.accept(newMessage3, chatId, builder3);

        verify(sqliteManager, times(1)).addTask(argThat(task ->
                task.getDescription().equals("Купить молоко 2024-10-22") &&
                        task.getChatId() == chatId &&
                        task.getDate().equals(LocalDate.parse("2024-10-22"))));
        assertEquals(expectedResponse3, builder3.toString());
    }

}

