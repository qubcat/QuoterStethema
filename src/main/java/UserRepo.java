import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserRepo {
    private final String url; // jdbc:sqlite:data/bot.db

    public UserRepo(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void ensureSchema() {
        try {
            Path p = Path.of("data");
            if (!Files.exists(p)) Files.createDirectories(p);
        } catch (Exception ignored) {}

        // Создаём таблицу, если её ещё нет (без поля phone — добавим ниже)
        String sql = """
        PRAGMA journal_mode=WAL;
        CREATE TABLE IF NOT EXISTS users(
          user_id     INTEGER PRIMARY KEY,
          username    TEXT,
          first_name  TEXT,
          last_name   TEXT,
          first_seen  INTEGER,
          last_seen   INTEGER,
          hits        INTEGER DEFAULT 0
        );
        """;
        try (Connection c = conn(); Statement st = c.createStatement()) {
            for (String part : sql.split(";")) {
                if (!part.isBlank()) st.execute(part);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // 🔧 Добавляем колонку phone, если её ещё нет
        ensurePhoneColumn();
    }



    /** Апдейт/инсерт при каждом событии от пользователя */
    public void upsertHit(long userId, String username, String firstName, String lastName) {
        long now = Instant.now().getEpochSecond();
        String sql = """
            INSERT INTO users(user_id, username, first_name, last_name, first_seen, last_seen, hits)
            VALUES(?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT(user_id) DO UPDATE SET
                username=excluded.username,
                first_name=excluded.first_name,
                last_name=excluded.last_name,
                last_seen=?,
                hits=users.hits+1;
            """;
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, username);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setLong(5, now);
            ps.setLong(6, now);
            ps.setLong(7, now);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countUsers() {
        try (Connection c = conn(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<UserStat> topByHits(int limit) {
        String sql = "SELECT user_id, username, first_name, last_name, phone, hits " +
                "FROM users ORDER BY hits DESC LIMIT ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<UserStat> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new UserStat(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("phone"),   // ← добавили телефон
                            rs.getInt("hits")
                    ));
                }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }


    public UserStat get(long userId) {
        String sql = "SELECT user_id, username, first_name, last_name, phone, hits FROM users WHERE user_id=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new UserStat(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getInt("hits")
                );
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void updatePhone(long userId, String phone) {
        String sql = "UPDATE users SET phone=? WHERE user_id=?"; // ← имя колонки
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone == null ? "—" : phone);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }


    /** ДТО для возврата статистики */
    public static class UserStat {
        public final long userId;
        public final String username, firstName, lastName, phone;
        public final int hits;
        public UserStat(long userId, String username, String firstName, String lastName, String phone, int hits) {
            this.userId = userId; this.username = username; this.firstName = firstName; this.lastName = lastName; this.phone = phone; this.hits = hits;
        }
        public String displayName() {
            if (username != null && !username.isBlank()) return "@"+username;
            String n = (firstName!=null?firstName:"") + (lastName!=null?(" "+lastName):"");
            n = n.trim();
            return n.isBlank() ? ("id:"+userId) : n;
        }
    }
    // Проверяет, есть ли в таблице нужная колонка
    private boolean columnExists(String table, String column) {
        String sql = "PRAGMA table_info(" + table + ")";
        try (Connection c = conn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null && name.equalsIgnoreCase(column)) return true;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Добавляет колонку phone, если её нет
    private void ensurePhoneColumn() {
        if (columnExists("users", "phone")) return; // если уже есть — ничего не делаем
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE users ADD COLUMN phone TEXT DEFAULT '—'");
            System.out.println("✅ Добавлена колонка 'phone' в таблицу users");
        } catch (SQLException e) {
            throw new RuntimeException("❌ Не удалось добавить колонку phone", e);
        }
    }

}
