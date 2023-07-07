package com.kamesuta.easydisplayeditor;

import com.kamesuta.easydisplayeditor.tool.Tool;
import com.kamesuta.easydisplayeditor.tool.ToolType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーのセッション
 */
public class PlayerSession {
    /**
     * プレイヤーのセッションデータ
     */
    public static final Map<UUID, PlayerSession> sessions = new HashMap<>();

    /**
     * プレイヤーのセッションを取得する
     *
     * @param player プレイヤー
     * @return プレイヤーのセッション
     */
    public static PlayerSession get(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), (s) -> new PlayerSession(player));
    }

    /**
     * 選択中のブロックディスプレイ -> 最初の行列
     */
    public final Map<BlockDisplay, Matrix4f> selected = new HashMap<>();

    /**
     * プレイヤー
     */
    public final Player player;

    /**
     * 現在有効なツール
     */
    public ToolType activeToolType = ToolType.NONE;

    /**
     * ツール
     */
    public final Map<ToolType, Tool> tools;

    /**
     * コンストラクター
     *
     * @param player プレイヤー
     */
    public PlayerSession(Player player) {
        this.player = player;
        this.tools = ToolType.createTools(this);
    }

    /**
     * ツールを左クリックしたときの処理
     */
    public void onLeftClick(ToolType toolType) {
        if (activeToolType != toolType) {
            Tool tool = tools.get(activeToolType);
            if (tool != null) {
                tool.onFinish();
            }
        }

        Tool tool = tools.get(toolType);
        if (tool != null) {
            tool.onLeftClick();
        }

        activeToolType = toolType;
    }

    /**
     * ツールを右クリックしたときの処理
     */
    public void onRightClick(ToolType toolType) {
        if (activeToolType != toolType) {
            Tool tool = tools.get(activeToolType);
            if (tool != null) {
                tool.onFinish();
            }
        }

        Tool tool = tools.get(toolType);
        if (tool != null) {
            tool.onRightClick();
        }

        activeToolType = toolType;
    }

    /**
     * Tickの処理
     */
    public void onTick() {
        Tool tool = tools.get(activeToolType);
        if (tool != null) {
            tool.onTick();
        }
    }

    /**
     * アイテムを切り替えたときの処理
     *
     * @param toolType ツールの種類
     */
    public void onItemChange(ToolType toolType) {
        if (activeToolType != toolType) {
            Tool tool = tools.get(activeToolType);
            if (tool != null) {
                tool.onFinish();
            }
        }

        activeToolType = toolType;
    }
}
