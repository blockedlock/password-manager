package com.company.password_manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

            if (text.equals("Отмена")) {
                userState.remove(chatId);
                userTempData.remove(chatId);
                sendMessage(chatId, "Отменено");
                return;
            }

            if (text.equals("/start")) {
                ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
                keyboard.setResizeKeyboard(true);
                KeyboardRow row = new KeyboardRow();
                row.add("📋 Список");
                row.add("➕ Добавить");
                KeyboardRow row2 = new KeyboardRow();
                row2.add("👥 Доступ");
                row2.add("🔗 Чужие пароли");
                KeyboardRow row3 = new KeyboardRow();
                row3.add("Отмена");
                keyboard.setKeyboard(List.of(row,row2,row3));



                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Привет! Я менеджер паролей.");
                message.setReplyMarkup(keyboard);
                try { execute(message); } catch (Exception e) { e.printStackTrace(); }

            } else if (text.equals("📋 Список") || text.equals("/list")) {
                handleList(chatId);
            } else if (text.equals("➕ Добавить") || text.equals("/add")) {
                userState.put(chatId, "WAITING_SERVICE");
                sendMessage(chatId, "Введи название сервиса:");
            } else if (text.equals("👥 Доступ") || text.equals("/share")) {
                userState.put(chatId, "WAITING_SHARE_ID");
                sendMessage(chatId, "Введи Telegram ID пользователя.\nЧтобы узнать свой ID — напиши @userinfobot в Telegram.");
            } else if (text.equals("🔗 Чужие пароли") || text.equals("/shared")) {
                handleSharedList(chatId);
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
            } else if (state.equals("WAITING_SHARE_ID")) {
                try {
                    Long viewerId = Long.parseLong(text);
                    passwordService.grantAccess(chatId, viewerId);
                    sendMessage(chatId, "Доступ предоставлен!");
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Неверный ID. Введи числовой Telegram ID.");
                }
                userState.remove(chatId);
            }

        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (data.startsWith("get_")) {
                Long id = Long.parseLong(data.substring(4));
                handleGet(chatId, id);
            } else if (data.startsWith("delete_")) {
                Long id = Long.parseLong(data.substring(7));
                handleDelete(chatId, id);
            } else if (data.startsWith("view_")) {
                Long id = Long.parseLong(data.substring(5));
                handleViewOnly(chatId, id);
            }
        }
    }


    private void handleList(long chatId) {

        var passwords = passwordService.getPasswords(chatId);
        if (passwords.isEmpty()) {
            sendMessage(chatId, "Паролей нет");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Password p : passwords) {
            InlineKeyboardButton btn = new InlineKeyboardButton(p.getServiceName());
            btn.setCallbackData("get_" + p.getId());
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Всего паролей: " + passwords.size());
        message.setReplyMarkup(markup);
        try { execute(message); } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleGet(long chatId, Long id) {
        try {
            Password p = passwordService.getPasswordById(id);
            String decrypted = passwordService.getDecryptedPassword(id);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton deleteBtn = new InlineKeyboardButton("🗑 Удалить");
            deleteBtn.setCallbackData("delete_" + id);
            markup.setKeyboard(List.of(List.of(deleteBtn)));

            SendMessage credentialsMsg = new SendMessage();
            credentialsMsg.setChatId(chatId);
            credentialsMsg.setText("🔑 " + p.getServiceName() + "\n👤 Логин: <code>" + p.getLogin() + "</code>\n🔒 Пароль: <code>" + decrypted + "</code>\n📅 Добавлен: " + p.getCreatedAt().toLocalDate());
            credentialsMsg.setParseMode("HTML");
            credentialsMsg.setReplyMarkup(markup);
            var sentMessage = execute(credentialsMsg);
            int messageId = sentMessage.getMessageId();

            SendMessage noticeMsg = new SendMessage();
            noticeMsg.setChatId(chatId);
            noticeMsg.setText("⏳ Пароль исчезнет через 60 секунд.");
            var sentNotice = execute(noticeMsg);
            int noticeId = sentNotice.getMessageId();

            new Thread(() -> {
                try {
                    Thread.sleep(60000);
                    DeleteMessage del1 = new DeleteMessage();
                    del1.setChatId(String.valueOf(chatId));
                    del1.setMessageId(messageId);
                    execute(del1);

                    DeleteMessage del2 = new DeleteMessage();
                    del2.setChatId(String.valueOf(chatId));
                    del2.setMessageId(noticeId);
                    execute(del2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            sendMessage(chatId, "Пароль не найден.");
        }
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

    private void handleSharedList(long chatId) {
        var passwords = passwordService.getSharedPasswords(chatId);
        if (passwords.isEmpty()) {
            sendMessage(chatId, "Нет доступа к чужим паролям.");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Password p : passwords) {
            InlineKeyboardButton btn = new InlineKeyboardButton(p.getServiceName());
            btn.setCallbackData("view_" + p.getId());
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Доступные пароли:");
        message.setReplyMarkup(markup);
        try { execute(message); } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleViewOnly(long chatId, Long id) {
        try {
            Password p = passwordService.getPasswordById(id);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("🔑 " + p.getServiceName() + "\nЛогин: " + p.getLogin() + "\nПароль: " + passwordService.getDecryptedPassword(id) + "\n\nСообщение исчезнет через 60 секунд.");
            var sentMessage = execute(message);
            int messageId = sentMessage.getMessageId();

            new Thread(() -> {
                try {
                    Thread.sleep(60000);
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(String.valueOf(chatId));
                    deleteMessage.setMessageId(messageId);
                    execute(deleteMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            sendMessage(chatId, "Пароль не найден.");
        }
    }

}

