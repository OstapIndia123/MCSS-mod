package com.example.hubmod.block;

import com.example.hubmod.blockentity.HubBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.NonNull;

public class HubBlock extends Block implements EntityBlock {

    // OUT-визуал по сторонам (вкл/выкл)
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public HubBlock(Properties props) {
        super(props);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST);
    }

    // --- Redstone output ---

    @Override
    public boolean isSignalSource(@NonNull BlockState state) {
        return true;
    }

    @Override
    public int getSignal(@NonNull BlockState state, BlockGetter level, @NonNull BlockPos pos, @NonNull Direction dir) {
        // Сила сигнала 0..15 берётся из BlockEntity, а не из булевок
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HubBlockEntity hub) {
            return hub.getOutputLevel(dir);
        }
        return 0;
    }

    @Override
    public int getDirectSignal(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull Direction dir) {
        return getSignal(state, level, pos, dir);
    }

    // --- BlockEntity ---

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new HubBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, p, st, be) -> {
            if (be instanceof HubBlockEntity hub) HubBlockEntity.tickServer(lvl, p, st, hub);
        };
    }

    @Override
    public void setPlacedBy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state, LivingEntity placer, @NonNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HubBlockEntity hub) {
                // если в ItemStack лежит hubId — подтянем
                String id = com.example.hubmod.item.HubBlockItem.getHubId(stack);
                if (id != null) hub.setHubId(id);
            }
        }
    }
}
