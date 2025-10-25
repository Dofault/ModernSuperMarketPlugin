package io.dofault.supermarket.managers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class LangManager {
    private final JavaPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();

    public LangManager(JavaPlugin plugin, String locale) {
        this.plugin = plugin;
        loadLanguage(locale);
    }

    private void loadLanguage(String locale) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            messages.put(key, ChatColor.translateAlternateColorCodes('&', config.getString(key, key)));
        }
    }

    public String get(String key, Map<String, String> placeholders) {
        String msg = messages.getOrDefault(key, key);
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return msg;
    }

    public String get(String key) {
        return get(key, null);
    }
}
