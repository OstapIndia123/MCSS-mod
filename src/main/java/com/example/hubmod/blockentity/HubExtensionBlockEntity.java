package com.example.hubmod.blockentity;

import com.example.hubmod.HubBlockIds;
import com.example.hubmod.config.HubModConfig;
import com.example.hubmod.net.HubWsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HubExtensionBlockEntity extends BlockEntity {

    private static final String NBT_HUB_ID = "hubId";

    private static final Set<HubExtensionBlockEntity> LOADED = ConcurrentHashMap.newKeySet();
    // редстоун-сэмплинг: раз в 2 тика (можно 2-5)
    private static final int SAMPLE_TICKS = 2;
    // последнее значение по каждой стороне (для change-detect)
    private final int[] lastIn = new int[6];
    // ВЫХОДЫ (0..15) по сторонам — именно это будет отдавать HubExtensionBlock#getSignal()
    private final int[] outLevels = new int[6];
    private int sampleCounter = 0;
    // Пинг вычисляется динамически из конфига: pingMs = min(60s, failAfter/3), но не меньше 5s
    private int pingCounterTicks = 0;
    private boolean registered = false;
    // ID конкретного хаба (переносится из предмета при установке)
    private String hubId;

    public HubExtensionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HUB_EXTENSION_BLOCK_ENTITY, pos, state);
        Arrays.fill(lastIn, Integer.MIN_VALUE);
        Arrays.fill(outLevels, 0);
    }

    public static Set<HubExtensionBlockEntity> loaded() {
        return LOADED;
    }

    // Нужно для вызова из BlockEntityTicker
    public static void tickServer(Level level, BlockPos pos, BlockState state, HubExtensionBlockEntity hub) {
        hub.tickServer();
    }

    private static int msToTicks(long ms) {
        long t = ms / 50L; // 1 tick = 50ms
        if (t < 1) t = 1;
        if (t > Integer.MAX_VALUE) t = Integer.MAX_VALUE;
        return (int) t;
    }

    private static int computePingTargetTicks() {
        HubModConfig cfg = HubModConfig.get();

        long failAfter = cfg.testFailAfterMs;
        if (failAfter < 5_000) failAfter = 5_000;

        // 2 пинга за окно failAfter:
        // pingMs = failAfter / 2
        // Пример: failAfter=300_000 => pingMs=150_000 (150s) => 2 пинга за 5 минут
        long pingMs = failAfter / 2L;

        // safety clamp: не чаще 5s и не реже failAfter (чтобы не уходить в "молчание" дольше окна)
        if (pingMs < 5_000L) pingMs = 5_000L;
        if (pingMs > failAfter) pingMs = failAfter;

        return msToTicks(pingMs);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String id) {
        if (id == null || id.isBlank()) return;
        this.hubId = id;
        setChanged();
    }

    // ===== NBT (1.21.11): ValueInput/ValueOutput =====

    public String ensureHubId() {
        if (hubId != null && !hubId.isBlank()) return hubId;
        hubId = HubBlockIds.newExtensionId();
        setChanged();
        return hubId;
    }

    // Выходной уровень для конкретной стороны (0..15)
    public int getOutputLevel(Direction dir) {
        if (dir == null) return 0;
        int v = outLevels[dir.ordinal()];
        if (v < 0) return 0;
        if (v > 15) return 15;
        return v;
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput in) {
        super.loadAdditional(in);

        String id = in.getString(NBT_HUB_ID).orElse("");
        this.hubId = id.isBlank() ? null : id;
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput out) {
        super.saveAdditional(out);

        if (hubId != null && !hubId.isBlank()) {
            out.putString(NBT_HUB_ID, hubId);
        }
    }

    // ===== main tick =====

    public void tickServer() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        // регистрация в LOADED
        if (!registered) {
            registered = true;
            LOADED.add(this);

            // СРАЗУ фиксируем hubId и делаем "пинг" на backend.
            String effectiveHubId = ensureHubId();
            HubWsClient.sendJson(Map.of(
                    "type", "HUB_PING",
                    "hubId", effectiveHubId,
                    "pos", Map.of("x", worldPosition.getX(), "y", worldPosition.getY(), "z", worldPosition.getZ()),
                    "ts", System.currentTimeMillis()
            ));

            pingCounterTicks = 0;
        }

        // периодический пинг (интервал зависит от testFailAfterMs в hubmod.yml)
        int pingTarget = computePingTargetTicks();
        if (++pingCounterTicks >= pingTarget) {
            pingCounterTicks = 0;

            String effectiveHubId = ensureHubId();
            HubWsClient.sendJson(Map.of(
                    "type", "HUB_PING",
                    "hubId", effectiveHubId,
                    "pos", Map.of("x", worldPosition.getX(), "y", worldPosition.getY(), "z", worldPosition.getZ()),
                    "ts", System.currentTimeMillis()
            ));
        }

        // сэмплинг редстоуна раз в SAMPLE_TICKS
        if (++sampleCounter < SAMPLE_TICKS) return;
        sampleCounter = 0;

        String effectiveHubId = ensureHubId();

        // читаем сигнал ОТ СОСЕДА в сторону хаба
        for (Direction d : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(d);
            int power = level.getSignal(neighbor, d.getOpposite());

            int idx = d.ordinal();
            int prev = lastIn[idx];

            if (prev != power) {
                lastIn[idx] = power;

                com.example.hubmod.util.HubLog.d("[HUB_EXT][IN] " + worldPosition
                        + " hubId=" + effectiveHubId
                        + " side=" + d.getName()
                        + " power=" + power
                        + " (changed)");

                HubWsClient.sendPortIn(effectiveHubId, worldPosition, d, power);
            }
        }
    }

    // Применение CONFIG от backend (пока заглушка)
    public void applyPortConfig(Map<String, Map<String, Object>> ports) {
        com.example.hubmod.util.HubLog.d("[HUB_EXT] CONFIG applied");
        setChanged();
    }

    // SET_OUTPUT от backend
    public void setOutput(Direction dir, int level15) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        if (dir == null) return;

        int lvl = Math.max(0, Math.min(15, level15));
        outLevels[dir.ordinal()] = lvl;

        // Визуал: булевки по сторонам (вкл/выкл)
        BlockState st = getBlockState();
        BlockState newSt = switch (dir) {
            case UP -> st.setValue(com.example.hubmod.block.HubExtensionBlock.UP, lvl > 0);
            case DOWN -> st.setValue(com.example.hubmod.block.HubExtensionBlock.DOWN, lvl > 0);
            case NORTH -> st.setValue(com.example.hubmod.block.HubExtensionBlock.NORTH, lvl > 0);
            case SOUTH -> st.setValue(com.example.hubmod.block.HubExtensionBlock.SOUTH, lvl > 0);
            case WEST -> st.setValue(com.example.hubmod.block.HubExtensionBlock.WEST, lvl > 0);
            case EAST -> st.setValue(com.example.hubmod.block.HubExtensionBlock.EAST, lvl > 0);
        };

        // Ставим state только если изменился (чтобы не дёргать лишний раз)
        if (newSt != st) {
            level.setBlock(worldPosition, newSt, 3);
        }

        // ВАЖНО: обновляем соседей, чтобы редстоун пересчитал силу
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Direction d : Direction.values()) {
            level.updateNeighborsAt(worldPosition.relative(d), getBlockState().getBlock());
        }

        level.sendBlockUpdated(worldPosition, st, getBlockState(), 3);
        setChanged();

        com.example.hubmod.util.HubLog.d("[HUB_EXT] OUT applied at " + worldPosition + " side=" + dir.getName() + " level=" + lvl);
    }
}
