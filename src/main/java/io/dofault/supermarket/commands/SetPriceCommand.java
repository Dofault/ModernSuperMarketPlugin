package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.PriceManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SetPriceCommand implements SupermarketCommand {

    private final PriceManager priceManager;

    public SetPriceCommand(PriceManager priceManager) {
        this.priceManager = priceManager;
    }

    @Override
    public String getName() {
        return "setprice";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Usage: /supermarket setprice <item> <prix>");
            return true;
        }

        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            player.sendMessage(ChatColor.RED + "Item invalide !");
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Prix invalide !");
            return true;
        }

        priceManager.setPrice(material.name(), price);
        player.sendMessage(ChatColor.GREEN + "Prix de " + material.name() + " défini à " + price);
        return true;
    }
}
