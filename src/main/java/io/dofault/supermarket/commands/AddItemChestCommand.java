package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.LangManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AddItemChestCommand implements SupermarketCommand {

    private final ChestManager chestManager;
    private final LangManager lang;

    public AddItemChestCommand(LangManager lang, ChestManager chestManager) {
        this.chestManager = chestManager;
        this.lang = lang;
    }

    @Override
    public String getName() {
        return "additemchest";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(lang.get("shop-additemchest-usage"));

            return false;
        }

        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            player.sendMessage(lang.get("shop-invalid-item"));

            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get("shop-invalid-number"));

            return false;
        }

        chestManager.addItemToChest(player, material, amount);
        return true;
    }
}
