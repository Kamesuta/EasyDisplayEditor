package com.kamesuta.easydisplayeditor;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class ToolEventHandler implements Listener {

    /**
     * 変換行列を取得する
     *
     * @param transformation 変換
     * @return 変換行列
     */
    private static Matrix4f getTransformationMatrix(Transformation transformation) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translation(transformation.getTranslation());
        matrix4f.rotate(transformation.getLeftRotation());
        matrix4f.scale(transformation.getScale());
        matrix4f.rotate(transformation.getRightRotation());
        return matrix4f;
    }

//    private boolean isGlowTick;

    public void tickSelection() {
//        PlayerSession.sessions.values().forEach(session -> {
//            session.selected.forEach(display -> {
//                display.setGlowing(isGlowTick);
//            });
//        });
//
//        isGlowTick = !isGlowTick;
    }

    public void tick() {
        // TODO: ツールの種類によって処理を分ける

        // デバッグ用にstartPosとendPosを表示する
        if (startPos != null && endPos != null) {
            // パーティクル表示
            startPos.getWorld().spawnParticle(Particle.ASH, startPos, 1, 0, 0, 0, 0);
            endPos.getWorld().spawnParticle(Particle.SPELL, endPos, 1, 0, 0, 0, 0);
        }
    }

    private Location startPos;
    private Location endPos;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ToolType type = ToolType.fromEvent(event);
        switch (type) {
            case SELECTOR: {
                // 半径
                double radius = 6;
                // 半径6ブロックの範囲のブロックディスプレイを選択する
                // TODO: BlockDisplay以外のDisplayも選択できるようにする
                Collection<BlockDisplay> displays = event.getPlayer().getWorld()
                        .getNearbyEntities(event.getPlayer().getLocation(), radius, radius, radius,
                                (entity) -> entity instanceof BlockDisplay)
                        .stream().map(entity -> (BlockDisplay) entity).toList();

                // ブロックディスプレイの選択結果
                record RayResult(BlockDisplay display, RayTraceResult result, double eyeDistance) {
                }
                // 目線上にあるブロックディスプレイを選択する
                Vector3f eyeSource = event.getPlayer().getEyeLocation().toVector().toVector3f();
                Vector3f eyeTarget = new Vector3f(eyeSource).add(event.getPlayer().getEyeLocation().getDirection().multiply(radius).toVector3f());
                Optional<RayResult> hits = displays.stream()
                        .map(display -> {
                            // 行列を取得する
                            Matrix4f matrix4f = getTransformationMatrix(display.getTransformation());
                            // 逆行列を取得する
                            Matrix4f matrix4fInv = matrix4f.invert();
                            // 目線の線分ABを目線の逆行列で変換した線分A'B'に変換する
                            Vector3f center = display.getLocation().toVector().toVector3f();
                            Vector3f eyeSourceTransformed = matrix4fInv.transformPosition(new Vector3f(eyeSource).sub(center));
                            Vector3f eyeTargetTransformed = matrix4fInv.transformPosition(new Vector3f(eyeTarget).sub(center));

                            // デバッグ用にstartPosとendPosを表示する
                            startPos = Vector.fromJOML(eyeSourceTransformed).toLocation(display.getWorld());
                            endPos = Vector.fromJOML(eyeTargetTransformed).toLocation(display.getWorld());

                            // 単位立方体との交差判定を行う
                            RayTraceResult result;
                            try {
                                // 詳細に当たり判定を行う
                                BlockData blockData = display.getBlock();
                                Object nmsOutline = BlockOutline.getOutline(event.getPlayer(), blockData);
                                result = BlockOutline.rayTraceOutline(nmsOutline, display.getWorld(), eyeSourceTransformed, eyeTargetTransformed);
                            } catch (ReflectiveOperationException e) {
                                // フォールバックとして単位立方体との交差判定を行う
                                Vector3f eyeSegmentTransformed = new Vector3f(eyeTargetTransformed).sub(eyeSourceTransformed);
                                Vector3f eyeDirectionTransformed = new Vector3f(eyeSegmentTransformed).normalize();
                                double eyeLengthTransformed = eyeSegmentTransformed.length();
                                result = new BoundingBox(0, 0, 0, 1, 1, 1)
                                        .rayTrace(Vector.fromJOML(eyeSourceTransformed), Vector.fromJOML(eyeDirectionTransformed), eyeLengthTransformed);
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

                // プレイヤーセッションを取得
                PlayerSession session = PlayerSession.get(event.getPlayer().getUniqueId());
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
            break;
            case GRAB: {

            }
            break;
            case DOT_PIVOT:
                break;
            case LINE_PIVOT:
                break;
            case ROTATE:
                break;
            case SCALE:
                break;
            default:
                break;
        }
    }
}
