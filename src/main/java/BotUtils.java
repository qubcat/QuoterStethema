import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

public class BotUtils {
    public static void sendContactRequest(AbsSender bot, long chatId) {
        KeyboardButton btn = new KeyboardButton("📱 Отправить номер");
        btn.setRequestContact(true); // ← ключевая строка

        KeyboardRow row = new KeyboardRow();
        row.add(btn);

        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setKeyboard(List.of(row));
        kb.setResizeKeyboard(true);
        kb.setOneTimeKeyboard(true); // спрячется после нажатия

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text("Нажми кнопку, чтобы поделиться номером:")
                .replyMarkup(kb)      // ← ReplyKeyboardMarkup, НЕ inline!
                .build();

        try { bot.execute(msg); } catch (Exception e) { e.printStackTrace(); }
    }
}
