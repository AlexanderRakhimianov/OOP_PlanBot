package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TelegramBotTest2 {

    private TelegramBot bot; // наш бот
    private SQLiteManager testSqliteManager;

    @BeforeEach
    public void setUp() {
        // перед каждым тестом --> создаём нового бота
        File dbFile = new File("test_tasks.db");
        if(dbFile.exists()){
            try{
                Files.delete(Paths.get(dbFile.getAbsolutePath()));
                System.out.println("Файл базы данных удалён.");
            } catch(IOException e){
                System.out.println("Ошибка при удалении файла " + e.getMessage());
            }
        }
        testSqliteManager = new SQLiteManager("jdbc:sqlite:test_tasks.db");
        bot = new TelegramBot(testSqliteManager);
    }


    @Test
    public void testAddTaskCommand() {
        // Подготовка
        String taskDescription = "Test task";
        long chatId = 12345;
        StringBuilder response = new StringBuilder();

        // Выполнение
        bot.getCommandMap().get("/addtask").accept("/addtask" + taskDescription, chatId, response);

        // Проверка
        assertTrue(response.toString().contains("Задача добавлена: " + taskDescription), "Сообщение об успешном добавлении задачи должно присутствовать.");
        assertTrue(testSqliteManager.hasTasks(chatId), "В бд должны быть задачи");
        // bot.getCommandMap().get("/remove").accept("/remove 1", chatId, response);
    }
    @Test
    public void testAllTasksCommandWhenNoTasks() {
        // Подготовка
        long chatId = 12345;
        StringBuilder response = new StringBuilder();

        // Выполнение
        bot.getCommandMap().get("/alltasks").accept("/alltasks", chatId, response);

        // Проверка
        assertEquals("У вас нет задач.", response.toString(), "Сообщение об отсутствии задач должно быть.");
    }

    @Test
    public void testAllTasksCommandWhenTasksExists() {
        // Подготовка
        long chatId = 12345;
        StringBuilder response = new StringBuilder();
        bot.getCommandMap().get("/addtask").accept("/addtask Test task", chatId, new StringBuilder());

        // Выполнение
        bot.getCommandMap().get("/alltasks").accept("/alltasks", chatId, response);

        // Проверка
        assertTrue(response.toString().contains("Текущие задачи:"), "Список задач должен присутствовать.");
        // bot.getCommandMap().get("/remove").accept("/remove 1", chatId, response);
    }
    @Test
    public void testDoneCommand() {
        // Подготовка
        long chatId = 12345;
        StringBuilder response = new StringBuilder();
        bot.getCommandMap().get("/addtask").accept("/addtask Test task", chatId, new StringBuilder());


        // Выполнение
        bot.getCommandMap().get("/done").accept("/done 1", chatId, response);

        // Проверка
        assertTrue(response.toString().contains("Задача 1 отмечена как выполненная"), "Сообщение об успешном выполнении задачи должно присутствовать.");
        assertTrue(testSqliteManager.getTaskById(testSqliteManager.getTaskId(1, chatId), chatId).isCompleted(), "Задача должна быть отмечена как выполненная");
        // bot.getCommandMap().get("/remove").accept("/remove 1", chatId, response);

    }

    @Test
    public void testRemoveCommand() {
        // Подготовка
        long chatId = 12345;
        StringBuilder response = new StringBuilder();
        bot.getCommandMap().get("/addtask").accept("/addtask Test task", chatId, new StringBuilder());


        // Выполнение
        bot.getCommandMap().get("/remove").accept("/remove 1", chatId, response);

        // Проверка
        assertTrue(response.toString().contains("Задача 1 удалена"), "Сообщение об успешном удалении задачи должно присутствовать.");
        assertFalse(testSqliteManager.hasTasks(chatId), "В бд не должно быть задач");
    }

    @Test
    public void testRemoveAllCommand() {
        // Подготовка
        long chatId = 12345;
        StringBuilder response = new StringBuilder();
        bot.getCommandMap().get("/addtask").accept("/addtask Test task one", chatId, new StringBuilder());
        bot.getCommandMap().get("/addtask").accept("/addtask Test task two", chatId, new StringBuilder());


        // Выполнение
        bot.getCommandMap().get("/removeall").accept("/removeall", chatId, response);

        // Проверка
        assertTrue(response.toString().contains("Все задачи удалены."), "Сообщение об успешном удалении всех задач должно присутствовать.");
        assertFalse(testSqliteManager.hasTasks(chatId), "В бд не должно быть задач");
    }


}
