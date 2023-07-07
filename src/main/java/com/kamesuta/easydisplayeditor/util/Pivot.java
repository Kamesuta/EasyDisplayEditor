package com.kamesuta.easydisplayeditor.util;

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
public class Pivot {
    /**
     * ピボットのオフセット
     */
    public static final float FRONT_OFFSET = 3f;
    /**
     * ピボットのオフセット
     */
    public static final float DOWN_OFFSET = 0.1f;
    /**
     * ピボット
     */
    public Vector3f pivot;
    /**
     * ピボット向き
     */
    public Quaternionf pivotDirection;
    /**
     * ピボットを表示するブロックディスプレイ
     */
    public BlockDisplay pivotDisplay;
    /**
     * モード
     */
    public PivotType mode = PivotType.NONE;

    /**
     * ピボット
     */
    public enum PivotType {
        /**
         * なし
         */
        NONE,
        /**
         * 点Pivot
         */
        POINT,
        /**
         * 線Pivot
         */
        LINE,
    }

    /**
     * ピボットを設定する
     *
     * @param player プレイヤー
     */
    public void setPivot(Player player) {
        // モードに応じてオフセットを設定
        Vector3f offset = mode == PivotType.LINE
                ? new Vector3f(0, -DOWN_OFFSET, 0)
                : new Vector3f(0, 0, FRONT_OFFSET);

        // ピボットを設定
        pivotDirection = MatrixUtils.getLocationRotation(player.getEyeLocation());
        pivot = player.getEyeLocation().toVector().toVector3f()
                .add(pivotDirection.transform(offset));
    }

    /**
     * 次のモードにする
     */
    public void updateDisplay(Player player) {
        switch (mode) {
            case NONE -> {
                // 表示を消す
                if (pivotDisplay != null) {
                    pivotDisplay.remove();
                    pivotDisplay = null;
                }
            }
            case POINT -> {
                // 表示をする
                Location location = Vector.fromJOML(pivot).toLocation(player.getWorld());
                if (pivotDisplay != null) {
                    pivotDisplay.teleport(location);
                } else {
                    pivotDisplay = player.getWorld().spawn(location, BlockDisplay.class);
                }
                pivotDisplay.setBlock(Material.BLUE_WOOL.createBlockData());
                float size = 0.2f;
                pivotDisplay.setTransformation(new Transformation(
                        new Vector3f(-size / 2, -size / 2, -size / 2),
                        new Quaternionf(),
                        new Vector3f(size, size, size),
                        new Quaternionf()
                ));
            }
            case LINE -> {
                // 表示をする
                Location location = Vector.fromJOML(pivot).toLocation(player.getWorld());
                if (pivotDisplay != null) {
                    pivotDisplay.teleport(location);
                } else {
                    pivotDisplay = player.getWorld().spawn(location, BlockDisplay.class);
                }
                pivotDisplay.setBlock(Material.BLUE_WOOL.createBlockData());
                float size = 0.05f;
                float longSize = 100f;
                pivotDisplay.setTransformation(new Transformation(
                        pivotDirection.transform(new Vector3f(-size / 2, -size / 2, -longSize / 2)),
                        pivotDirection,
                        new Vector3f(size, size, longSize),
                        new Quaternionf()
                ));
            }
        }
    }

    /**
     * プラグインが無効になったときの処理
     */
    public void onDisable() {
        // 表示を消す
        if (pivotDisplay != null) {
            pivotDisplay.remove();
            pivotDisplay = null;
        }
    }
}
