package com.kamesuta.easydisplayeditor.tool;

import com.kamesuta.easydisplayeditor.PlayerSession;
import com.kamesuta.easydisplayeditor.util.MatrixUtils;
import com.kamesuta.easydisplayeditor.util.Pivot;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
        return switch (session.pivot.mode) {
            case NONE -> MatrixUtils.getLocationMatrix(player.getEyeLocation());
            case POINT -> new Matrix4f()
                    .translate(session.pivot.pivot)
                    .rotate(MatrixUtils.getLocationRotation(player.getEyeLocation()));
            case LINE -> {
                // pivotDirection = (0, 0, 1) -> line
                // rotation の axis成分の回転を取り出す

                Vector3f look = player.getEyeLocation().getDirection().toVector3f();
                Vector3f axis = session.pivot.pivotDirection.transform(new Vector3f(0, 0, 1));
                Vector3f axisLook = new Vector3f(look).sub(new Vector3f(axis).mul(look.dot(axis))).normalize();
                Vector3f baseAxis = session.pivot.pivotDirection.transform(new Vector3f(1, 0, 0));
                Vector3f crossAxis = new Vector3f(baseAxis).cross(axisLook).normalize();
                // baseAxisとaxisLookのなす角を求める
                float angle = (float) Math.acos(baseAxis.dot(axisLook));
                yield new Matrix4f()
                        .translate(session.pivot.pivot)
                        .rotate(new AxisAngle4f(angle, crossAxis));
            }
        };
    }

    @Override
    public void onLeftClick() {
        if (session.pivot.mode == Pivot.PivotType.NONE) {
            // ピボットなしの場合
            if (session.player.isSneaking()) {
                // スニーク中の場合、線ピボットを配置
                session.pivot.mode = Pivot.PivotType.LINE;
            } else {
                // スニーク中でない場合、点ピボットを配置
                session.pivot.mode = Pivot.PivotType.POINT;
            }
        } else {
            // ピボットありの場合、ピボットを削除
            session.pivot.mode = Pivot.PivotType.NONE;
        }

        // ピボットの座標をプレイヤーの座標に設定
        session.pivot.setPivot(session.player);
        // ピボットの表示を更新
        session.pivot.updateDisplay(session.player);
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
