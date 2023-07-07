package com.kamesuta.easydisplayeditor.tool;

import com.kamesuta.easydisplayeditor.PlayerSession;
import com.kamesuta.easydisplayeditor.util.BlockOutline;
import com.kamesuta.easydisplayeditor.util.MatrixUtils;
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
 * 選択ツール
 */
public class SelectorTool implements Tool {
    /**
     * ピボットのオフセット
     */
    public static final int FRONT_OFFSET = 3;
    /**
     * 距離
     */
    public static final int SELECT_RADIUS = 32;

    /**
     * プレイヤーセッション
     */
    private final PlayerSession session;
    /**
     * 選択中かどうか
     */
    private boolean isSelecting = false;
    /**
     * 選択開始位置
     */
    private Vector3f selectionStart;
    /**
     * 選択を表示するブロックディスプレイ
     */
    private BlockDisplay selectionDisplay;
    /**
     * 選択中のブロックディスプレイ
     */
    private List<BlockDisplay> selected = Collections.emptyList();

    public SelectorTool(PlayerSession session) {
        this.session = session;
    }

    @Override
    public ToolType getType() {
        return ToolType.SELECTOR;
    }

    @Override
    public void onLeftClick() {
        // プレイヤー
        Player player = session.player;

        if (isSelecting) {
            // 選択中の場合

            // 選択範囲解除
            deselect(true);
        } else {
            // 選択中ではない場合

            // 選択開始位置を記録
            Location selStart = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(FRONT_OFFSET));
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
            isSelecting = true;
        }
    }

    /**
     * 選択範囲解除
     *
     * @param apply 選択範囲を反映するかどうか
     */
    private void deselect(boolean apply) {
        // 選択範囲を非表示にする
        if (selectionDisplay != null) {
            selectionDisplay.remove();
            selectionDisplay = null;
        }

        // 反映する場合
        if (apply) {
            // 選択を反映する
            session.selected.addAll(selected);
        } else {
            // 選択を破棄する
            selected.forEach(display -> display.setGlowing(false));
        }
        selected = Collections.emptyList();

        // 選択中を解除する
        isSelecting = false;
    }

    @Override
    public void onFinish() {
        // 選択範囲解除
        deselect(false);
    }

    @Override
    public void onRightClick() {
        // 選択範囲解除
        if (isSelecting) {
            deselect(true);
            return;
        }

        // プレイヤー
        Player player = session.player;

        // 半径16ブロックの範囲のブロックディスプレイを選択する
        // TODO: BlockDisplay以外のDisplayも選択できるようにする
        Collection<BlockDisplay> displays = player.getWorld()
                .getNearbyEntities(player.getLocation(), SELECT_RADIUS, SELECT_RADIUS, SELECT_RADIUS,
                        (entity) -> entity instanceof BlockDisplay)
                .stream().map(entity -> (BlockDisplay) entity).toList();

        // ブロックディスプレイの選択結果
        record RayResult(BlockDisplay display, RayTraceResult result, double eyeDistance) {
        }
        // 目線上にあるブロックディスプレイを選択する
        Vector3f eyeSource = player.getEyeLocation().toVector().toVector3f();
        Vector3f eyeTarget = new Vector3f(eyeSource).add(player.getEyeLocation().getDirection().multiply(SELECT_RADIUS).toVector3f());
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
            session.selected.forEach(display -> display.setGlowing(false));
            session.selected.clear();
        }

        // 一番近いブロックディスプレイを選択する
        if (hits.isEmpty()) {
            return;
        }

        // 選択中に追加/削除をトグル
        BlockDisplay display = hits.get().display;
        if (session.selected.contains(display)) {
            display.setGlowing(false);
            session.selected.remove(display);
        } else {
            display.setGlowing(true);
            session.selected.add(display);
        }
    }

    @Override
    public void onTick() {
        if (selectionDisplay == null) {
            return;
        }

        // プレイヤー
        Player player = session.player;

        // 選択開始位置から現在位置までの範囲を選択する
        Vector3f selectionEnd = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(3)).toVector().toVector3f();
        Vector3f min = new Vector3f(selectionStart).min(selectionEnd);
        Vector3f max = new Vector3f(selectionStart).max(selectionEnd);

        // 範囲内のエンティティを光らせる
        BoundingBox box = new BoundingBox(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        List<BlockDisplay> displays = player.getWorld().getNearbyEntities(box)
                .stream()
                .filter(entity -> entity instanceof BlockDisplay)
                .filter(entity -> entity != selectionDisplay)
                .map(entity -> (BlockDisplay) entity)
                .toList();
        // 新規に追加されたエンティティを光らせる
        displays.stream()
                .filter(display -> !selected.contains(display))
                .forEach(display -> display.setGlowing(true));
        // 削除されたエンティティを光らせない
        selected.stream()
                .filter(display -> !displays.contains(display) && !session.selected.contains(display))
                .forEach(display -> display.setGlowing(false));
        // 選択中を更新
        selected = displays;

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
}
