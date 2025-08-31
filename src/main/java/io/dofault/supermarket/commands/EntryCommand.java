package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class EntryCommand implements SupermarketCommand {

    private final ShopManager shopManager;
    private final ChestManager chestManager;

    public EntryCommand(ShopManager shopManager, ChestManager chestManager) {
        this.shopManager = shopManager;
        this.chestManager = chestManager;
    }

    @Override
    public String getName() {
        return "entry";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        chestManager.reloadChests();
        shopManager.setEntryPosSetting(player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Position d'entrée enregistrée !");
        return true;
    }
}