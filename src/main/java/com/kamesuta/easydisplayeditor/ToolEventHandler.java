package com.kamesuta.easydisplayeditor;

import com.kamesuta.easydisplayeditor.tool.ToolType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * ツールイベントハンドラー
 */
public class ToolEventHandler implements Listener {
    public static Location startPos;
    public static Location endPos;

    /**
     * Tickの処理
     */
    public void tick() {
        // デバッグ用にstartPosとendPosを表示する
        if (startPos != null && endPos != null) {
            // パーティクル表示
            startPos.getWorld().spawnParticle(Particle.ASH, startPos, 1, 0, 0, 0, 0);
            endPos.getWorld().spawnParticle(Particle.SPELL, endPos, 1, 0, 0, 0, 0);
        }

        // プレイヤーごとに更新
        PlayerSession.sessions.values().forEach(PlayerSession::onTick);
    }

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

        // 左クリックか
        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_AIR;

        // 呼び出し
        if (isLeftClick) {
            session.onLeftClick(type);
        } else {
            session.onRightClick(type);
        }
    }

    @EventHandler
    public void onItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // ツールの種類を取得
        ToolType type = ToolType.fromItemStack(player.getInventory().getItem(event.getNewSlot()));

        // プレイヤーセッションを取得
        PlayerSession session = PlayerSession.get(player);

        // 呼び出し
        session.onItemChange(type);
    }
}
