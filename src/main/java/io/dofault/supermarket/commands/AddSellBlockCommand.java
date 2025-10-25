package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import io.dofault.supermarket.managers.LangManager;
import io.dofault.supermarket.managers.SellManager;

import org.bukkit.entity.Player;

public class AddSellBlockCommand implements SupermarketCommand {

    private final SellManager sellManager;

    public AddSellBlockCommand(SellManager sellManager) {
        this.sellManager = sellManager;
    }

    @Override
    public String getName() {
        return "addsellblock";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        sellManager.addSellBlock(player);
        return true;
    }
}
