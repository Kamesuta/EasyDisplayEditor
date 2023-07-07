package com.kamesuta.easydisplayeditor.tool;

import com.kamesuta.easydisplayeditor.util.MatrixUtils;
import com.kamesuta.easydisplayeditor.PlayerSession;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * つかみツール
 */
public class GrabTool implements Tool {
    private final PlayerSession session;

    public GrabTool(PlayerSession session) {
        this.session = session;
    }

    /**
     * 選択中かどうか
     */
    private boolean isGrabbing = false;

    @Override
    public ToolType getType() {
        return ToolType.GRAB;
    }

    @Override
    public void onRightClick() {
        // プレイヤー
        Player player = session.player;

        if (isGrabbing) {
            // Grab中の場合

            // Grab初期位置をリセット
            for (Map.Entry<BlockDisplay, Matrix4f> entry : session.selected.entrySet()) {
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
            Matrix4f playerMatrixInvert = MatrixUtils.getLocationMatrix(player.getEyeLocation()).invert();

            // Grab初期位置を記録
            for (Map.Entry<BlockDisplay, Matrix4f> entry : session.selected.entrySet()) {
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
        Matrix4f playerMatrix = MatrixUtils.getLocationMatrix(player.getEyeLocation());

        // Grab対象を更新する
        for (Map.Entry<BlockDisplay, Matrix4f> entry : session.selected.entrySet()) {
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
