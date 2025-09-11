package com.example.test.telegram.bot;

import com.example.test.telegram.bot.entity.BotResources;
import com.example.test.telegram.bot.entity.Feedback;
import com.example.test.telegram.bot.entity.User;
import com.example.test.telegram.bot.enums.Affiliate;
import com.example.test.telegram.bot.enums.Position;
import com.example.test.telegram.bot.enums.RegistrationStatus;
import com.example.test.telegram.bot.enums.Role;
import com.example.test.telegram.bot.repository.FeedbackRepository;
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
    @Autowired
    private FeedbackRepository feedbackRepository;

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

    // Тимчасово зберігаються дані фільтрації
    private final Map<Long, String> filterMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                Optional<User> userOpt = userRepository.findById(chatId);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    if (user.getRole() == Role.ADMIN) {
                        sendMessage(chatId, "Вітаю, адмін. Оберіть дію:", adminPanelKeyboard());
                    } else {
                        sendMessage(chatId, "Ви вже зареєстровані");
                    }
                } else {
                    User newUser = new User();
                    newUser.setId(chatId);
                    newUser.setRegistrationStatus(RegistrationStatus.FIRST_ENTRY);
                    newUser.setRole(Role.USER);

                    users.put(chatId, newUser);

                    sendMessage(chatId, "Вітаю\nОберіть вашу роль:", roleKeyboard());
                }
            }

            // Обробка повідомлень OpenAL, відпрака в Google Docs, збереження БД
            // Створення Trello карт в FeedbackService
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

            Optional<User> userOpt = userRepository.findById(chatId);
            User user;
            if (userOpt.isEmpty()) {
                user = users.get(chatId);
                if (user == null) {
                    sendMessage(chatId, "Будь ласка, почніть з команди /start");
                    return;
                };
            } else {
                user = userOpt.get();
            }

            if (user.getRole() == Role.ADMIN) {
                switch (data) {
                    case "FILTER_AFFILIATE" -> {
                        sendMessage(chatId, "Оберіть філію:", affiliateKeyboard());
                        filterMap.put(chatId, "AFFILIATE");
                        return;
                    }
                    case "FILTER_POSITION" -> {
                        sendMessage(chatId, "Оберіть посаду:", roleKeyboard());
                        filterMap.put(chatId, "POSITION");
                        return;
                    }
                    case "FILTER_CRITICALITY" -> {
                        sendMessage(chatId, "Оберіть від якой позицій сортувати:", criticalityKeyboard());
                        return;
                    }
                }

                String filterType = filterMap.get(chatId);

                if ("AFFILIATE".equals(filterType)) {
                    Affiliate selectedAffiliate = Affiliate.valueOf(data);
                    List<User> users = userRepository.findAllByAffiliate(selectedAffiliate);
                    sendFilteredUsers(chatId, users);
                    filterMap.remove(chatId);
                    sendMessage(chatId, "Оберіть дію:", adminPanelKeyboard());
                }
                else if ("POSITION".equals(filterType)) {
                    Position selectedPosition = Position.valueOf(data);
                    List<User> users = userRepository.findAllByPosition(selectedPosition);
                    sendFilteredUsers(chatId, users);
                    filterMap.remove(chatId);
                    sendMessage(chatId, "Оберіть дію:", adminPanelKeyboard());
                }
                else if (data.startsWith("CRITICALITY_")) {
                    int level = Integer.parseInt(data.split("_")[1]); // отримуємо число 1–5
                    List<Feedback> feedbacks = feedbackRepository.findAllByCriticality(level);
                    sendFilteredFeedbacks(chatId, feedbacks);
                    sendMessage(chatId, "Оберіть дію:", adminPanelKeyboard());
                }
            } else {
                if (user.getRegistrationStatus() == RegistrationStatus.FIRST_ENTRY) {
                    user.setPosition(Position.valueOf(data));
                    user.setRegistrationStatus(RegistrationStatus.LOGIN_COMPLETE);

                    sendMessage(chatId, "Оберіть вашу філію:", affiliateKeyboard());
                } else if (user.getRegistrationStatus() == RegistrationStatus.LOGIN_COMPLETE) {
                    user.setAffiliate(Affiliate.valueOf(data));
                    user.setRegistrationStatus(RegistrationStatus.ACTIVE);

                    userRepository.save(user);
                    users.remove(chatId);

                    sendMessage(chatId, "Реєстрація завершена!\nВаша роль: "
                            + user.getPosition().getDisplayName() +
                            "\nФілія: " + user.getAffiliate().getDisplayName());
                }
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
            sendMessage(chatId, "Помилка відправлення повідомлення", null);
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
            sendMessage(chatId, "Помилка при видаленні повідомлення", null);
        }
    }

    // Вибір ролі
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

    // Вибір філій
    private InlineKeyboardMarkup affiliateKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Affiliate affiliate : Affiliate.values()) {
            InlineKeyboardButton btn = new InlineKeyboardButton(affiliate.getDisplayName());
            btn.setCallbackData(affiliate.name());
            rows.add(List.of(btn));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    // Вибір критичності відгука для фільтрації
    private InlineKeyboardMarkup criticalityKeyboard() {
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton(String.valueOf(i));
            button.setCallbackData("CRITICALITY_" + i);
            row.add(button);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(row));
        return markup;
    }

    // Адмін панель
    private InlineKeyboardMarkup adminPanelKeyboard() {
        InlineKeyboardButton affiliateBtn = new InlineKeyboardButton("Фільтрувати по філії");
        affiliateBtn.setCallbackData("FILTER_AFFILIATE");

        InlineKeyboardButton positionBtn = new InlineKeyboardButton("Фільтрувати по посаді");
        positionBtn.setCallbackData("FILTER_POSITION");

        InlineKeyboardButton criticalityBtn = new InlineKeyboardButton("Фільтрувати по критичності");
        criticalityBtn.setCallbackData("FILTER_CRITICALITY");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(affiliateBtn));
        rows.add(List.of(positionBtn));
        rows.add(List.of(criticalityBtn));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    // Відправка результату фільтрації по працівникам в чат
    private void sendFilteredUsers(long chatId, List<User> users) {
        if (users.isEmpty()) {
            sendMessage(chatId, "Працівників не знайдено.");
            return;
        }

        StringBuilder sb = new StringBuilder("Знайдені працівники:\n\n");

        for (User u : users) {
            int count = 1;
            sb.append("ID: ").append(u.getId())
                    .append("\nРоль: ").append(u.getRole())
                    .append("\nПосада: ").append(u.getPosition().getDisplayName())
                    .append("\nФілія: ").append(u.getAffiliate().getDisplayName())
                    .append("\nВідгуки:\n");

            List<Feedback> feedbacks = feedbackRepository.findAllByUserId(u.getId());
            if (feedbacks.isEmpty()) {
                sb.append("Відгуків немає\n");
            } else {
                for (Feedback f : feedbacks) {
                    sb.append(count).append(": ").append(f.getFeedback())
                            .append(" (Критичність: ").append(f.getCriticality())
                            .append(", Рекомендація: ").append(f.getRecommendation()).append(")\n");
                    count++;
                }
            }
            sb.append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

    // Відправка результату фільтрації по відгукам в чат
    private void sendFilteredFeedbacks(long chatId, List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            sendMessage(chatId, "Відгуків не знайдено.");
            return;
        }

        StringBuilder sb = new StringBuilder("Відгуки:\n\n");
        for (Feedback f : feedbacks) {
            sb.append("ID: ").append(f.getUser().getId())
                    .append("\nРоль: ").append(f.getUser().getRole())
                    .append("\nПосада: ").append(f.getUser().getPosition().getDisplayName())
                    .append("\nФілія: ").append(f.getUser().getAffiliate().getDisplayName())
                    .append("\nВідгук: ").append(f.getFeedback())
                    .append("\nКритичність: ").append(f.getCriticality())
                    .append("\nРекомендація: ").append(f.getRecommendation())
                    .append("\n\n");
        }

        sendMessage(chatId, sb.toString());
    }
}