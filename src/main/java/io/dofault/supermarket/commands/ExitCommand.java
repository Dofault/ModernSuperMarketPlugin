package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ExitCommand implements SupermarketCommand {

    private final ShopManager shopManager;

    public ExitCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        shopManager.setExitPosSetting(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Position de sortie du shop enregistrée !");
        return true;
    }
}
