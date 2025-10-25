package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.SellManager;
import org.bukkit.entity.Player;

public class RemoveSellBlockCommand implements SupermarketCommand {

    private final SellManager sellManager;

    public RemoveSellBlockCommand(SellManager sellManager) {
        this.sellManager = sellManager;
    }

    @Override
    public String getName() {
        return "removesellblock";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        sellManager.removeSellBlock(player);
        return true;
    }
}
