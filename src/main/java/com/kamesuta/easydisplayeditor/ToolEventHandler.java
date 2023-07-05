package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
                case GRAB: {
                    session.tickGrabTool();
                }
                break;
                case ROTATE:
                    break;
                default:
                    break;
            }
        });
    }

    private Location startPos;
    private Location endPos;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // プレイヤー
        Player player = event.getPlayer();

        ToolType type = ToolType.fromEvent(event);
        // ツールが有効でない場合は何もしない
        if (type == ToolType.NONE) {
            return;
        }

        // プレイヤーセッションを取得
        PlayerSession session = PlayerSession.get(player);
        // 他のツールが選択された場合、現在有効なツールを無効にする
        if (session.activeTool != type) {
            session.activeTool = ToolType.NONE;
        }

        // ツールの種類によって処理を分ける
        switch (type) {
            case SELECTOR: {
                // 選択
                session.selectorTool();
            }
            break;
            case GRAB: {
                // 選択
                session.grabTool();
            }
            break;
            case DOT_PIVOT:
                break;
            case LINE_PIVOT:
                break;
            case ROTATE:
                break;
            case SCALE:
                break;
            default:
                break;
        }
    }
}
