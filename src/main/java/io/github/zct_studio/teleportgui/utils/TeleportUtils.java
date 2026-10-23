package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.StreamSupport;

import static net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT;

@SuppressWarnings("unused")
public final class TeleportUtils {

    private static final Set<String> UNSAFE_NON_COLLIDABLE_BLOCKS = Set.of(
            "block.minecraft.lava", "block.minecraft.flowing_lava", "block.minecraft.end_portal",
            "block.minecraft.end_gateway", "block.minecraft.fire", "block.minecraft.soul_fire",
            "block.minecraft.powder_snow", "block.minecraft.nether_portal"
    );

    private static final Map<String, PreviousPos> PREV_POS = new HashMap<>();

    private static final String CLASS_GEYSER_API = Constants.CLASS_GEYSER_API;
    private static final String CLASS_FLOODGATE_API = Constants.CLASS_FLOODGATE_API;
    private static final String CLASS_CUMULUS_FORM = "org.geysermc.cumulus.form.Form";

    private static final int SAFE_SEARCH_RADIUS = 3;

    private static volatile GeyserHandle GEYSER_HANDLE;
    private static volatile Method FLOODGATE_API_GETTER;
    private static volatile Object FLOODGATE_API_INSTANCE;
    private static volatile boolean CUMULUS_AVAILABLE;

    private TeleportUtils() {
    }

    public static void recordPreviousPos(ServerPlayer player) {
        PREV_POS.put(player.getStringUUID(),
                new PreviousPos(player.position(), player.level().dimension()));
    }

    public static Optional<PreviousPos> getPreviousPos(String uuid) {
        return Optional.ofNullable(PREV_POS.get(uuid));
    }

    public static void clearPreviousPos(String uuid) {
        PREV_POS.remove(uuid);
    }

    public static net.minecraft.server.MinecraftServer getServer() {
        return ServerMain.SERVER;
    }

    public static BlockPos getTeleportPos(Entity entity) {
        return new BlockPos(
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY() + 0.5D),
                (int) Math.floor(entity.getZ()));
    }

    public static BlockPos getStandablePos(ServerLevel world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 8 && !hasRoomForFeet(world, cursor); i++) {
            cursor.move(Direction.UP);
        }
        return cursor.immutable();
    }

    private static boolean hasRoomForFeet(ServerLevel world, BlockPos feet) {
        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()) {
            return false;
        }
        BlockPos below = feet.below();
        VoxelShape belowShape = world.getBlockState(below).getCollisionShape(world, below);
        return belowShape.isEmpty() || belowShape.max(Direction.Axis.Y) <= 1.0D;
    }

    public static boolean isBedrockPlayer(ServerPlayer player) {
        GeyserHandle handle = geyserHandle();
        if (handle == null) return false;
        try {
            Object api = handle.api().invoke(null);
            if (api == null) return false;
            Object connection = handle.connectionByUuid().invoke(api, player.getUUID());
            return connection != null;
        } catch (Exception e) {
            GEYSER_HANDLE = null;
            return false;
        }
    }

    private static GeyserHandle geyserHandle() {
        GeyserHandle cached = GEYSER_HANDLE;
        if (cached != null) return cached;
        try {
            Class<?> apiClass = Class.forName(CLASS_GEYSER_API);
            GeyserHandle handle = new GeyserHandle(
                    apiClass.getMethod("api"),
                    apiClass.getMethod("connectionByUuid", UUID.class));
            GEYSER_HANDLE = handle;
            return handle;
        } catch (Throwable e) {
            return null;
        }
    }

    private record GeyserHandle(Method api, Method connectionByUuid) {
    }

    public static boolean hasFloodgate() {
        if (!cumulusAvailable()) return false;
        return floodgateApi() != null;
    }

    private static boolean cumulusAvailable() {
        if (CUMULUS_AVAILABLE) return true;
        try {
            Class.forName(CLASS_CUMULUS_FORM);
            CUMULUS_AVAILABLE = true;
        } catch (Throwable ignored) {
        }
        return CUMULUS_AVAILABLE;
    }

    public static Object floodgateApi() {
        Object cached = floodgateApiInstance();
        if (cached != null) return cached;

        Method getter = floodgateApiGetter();
        if (getter == null) return null;
        try {
            Object api = getter.invoke(null);
            if (api != null) {
                FLOODGATE_API_INSTANCE = api;
            }
            return api;
        } catch (Throwable e) {
            FLOODGATE_API_GETTER = null;
            return null;
        }
    }

    private static Object floodgateApiInstance() {
        return FLOODGATE_API_INSTANCE;
    }

    private static Method floodgateApiGetter() {
        Method cached = FLOODGATE_API_GETTER;
        if (cached != null) return cached;
        try {
            Class<?> apiClass = Class.forName(CLASS_FLOODGATE_API);
            for (String name : new String[]{"getInstance", "api"}) {
                Method m;
                try {
                    m = apiClass.getMethod(name);
                } catch (NoSuchMethodException ignored) {
                    continue;
                }
                if (!Modifier.isStatic(m.getModifiers())) continue;
                m.setAccessible(true);
                FLOODGATE_API_GETTER = m;
                return m;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static List<String> getWorldIds() {
        var server = getServer();
        if (server == null) return List.of();
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .map(level -> level.dimension().identifier().toString())
                .toList();
    }

    public static void teleport(ServerPlayer player, ServerLevel world, Vec3 coords) {
        recordPreviousPos(player);
        world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.0D, 0.0D, 0.0D, 0.01);
        world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 1.0D, 0.0D, 0.03);
        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);

        boolean flying = player.getAbilities().flying;
        player.teleportTo(world, coords.x, coords.y, coords.z, Set.of(), player.getYRot(), player.getXRot(), false);

        if (flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);
        world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY(), player.getZ(), 20, 0.0D, 1.0D, 0.0D, 0.01);
        world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static Optional<BlockPos> findSafeBlock(BlockPos pos, ServerLevel world) {
        if (isSafe(pos, world)) return Optional.of(pos);

        for (int radius = 1; radius <= SAFE_SEARCH_RADIUS; radius++) {
            Optional<BlockPos> found = searchShell(pos, world, radius);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> searchShell(BlockPos center, ServerLevel world, int radius) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int y : new int[]{-radius, radius}) {
            for (int z = -radius; z <= radius; z++) {
                for (int x = -radius; x <= radius; x++) {
                    Optional<BlockPos> found = check(world, cx + x, cy + y, cz + z);
                    if (found.isPresent()) return found;
                }
            }
        }

        for (int x : new int[]{-radius, radius}) {
            for (int z = -radius + 1; z <= radius - 1; z++) {
                for (int y = -radius + 1; y <= radius - 1; y++) {
                    Optional<BlockPos> found = check(world, cx + x, cy + y, cz + z);
                    if (found.isPresent()) return found;
                }
            }
        }
        for (int z : new int[]{-radius, radius}) {
            for (int x = -radius + 1; x <= radius - 1; x++) {
                for (int y = -radius + 1; y <= radius - 1; y++) {
                    Optional<BlockPos> found = check(world, cx + x, cy + y, cz + z);
                    if (found.isPresent()) return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> check(ServerLevel world, int x, int y, int z) {
        BlockPos candidate = new BlockPos(x, y, z);
        return isSafe(candidate, world) ? Optional.of(candidate) : Optional.empty();
    }

    private static boolean isSafe(BlockPos bottomPlayer, ServerLevel world) {
        BlockPos below = bottomPlayer.below();
        BlockState belowState = world.getBlockState(below);
        BlockState feetState = world.getBlockState(bottomPlayer);
        BlockPos top = bottomPlayer.above();
        BlockState topState = world.getBlockState(top);

        String belowId = belowState.getBlock().getDescriptionId();
        boolean hasStandableGround = !"block.minecraft.water".equals(belowId)
                && !belowState.getCollisionShape(world, below).isEmpty();
        boolean bottomIsPassable = feetState.getCollisionShape(world, bottomPlayer).isEmpty()
                && !UNSAFE_NON_COLLIDABLE_BLOCKS.contains(feetState.getBlock().getDescriptionId());
        boolean topIsNotDanger =
                !UNSAFE_NON_COLLIDABLE_BLOCKS.contains(topState.getBlock().getDescriptionId());
        return hasStandableGround && bottomIsPassable && topIsNotDanger;
    }

    public static boolean isUnsafeNonCollidable(BlockState state) {
        return UNSAFE_NON_COLLIDABLE_BLOCKS.contains(state.getBlock().getDescriptionId());
    }

    public record PreviousPos(Vec3 pos, ResourceKey<Level> dim) {
    }
}
