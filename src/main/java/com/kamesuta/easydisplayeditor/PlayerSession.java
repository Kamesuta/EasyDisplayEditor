package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
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
        // 半径
        double radius = 16;
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
        double radiusEye = 6;
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

    /**
     * つかみツール
     */
    public void grabTool() {
        // Grab中の場合
        if (activeTool != ToolType.GRAB) {
            // Zero -> Player
            Matrix4f playerMatrix = MatrixUtils.getLocationMatrix(player.getEyeLocation());

            // Grab初期位置を記録
            for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
                BlockDisplay display = entry.getKey();

                // Zero -> Display
                Matrix4f displayMatrix = MatrixUtils.getLocationMatrix(display.getLocation());
                // Display -> DisplayLocal
                Matrix4f displayLocalMatrix = MatrixUtils.getTransformationMatrix(display.getTransformation());

                // Player -> DisplayLocal
                Matrix4f offsetMatrix = new Matrix4f()
                        .mul(new Matrix4f(playerMatrix).invert())
                        .mul(new Matrix4f(displayMatrix))
                        .mul(new Matrix4f(displayLocalMatrix))
                        ;


                // (0, 0, 1)をデバッグ表示
                Matrix4f debugMatrix = new Matrix4f(offsetMatrix);
                Vector3f start = debugMatrix.transformPosition(new Vector3f(0, 0, 0));
                Vector3f end = debugMatrix.transformPosition(new Vector3f(0, 0, 1));
                ToolEventHandler.endPos = new Location(player.getWorld(), 0, 0, 0).add(Vector.fromJOML(end));
                ToolEventHandler.startPos = new Location(player.getWorld(), 0, 0, 0).add(Vector.fromJOML(start));
//                ToolEventHandler.endPos = display.getLocation().add(Vector.fromJOML(end));
//                ToolEventHandler.startPos = display.getLocation().add(Vector.fromJOML(start));
//                ToolEventHandler.startPos = Vector.fromJOML(start).toLocation(player.getWorld());
//                ToolEventHandler.endPos = Vector.fromJOML(end).toLocation(player.getWorld());

                // 記録する
                entry.setValue(offsetMatrix);
            }

            // Grab中にする
            activeTool = ToolType.GRAB;
        } else {
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
        // Grab中でない場合
        if (activeTool != ToolType.GRAB) {
            return;
        }

        // Zero -> Player
        Matrix4f playerMatrix = MatrixUtils.getLocationMatrix(player.getEyeLocation());

        // Grab対象を更新する
        for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
            BlockDisplay display = entry.getKey();

            // Player -> DisplayLocal
            Matrix4f offsetMatrix = entry.getValue();

            // Zero -> Display
            Matrix4f displayMatrix = MatrixUtils.getLocationMatrix(display.getLocation());

            // Old DisplayLocal -> New DisplayLocal
            Matrix4f matrix4f = new Matrix4f()
                    .mul(new Matrix4f(displayMatrix).invert())
                    .mul(new Matrix4f(playerMatrix))
                    .mul(new Matrix4f(offsetMatrix))
                    ;

            // 変換を適用する
            display.setTransformationMatrix(matrix4f);
//            display.setInterpolationDelay(0);
//            display.setInterpolationDuration(1);
        }
    }
}
