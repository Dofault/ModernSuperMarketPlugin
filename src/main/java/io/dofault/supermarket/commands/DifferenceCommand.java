package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.PlayerListManager;
import io.dofault.supermarket.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DifferenceCommand implements SupermarketCommand {

    private final ShopManager shopManager;
    private final PlayerListManager playerListManager;

    public DifferenceCommand(ShopManager shopManager, PlayerListManager playerListManager) {
        this.shopManager = shopManager;
        this.playerListManager = playerListManager;
    }

    @Override
    public String getName() {
        return "difference";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (!playerListManager.isInShop(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vous devez être dans le shop !");
            return true;
        }

        shopManager.showInventoryDifference(player);
        return true;
    }
}
