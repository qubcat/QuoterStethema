import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("BOT_TOKEN");
        String username = System.getenv("BOT_USERNAME");
        if (token == null || username == null) {
            System.err.println("Set BOT_TOKEN and BOT_USERNAME env vars");
            System.exit(1);
        }
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new MyQuoteBot(token, username));
        System.out.println("Quotes bot started as @" + username);
    }

}
