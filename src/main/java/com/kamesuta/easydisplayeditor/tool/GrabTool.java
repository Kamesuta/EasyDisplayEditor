package com.kamesuta.easydisplayeditor.tool;

import com.kamesuta.easydisplayeditor.PlayerSession;
import com.kamesuta.easydisplayeditor.pivot.PivotDisplay;
import com.kamesuta.easydisplayeditor.util.MatrixUtils;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * つかみツール
 */
public class GrabTool implements Tool {
    /**
     * プレイヤーセッション
     */
    private final PlayerSession session;
    /**
     * 選択中かどうか
     */
    private boolean isGrabbing = false;
    /**
     * 選択中のブロックディスプレイ -> 最初の行列
     */
    private final Map<BlockDisplay, Matrix4f> selected = new HashMap<>();

    public GrabTool(PlayerSession session) {
        this.session = session;
    }

    @Override
    public ToolType getType() {
        return ToolType.GRAB;
    }

    /**
     * プレイヤーの行列を取得する
     *
     * @param player プレイヤー
     * @return プレイヤーの行列
     */
    private Matrix4f getPlayerMatrix(Player player) {
        PivotDisplay pivotDisplay = session.pivotDisplay;
        return switch (pivotDisplay.mode) {
            case NONE -> MatrixUtils.getLocationMatrix(player.getEyeLocation());
            case POINT -> new Matrix4f()
                    .translate(pivotDisplay.pivot.position())
                    .rotate(MatrixUtils.getLocationRotation(player.getEyeLocation()));
            case LINE -> {
                // プレイヤーの視線ベクトル
                Vector3f look = player.getEyeLocation().getDirection().toVector3f();
                // ピボット線(回転軸)のベクトル
                Vector3f axis = pivotDisplay.pivot.rotate().transform(new Vector3f(0, 0, 1));
                // プレイヤーの視線ベクトルを回転軸と垂直な平面に射影したベクトル
                Vector3f lookAxis = new Vector3f(look).sub(new Vector3f(axis).mul(look.dot(axis))).normalize();
                // ピボット線(回転軸)と垂直な基準ベクトル
                Vector3f baseAxis = pivotDisplay.pivot.rotate().transform(new Vector3f(1, 0, 0));
                // 回転軸と垂直な平面上にある視線ベクトルと基準ベクトルに直交するベクトル
                Vector3f crossAxis = new Vector3f(baseAxis).cross(lookAxis).normalize();
                // 回転軸と垂直な平面上にある視線ベクトルと基準ベクトルのなす角
                float angle = (float) Math.acos(baseAxis.dot(lookAxis));
                // 軸を中心になす角だけ回転する行列
                yield new Matrix4f()
                        .translate(pivotDisplay.pivot.position())
                        .rotate(new AxisAngle4f(angle, crossAxis));
            }
        };
    }

    @Override
    public void onRightClick() {
        // プレイヤー
        Player player = session.player;

        if (isGrabbing) {
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
            isGrabbing = false;
        } else {
            // Grab中ではない場合

            // (Zero -> Player)' = Player -> Zero
            Matrix4f playerMatrixInvert = getPlayerMatrix(player).invert();

            // 一旦クリア
            selected.clear();

            // Grab初期位置を記録
            for (BlockDisplay display : session.selected) {
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
                selected.put(display, offsetMatrix);
            }

            // Grab中にする
            isGrabbing = true;
        }
    }

    @Override
    public void onFinish() {
        // Grab中を解除する
        isGrabbing = false;
    }

    @Override
    public void onTick() {
        // プレイヤー
        Player player = session.player;

        // Zero -> Player
        Matrix4f playerMatrix = getPlayerMatrix(player);

        // Grab対象を更新する
        for (Map.Entry<BlockDisplay, Matrix4f> entry : selected.entrySet()) {
            BlockDisplay display = entry.getKey();

            // Player -> DisplayLocal
            Matrix4f offsetMatrix = entry.getValue();
            if (offsetMatrix == null) continue;

            // (Zero -> Display)' = Display -> Zero
            Matrix4f displayMatrixInvert = MatrixUtils.getLocationMatrix(display.getLocation()).invert();

            // Old DisplayLocal -> New DisplayLocal
            Matrix4f displayLocalMatrix = new Matrix4f()
                    .mul(displayMatrixInvert)
                    .mul(playerMatrix)
                    .mul(offsetMatrix);

            // 変換を適用する
            display.setTransformationMatrix(displayLocalMatrix);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(1);
        }
    }
}
