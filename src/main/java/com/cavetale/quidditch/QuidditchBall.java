package com.cavetale.quidditch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * Runtime of a ball.
 */
@Data
@RequiredArgsConstructor
public final class QuidditchBall {
    private final QuidditchGame game;
    private final QuidditchBallType type;
    private final ThrowableProjectile entity;
    private final QuidditchPlayer thrower;
    private UUID target;
    private boolean dead;
    private Location oldLocation;
    private Location newLocation;
    private Vector velocity;
    private double speed;

    public void init() {
        newLocation = entity.getLocation();
        velocity = entity.getVelocity();
        speed = velocity.length();
    }

    public void tick() {
        if (!entity.isValid() || entity.isDead()) {
            dead = true;
            return;
        }
        oldLocation = newLocation;
        newLocation = entity.getLocation();
        if (!game.isInGameArea(newLocation)) {
            entity.remove();
            dead = true;
            return;
        }
        switch (type) {
        case BLUDGER:
            tickBludger();
            break;
        case QUAFFLE:
            tickQuaffle();
            break;
        default: break;
        }
    }

    private void tickBludger() {
        // Chase
        final Player targetPlayer;
        final var ballVector = newLocation.toVector();
        if (target == null) {
            // Find the player closest to the current trajectory
            List<Player> players = new ArrayList<>();
            for (var qp : game.getTeamPlayers(game.getOtherTeam(thrower.getTeam()))) {
                final var player = qp.getPlayer();
                if (player != null) players.add(player);
            }
            if (players.isEmpty()) return;
            Player newTarget = null;
            float minAngle = 0f;
            final var velo = entity.getVelocity();
            for (var player : players) {
                final var toVector = player.getEyeLocation().toVector().subtract(ballVector);
                final float angle = velo.angle(toVector);
                if (newTarget == null || angle < minAngle) {
                    newTarget = player;
                    minAngle = angle;
                }
            }
            target = newTarget.getUniqueId();
            targetPlayer = newTarget;
        } else {
            targetPlayer = Bukkit.getPlayer(target);
        }
        if (targetPlayer == null) {
            target = null;
            return;
        }
        final var newVelo = targetPlayer.getEyeLocation().toVector().subtract(ballVector)
            .normalize()
            .multiply(speed);
        entity.setVelocity(newVelo);
        final var dust = new Particle.DustOptions(thrower.getTeam().getHouse().getBukkitColor(), 1f);
        entity.getWorld().spawnParticle(Particle.DUST, newLocation, 1, 0.0, 0.0, 0.0, 0.0, dust);
    }

    private void tickQuaffle() {
        //entity.setVelocity(velocity.normalize().multiply(speed));
        entity.getWorld().spawnParticle(Particle.WAX_ON, newLocation, 1, 0.0, 0.0, 0.0, 0.0);
        final var direction = newLocation.toVector().subtract(oldLocation.toVector());
        if (direction.isZero()) return;
        final double length = direction.length();
        final BlockIterator iter = new BlockIterator(oldLocation.getWorld(), oldLocation.toVector(), direction, 0.0, Math.max(1, (int) (length * 2.0)));
        final Block goalBlock = newLocation.getBlock();
        while (iter.hasNext()) {
            final Block block = iter.next();
            final QuidditchTeam goalTeam = game.getGoalTeam(block);
            if (goalTeam != null) {
                game.goal(goalTeam, this);
                entity.remove();
                return;
            }
            if (block.equals(goalBlock)) break;
        }
    }
}
