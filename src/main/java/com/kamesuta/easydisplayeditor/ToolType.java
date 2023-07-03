package com.kamesuta.easydisplayeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * ツールの種類
 */
public enum ToolType {
    /**
     * 何もしない
     */
    NONE,
    /**
     * 選択 (左クリック: 選択解除、右クリック: 選択)
     */
    SELECTOR,
    /**
     * 範囲選択
     */
    RECT_SELECTOR,
    /**
     * 移動、回転 (左クリック: ピボットに従って一方向移動回転、右クリック: 全方向移動回転)
     */
    GRAB,
    /**
     * ドットピボット (左クリック: ピボットを解除、右クリック: ピボットを設定、Shiftでスナップ)
     */
    DOT_PIVOT,
    /**
     * 線ピボット (左クリック: ピボットを解除、右クリック: ピボットを設定、Shiftでスナップ)
     */
    LINE_PIVOT,
    /**
     * 回転
     **/
    ROTATE,
    /**
     * 拡大、縮小 (左クリック: ピボットに従って一方向拡大縮小、右クリック: ピボットに従って全方向拡大縮小)
     */
    SCALE,

    ;

    /** アイテムを作成 */
    public ItemStack createItem() {
        // 棒をつくる
        ItemStack item = new ItemStack(Material.STICK);
        // ItemMetaを作成
        ItemMeta meta = item.getItemMeta();
        // ツールの種類を設定
        Objects.requireNonNull(meta).getPersistentDataContainer().set(KEY, PersistentDataType.STRING, name());
        // 名前を設定
        meta.setDisplayName("§6" + name());
        // ItemMetaを設定
        item.setItemMeta(meta);
        // アイテムを返す
        return item;
    }

    /**
     * ツールの種類キー
     */
    private static final NamespacedKey KEY = new NamespacedKey(EasyDisplayEditor.instance, "edetool");

    /**
     * イベントからツールを取得する
     *
     * @param event イベント
     * @return ツール
     */
    public static ToolType fromEvent(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR
                && action != Action.LEFT_CLICK_BLOCK) {
            return NONE;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return NONE;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return NONE;
        }

        PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        String key = data.get(KEY, PersistentDataType.STRING);
        if (key == null) {
            return NONE;
        }

        for (ToolType type : ToolType.values()) {
            if (type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }

        return NONE;
    }

    /**
     * 名前からツールを取得する
     * @param name 名前
     * @return ツール
     */
    public static ToolType fromName(String name) {
        for (ToolType type : ToolType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }
}