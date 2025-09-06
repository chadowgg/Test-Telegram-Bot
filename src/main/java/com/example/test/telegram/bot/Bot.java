package com.example.test.telegram.bot;

import com.example.test.telegram.bot.entity.BotResources;
import com.example.test.telegram.bot.service.BotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.ByteArrayContent;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class Bot extends TelegramLongPollingBot {
    private final BotResources botResources;
    private final BotService botService;

    public Bot(BotResources botResources, BotService botService) {
        this.botResources = botResources;
        this.botService = botService;
    }

    @Override
    public String getBotUsername() {
        return botResources.getBotName();
    }

    @Override
    public String getBotToken() {
        return botResources.getToken();
    }

    // Зберігаються дані користувачів, які вони обрали
    private final Map<Long, UserState> users = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                if (!users.containsKey(chatId)) {
                    users.put(chatId, new UserState("first_entry"));
                    sendMessage(chatId, "Вітаю\nОберіть вашу роль:", roleKeyboard());
                } else {
                    sendMessage(chatId, "Ви вже зареєстровані");
                }
            }

            if (!text.startsWith("/") && !users.get(chatId).status.equals("first_entry")) {
                String result = botService.analyzeFeedback(text);
                System.out.println("Результат OpenAI:\n" + result);

                String documentId = "1ZLsi6yRa-IiCbDFsG04wKNpzOdP9sbg_oESyZ5fv00g";
                String jsonRequest = String.format(
                        "{ \"requests\": [ { \"insertText\": { \"text\": \"Відгук користувача: %s\\n%s\\n\\n\\n\", \"endOfSegmentLocation\": {} } } ] }",
                        text, result
                );

                // Завантаження Service Account
                GoogleCredentials credentials = null;
                try {
                    credentials = GoogleCredentials.fromStream(
                                    Objects.requireNonNull(Bot.class.getClassLoader().getResourceAsStream("feedback-470713-59c20c11ea24.json")))
                            .createScoped("https://www.googleapis.com/auth/documents");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);

                HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(adapter);

                GenericUrl url = new GenericUrl("https://docs.googleapis.com/v1/documents/" + documentId + ":batchUpdate");
                HttpContent content = new ByteArrayContent("application/json", jsonRequest.getBytes(StandardCharsets.UTF_8));

                try {
                    requestFactory.buildPostRequest(url, content).execute();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                sendMessage(chatId, "Ваш фідбек збережено.");
            }
        }

        // Обробка натискання кнопок
        if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();

            if (!users.containsKey(chatId)) {
                sendMessage(chatId, "Будь ласка, почніть з команди /start");
                return;
            }

            UserState state = users.get(chatId);

            if (state != null && "first_entry".equals(state.status)) {
                state.role = data;
                state.status = "login_complete";
                deleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                sendMessage(chatId, "Оберіть вашу філію:", branchKeyboard());
            } else if (state != null && "login_complete".equals(state.status)) {
                state.branch = data;
                state.status = "active";
                deleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());
                sendMessage(chatId, "Реєстрація завершена!\nВаша роль: " + state.role +
                        "\nФілія: " + state.branch);
            }
        }
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(keyboard);
            execute(message);
        } catch (Exception e) {
            System.out.println("Помилка відправлення повідомлення: " + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    private void deleteMessage(long chatId, int messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);
            execute(deleteMessage); // Використовуємо метод execute з вашого бота
        } catch (TelegramApiException e) {
            System.out.println("Помилка при видаленні повідомлення: " + e.getMessage());
        }
    }

    // Ролі
    private InlineKeyboardMarkup roleKeyboard() {
        InlineKeyboardButton mech = new InlineKeyboardButton();
        mech.setText("Механік");
        mech.setCallbackData("Механік");

        InlineKeyboardButton electric = new InlineKeyboardButton();
        electric.setText("Електрик");
        electric.setCallbackData("Електрик");

        InlineKeyboardButton manager = new InlineKeyboardButton();
        manager.setText("Менеджер");
        manager.setCallbackData("Менеджер");

        List<InlineKeyboardButton> row = List.of(mech, electric, manager);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Філій
    private InlineKeyboardMarkup branchKeyboard() {
        InlineKeyboardButton f1 = new InlineKeyboardButton();
        f1.setText("Київ");
        f1.setCallbackData("Київ");

        InlineKeyboardButton f2 = new InlineKeyboardButton();
        f2.setText("Львів");
        f2.setCallbackData("Львів");

        InlineKeyboardButton f3 = new InlineKeyboardButton();
        f3.setText("Запоріжжя");
        f3.setCallbackData("Запоріжжя");

        InlineKeyboardButton f4 = new InlineKeyboardButton();
        f4.setText("Дніпро");
        f4.setCallbackData("Дніпро");

        InlineKeyboardButton f5 = new InlineKeyboardButton();
        f5.setText("Ужгород");
        f5.setCallbackData("Ужгород");

        List<InlineKeyboardButton> row = List.of(f1, f2, f3, f4, f5);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Збереження стану користувача
    static class UserState {
        String status;
        String role;
        String branch;

        public UserState(String status) {
            this.status = status;
        }
    }
}