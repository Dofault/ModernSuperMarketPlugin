package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SaveCommand implements SupermarketCommand {

    private final ShopManager shopManager;

    public SaveCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (shopManager.saveShop()) {
            player.sendMessage(ChatColor.GREEN + "Shop sauvegardé !");
        } else {
            player.sendMessage(ChatColor.RED + "Vous devez d'abord définir l'entrée et la sortie !");
        }
        return true;
    }
}
