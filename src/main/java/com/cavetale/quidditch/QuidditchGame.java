package com.cavetale.quidditch;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import static com.cavetale.quidditch.QuidditchPlugin.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Data
public final class QuidditchGame {
    private QuidditchState state = QuidditchState.SETUP;
    private long now;
    private long stateStarted;
    private long stateDuration;
    private long gameStarted;
    private long gameDuration;
    private World world;
    private final List<QuidditchTeam> teams = new ArrayList<>();
    private final Map<UUID, QuidditchPlayer> players = new HashMap<>();
    private List<Cuboid> gameAreas = new ArrayList<>();
    private List<Cuboid> ballsAreas = new ArrayList<>();
    private List<Cuboid> snitchAreas = new ArrayList<>();
    private Set<Vec3i> snitchBlocks = new HashSet<>();
    private BukkitTask task;
    private final Random random = new Random();
    private final Map<UUID, QuidditchBall> balls = new HashMap<>();
    private Allay snitch;
    private long snitchCooldown;

    public void start() {
        makeTeams();
        loadAreas();
        loadPlayers();
        spawnStuff();
        task = Bukkit.getScheduler().runTaskTimer(plugin(), this::tick, 1L, 1L);
        gameStarted = System.currentTimeMillis();
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAllBalls();
        for (var player : players.values()) {
            final var entity = player.getPlayer();
            if (entity == null) continue;
            entity.leaveVehicle();
            entity.getInventory().clear();
        }
        for (var team : teams) {
            if (team.getGoalDisplay() != null) {
                team.getGoalDisplay().remove();
                team.setGoalDisplay(null);
            }
        }
        if (snitch != null) {
            snitch.remove();
            snitch = null;
        }
    }

    private void warn(String msg) {
        final var worldName = world != null ? world.getName() : "NOWORLD";
        plugin().getLogger().severe("[" + worldName + "] " + msg);
    }

    private void log(String msg) {
        final var worldName = world != null ? world.getName() : "NOWORLD";
        plugin().getLogger().info("[" + worldName + "] " + msg);
    }

    private void announce(Component msg) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(msg);
        }
    }

    private void announce(Title title) {
        for (Player player : world.getPlayers()) {
            player.showTitle(title);
        }
    }

    private void makeTeams() {
        List<QuidditchHouse> houses = new ArrayList<>(List.of(QuidditchHouse.values()));
        Collections.shuffle(houses);
        for (int i = 0; i < 2; i += 1) {
            QuidditchTeam team = new QuidditchTeam(this);
            team.setOrdinal(i);
            team.setHouse(houses.get(i));
            teams.add(team);
        }
    }

    private void loadAreas() {
        final AreasFile areasFile = AreasFile.load(world, "Quidditch");
        if (areasFile == null) {
            warn("No areas file");
            return;
        }
        List<Area> list;
        list = areasFile.getAreas().get("game");
        if (list == null || list.isEmpty()) {
            warn("No game areas!");
        } else {
            for (var it : list) {
                gameAreas.add(it.toCuboid());
            }
        }
        list = areasFile.getAreas().get("balls");
        if (list == null || list.isEmpty()) {
            warn("No balls areas!");
        } else {
            for (var it : list) {
                ballsAreas.add(it.toCuboid());
            }
        }
        list = areasFile.getAreas().get("snitch");
        if (list == null || list.isEmpty()) {
            warn("No snitch areas!");
        } else {
            for (var it : list) {
                snitchAreas.add(it.toCuboid());
            }
        }
        for (var snitchArea : snitchAreas) {
            for (var vec : snitchArea.enumerate()) {
                final var block = vec.toBlock(world);
                if (!block.isEmpty()) continue;
                snitchBlocks.add(vec);
            }
        }
        for (var team : teams) {
            list = areasFile.getAreas().get("goal" + team.getOrdinal());
            if (list == null || list.isEmpty()) {
                warn("No goal areas for team " + team.getOrdinal());
            } else {
                for (var it : list) {
                    team.getGoalAreas().add(it.toCuboid());
                }
                List<Vec3i> vecs = new ArrayList<>();
                for (var goal : team.getGoalAreas()) {
                    vecs.addAll(goal.enumerate());
                }
                int x = 0;
                int y = 0;
                int z = 0;
                final int size = vecs.size();
                for (var vec : vecs) {
                    x += vec.x;
                    y += vec.y;
                    z += vec.z;
                }
                team.setGoalAverage(Vec3i.of(x / size, y / size, z / size));
            }
        }
    }

    // /**
    //  * Change the colors of all blocks colored with the color of any
    //  * house into the new houses.
    //  * For example, this will turn Blue Terracotta into Red Terracotta
    //  * if the block is closest to Gryffindor's goal than the other
    //  * team's goal.
    //  */
    // private void colorize() {
    //     for (var gameArea : gameAreas) {
    //         for (var vec : gameArea) {
    //             final var block = vec.toBlock(world);
    //             final Material oldMaterial = block.getType();
    //             final BlockColor blockColor = BlockColor.of(oldMaterial);
    //             if (blockColor == null) continue;
    //             if (QuidditchHouse.ofBlockColor(blockColor) == null) continue;
    //             System.out.println("" + blockColor);
    //             final QuidditchTeam newTeam = vec.maxHorizontalDistance(teams.get(0).getGoalAverage()) < vec.maxHorizontalDistance(teams.get(1).getGoalAverage())
    //                 ? teams.get(0)
    //                 : teams.get(1);
    //             final BlockColor newColor = newTeam.getHouse().getBlockColor();
    //             final var oldSuffix = blockColor.suffixOf(oldMaterial);
    //             final Material newMaterial = newColor.getMaterial(oldSuffix);
    //             final BlockData newBlockData = newMaterial.createBlockData();
    //             block.getBlockData().copyTo(newBlockData);
    //             block.setBlockData(newBlockData, false);
    //         }
    //     }
    // }

    private void loadPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player.getUniqueId(), new QuidditchPlayer(player));
        }
        List<QuidditchPlayer> allPlayers = new ArrayList<>(players.values());
        Collections.shuffle(allPlayers);
        final int half = allPlayers.size() / 2;
        for (int i = 0; i < allPlayers.size(); i += 1) {
            allPlayers.get(i).setTeam(i < half ? teams.get(0) : teams.get(1));
        }
        for (var team : teams) {
            var list = new ArrayList<>(getPlayers(team));
            Collections.shuffle(list);
            for (int i = 0; i < list.size(); i += 1) {
                final var player = list.get(i);
                final var role = switch (i) {
                case 0 -> QuidditchRole.SEEKER;
                case 1 -> QuidditchRole.CHASER;
                case 2 -> QuidditchRole.BEATER;
                case 3 -> QuidditchRole.KEEPER;
                case 4 -> QuidditchRole.CHASER;
                case 5 -> QuidditchRole.BEATER;
                case 6 -> QuidditchRole.CHASER;
                // Complete
                default -> (i % 3 == 0
                            ? QuidditchRole.BEATER
                            : QuidditchRole.CHASER);
                };
                player.setRole(role);
                prepPlayer(player, player.getPlayer());
            }
        }
        state = QuidditchState.PLAY;
    }

    private void prepPlayer(QuidditchPlayer quidditchPlayer, Player player) {
        player.leaveVehicle();
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFallDistance(0);
        final var team = quidditchPlayer.getTeam();
        final var house = team.getHouse();
        final var role = quidditchPlayer.getRole();
        player.getEquipment().setHelmet(house.makeLeatherArmor(Material.LEATHER_HELMET));
        player.getEquipment().setChestplate(house.makeLeatherArmor(Material.LEATHER_CHESTPLATE));
        player.getEquipment().setLeggings(house.makeLeatherArmor(Material.LEATHER_LEGGINGS));
        player.getEquipment().setBoots(house.makeLeatherArmor(Material.LEATHER_BOOTS));
        player.getInventory().addItem(Mytems.WITCH_BROOM.createItemStack());
        final var color = house.getTextColor();
        player.sendMessage(empty());
        player.sendMessage(textOfChildren(text("You are a ", color),
                                          Mytems.WITCH_BROOM,
                                          text(role.getHumanName(), color),
                                          text(" for team ", color),
                                          house.getDisplayName()));
        switch (role) {
        case SEEKER:
            player.sendMessage(text("As the Seeker, you have to look for the Golden Snitch and catch it before the other team does, which earns you 150 points and ends the game.", GRAY));
            break;
        case CHASER:
            player.sendMessage(text("As the Chaser, you can pick up the Quaffle and throw it into your opponent's goal", GRAY));
            break;
        case BEATER:
            player.sendMessage(text("As the Beater, you can pick up the Bludger and throw it at the other team's Chasers", GRAY));
            break;
        case KEEPER:
            player.sendMessage(text("As the Keeper, you can body block the Quaffle and stop it from entering your goal.", GRAY));
            break;
        default: break;
        }
        player.sendMessage(empty());
        final var vec = randomSpawnVector();
        final var location = vec.toCenterFloorLocation(world);
        final var playerLocation = player.getLocation();
        location.setYaw(playerLocation.getYaw());
        location.setPitch(playerLocation.getPitch());
        player.teleport(location);
    }

    private void spawnStuff() {
        for (var team : teams) {
            final var loc = team.getGoalAverage().toCenterLocation(world).add(0.0, 8.0, 0.0);
            final var textDisplay = world.spawn(loc, TextDisplay.class, e -> {
                    e.setPersistent(false);
                    e.text(team.getHouse().getDisplayName());
                    e.setViewRange(128f);
                    final var scale = 8f;
                    e.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f),
                                                           new AxisAngle4f(0f, 0f, 0f, 0f),
                                                           new Vector3f(scale, scale, scale),
                                                           new AxisAngle4f(0f, 0f, 0f, 0f)));
                    e.setBillboard(Billboard.CENTER);
                    e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    e.setShadowed(true);
                });
            team.setGoalDisplay(textDisplay);
        }
    }

    private Vec3i randomSpawnVector() {
        final List<Vec3i> spawns = new ArrayList<>();
        for (var area : ballsAreas) {
            spawns.addAll(area.enumerate());
        }
        return spawns.get(random.nextInt(spawns.size()));
    }

    public void removeAllBalls() {
        for (Player player : world.getPlayers()) {
            for (int i = 0; i < player.getInventory().getSize(); i += 1) {
                final ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                if (QuidditchBallType.ofItem(item) != null) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ThrowableProjectile thrown) {
                final ItemStack item = thrown.getItem();
                if (item == null) continue;
                if (QuidditchBallType.ofItem(item) != null) {
                    entity.remove();
                }
            } else if (entity instanceof Item itemEntity) {
                final ItemStack item = itemEntity.getItemStack();
                if (QuidditchBallType.ofItem(item) != null) {
                    entity.remove();
                }
            }
        }
    }

    public int countBalls(QuidditchBallType type) {
        int result = 0;
        for (Player player : world.getPlayers()) {
            for (int i = 0; i < player.getInventory().getSize(); i += 1) {
                final ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                if (QuidditchBallType.ofItem(item) == type) {
                    result += 1;
                }
            }
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ThrowableProjectile thrown) {
                final ItemStack item = thrown.getItem();
                if (item == null) continue;
                if (QuidditchBallType.ofItem(item) == type) {
                    result += 1;
                }
            } else if (entity instanceof Item itemEntity) {
                final ItemStack item = itemEntity.getItemStack();
                if (QuidditchBallType.ofItem(item) == type) {
                    result += 1;
                }
            }
        }
        return result;
    }

    public boolean isInGameArea(Location location) {
        for (var area : gameAreas) {
            if (area.contains(location)) return true;
        }
        return false;
    }

    public QuidditchTeam getGoalTeam(Block block) {
        for (var team : teams) {
            for (var goal : team.getGoalAreas()) {
                if (goal.contains(block)) return team;
            }
        }
        return null;
    }

    public void goal(QuidditchTeam goalTeam, QuidditchBall quaffle) {
        if (state != QuidditchState.PLAY) return;
        final QuidditchTeam scoreTeam = getOtherTeam(goalTeam);
        scoreTeam.addScore(10);
        announce(empty());
        announce(textOfChildren(text(quaffle.getThrower().getName() + " scored a goal for ", GREEN),
                                scoreTeam.getHouse().getDisplayName()));
        announce(textOfChildren(text("10 points for ", scoreTeam.getHouse().getTextColor()),
                                scoreTeam.getHouse().getDisplayName(),
                                text("!", scoreTeam.getHouse().getTextColor())));
        announce(empty());
        announce(Title.title(text("10 points for ", scoreTeam.getHouse().getTextColor()),
                             scoreTeam.getHouse().getDisplayName()));
        state = QuidditchState.GOAL;
        log(quaffle.getThrower().getName() + " scored a goal for " + scoreTeam.getHouse());
    }

    private void tick() {
        now = System.currentTimeMillis();
        stateDuration = now - stateStarted;
        gameDuration = now - gameStarted;
        final var newState = switch (state) {
        case SETUP -> null;
        case PLAY -> tickPlay();
        case GOAL -> tickGoal();
        case GAME_OVER -> null;
        };
        for (var player : players.values()) {
            final var entity = player.getPlayer();
            if (entity == null) continue;
            if (!isInGameArea(entity.getLocation())) {
                prepPlayer(player, entity);
            }
        }
        if (newState != null && newState != state) {
            changeState(newState);
        }
    }

    private void changeState(QuidditchState newState) {
        state = newState;
        stateStarted = System.currentTimeMillis();
        stateDuration = 0L;
    }

    private QuidditchState tickPlay() {
        // Tick balls
        for (var iter = balls.entrySet().iterator(); iter.hasNext();) {
            final var entry = iter.next();
            final var ball = entry.getValue();
            ball.tick();
            if (ball.isDead()) {
                iter.remove();
            }
        }
        final int bludgerCount = countBalls(QuidditchBallType.BLUDGER);
        if (bludgerCount < 2) {
            final var loc = randomSpawnVector().toCenterLocation(world);
            world.dropItem(loc, QuidditchBallType.BLUDGER.createItemStack());
        }
        final int quaffleCount = countBalls(QuidditchBallType.QUAFFLE);
        if (quaffleCount < 1) {
            final var loc = randomSpawnVector().toCenterLocation(world);
            world.dropItem(loc, QuidditchBallType.QUAFFLE.createItemStack());
        }
        // Snitch stuff
        if (snitch != null) {
            if (!snitch.isValid() || snitch.isDead()) {
                snitch = null;
                log("Snitch despawned");
            } else if (!isInGameArea(snitch.getLocation())) {
                snitch.remove();
                snitch = null;
            } else {
                world.spawnParticle(Particle.END_ROD, snitch.getLocation(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (now >= snitchCooldown) {
            if (snitch != null) {
                snitch.remove();
                snitch = null;
            }
            snitchCooldown = now + 60_000L;
            // final int snitchChance = (int) (gameDuration / 60_000L);
            // final int snitchRoll = random.nextInt(5);
            // final var snitchSuccess = snitchRoll < snitchChance;
            // plugin().getLogger().info("Snitch " + snitchRoll + "/" + snitchChance + " => " + snitchSuccess);
            final var snitchSuccess = true;
            if (snitchSuccess) {
                final var list = new ArrayList<Vec3i>(snitchBlocks);
                final var vec = list.get(random.nextInt(list.size()));
                log("Snitch " + vec);
                snitch = world.spawn(vec.toCenterLocation(world), Allay.class, e -> {
                        e.setPersistent(false);
                        e.setRemoveWhenFarAway(false);
                        e.getAttribute(Attribute.GENERIC_FLYING_SPEED).setBaseValue(0.3);
                    });
            }
        }
        return null;
    }

    private QuidditchState tickGoal() {
        if (stateDuration >= 10_000L) {
            removeAllBalls();
            return QuidditchState.PLAY;
        }
        return null;
    }

    public QuidditchTeam getTeam(int ordinal) {
        return teams.get(ordinal);
    }

    public QuidditchTeam getOtherTeam(QuidditchTeam team) {
        for (var it : teams) {
            if (it != team) return it;
        }
        return null;
    }

    public List<QuidditchPlayer> getTeamPlayers(QuidditchTeam team) {
        final var result = new ArrayList<QuidditchPlayer>();
        for (var player : players.values()) {
            if (player.getTeam() == team) result.add(player);
        }
        return result;
    }

    public List<QuidditchPlayer> getPlayers(QuidditchTeam team) {
        final var result = new ArrayList<QuidditchPlayer>();
        for (var player : players.values()) {
            if (player.getTeam() == team) result.add(player);
        }
        return result;
    }

    public QuidditchPlayer getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public void onProjectileHit(ProjectileHitEvent event) {
        switch (state) {
        case PLAY:
        case GOAL:
            if (event.getEntity() instanceof ThrowableProjectile thrown) {
                onProjectileHit(event, thrown);
            }
            break;
        default: break;
        }
    }

    private void onProjectileHit(ProjectileHitEvent event, ThrowableProjectile thrown) {
        final var item = thrown.getItem();
        if (item == null) return;
        final var type = QuidditchBallType.ofItem(item);
        if (type == null) return;
        event.setCancelled(true);
        final var dropped = thrown.getWorld().dropItem(thrown.getLocation(), type.createItemStack());
        dropped.setPickupDelay(0);
        dropped.setPersistent(false);
        thrown.remove();
        if (event.getHitEntity() instanceof Player target) {
            final var quidditchPlayer = getPlayer(target);
            if (quidditchPlayer == null) {
                return;
            }
            final var role = quidditchPlayer.getRole();
            if (type == QuidditchBallType.BLUDGER && role == QuidditchRole.CHASER) {
                target.leaveVehicle();
                for (var i = 0; i < target.getInventory().getSize(); i += 1) {
                    final var hasItem = target.getInventory().getItem(i);
                    if (hasItem == null) continue;
                    if (QuidditchBallType.ofItem(hasItem) == null) continue;
                    target.getInventory().setItem(i, null);
                }
                world.spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation(), 8, 1.0, 1.0, 1.0, 0.0);
                world.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            } else if (type == QuidditchBallType.BLUDGER && role == QuidditchRole.BEATER) {
                target.leaveVehicle();
            } else if (type == QuidditchBallType.BLUDGER && role == QuidditchRole.CHASER) {
                target.leaveVehicle();
            }
        }
    }

    public void onPlayerHud(PlayerHudEvent event) {
        final var quidditchPlayer = getPlayer(event.getPlayer());
        List<Component> boss = new ArrayList<>();
        if (quidditchPlayer != null && quidditchPlayer.getTeam().getOrdinal() == 0) {
            boss.add(textOfChildren(Mytems.WITCH_BROOM,
                                    text(quidditchPlayer.getRole().getHumanName(), teams.get(0).getHouse().getTextColor()),
                                    Mytems.WITCH_BROOM));
        }
        boss.add(teams.get(0).getHouse().getDisplayName());
        boss.add(text(teams.get(0).getScore(), teams.get(0).getHouse().getTextColor()));
        boss.add(text(":", GRAY));
        boss.add(text(teams.get(1).getScore(), teams.get(1).getHouse().getTextColor()));
        boss.add(teams.get(1).getHouse().getDisplayName());
        if (quidditchPlayer != null && quidditchPlayer.getTeam().getOrdinal() == 1) {
            boss.add(textOfChildren(Mytems.WITCH_BROOM,
                                    text(quidditchPlayer.getRole().getHumanName(), teams.get(1).getHouse().getTextColor()),
                                    Mytems.WITCH_BROOM));
        }
        event.bossbar(PlayerHudPriority.HIGH, join(separator(space()), boss), BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, 1f);
    }

    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (state != QuidditchState.PLAY) {
            event.setCancelled(true);
            return;
        }
        final QuidditchPlayer quidditchPlayer = getPlayer(player);
        if (quidditchPlayer == null) {
            event.setCancelled(true);
            return;
        }
        final var type = QuidditchBallType.ofItem(event.getItem().getItemStack());
        if (type == null) {
            event.setCancelled(true);
            return;
        }
        final QuidditchRole role = quidditchPlayer.getRole();
        if (role == QuidditchRole.CHASER && type == QuidditchBallType.QUAFFLE) {
            // Chasers can pick up the quaffle
            return;
        } else if (role == QuidditchRole.BEATER && type == QuidditchBallType.BLUDGER) {
            // Beaters can pick up the bludger
            return;
        } else {
            // Nobody else can pick up anything
            event.setCancelled(true);
        }
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrowableProjectile thrown)) {
            event.setCancelled(true);
            return;
        }
        if (!(thrown.getShooter() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        final var quidditchPlayer = getPlayer(player);
        if (quidditchPlayer == null) {
            event.setCancelled(true);
            return;
        }
        final QuidditchBallType type = QuidditchBallType.ofItem(thrown.getItem());
        if (type == null) {
            event.setCancelled(true);
            return;
        }
        if (!player.isInsideVehicle()) {
            event.setCancelled(true);
            return;
        }
        thrown.setPersistent(false);
        final var ball = new QuidditchBall(this, type, thrown, quidditchPlayer);
        ball.init();
        balls.put(thrown.getUniqueId(), ball);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        onClick(player, event.getEntity());
    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        final Player player = event.getPlayer();
        onClick(player, event.getRightClicked());
    }

    private void onClick(Player player, Entity clicked) {
        final QuidditchPlayer quidditchPlayer = getPlayer(player);
        if (quidditchPlayer == null) {
            return;
        }
        if (clicked == snitch && quidditchPlayer.getRole() == QuidditchRole.SEEKER) {
            if (state != QuidditchState.PLAY && state != QuidditchState.GOAL) return;
            if (snitch != null) {
                snitch.remove();
                snitch = null;
            }
            final var team = quidditchPlayer.getTeam();
            state = QuidditchState.GAME_OVER;
            quidditchPlayer.getTeam().addScore(150);
            announce(empty());
            announce(textOfChildren(text(player.getName() + " caught the Golden Snitch for ", GOLD),
                                    team.getHouse().getDisplayName()));
            announce(textOfChildren(text("150 points for ", team.getHouse().getTextColor()),
                                    team.getHouse().getDisplayName(),
                                    text("!", team.getHouse().getTextColor())));
            log(player.getName() + " caught the Golden Snitch for " + team.getHouse());
            if (teams.get(0).getScore() == teams.get(1).getScore()) {
                announce(text("The game is a Draw!", RED));
                announce(Title.title(text("Draw", RED), empty()));
                log("Draw");
            } else {
                final var winner = teams.get(0).getScore() > teams.get(1).getScore()
                    ? teams.get(0)
                    : teams.get(1);
                announce(Title.title(winner.getHouse().getDisplayName(),
                                     text(" wins the game!", GOLD)));
                announce(textOfChildren(winner.getHouse().getDisplayName(), text(" wins the game!", GOLD)));
                log(winner.getHouse() + " wins the game");
            }
            announce(empty());
        }
    }

    public Vec3i getSnitchVector() {
        return snitch != null
            ? Vec3i.of(snitch.getLocation())
            : null;
    }
}
