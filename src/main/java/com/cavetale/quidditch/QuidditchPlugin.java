package com.cavetale.quidditch;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class QuidditchPlugin extends JavaPlugin {
    protected static QuidditchPlugin instance;
    protected final QuidditchCommand quidditchCommand = new QuidditchCommand(this);
    protected final QuidditchAdminCommand quidditchAdminCommand = new QuidditchAdminCommand(this);
    protected final EventListener eventListener = new EventListener();
    protected final Quidditch quidditch = new Quidditch();

    public QuidditchPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        quidditchCommand.enable();
        quidditchAdminCommand.enable();
        eventListener.enable();
    }

    @Override
    public void onDisable() {
        quidditch.disable();
    }

    public static QuidditchPlugin plugin() {
        return instance;
    }
}
