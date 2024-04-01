package com.cavetale.quidditch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Data;
import org.bukkit.World;

@Data
public final class Quidditch {
    private final List<QuidditchGame> games = new ArrayList<>();

    protected void disable() {
        disableAll();
    }

    public void disableAll() {
        for (QuidditchGame game : games) {
            game.disable();
        }
        games.clear();
    }

    public static Quidditch quidditch() {
        return QuidditchPlugin.plugin().getQuidditch();
    }

    public boolean apply(World world, Consumer<QuidditchGame> callback) {
        for (var game : games) {
            if (world.equals(game.getWorld())) {
                callback.accept(game);
                return true;
            }
        }
        return false;
    }
}
