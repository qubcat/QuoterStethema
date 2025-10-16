import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;


import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MyQuoteBot extends TelegramLongPollingBot {
    private final String token, username;

    // Базовый набор цитат
    private static final List<String> QUOTES = List.of(
            "Все гениальное — просто.",
            "Делай, что можешь, с тем, что имеешь.",
            "Путь в тысячу ли начинается с первого шага.",
            "Капля камень точит — не силой, а частым падением.",
            "Сложное — это просто, но сделанное."
    );

    public MyQuoteBot(String token, String username) {
        this.token = token;
        this.username = username;
        try {
            execute(new SetMyCommands(
                    List.of(
                            new BotCommand("start", "приветствие и подсказки"),
                            new BotCommand("help",  "как пользоваться ботом"),
                            new BotCommand("quote", "случайная цитата или поиск: /quote <тема>")
                    ),
                    new BotCommandScopeDefault(),
                    null
            ));
        } catch (Exception e) {
            // не падаем, если не удалось выставить меню
            e.printStackTrace();
        }
    }



    @Override public String getBotToken() { return token; }
    @Override public String getBotUsername() { return username; }

    @Override
    public void onUpdateReceived(Update u) {
        if (!u.hasMessage() || !u.getMessage().hasText()) return;
        long chatId = u.getMessage().getChatId();
        String text = u.getMessage().getText().trim();

        if (text.equals("/start")) {
            reply(chatId, """
          Привет! Я отправляю вдохновляющие цитаты.
          Команды:
          • /quote — случайная цитата
          • /quote <тема> — поиск по слову (например: /quote камень)
          """);
            return;
        }

        if (text.equals("/help")) { reply(chatId, """
            Справка:
            • /quote — пришлю случайную цитату.
            • /quote <тема> — найду цитату по слову (например: /quote путь).
            Подсказки:
            • можно просто нажать кнопку «Меню» и выбрать команду;
            • если ничего не найдено — уточни ключевое слово.
            """);
            return;
        }

        if (text.startsWith("/quote")) {
            String query = text.replaceFirst("^/quote\\s*", "").trim();
            if (query.isEmpty()) {
                reply(chatId, randomQuote());
            } else {
                String qLower = query.toLowerCase();
                List<String> hits = QUOTES.stream()
                        .filter(s -> s.toLowerCase().contains(qLower))
                        .collect(Collectors.toList());
                if (hits.isEmpty()) reply(chatId, "Ничего по запросу: " + query);
                else reply(chatId, hits.get(ThreadLocalRandom.current().nextInt(hits.size())));
            }
            return;
        }

        if (text.startsWith("/")) {
            reply(chatId, "Не знаю такой команды. Открой /help — там список доступных.");
        }
    }

    private String randomQuote() {
        return QUOTES.get(ThreadLocalRandom.current().nextInt(QUOTES.size()));
    }

    private void reply(long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
