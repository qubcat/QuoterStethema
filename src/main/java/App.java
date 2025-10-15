import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String token = System.getenv("BOT_TOKEN");
        if (token == null) token = dotenv.get("BOT_TOKEN");

        String username = System.getenv("BOT_USERNAME");
        if (username == null) username = dotenv.get("BOT_USERNAME");

        if (token == null || username == null) {
            System.err.println("❌ Set BOT_TOKEN and BOT_USERNAME (env или .env)");
            System.exit(1);
        }

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new MyQuoteBot(token, username));
        System.out.println("✅ Quotes bot started as @" + username);
    }
}
