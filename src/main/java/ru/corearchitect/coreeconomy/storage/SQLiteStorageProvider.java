package ru.corearchitect.coreeconomy.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.model.TransactionRecord;
import ru.corearchitect.coreeconomy.model.TransactionType;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteStorageProvider implements StorageProvider {

    private final CoreEconomy plugin;
    private Connection connection;

    private static final String CREATE_PLAYERS_TABLE = "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, balance TEXT NOT NULL, is_frozen BOOLEAN NOT NULL, scoreboard_enabled BOOLEAN NOT NULL);";
    private static final String CREATE_STATS_TABLE = "CREATE TABLE IF NOT EXISTS economy_stats (stat_key VARCHAR(255) PRIMARY KEY, stat_value TEXT NOT NULL);";
    private static final String CREATE_TRANSACTIONS_TABLE = "CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp DATETIME NOT NULL, initiator_name VARCHAR(16) NOT NULL, target_name VARCHAR(16) NOT NULL, transaction_type VARCHAR(32) NOT NULL, amount TEXT NOT NULL, commission TEXT NOT NULL);";

    public SQLiteStorageProvider(CoreEconomy plugin) {
        this.plugin = plugin;
        connect();
        initializeDatabase();
    }

    private void connect() {
        File databaseFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSQLiteFilename());
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not connect to SQLite database!");
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_PLAYERS_TABLE);
            statement.execute(CREATE_STATS_TABLE);
            statement.execute(CREATE_TRANSACTIONS_TABLE);
            statement.execute("INSERT OR IGNORE INTO economy_stats (stat_key, stat_value) VALUES ('total_commission', '0.0');");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create database tables!");
            e.printStackTrace();
        }
    }

    @Override
    public void createAccount(UUID uuid) {
        String sql = "INSERT OR IGNORE INTO players(uuid, balance, is_frozen, scoreboard_enabled) VALUES(?,?,?,?);";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, "0.0");
            pstmt.setBoolean(3, false);
            pstmt.setBoolean(4, plugin.getConfigManager().isScoreboardEnabledByDefault());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        String sql = "SELECT uuid FROM players WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public BigDecimal getBalance(UUID uuid) {
        String sql = "SELECT balance FROM players WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BigDecimal(rs.getString("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void setBalance(UUID uuid, BigDecimal amount) {
        String sql = "UPDATE players SET balance = ? WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, amount.toPlainString());
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isFrozen(UUID uuid) {
        String sql = "SELECT is_frozen FROM players WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_frozen");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setFrozen(UUID uuid, boolean frozen) {
        String sql = "UPDATE players SET is_frozen = ? WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, frozen);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getScoreboardState(UUID uuid) {
        String sql = "SELECT scoreboard_enabled FROM players WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("scoreboard_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plugin.getConfigManager().isScoreboardEnabledByDefault();
    }

    @Override
    public void setScoreboardState(UUID uuid, boolean enabled) {
        String sql = "UPDATE players SET scoreboard_enabled = ? WHERE uuid = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, BigDecimal> getAllBalances() {
        Map<UUID, BigDecimal> allBalances = new ConcurrentHashMap<>();
        String sql = "SELECT uuid, balance FROM players;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                BigDecimal balance = new BigDecimal(rs.getString("balance"));
                allBalances.put(uuid, balance);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allBalances;
    }

    @Override
    public void addCommission(BigDecimal amount) {
        String sql = "UPDATE economy_stats SET stat_value = CAST(stat_value AS REAL) + ? WHERE stat_key = 'total_commission';";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, amount.toPlainString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BigDecimal getTotalCommission() {
        String sql = "SELECT stat_value FROM economy_stats WHERE stat_key = 'total_commission';";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new BigDecimal(rs.getString("stat_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateTotalBalance() {
        String sql = "SELECT SUM(CAST(balance AS REAL)) FROM players;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return BigDecimal.valueOf(rs.getDouble(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void saveData() {
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean migrateFrom(FileConfiguration oldConfig) {
        String insertSql = "INSERT OR REPLACE INTO players(uuid, balance, is_frozen, scoreboard_enabled) VALUES(?,?,?,?);";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);

            ConfigurationSection balancesSection = oldConfig.getConfigurationSection("balances");
            if (balancesSection != null) {
                for (String key : balancesSection.getKeys(false)) {
                    UUID uuid = UUID.fromString(key);
                    String balance = balancesSection.getString(key, "0.0");
                    boolean isFrozen = oldConfig.getBoolean("frozen_accounts." + key, false);
                    boolean scoreboardEnabled = oldConfig.getBoolean("scoreboard_states." + key, plugin.getConfigManager().isScoreboardEnabledByDefault());

                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, balance);
                    pstmt.setBoolean(3, isFrozen);
                    pstmt.setBoolean(4, scoreboardEnabled);
                    pstmt.addBatch();
                }
            }

            pstmt.executeBatch();
            connection.commit();

            String commission = oldConfig.getString("economy_stats.total_commission", "0.0");
            String updateCommissionSql = "UPDATE economy_stats SET stat_value = ? WHERE stat_key = 'total_commission';";
            try (PreparedStatement commissionPstmt = connection.prepareStatement(updateCommissionSql)) {
                commissionPstmt.setString(1, commission);
                commissionPstmt.executeUpdate();
            }

            return true;
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void logTransaction(TransactionRecord record) {
        String sql = "INSERT INTO transactions(timestamp, initiator_name, target_name, transaction_type, amount, commission) VALUES(?,?,?,?,?,?);";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(record.getTimestamp()));
            pstmt.setString(2, record.getInitiatorName());
            pstmt.setString(3, record.getTargetName());
            pstmt.setString(4, record.getTransactionType().name());
            pstmt.setString(5, record.getAmount().toPlainString());
            pstmt.setString(6, record.getCommission().toPlainString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<TransactionRecord>> getPlayerHistory(UUID playerUuid, int page) {
        return CompletableFuture.supplyAsync(() -> {
            List<TransactionRecord> history = new ArrayList<>();
            String playerName = Bukkit.getOfflinePlayer(playerUuid).getName();
            int limit = plugin.getConfigManager().getHistoryEntriesPerPage();
            int offset = (page - 1) * limit;

            String sql = "SELECT * FROM transactions WHERE initiator_name = ? OR target_name = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?;";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, playerName);
                pstmt.setInt(3, limit);
                pstmt.setInt(4, offset);

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    history.add(new TransactionRecord(
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("initiator_name"),
                            rs.getString("target_name"),
                            TransactionType.valueOf(rs.getString("transaction_type")),
                            new BigDecimal(rs.getString("amount")),
                            new BigDecimal(rs.getString("commission"))
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return history;
        });
    }

    @Override
    public CompletableFuture<Integer> countPlayerHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String playerName = Bukkit.getOfflinePlayer(playerUuid).getName();
            String sql = "SELECT COUNT(id) FROM transactions WHERE initiator_name = ? OR target_name = ?;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, playerName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }
}