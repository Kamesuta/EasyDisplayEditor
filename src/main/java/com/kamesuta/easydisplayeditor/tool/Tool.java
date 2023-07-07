package com.kamesuta.easydisplayeditor.tool;

/**
 * ツール
 */
public interface Tool {
    /**
     * ツールの種類を取得する
     *
     * @return ツールの種類
     */
    ToolType getType();

    /**
     * ツールを左クリックしたときの処理
     */
    default void onLeftClick() {
    }

    /**
     * ツールを右クリックしたときの処理
     */
    default void onRightClick() {
    }

    /**
     * Tickの処理
     */
    default void onTick() {
    }

    /**
     * ツールを使い終わったときの処理
     */
    default void onFinish() {
    }
}
