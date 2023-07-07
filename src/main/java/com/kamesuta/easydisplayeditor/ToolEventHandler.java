package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ToolEventHandler implements Listener {
    private boolean isGlowTick;

    public void tickSelection() {
//        PlayerSession.sessions.values().forEach(session -> {
//            session.selected.forEach(display -> {
//                display.setGlowing(isGlowTick);
//            });
//        });
//
//        isGlowTick = !isGlowTick;
    }

    public void tick() {
        // TODO: ツールの種類によって処理を分ける

        // デバッグ用にstartPosとendPosを表示する
        if (startPos != null && endPos != null) {
            // パーティクル表示
            startPos.getWorld().spawnParticle(Particle.ASH, startPos, 1, 0, 0, 0, 0);
            endPos.getWorld().spawnParticle(Particle.SPELL, endPos, 1, 0, 0, 0, 0);
        }

        // プレイヤーごとに更新
        PlayerSession.sessions.values().forEach(session -> {
            ToolType type = session.activeTool;
            switch (type) {
                case SELECTOR -> session.tickAreaSelectorTool();
                case GRAB -> session.tickGrabTool();
                default -> {
                }
            }
        });
    }

    public static Location startPos;
    public static Location endPos;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // プレイヤー
        Player player = event.getPlayer();

        ToolType type = ToolType.fromEvent(event);
        // ツールが有効でない場合は何もしない
        if (type == ToolType.NONE) {
            return;
        }
        // キャンセル
        event.setCancelled(true);

        // プレイヤーセッションを取得
        PlayerSession session = PlayerSession.get(player);
        // 他のツールが選択された場合、現在有効なツールを無効にする
        if (session.activeTool != type) {
            session.activeTool = ToolType.NONE;
        }

        // ツールの種類によって処理を分ける
        switch (type) {
            case SELECTOR -> {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK
                        || event.getAction() == Action.LEFT_CLICK_AIR) {
                    // 左クリックの場合範囲選択
                    session.areaSelectorTool();
                } else {
                    // 右クリックの場合選択
                    session.selectorTool();
                }
            }
            case GRAB -> {
                // 選択
                session.grabTool();
            }
            default -> {
            }
        }
    }
}
