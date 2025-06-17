package ru.corearchitect.coreeconomy.manager;

import org.bukkit.Bukkit;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionLogger {

    private final CoreEconomy plugin;
    private final File logFile;
    private PrintWriter writer;

    public TransactionLogger(CoreEconomy plugin) {
        this.plugin = plugin;
        File logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        this.logFile = new File(logsFolder, "transactions.log");
        try {
            this.writer = new PrintWriter(new FileWriter(logFile, true), true);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not initialize TransactionLogger!");
            e.printStackTrace();
        }
    }

    public void log(String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("[" + timestamp + "] " + message);
        });
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}