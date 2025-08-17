package managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class PriceManager {

    private final JavaPlugin plugin;
    private File priceFile;
    private FileConfiguration priceConfig;

    public PriceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.priceFile = new File(plugin.getDataFolder(), "prices.yml");

        if (!priceFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs(); // crée le dossier plugin si nécessaire
                priceFile.createNewFile();       // crée prices.yml vide
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer prices.yml !");
                e.printStackTrace();
            }
        }

        priceConfig = YamlConfiguration.loadConfiguration(priceFile);
    }


    public void savePriceFile() {
        try {
            priceConfig.save(priceFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder prices.yml !");
            e.printStackTrace();
        }
    }

    public void setPrice(String itemID, double price) {
        priceConfig.set("items." + itemID + ".price", price);
        savePriceFile();
    }

    public double getPrice(String itemID) {
        return priceConfig.getDouble("items." + itemID + ".price", -1); // -1 si pas trouvé
    }

    public boolean hasPrice(String itemID) {
        return priceConfig.contains("items." + itemID + ".price");
    }

    public void sendPriceToPlayer(Player player, String itemID) {
        if (hasPrice(itemID)) {
            double price = getPrice(itemID);
            player.sendMessage(ChatColor.GREEN + "Prix de " + itemID + " : " + price);
        } else {
            player.sendMessage(ChatColor.RED + "Aucun prix défini pour " + itemID);
        }
    }
}