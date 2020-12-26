package io.pronze.hypixelify.service;

import io.pronze.hypixelify.SBAHypixelify;
import io.pronze.hypixelify.api.party.PartyManager;
import io.pronze.hypixelify.api.service.WrapperService;
import io.pronze.hypixelify.game.PlayerData;
import io.pronze.hypixelify.game.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerWrapperService implements WrapperService<Player> {

    private final Map<UUID, PlayerWrapper> playerData = new HashMap<>();
    private final Map<UUID, BukkitTask> deletionTasks = new HashMap<>();

    public PlayerWrapperService() {
        Bukkit.getOnlinePlayers().stream().filter(Objects::nonNull).forEach(this::register);
    }

    public void handleOffline(Player player) {
        if (!playerData.containsKey(player.getUniqueId())) return;
        final var partyManager = SBAHypixelify.getPartyManager();
        final var playerDatabase = getWrapper(player);

        if(partyManager == null){
            unregister(player);
            return;
        }

        deletionTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(SBAHypixelify.getInstance(), () -> {
            partyManager.removeFromInvitedParty(player);
            if (playerDatabase.isInParty() && playerDatabase.getPartyLeader() != null) {
                partyManager.databaseDeletionFromParty(player, playerDatabase.getPartyLeader());
            }
            unregister(player);
            updateAll();
            deletionTasks.remove(player.getUniqueId());
        }, 20L * 60));
    }

    public PlayerWrapper getWrapper(Player player) {
        return playerData.get(player.getUniqueId());
    }

    public void updateAll() {
        if (playerData.isEmpty()) return;

        Bukkit.getScheduler().runTask(SBAHypixelify.getInstance(), ()->{
            playerData.values()
                    .forEach(PlayerWrapper::updateDatabase);
        });

    }

    @Override
    public void register(Player player) {
        playerData.put(player.getUniqueId(), new PlayerWrapper(player));
        SBAHypixelify.debug("Registered player: " + player.getName());
        cancelTasksIfExists(player);
    }

    public void cancelTasksIfExists(Player player){
        final UUID playerUUID = player.getUniqueId();

        if(deletionTasks.containsKey(playerUUID)) {
            final var deletionTask = deletionTasks.get(playerUUID);
            if(deletionTask != null && !deletionTask.isCancelled()) {
                try {
                    deletionTask.cancel();
                } catch (Throwable t) {
                    t.printStackTrace();
                    SBAHypixelify.debug("Could not cancel player task!");
                }
            }
        }
    }

    @Override
    public void unregister(Player player) {
        playerData.remove(player.getUniqueId());
        SBAHypixelify.debug("Unregistered player: " + player.getName());
    }
}
