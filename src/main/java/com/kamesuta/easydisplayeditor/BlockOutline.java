package com.kamesuta.easydisplayeditor;

import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BlockOutline {
    /**
     * 初期化が完了しているかどうか
     */
    private static boolean initialized = false;

    /**
     * NMS: CraftBlockData.getState
     */
    private static Method craftBlockDataGetStateMethod;

    /**
     * NMS: CraftWorld.getHandle
     */
    private static Method craftWorldGetHandleMethod;

    /**
     * NMS: CraftEntity.getHandle
     */
    private static Method craftEntityGetHandleMethod;

    /**
     * NMS: CraftRayTraceResult.fromNMS
     */
    private static Method craftRayTraceResultFromNMSMethod;

    /**
     * NMS: BlockPosition::new
     */
    private static Constructor<?> blockPositionConstructor;

    /**
     * NMS: VoxelShapeCollision.of
     */
    private static Method voxelShapeCollisionOfMethod;

    /**
     * NMS: BlockBase.BlockData.a(IBlockAccess world, BlockPosition pos, VoxelShapeCollision context)
     */
    private static Method blockDataGetOutlineShapeMethod;

    /**
     * NMS: Vec3D::new
     */
    private static Constructor<?> vec3dConstructor;

    /**
     * NMS: VoxelShape.a(Vec3D start, Vec3D end, BlockPosition pos)
     */
    private static Method voxelShapeRaycastMethod;

    /**
     * 必要なリフレクションフィールドを取得
     */
    public static void init() throws ReflectiveOperationException {
        Class<?> craftBlockDataClass = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
        craftBlockDataGetStateMethod = craftBlockDataClass.getMethod("getState");

        Class<?> craftWorldClass = MinecraftReflection.getCraftBukkitClass("CraftWorld");
        craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle");

        Class<?> craftEntityClass = MinecraftReflection.getCraftBukkitClass("entity.CraftEntity");
        craftEntityGetHandleMethod = craftEntityClass.getMethod("getHandle");

        Class<?> movingObjectPosition = MinecraftReflection.getMinecraftClass("world.phys.MovingObjectPosition");
        Class<?> craftRayTraceResultClass = MinecraftReflection.getCraftBukkitClass("util.CraftRayTraceResult");
        craftRayTraceResultFromNMSMethod = craftRayTraceResultClass.getMethod("fromNMS", World.class, movingObjectPosition);

        Class<?> blockPositionClass = MinecraftReflection.getMinecraftClass("core.BlockPosition");
        blockPositionConstructor = blockPositionClass.getConstructor(int.class, int.class, int.class);

        Class<?> entityClass = MinecraftReflection.getMinecraftClass("world.entity.Entity");
        Class<?> voxelShapeCollisionClass = MinecraftReflection.getMinecraftClass("world.phys.shapes.VoxelShapeCollision");
        voxelShapeCollisionOfMethod = voxelShapeCollisionClass.getMethod("a", entityClass);

        Class<?> blockAccessClass = MinecraftReflection.getMinecraftClass("world.level.IBlockAccess");
        Class<?> blockDataClass = MinecraftReflection.getMinecraftClass("world.level.block.state.BlockBase$BlockData");
        blockDataGetOutlineShapeMethod = blockDataClass.getMethod("a", blockAccessClass, blockPositionClass, voxelShapeCollisionClass);

        Class<?> vec3dClass = MinecraftReflection.getMinecraftClass("world.phys.Vec3D");
        vec3dConstructor = vec3dClass.getConstructor(double.class, double.class, double.class);

        Class<?> voxelShapeClass = MinecraftReflection.getMinecraftClass("world.phys.shapes.VoxelShape");
        voxelShapeRaycastMethod = voxelShapeClass.getMethod("a", vec3dClass, vec3dClass, blockPositionClass);

        // 初期化完了
        initialized = true;
    }

    /**
     * アウトラインを取得
     *
     * @param block ブロック
     * @return NMS: VoxelShape
     */
    public static Object getOutline(Entity entity, BlockData block) throws ReflectiveOperationException {
        if (!initialized) {
            throw new ReflectiveOperationException("BlockOutline is not initialized.");
        }

        // ワールドを取得
        World world = entity.getWorld();
        // NMS: worldを取得
        Object nmsWorld = craftWorldGetHandleMethod.invoke(world);
        // NMS: entityを取得
        Object nmsEntity = craftEntityGetHandleMethod.invoke(entity);

        // NMS: VoxelShapeCollisionを作成
        Object nmsVoxelShapeCollision = voxelShapeCollisionOfMethod.invoke(null, nmsEntity);
        // NMS: BlockPositionを作成
        Object nmsBlockPosition = blockPositionConstructor.newInstance(0, 0, 0);

        // NMS: stateを取得
        Object nmsState = craftBlockDataGetStateMethod.invoke(block);
        // NMS: getOutlineShapeを呼び出し
        return blockDataGetOutlineShapeMethod.invoke(nmsState, nmsWorld, nmsBlockPosition, nmsVoxelShapeCollision);
    }

    /**
     * アウトラインにレイを当てたかどうか
     * ブロックが0,0,0にある想定で当たり判定をします
     *
     * @param nmsOutline NMS: VoxelShape
     * @param start      レイの始点
     * @param end        レイの終点
     * @return レイを当てたかどうか
     */
    public static RayTraceResult rayTraceOutline(Object nmsOutline, World world, Vector3f start, Vector3f end) throws ReflectiveOperationException {
        if (!initialized) {
            throw new ReflectiveOperationException("BlockOutline is not initialized.");
        }

        // NMS: Vec3Dを作成
        Object nmsStart = vec3dConstructor.newInstance(start.x, start.y, start.z);
        Object nmsEnd = vec3dConstructor.newInstance(end.x, end.y, end.z);

        // NMS: BlockPositionを作成
        Object nmsBlockPosition = blockPositionConstructor.newInstance(0, 0, 0);

        // NMS: raycastを呼び出し
        Object nmsRayTraceResult = voxelShapeRaycastMethod.invoke(nmsOutline, nmsStart, nmsEnd, nmsBlockPosition);
        return (RayTraceResult) craftRayTraceResultFromNMSMethod.invoke(null, world, nmsRayTraceResult);
    }
}
