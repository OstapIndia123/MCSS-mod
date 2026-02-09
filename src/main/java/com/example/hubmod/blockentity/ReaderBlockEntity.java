package com.example.hubmod.blockentity;

import com.example.hubmod.HubBlockIds;
import com.example.hubmod.block.ReaderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReaderBlockEntity extends BlockEntity {

    private static final String NBT_READER_ID = "readerId";
    private static final String NBT_OUT_LEVEL = "outLevel";

    private static final Set<ReaderBlockEntity> LOADED = ConcurrentHashMap.newKeySet();
    private boolean registered = false;
    private String readerId;
    // выход 0..15 (как у Hub)
    private int outLevel = 0;

    public ReaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.READER_BLOCK_ENTITY, pos, state);
    }

    public static Set<ReaderBlockEntity> loaded() {
        return LOADED;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, ReaderBlockEntity reader) {
        reader.tickServer();
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String id) {
        if (id == null || id.isBlank()) return;
        this.readerId = id;
        setChanged();
    }

    public String ensureReaderId() {
        if (readerId != null && !readerId.isBlank()) return readerId;
        readerId = HubBlockIds.newId("READER-");
        setChanged();
        return readerId;
    }

    // ---- REDSTONE OUT ----

    public int getOutputLevel() {
        int v = outLevel;
        if (v < 0) return 0;
        if (v > 15) return 15;
        return v;
    }

    public void setOutputLevel(int level15) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        int lvl = Math.max(0, Math.min(15, level15));
        if (this.outLevel == lvl) return;

        this.outLevel = lvl;

        // визуал: powered=true если lvl>0
        BlockState st = getBlockState();
        if (st.hasProperty(ReaderBlock.POWERED)) {
            boolean want = lvl > 0;
            boolean cur = st.getValue(ReaderBlock.POWERED);
            if (cur != want) {
                BlockState newSt = st.setValue(ReaderBlock.POWERED, want);
                level.setBlock(worldPosition, newSt, 3);
            }
        }

        // обновляем редстоун вокруг (как у Hub)
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Direction d : Direction.values()) {
            level.updateNeighborsAt(worldPosition.relative(d), getBlockState().getBlock());
        }
        level.sendBlockUpdated(worldPosition, st, getBlockState(), 3);

        setChanged();

        com.example.hubmod.util.HubLog.d("[READER] OUT applied at " + worldPosition + " level=" + lvl);
    }

    // ===== NBT (ValueInput/ValueOutput) =====

    @Override
    protected void loadAdditional(@NonNull ValueInput in) {
        super.loadAdditional(in);

        String id = in.getString(NBT_READER_ID).orElse("");
        this.readerId = id.isBlank() ? null : id;

        Integer lvl = in.getInt(NBT_OUT_LEVEL).orElse(0);
        this.outLevel = Math.max(0, Math.min(15, lvl));
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput out) {
        super.saveAdditional(out);

        if (readerId != null && !readerId.isBlank()) {
            out.putString(NBT_READER_ID, readerId);
        }
        out.putInt(NBT_OUT_LEVEL, getOutputLevel());
    }

    public void tickServer() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        if (!registered) {
            registered = true;
            LOADED.add(this);
            ensureReaderId();
            setChanged();
        }
    }
}
