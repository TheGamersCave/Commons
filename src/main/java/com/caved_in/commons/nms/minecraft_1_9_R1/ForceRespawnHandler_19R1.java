package com.caved_in.commons.nms.minecraft_1_9_R1;

import com.caved_in.commons.nms.ForceRespawnHandler;
import org.bukkit.entity.Player;

import java.util.ConcurrentModificationException;

public class ForceRespawnHandler_19R1 implements ForceRespawnHandler {
	@Override
	public void forceRespawn(Player player) throws ConcurrentModificationException {
		player.spigot().respawn();
	}
}
