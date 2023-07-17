package me.waterbroodje;

import okhttp3.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "YOUR_BOT_USERNAME_HERE";
    private static final String BOT_TOKEN = "YOUR_BOT_TOKEN_HERE";
    private static final String API_KEY = "YOUR_API_KEY_HERE";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String messageText = update.getMessage().getText().toLowerCase();
            if (messageText.equals("/cat")) {
                try {
                    sendRandomCatMessage(update.getMessage().getChatId());
                } catch (IOException | TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            Long chatId = callbackQuery.getMessage().getChatId();

            if (data.startsWith("vote_")) {
                String[] parts = data.split("_");
                String imageId = parts[1];
                int value = Integer.parseInt(parts[2]);

                try {
                    createVote(imageId, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());

                if (value == 1 || value == -1) {
                    // Send a new cat image along with the asking message
                    try {
                        sendRandomCatMessage(chatId);
                    } catch (IOException | TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            } else if (data.equals("stop")) {
                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
            } else if (data.equals("yes") || data.equals("no")) {
                // Vote Yes or No and then stop
                String imageId = callbackQuery.getMessage().getPhoto().get(0).getFileId();
                int value = data.equals("yes") ? 1 : -1;

                try {
                    createVote(imageId, value);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Delete the asking message
                deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
            }
        }
    }

    private void createVote(String imageId, int value) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String apiUrl = "https://api.thecatapi.com/v1/votes";

        RequestBody requestBody = new FormBody.Builder()
                .add("image_id", imageId)
                .add("value", String.valueOf(value))
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", API_KEY)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        response.close();
    }

    private void sendRandomCatMessage(Long chatId) throws IOException, TelegramApiException {
        RandomCatImageFetcher.CatImage catImage = RandomCatImageFetcher.getRandomCatImage();

        if (catImage != null) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(catImage.getUrl()));
            execute(sendPhoto);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Here's a random cat image! Do you like it?");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> voteRow = new ArrayList<>();
            InlineKeyboardButton upvoteButton = new InlineKeyboardButton();
            upvoteButton.setText("Yes & Send New Cat");
            upvoteButton.setCallbackData("vote_" + catImage.getId() + "_1");
            voteRow.add(upvoteButton);

            InlineKeyboardButton downvoteButton = new InlineKeyboardButton();
            downvoteButton.setText("No & Send New Cat");
            downvoteButton.setCallbackData("vote_" + catImage.getId() + "_-1");
            voteRow.add(downvoteButton);
            rows.add(voteRow);

            List<InlineKeyboardButton> yesNoRow = new ArrayList<>();
            InlineKeyboardButton yesButton = new InlineKeyboardButton();
            yesButton.setText("Yes");
            yesButton.setCallbackData("yes");
            yesNoRow.add(yesButton);

            InlineKeyboardButton noButton = new InlineKeyboardButton();
            noButton.setText("No");
            noButton.setCallbackData("no");
            yesNoRow.add(noButton);
            rows.add(yesNoRow);

            List<InlineKeyboardButton> stopRow = new ArrayList<>();
            InlineKeyboardButton stopButton = new InlineKeyboardButton();
            stopButton.setText("Stop");
            stopButton.setCallbackData("stop");
            stopRow.add(stopButton);
            rows.add(stopRow);

            markup.setKeyboard(rows);
            message.setReplyMarkup(markup);

            execute(message);
        }
    }

    private void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}
