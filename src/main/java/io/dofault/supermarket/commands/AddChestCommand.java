package io.dofault.supermarket.commands;

import io.dofault.supermarket.managers.ChestManager;
import org.bukkit.entity.Player;

public class AddChestCommand implements SupermarketCommand {

    private final ChestManager chestManager;

    public AddChestCommand(ChestManager chestManager) {
        this.chestManager = chestManager;
    }

    @Override
    public String getName() {
        return "addchest";
    }

    @Override
    public boolean execute(Player player, String[] args) {
        chestManager.addChest(player);
        return true;
    }
}
