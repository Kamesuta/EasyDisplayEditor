package com.kamesuta.easydisplayeditor.pivot;

import com.kamesuta.easydisplayeditor.util.MatrixUtils;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.stream.IntStream;

/**
 * ピボットデータ
 *
 * @param position ピボット位置
 * @param rotate   ピボット向き
 */
public record Pivot(Vector3f position, Quaternionf rotate) {
    /**
     * ピボットのオフセット
     */
    public static final float FRONT_OFFSET = 3f;
    /**
     * ピボットのオフセット
     */
    public static final float DOWN_OFFSET = 0.1f;

    /**
     * コンストラクタ
     *
     * @param position ピボット位置
     * @param rotate   ピボット向き
     */
    public Pivot {
    }

    /**
     * プレイヤーから作成
     *
     * @param player プレイヤー
     * @param mode   モード
     * @return ピボット
     */
    public static Pivot fromPlayer(Player player, PivotType mode) {
        // モードに応じてオフセットを設定
        Vector3f offset = new Vector3f(0, -DOWN_OFFSET, FRONT_OFFSET);

        // ピボットを設定
        Quaternionf r = MatrixUtils.getLocationRotation(player.getEyeLocation());
        return new Pivot(
                player.getEyeLocation().toVector().toVector3f().add(r.transform(offset)),
                r
        );
    }

    /**
     * ピボットをスナップする
     *
     * @return スナップしたピボット
     */
    public Pivot snap() {
        // 向き
        Vector3f direction = rotate.transform(new Vector3f(0, 0, -1));

        // スナップした向き
        Vector3f minDirection = IntStream.rangeClosed(-1, 1).mapToObj(x ->
                        IntStream.rangeClosed(-1, 1).mapToObj(y ->
                                IntStream.rangeClosed(-1, 1).mapToObj(z -> new Vector3f(x, y, z))
                        )
                )
                .flatMap(x -> x.flatMap(y -> y))
                .filter(vec -> vec.lengthSquared() > 0)
                .map(Vector3f::normalize)
                .min((a, b) -> Float.compare(direction.dot(a), direction.dot(b)))
                .orElseGet(() -> new Vector3f(0, 0, 1));

        // スナップした向きを設定
        float yaw = (float) Math.atan2(-minDirection.x, minDirection.z);
        float pitch = (float) Math.atan2(
                -minDirection.y,
                Math.sqrt(minDirection.x * minDirection.x + minDirection.z * minDirection.z)
        );
        Quaternionf r = MatrixUtils.getYawPitchRotation(yaw, pitch);

        // 位置をスナップ
        Vector3f p = new Vector3f(
                (float) Math.round(position.x * 2) / 2,
                (float) Math.round(position.y * 2) / 2,
                (float) Math.round(position.z * 2) / 2
        );
        return new Pivot(p, r);
    }
}
