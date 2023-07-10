package com.kamesuta.easydisplayeditor.tool;

import com.kamesuta.easydisplayeditor.PlayerSession;
import com.kamesuta.easydisplayeditor.pivot.Pivot;
import com.kamesuta.easydisplayeditor.pivot.PivotType;

/**
 * ピボットツール
 */
public class PivotTool implements Tool {
    /**
     * プレイヤーセッション
     */
    private final PlayerSession session;

    public PivotTool(PlayerSession session) {
        this.session = session;
    }

    @Override
    public ToolType getType() {
        return ToolType.PIVOT;
    }

    @Override
    public void onLeftClick() {
        if (session.pivotDisplay.mode == PivotType.NONE) {
            // スニーク中の場合、線ピボットを配置
            session.pivotDisplay.mode = PivotType.LINE;
        } else {
            // ピボットありの場合、ピボットを削除
            session.pivotDisplay.mode = PivotType.NONE;
        }

        // ピボットの座標をプレイヤーの座標に設定
        Pivot pivot = Pivot.fromPlayer(session.player, session.pivotDisplay.mode);
        // スニーク中の場合、マスにスナップする
        if (session.player.isSneaking()) {
            pivot = pivot.snap();
        }
        // ピボットの表示を更新
        session.pivotDisplay.updateDisplay(session.player, pivot);
        session.pivotDisplay.pivot = pivot;
    }

    @Override
    public void onRightClick() {
        if (session.pivotDisplay.mode == PivotType.NONE) {
            // スニーク中でない場合、点ピボットを配置
            session.pivotDisplay.mode = PivotType.POINT;
        } else {
            // ピボットありの場合、ピボットを削除
            session.pivotDisplay.mode = PivotType.NONE;
        }

        // ピボットの座標をプレイヤーの座標に設定
        Pivot pivot = Pivot.fromPlayer(session.player, session.pivotDisplay.mode);
        // スニーク中の場合、マスにスナップする
        if (session.player.isSneaking()) {
            pivot = pivot.snap();
        }
        // ピボットの表示を更新
        session.pivotDisplay.updateDisplay(session.player, pivot);
        session.pivotDisplay.pivot = pivot;
    }

    @Override
    public void onTick() {
        // ピボットの座標をプレイヤーの座標に設定
        Pivot pivot = Pivot.fromPlayer(session.player, session.pivotDisplay.mode);
        // スニーク中の場合、マスにスナップして表示を更新
        if (session.player.isSneaking()) {
            // スニーク中の場合、マスにスナップする
            pivot = pivot.snap();
            // プレビューの更新
            session.pivotDisplay.updatePreview(session.player, pivot);
        } else {
            // プレビューの削除
            session.pivotDisplay.removePreview();
        }
    }

    @Override
    public void onFinish() {
        // プレビューの削除
        session.pivotDisplay.removePreview();
    }
}
