package com.cavetale.quidditch;

import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Data
public final class QuidditchPlayer {
    private final UUID uuid;
    private final String name;
    private QuidditchTeam team;
    private QuidditchRole role;

    public QuidditchPlayer(final Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public static QuidditchPlayer find(Player player) {
        for (var game : Quidditch.quidditch().getGames()) {
            final var result = game.getPlayer(player);
            if (result != null) return result;
        }
        return null;
    }
}
