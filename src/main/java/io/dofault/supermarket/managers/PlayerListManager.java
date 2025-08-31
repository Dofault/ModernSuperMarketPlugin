
package io.dofault.supermarket.managers;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;


import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class PlayerListManager {
    private final JavaPlugin plugin;
    private Connection connection;
    private final Set<UUID> playersInShop = new HashSet<>();

    public PlayerListManager(JavaPlugin plugin) {
        this.plugin = plugin;
        connect();
        createTable();
        loadPlayersFromDB();

    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/shop.db");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS shop_players (" +
                "uuid TEXT PRIMARY KEY, " +
                "inventory TEXT)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean canPlayerExit(Player player) {
        ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item == null) continue;

            if (item.getAmount() > 0) return false;
        }

        return true;
    }


    private void loadPlayersFromDB() {
        try {
            String sql = "SELECT uuid FROM shop_players";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    playersInShop.add(UUID.fromString(uuidStr));
                }
            }

            rs.close();
            ps.close();
            plugin.getLogger().info("[DEBUG] " + playersInShop.size() + " joueurs chargés depuis la base.");
        } catch (SQLException e) {
            plugin.getLogger().severe("[DEBUG] Erreur chargement joueurs depuis DB : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean enterShop(Player player) {
        if (!saveInventory(player, true)) {
            player.sendMessage("§cVous n'avez pas le droit d'entrer avec un BUNDLE !");
            return false; // on stoppe ici
        }
        playersInShop.add(player.getUniqueId());
        return true;
    }

    public void leaveShop(Player player) {
        playersInShop.remove(player.getUniqueId());
        loadInventory(player);
    }

    public boolean isInShop(UUID playerUUID) {
        return playersInShop.contains(playerUUID);
    }


    public ItemStack[] getSavedInventory(Player player) {
        try {
            String sql = "SELECT inventory FROM shop_players WHERE uuid=?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String data = rs.getString("inventory");
                if (data != null) {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(data);
                    List<ItemStack> savedItems = (List<ItemStack>) yaml.getList("items");
                    rs.close();
                    ps.close();
                    return savedItems.toArray(new ItemStack[0]);
                }
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG] Erreur récupération inventaire : " + e.getMessage());
            e.printStackTrace();
        }

        return new ItemStack[0]; // retourne un inventaire vide si aucun sauvegardé
    }

    public boolean saveInventory(Player player, boolean isTryingToEnter) {
        try {
            ItemStack[] contents = player.getInventory().getContents();

            // Vérifie s'il y a un bundle
            if(isTryingToEnter) {
                for (ItemStack item : contents) {
                    if (item == null) continue;
                    if (item.getType().getKey().getKey().contains("bundle")) {
                        plugin.getLogger().warning(player.getName() + " tried to enter with a bundle!");
                        return false; // interdit
                    }
                }
            }


            // Sérialisation via YamlConfiguration
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("items", Arrays.asList(contents));
            String data = yaml.saveToString();

            String sql = "INSERT OR REPLACE INTO shop_players(uuid, inventory) VALUES(?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, data);
            ps.executeUpdate();
            ps.close();

            plugin.getLogger().info("[DEBUG] Inventory of " + player.getName() + " saved.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG] Error saving inventory: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private void loadInventory(Player player) {
        try {
            String sql = "SELECT inventory FROM shop_players WHERE uuid=?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String data = rs.getString("inventory");
                if (data != null) {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(data);
                    List<ItemStack> items = (List<ItemStack>) yaml.getList("items");
                    player.getInventory().setContents(items.toArray(new ItemStack[0]));
                    plugin.getLogger().info("[DEBUG] Inventaire de " + player.getName() + " chargé.");

                    // Supprimer l'entrée de la DB après le chargement
                    String deleteSql = "DELETE FROM shop_players WHERE uuid=?";
                    PreparedStatement deletePs = connection.prepareStatement(deleteSql);
                    deletePs.setString(1, player.getUniqueId().toString());
                    deletePs.executeUpdate();
                    deletePs.close();
                    plugin.getLogger().info("[DEBUG] Entrée DB de " + player.getName() + " supprimee.");
                }
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG] Erreur chargement inventaire : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
