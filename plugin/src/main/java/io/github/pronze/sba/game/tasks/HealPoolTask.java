package io.github.pronze.sba.game.tasks;

import io.github.pronze.sba.config.SBAConfig;
import io.github.pronze.sba.game.Arena;
import io.github.pronze.sba.game.IArena;
import lombok.RequiredArgsConstructor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.screamingsandals.bedwars.Main;

@RequiredArgsConstructor
public class HealPoolTask implements Runnable {

    private final IArena arena;
    private final double radius = Math.pow(SBAConfig.getInstance().node("upgrades", "trap-detection-range").getInt(7), 2);

    @Override
    public void run() {
        if (!arena.getStorage().arePoolEnabled()) {
            return;
        }

        arena.getGame().getRunningTeams()
                .stream()
                .filter(arena.getStorage()::arePoolEnabled)
                .forEach(team -> team.getConnectedPlayers()
                        .stream()
                        .filter(player -> !Main.getPlayerGameProfile(player).isSpectator)
                        .forEach(player -> {
                            if (arena.getStorage().getTargetBlockLocation(team).orElseThrow().distanceSquared(player.getLocation()) <= radius) {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 1));
                            }
                        }));
    }
}
