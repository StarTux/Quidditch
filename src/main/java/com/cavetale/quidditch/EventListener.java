package com.cavetale.quidditch;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.player.PlayerTeamQuery;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class EventListener implements Listener {
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, QuidditchPlugin.plugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    private void onPlayerTeamQuery(PlayerTeamQuery query) {
        for (var game : Quidditch.quidditch().getGames()) {
            for (var player : game.getPlayers().values()) {
                query.setTeam(player.getUuid(), player.getTeam().getQueryTeam());
            }
        }
    }

    @EventHandler
    private void onProjectileHit(ProjectileHitEvent event) {
        Quidditch.quidditch().apply(event.getEntity().getWorld(), game -> game.onProjectileHit(event));
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final var s = Quidditch.quidditch().apply(event.getPlayer().getWorld(), game -> game.onPlayerHud(event));
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        // No item dropping
        Quidditch.quidditch().apply(event.getPlayer().getWorld(), game -> {
                if (event.getPlayer().isOp()) return;
                event.setCancelled(true);
            });
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        // No armor changing
        Quidditch.quidditch().apply(event.getWhoClicked().getWorld(), game -> {
                if (event.getWhoClicked().isOp()) return;
                switch (event.getSlotType()) {
                case ARMOR:
                    event.setCancelled(true);
                default:
                    break;
                }
            });
    }

    @EventHandler
    private void onEntityPickupItem(EntityPickupItemEvent event) {
        Quidditch.quidditch().apply(event.getEntity().getWorld(), game -> game.onEntityPickupItem(event));
    }

    @EventHandler
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        Quidditch.quidditch().apply(event.getEntity().getWorld(), game -> game.onProjectileLaunch(event));
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Quidditch.quidditch().apply(event.getPlayer().getWorld(), game -> game.onPlayerInteract(event));
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Quidditch.quidditch().apply(event.getPlayer().getWorld(), game -> game.onPlayerInteractEntity(event));
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Quidditch.quidditch().apply(event.getEntity().getWorld(), game -> game.onEntityDamageByEntity(event));
    }
}
