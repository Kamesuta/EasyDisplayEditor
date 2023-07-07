package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    public ToolType activeTool = ToolType.NONE;

    /**
     * 選択開始位置
     */
    private Vector3f selectionStart;

    /**
     * 選択を表示するブロックディスプレイ
     */
    private BlockDisplay selectionDisplay;

    /**
     * コンストラクター
     *
     * @param player プレイヤー
     */
    public PlayerSession(Player player) {
        this.player = player;
    }

    /**
     * セレクターツール
     */
    public void selectorTool() {
        // 距離
        double radius = 32;
        double radiusEye = radius + 4;

        // 半径16ブロックの範囲のブロックディスプレイを選択する
        // TODO: BlockDisplay以外のDisplayも選択できるようにする
        // TODO: もうちょい実体だけが遠いオブジェクトも選択できるようにする
        Collection<BlockDisplay> displays = player.getWorld()
                .getNearbyEntities(player.getLocation(), radius, radius, radius,
                        (entity) -> entity instanceof BlockDisplay)
                .stream().map(entity -> (BlockDisplay) entity).toList();

        // ブロックディスプレイの選択結果
        record RayResult(BlockDisplay display, RayTraceResult result, double eyeDistance) {
        }
        // 目線上にあるブロックディスプレイを選択する
        Vector3f eyeSource = player.getEyeLocation().toVector().toVector3f();
        Vector3f eyeTarget = new Vector3f(eyeSource).add(player.getEyeLocation().getDirection().multiply(radiusEye).toVector3f());
        Optional<RayResult> hits = displays.stream()
                .map(display -> {
                    // 行列を取得する
                    Matrix4f matrix4f = MatrixUtils.getTransformationMatrix(display.getTransformation());
                    // 逆行列を取得する
                    Matrix4f matrix4fInv = matrix4f.invert();
                    // 目線の線分ABを目線の逆行列で変換した線分A'B'に変換する
                    Vector3f center = display.getLocation().toVector().toVector3f();
                    Vector3f eyeSourceTransformed = matrix4fInv.transformPosition(new Vector3f(eyeSource).sub(center));
                    Vector3f eyeTargetTransformed = matrix4fInv.transformPosition(new Vector3f(eyeTarget).sub(center));

                    // 単位立方体との交差判定を行う
                    RayTraceResult result;
                    try {
                        // 詳細に当たり判定を行う
                        BlockData blockData = display.getBlock();
                        Object nmsOutline = BlockOutline.getOutline(player, blockData);
                        result = BlockOutline.rayTraceOutline(nmsOutline, display.getWorld(), eyeSourceTransformed, eyeTargetTransformed);
                    } catch (ReflectiveOperationException e) {
                        // フォールバックとして単位立方体との交差判定を行う
                        Vector3f eyeSegmentTransformed = new Vector3f(eyeTargetTransformed).sub(eyeSourceTransformed);
                        Vector3f eyeDirectionTransformed = new Vector3f(eyeSegmentTransformed).normalize();
                        double eyeLengthTransformed = eyeSegmentTransformed.length();
                        result = new BoundingBox(0, 0, 0, 1, 1, 1)
                                .rayTrace(org.bukkit.util.Vector.fromJOML(eyeSourceTransformed), Vector.fromJOML(eyeDirectionTransformed), eyeLengthTransformed);
                    }

                    // 視線との距離を計算する
                    double eyeDistance = result != null ? eyeSourceTransformed.distance(result.getHitPosition().toVector3f()) : Double.MAX_VALUE;

                    return new RayResult(display, result, eyeDistance);
                })
                .filter(rayResult -> rayResult.result != null)
                .min(
                        Comparator.comparingDouble(rayResult -> rayResult.eyeDistance)
                );

        // Shiftを押していなかったら選択をクリア
        if (!player.isSneaking()) {
            for (BlockDisplay display : selected.keySet()) {
                display.setGlowing(false);
            }
            selected.clear();
        }

        // 一番近いブロックディスプレイを選択する
        if (hits.isEmpty()) {
            return;
        }

        // 選択中に追加/削除をトグル
        BlockDisplay display = hits.get().display;
        if (selected.containsKey(display)) {
            display.setGlowing(false);
            selected.remove(display);
        } else {
            display.setGlowing(true);
            selected.put(display, null);
        }
    }

    public void areaSelectorTool() {
        if (activeTool != ToolType.SELECTOR) {
            // 選択中ではない場合

            // 選択開始位置を記録
            Location selStart = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3));
            selectionStart = selStart.toVector().toVector3f();

            // 選択範囲を表示する[
            if (selectionDisplay != null) {
                selectionDisplay.teleport(selStart);
            } else {
                selectionDisplay = player.getWorld().spawn(selStart, BlockDisplay.class);
            }
            selectionDisplay.setBlock(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData());
            selectionDisplay.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0, 0, 0),
                    new Quaternionf()
            ));
            
            // 選択中にする
            activeTool = ToolType.SELECTOR;
        } else {
            // 選択中の場合

            // 選択範囲を非表示にする
            selectionDisplay.remove();
            selectionDisplay = null;

            // 選択中を解除する
            activeTool = ToolType.NONE;
        }

    }

    public void tickAreaSelectorTool() {
        if (selectionDisplay == null) {
            return;
        }

        // 選択開始位置から現在位置までの範囲を選択する
        Vector3f selectionEnd = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3)).toVector().toVector3f();
        Vector3f min = new Vector3f(selectionStart).min(selectionEnd);
        Vector3f max = new Vector3f(selectionStart).max(selectionEnd);

        // 範囲内のエンティティを光らせる
        BoundingBox box = new BoundingBox(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        player.getWorld().getNearbyEntities(box)
                .stream()
                .filter(entity -> entity instanceof BlockDisplay)
                .map(entity -> (BlockDisplay) entity)
                .forEach(display -> display.setGlowing(true));

        // 選択範囲を表示する
        Vector3f displayPosition = selectionDisplay.getLocation().toVector().toVector3f();
        selectionDisplay.setTransformation(new Transformation(
                new Vector3f(min).sub(displayPosition),
                new Quaternionf(),
                new Vector3f(max).sub(min),
                new Quaternionf()
        ));
        selectionDisplay.setInterpolationDelay(0);
        selectionDisplay.setInterpolationDuration(1);
    }

    /**
     * つかみツール
     */
    public void grabTool() {
        if (activeTool != ToolType.GRAB) {
            // Grab中ではない場合

            // (Zero -> Player)' = Player -> Zero
            Matrix4f playerMatrixInvert = MatrixUtils.getLocationMatrix(player.getEyeLocation()).invert();

            // Grab初期位置を記録
            for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
                BlockDisplay display = entry.getKey();

                // Zero -> Display
                Matrix4f displayMatrix = MatrixUtils.getLocationMatrix(display.getLocation());
                // Display -> DisplayLocal
                Matrix4f displayLocalMatrix = MatrixUtils.getTransformationMatrix(display.getTransformation());

                // Player -> DisplayLocal
                Matrix4f offsetMatrix = new Matrix4f()
                        .mul(playerMatrixInvert)
                        .mul(displayMatrix)
                        .mul(displayLocalMatrix);

                // 記録する
                entry.setValue(offsetMatrix);
            }

            // Grab中にする
            activeTool = ToolType.GRAB;
        } else {
            // Grab中の場合

            // Grab初期位置をリセット
            for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
                BlockDisplay display = entry.getKey();

                // Transformationオフセットを位置に反映する
                Transformation transformation = display.getTransformation();
                display.teleport(display.getLocation().add(Vector.fromJOML(transformation.getTranslation())));
                transformation.getTranslation().set(0, 0, 0);
                display.setTransformation(transformation);

                // Grab初期位置をリセットする
                entry.setValue(null);
            }

            // Grab中を解除する
            activeTool = ToolType.NONE;
        }
    }

    /**
     * つかみツールのTick処理
     */
    public void tickGrabTool() {
        // Zero -> Player
        Matrix4f playerMatrix = MatrixUtils.getLocationMatrix(player.getEyeLocation());

        // Grab対象を更新する
        for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
            BlockDisplay display = entry.getKey();

            // Player -> DisplayLocal
            Matrix4f offsetMatrix = entry.getValue();

            // (Zero -> Display)' = Display -> Zero
            Matrix4f displayMatrixInvert = MatrixUtils.getLocationMatrix(display.getLocation()).invert();

            // Old DisplayLocal -> New DisplayLocal
            Matrix4f matrix4f = new Matrix4f()
                    .mul(displayMatrixInvert)
                    .mul(playerMatrix)
                    .mul(offsetMatrix);

            // 変換を適用する
            display.setTransformationMatrix(matrix4f);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
        }
    }
}
