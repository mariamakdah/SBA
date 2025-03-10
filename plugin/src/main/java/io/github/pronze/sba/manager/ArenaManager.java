package io.github.pronze.sba.manager;

import io.github.pronze.sba.game.Arena;
import io.github.pronze.sba.game.IArena;
import io.github.pronze.sba.game.IGameStorage;
import io.github.pronze.sba.utils.Logger;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.lib.plugin.ServiceManager;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArenaManager implements IArenaManager {

    public static ArenaManager getInstance() {
        return ServiceManager.get(ArenaManager.class);
    }

    @Getter
    private final Map<String, IArena> arenaMap = new HashMap<>();

    public List<IArena> getRegisteredArenas() {
        return List.copyOf(arenaMap.values());
    }

    @Override
    public IArena createArena(@NotNull Game game) {
        final var gameName = game.getName();
        if (arenaMap.containsKey(gameName)) {
            throw new UnsupportedOperationException("Arena: " + gameName + " already exists!");
        }
        Logger.trace("Creating arena for game: {}", gameName);
        final var arena = new Arena(game);
        arenaMap.put(gameName, arena);
        return arena;
    }

    @Override
    public void removeArena(@NotNull Game game) {
        Logger.trace("Removing arena for game: {}", game.getName());
        arenaMap.remove(game.getName());
    }

    @Override
    public Optional<IArena> get(String gameName) {
        return Optional.ofNullable(arenaMap.get(gameName));
    }

    @Override
    public Optional<IGameStorage> getGameStorage(String gameName) {
        if (!arenaMap.containsKey(gameName)) {
            return Optional.empty();
        }
        return Optional.of(arenaMap.get(gameName).getStorage());
    }
}
