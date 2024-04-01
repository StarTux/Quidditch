package com.cavetale.quidditch;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class QuidditchAdminCommand extends AbstractCommand<QuidditchPlugin> {
    protected QuidditchAdminCommand(final QuidditchPlugin plugin) {
        super(plugin, "quidditchadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Game Info")
            .senderCaller(this::info);
        rootNode.addChild("start").denyTabCompletion()
            .description("Start a game")
            .senderCaller(this::start);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop a game")
            .senderCaller(this::stop);
        rootNode.addChild("role").arguments("<player> <role>")
            .description("Change player role")
            .completers(CommandArgCompleter.NULL,
                        CommandArgCompleter.enumLowerList(QuidditchRole.class))
            .senderCaller(this::role);
    }

    private void info(CommandSender sender) {
        if (Quidditch.quidditch().getGames().isEmpty()) {
            throw new CommandWarn("No games active");
        }
        for (var game : Quidditch.quidditch().getGames()) {
            sender.sendMessage(text("Game " + game.getWorld().getName(), GRAY));
            for (var team : game.getTeams()) {
                sender.sendMessage(textOfChildren(team.getHouse().getDisplayName(), text(" " + team.getScore())));
                for (var player : game.getTeamPlayers(team)) {
                    sender.sendMessage(textOfChildren(text(player.getName()), text(" " + player.getRole().getHumanName())));
                }
            }
            sender.sendMessage(text("Snitch " + game.getSnitchVector(), GOLD));
        }
    }

    private void start(CommandSender sender) {
        if (!Quidditch.quidditch().getGames().isEmpty()) {
            throw new CommandWarn("Game already running!");
        }
        final var game = new QuidditchGame();
        game.setWorld(Bukkit.getWorld("quidditch"));
        Quidditch.quidditch().getGames().add(game);
        game.start();
        sender.sendMessage(text("Game started", YELLOW));
    }

    private void stop(CommandSender sender) {
        if (Quidditch.quidditch().getGames().isEmpty()) {
            throw new CommandWarn("No game is running!");
        }
        Quidditch.quidditch().disableAll();
        sender.sendMessage(text("Game stopped", YELLOW));
    }

    private boolean role(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final Player target = CommandArgCompleter.requirePlayer(args[0]);
        final QuidditchPlayer qp = QuidditchPlayer.find(target);
        if (qp == null) throw new CommandWarn("Not playing quidditch: " + target.getName());
        final QuidditchRole role = CommandArgCompleter.requireEnum(QuidditchRole.class, args[1]);
        if (qp.getRole() == role) {
            throw new CommandWarn(qp.getName() + " already has role " + role.getHumanName());
        }
        qp.setRole(role);
        sender.sendMessage(text("Role of " + qp.getName() + " changed to " + role.getHumanName(), YELLOW));
        return true;
    }
}
