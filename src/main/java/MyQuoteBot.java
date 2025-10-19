import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MyQuoteBot extends TelegramLongPollingBot {
    private final String token, username;

    // 🔒 твой Telegram ID (поменяй при необходимости)
    private static final long ADMIN_ID = 419535607L;

    // ✅ Репозиторий для статистики пользователей (SQLite)
    private final UserRepo repo = new UserRepo("data/bot.db");

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

        // Инициализируем БД
        repo.ensureSchema();

        // Регистрируем команды (меню Telegram)
        try {
            execute(new SetMyCommands(
                    List.of(
                            new BotCommand("start", "приветствие и подсказки"),
                            new BotCommand("help",  "как пользоваться ботом"),
                            new BotCommand("quote", "случайная цитата или поиск: /quote <тема>"),
                            new BotCommand("me",    "инфа о тебе"),
                            new BotCommand("stats", "общая статистика (только админ)")
                    ),
                    new BotCommandScopeDefault(),
                    null
            ));
        } catch (Exception e) {
            e.printStackTrace(); // не падаем, если меню не выставилось
        }
    }

    @Override public String getBotToken() { return token; }
    @Override public String getBotUsername() { return username; }

    @Override
    public void onUpdateReceived(Update u) {
        // 1) Если пользователь отправил контакт — сохраняем номер и выходим
        if (u.hasMessage() && u.getMessage().getFrom() != null) {
            User from = u.getMessage().getFrom();
            repo.upsertHit(from.getId(), from.getUserName(), from.getFirstName(), from.getLastName());
        } else if (u.hasCallbackQuery()) {
            User from = u.getCallbackQuery().getFrom();
            repo.upsertHit(from.getId(), from.getUserName(), from.getFirstName(), from.getLastName());
        }

        // 2) Обработчики команд
        if (!u.hasMessage() || !u.getMessage().hasText()) return;
        long chatId = u.getMessage().getChatId();
        String text = u.getMessage().getText().trim();

        if (text.equals("/start")) {
            // Покажем приветствие
            reply(chatId, """
        Привет! Я присылаю вдохновляющие цитаты.
        Чтобы я мог записать твой номер — нажми кнопку ниже 👇
        """);

            // Покажем кнопку "📱 Отправить номер"
            BotUtils.sendContactRequest(this, chatId);
            return;
        }


        if (text.equals("/help")) {
            reply(chatId, """
                    Справка:
                    • /quote — пришлю случайную цитату.
                    • /quote <тема> — найду цитату по слову (например: /quote путь).
                    Подсказки:
                    • можно нажать «Меню» и выбрать команду;
                    • если ничего не найдено — уточни ключевое слово.
                    """);
            return;
        }

        if (text.equals("/me")) {
            long userId = u.getMessage().getFrom().getId();
            var s = repo.get(userId);
            String out = (s == null)
                    ? "Пока данных мало."
                    : String.format("Ты: %s\nid: %d\nТелефон: %s\nЗапросов: %d",
                    s.displayName(), s.userId, (s.phone==null? "—" : s.phone), s.hits);
            reply(chatId, out);
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


        if (text.equals("/phone")) {
            BotUtils.sendContactRequest(this, chatId);
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
