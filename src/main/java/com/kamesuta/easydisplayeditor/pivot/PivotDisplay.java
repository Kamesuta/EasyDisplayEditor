package com.kamesuta.easydisplayeditor.pivot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * ピボット
 */
public class PivotDisplay {
    /**
     * ピボット
     */
    public Pivot pivot;
    /**
     * ピボットを表示するブロックディスプレイ
     */
    public BlockDisplay placedDisplay;
    /**
     * プレビューを表示するブロックディスプレイ
     */
    public BlockDisplay previewDisplay;
    /**
     * モード
     */
    public PivotType mode = PivotType.NONE;

    /**
     * ピボット表示の更新
     */
    public void updateDisplay(Player player, Pivot pivot) {
        switch (mode) {
            case NONE -> {
                // 表示を消す
                if (placedDisplay != null) {
                    placedDisplay.remove();
                    placedDisplay = null;
                }
            }
            case POINT -> {
                // 表示をする
                Location location = Vector.fromJOML(pivot.position()).toLocation(player.getWorld());
                if (placedDisplay != null) {
                    placedDisplay.teleport(location);
                } else {
                    placedDisplay = player.getWorld().spawn(location, BlockDisplay.class);
                }
                placedDisplay.setBlock(Material.BLUE_WOOL.createBlockData());
                float size = 0.2f;
                placedDisplay.setTransformation(new Transformation(
                        new Vector3f(-size / 2, -size / 2, -size / 2),
                        new Quaternionf(),
                        new Vector3f(size, size, size),
                        new Quaternionf()
                ));
            }
            case LINE -> {
                // 表示をする
                Location location = Vector.fromJOML(pivot.position()).toLocation(player.getWorld());
                if (placedDisplay != null) {
                    placedDisplay.teleport(location);
                } else {
                    placedDisplay = player.getWorld().spawn(location, BlockDisplay.class);
                }
                placedDisplay.setBlock(Material.BLUE_WOOL.createBlockData());
                float size = 0.05f;
                float longSize = 100f;
                placedDisplay.setTransformation(new Transformation(
                        pivot.rotate().transform(new Vector3f(-size / 2, -size / 2, -longSize / 2)),
                        pivot.rotate(),
                        new Vector3f(size, size, longSize),
                        new Quaternionf()
                ));
            }
        }
    }

    /**
     * プレビュー表示の更新
     */
    public void updatePreview(Player player, Pivot pivot) {
        if (pivot == null) return;

        // 表示をする
        Location location = Vector.fromJOML(pivot.position()).toLocation(player.getWorld());
        if (previewDisplay != null) {
            previewDisplay.teleport(location);
        } else {
            previewDisplay = player.getWorld().spawn(location, BlockDisplay.class);
        }
        previewDisplay.setBlock(Material.RED_WOOL.createBlockData());

        // 表示を更新
        float size = 0.05f;
        float longSize = 1f;
        previewDisplay.setTransformation(new Transformation(
                pivot.rotate().transform(new Vector3f(-size / 2, -size / 2, 0)),
                pivot.rotate(),
                new Vector3f(size, size, longSize),
                new Quaternionf()
        ));
    }

    /**
     * プレビューの削除
     */
    public void removePreview() {
        // 表示を消す
        if (previewDisplay != null) {
            previewDisplay.remove();
            previewDisplay = null;
        }
    }

    /**
     * プラグインが無効になったときの処理
     */
    public void onDisable() {
        // 表示を消す
        if (placedDisplay != null) {
            placedDisplay.remove();
            placedDisplay = null;
        }

        // 表示を消す
        removePreview();
    }
}
