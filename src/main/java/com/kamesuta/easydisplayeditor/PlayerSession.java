package com.kamesuta.easydisplayeditor;

import com.kamesuta.easydisplayeditor.tool.Tool;
import com.kamesuta.easydisplayeditor.tool.ToolType;
import com.kamesuta.easydisplayeditor.pivot.PivotDisplay;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * プレイヤーのセッション
 */
public class PlayerSession {
    /**
     * プレイヤーのセッションデータ
     */
    public static final Map<UUID, PlayerSession> sessions = new HashMap<>();
    /**
     * 選択中のブロックディスプレイ
     */
    public final Set<BlockDisplay> selected = new HashSet<>();
    /**
     * プレイヤー
     */
    public final Player player;
    /**
     * ツール
     */
    public final Map<ToolType, Tool> tools;
    /**
     * 現在有効なツール
     */
    public ToolType activeToolType = ToolType.NONE;
    /**
     * ピボット
     */
    public PivotDisplay pivotDisplay = new PivotDisplay();

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
     * プレイヤーのセッションを取得する
     *
     * @param player プレイヤー
     * @return プレイヤーのセッション
     */
    public static PlayerSession get(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), (s) -> new PlayerSession(player));
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

    /**
     * プラグインが無効になったときの処理
     */
    public void onDisable() {
        // ツールの終了処理
        Tool tool = tools.get(activeToolType);
        if (tool != null) {
            tool.onFinish();
        }

        // ピボットの終了処理
        pivotDisplay.onDisable();

        // 選択中のブロックディスプレイの発光を消す
        selected.forEach(display -> display.setGlowing(false));
    }
}
