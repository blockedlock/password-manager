package com.company.password_manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final PasswordService passwordService;
    private final String token;
    private final String username;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, String[]> userTempData = new HashMap<>();


    public TelegramBot(PasswordService passwordService,
                       @Value("${telegram.bot.token}") String token,
                       @Value("${telegram.bot.username}") String username) {
        super(token);
        this.passwordService = passwordService;
        this.token = token;
        this.username = username;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String state = userState.getOrDefault(chatId, "IDLE");

            if (text.equals("/start")) {
                sendMessage(chatId, "Привет! Я менеджер паролей.\n/list - список паролей\n/add - добавить пароль");
            } else if (text.equals("/list")) {
                handleList(chatId);
            } else if (text.equals("/add")) {
                userState.put(chatId, "WAITING_SERVICE");
                sendMessage(chatId, "Введи название сервиса:");
            } else if (state.equals("WAITING_SERVICE")) {
                userTempData.put(chatId, new String[]{text, null, null});
                userState.put(chatId, "WAITING_LOGIN");
                sendMessage(chatId, "Введи логин:");
            } else if (state.equals("WAITING_LOGIN")) {
                userTempData.get(chatId)[1] = text;
                userState.put(chatId, "WAITING_PASSWORD");
                sendMessage(chatId, "Введи пароль:");
            } else if (state.equals("WAITING_PASSWORD")) {
                userTempData.get(chatId)[2] = text;
                handleSave(chatId);
            } else if (text.startsWith("/get ")) {
            Long id = Long.parseLong(text.substring(5));
            handleGet(chatId, id);
            } else if (text.startsWith("/delete ")) {
                Long id = Long.parseLong(text.substring(8));
                handleDelete(chatId, id);
            }


        }
    }

    private void handleGet(long chatId, Long id) {
        try {
            String password = passwordService.getDecryptedPassword(id);
            sendMessage(chatId, "Пароль: " + password);
        } catch (Exception e) {
            sendMessage(chatId, "Пароль не найден.");
        }
    }

    private void handleList(long chatId) {
        var passwords = passwordService.getPasswords(chatId);
        if (passwords.isEmpty()) {
            sendMessage(chatId, "Паролей нет. Используй /add чтобы добавить.");
            return;
        }
        StringBuilder sb = new StringBuilder("Твои сервисы:\n");
        for (Password p : passwords) {
            sb.append(p.getId()).append(". ").append(p.getServiceName()).append("\n");
        }
        sendMessage(chatId, sb.toString());
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void handleSave(long chatId) {
        String[] data = userTempData.get(chatId);
        try {
            passwordService.addPassword(chatId, data[0], data[1], data[2]);
            sendMessage(chatId, "Пароль сохранён!");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка сохранения.");
        }
        userState.remove(chatId);
        userTempData.remove(chatId);
    }
    private void handleDelete(long chatId, Long id) {
        if (passwordService.deletePassword(id)) {
            sendMessage(chatId, "Пароль удалён.");
        } else {
            sendMessage(chatId, "Пароль не найден.");
        }
    }

}
