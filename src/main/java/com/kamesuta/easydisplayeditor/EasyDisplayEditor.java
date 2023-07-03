package com.kamesuta.easydisplayeditor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class EasyDisplayEditor extends JavaPlugin {
    // インスタンス
    public static EasyDisplayEditor instance;

    // ロガー
    public static Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // リフレクションの初期化
        try {
            BlockOutline.init();
        } catch (RuntimeException | ReflectiveOperationException e) {
            logger.log(Level.WARNING, "ブロックアウトラインの取得に失敗しました。BlockDisplayの選択はブロックアウトラインではなくブロックサイズの当たり判定で行われます。", e);
        }

        // Plugin startup logic
        ToolEventHandler handler = new ToolEventHandler();
        getServer().getPluginManager().registerEvents(handler, this);
        getServer().getScheduler().runTaskTimer(this, handler::tick, 0, 1);
        getServer().getScheduler().runTaskTimer(this, handler::tickSelection, 0, 10);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /ede give <player> <tool>
        if (command.getName().equalsIgnoreCase("ede")) {
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                getServer().selectEntities(sender, args[1]).forEach(entity -> {
                    // プレイヤー以外は無視
                    if (!entity.getType().isAlive()) {
                        return;
                    }
                    Player player = (Player) entity;

                    // ツールを渡す
                    ToolType type = ToolType.fromName(args[2]);
                    if (type != null) {
                        player.getInventory().addItem(type.createItem());
                    }
                });
            }
        }

        return true;
    }
}
