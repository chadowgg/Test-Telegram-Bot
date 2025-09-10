package com.example.test.telegram.bot;

import com.example.test.telegram.bot.entity.BotResources;
import com.example.test.telegram.bot.entity.User;
import com.example.test.telegram.bot.enums.Affiliate;
import com.example.test.telegram.bot.enums.Position;
import com.example.test.telegram.bot.enums.RegistrationStatus;
import com.example.test.telegram.bot.repository.UserRepository;
import com.example.test.telegram.bot.service.BotService;
import com.example.test.telegram.bot.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FeedbackService feedbackService;

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

    // Тимчасово зберігаються дані користувачів
    private final Map<Long, User> users = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                if (!userRepository.existsById(chatId)) {
                    User newUser = new User();
                    newUser.setId(chatId);
                    newUser.setRegistrationStatus(RegistrationStatus.FIRST_ENTRY);

                    users.put(chatId, newUser);

                    sendMessage(chatId, "Вітаю\nОберіть вашу роль:", roleKeyboard());
                } else {
                    sendMessage(chatId, "Ви вже зареєстровані");
                }
            }

            // Обробка повідомлень
            Optional<User> userOpt = userRepository.findById(chatId);
            if (userOpt.isPresent() && !text.startsWith("/") &&
                    userOpt.get().getRegistrationStatus() != RegistrationStatus.FIRST_ENTRY) {

                User user = userOpt.get();
                String result = botService.analyzeFeedback(text);
                feedbackService.saveFeedbackFromAI(text, result, user);

                System.out.println("Результат OpenAI:\n" + result);

                String documentId = "1R4VB7iC8i4vlR1vA0wGkYxGasahXUCU_mKbzVcaUZ4M";
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
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            String data = update.getCallbackQuery().getData();

            deleteMessage(chatId, messageId);

            User user = users.get(chatId);
            if (user == null) {
                sendMessage(chatId, "Будь ласка, почніть з команди /start");
                return;
            }

            if (user.getRegistrationStatus() == RegistrationStatus.FIRST_ENTRY) {
                user.setPosition(Position.valueOf(data));
                user.setRegistrationStatus(RegistrationStatus.LOGIN_COMPLETE);

                sendMessage(chatId, "Оберіть вашу філію:", branchKeyboard());
            } else if (user.getRegistrationStatus() == RegistrationStatus.LOGIN_COMPLETE) {
                user.setAffiliate(Affiliate.valueOf(data));
                user.setRegistrationStatus(RegistrationStatus.ACTIVE);

                userRepository.save(user);
//                users.remove(chatId);

                sendMessage(chatId, "Реєстрація завершена!\nВаша роль: "
                        + user.getPosition().getDisplayName() +
                        "\nФілія: " + user.getAffiliate().getDisplayName());
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
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (Position position : Position.values()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(position.getDisplayName());
            button.setCallbackData(position.name());
            row.add(button);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Філій
    private InlineKeyboardMarkup branchKeyboard() {
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (Affiliate affiliate : Affiliate.values()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(affiliate.getDisplayName());
            button.setCallbackData(affiliate.name());
            row.add(button);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }
}