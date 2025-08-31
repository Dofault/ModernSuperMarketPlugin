package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import org.bukkit.entity.Player;

public class RemoveChestCommand implements SupermarketCommand {

    private final ChestManager chestManager;

    public RemoveChestCommand(ChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @Override
    public String getName() {
        return "removechest";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        chestManager.removeChest(player);
        return true;
    }
}
