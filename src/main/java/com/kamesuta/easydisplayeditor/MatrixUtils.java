package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 行列ユーティリティ
 */
public class MatrixUtils {
    /**
     * 変換行列を取得する
     *
     * @param transformation 変換
     * @return 変換行列
     */
    public static Matrix4f getTransformationMatrix(Transformation transformation) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translation(transformation.getTranslation());
        matrix4f.rotate(transformation.getLeftRotation());
        matrix4f.scale(transformation.getScale());
        matrix4f.rotate(transformation.getRightRotation());
        return matrix4f;
    }

    /**
     * ロケーションから回転を取得する
     *
     * @param location ロケーション
     * @return 回転
     */
    public static Quaternionf getLocationRotation(Location location) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(location.getYaw()), 0, -1, 0))
                .mul(new Quaternionf(new AxisAngle4f((float) Math.toRadians(location.getPitch()), 1, 0, 0)));
    }

    /**
     * ロケーションから行列を取得する
     *
     * @param location ロケーション
     * @return 行列
     */
    public static Matrix4f getLocationMatrix(Location location) {
        Vector3f position = location.toVector().toVector3f();
        Quaternionf rotation = getLocationRotation(location);
        return new Matrix4f().translate(position).rotate(rotation);
    }
}
