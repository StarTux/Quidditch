package com.cavetale.quidditch;

import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.bukkit.entity.Entity;

@Data
public final class QuidditchTeam {
    private final QuidditchGame game;
    private int ordinal;
    private QuidditchHouse house;
    private PlayerTeamQuery.Team queryTeam;
    private int score;
    private final List<Cuboid> goalAreas = new ArrayList<>();
    private Vec3i goalAverage = Vec3i.ZERO;
    private Entity goalDisplay;

    public PlayerTeamQuery.Team getQueryTeam() {
        if (queryTeam == null) {
            queryTeam = new PlayerTeamQuery.Team(game.getWorld().getName() + "/" + house.name().toLowerCase(),
                                                 house.getDisplayName(),
                                                 house.getTextColor());
        }
        return queryTeam;
    }

    public void addScore(int value) {
        score += value;
    }
}
