package com.cavetale.quidditch;

import com.cavetale.core.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public final class QuidditchCommand extends AbstractCommand<QuidditchPlugin> {
    protected QuidditchCommand(final QuidditchPlugin plugin) {
        super(plugin, "quidditch");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
    }

    protected boolean info(CommandSender sender, String[] args) {
        return false;
    }
}
