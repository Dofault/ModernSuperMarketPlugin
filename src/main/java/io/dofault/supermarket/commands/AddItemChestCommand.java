package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AddItemChestCommand implements SupermarketCommand {

    private final ChestManager chestManager;

    public AddItemChestCommand(ChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @Override
    public String getName() {
        return "additemchest";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /supermarket additemchest <item> <amount>");
            return false;
        }

        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            player.sendMessage(ChatColor.RED + "Item invalide !");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Nombre invalide !");
            return false;
        }

        chestManager.addItemToChest(player, material, amount);
        return true;
    }
}
