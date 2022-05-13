package ca.fxco.configurablepistons.blocks.pistons.slipperyPiston;

import ca.fxco.configurablepistons.base.ModBlockEntities;
import ca.fxco.configurablepistons.base.ModBlocks;
import ca.fxco.configurablepistons.base.ModTags;
import ca.fxco.configurablepistons.blocks.pistons.basePiston.BasicPistonExtensionBlock;
import ca.fxco.configurablepistons.blocks.slipperyBlocks.AbstractSlipperyBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static ca.fxco.configurablepistons.base.ModProperties.SLIPPERY_DISTANCE;
import static ca.fxco.configurablepistons.blocks.slipperyBlocks.AbstractSlipperyBlock.*;

public class SlipperyPistonExtensionBlock extends BasicPistonExtensionBlock {
    public SlipperyPistonExtensionBlock() {
        super();
        this.setDefaultState(this.stateManager.getDefaultState().with(SLIPPERY_DISTANCE, MAX_DISTANCE));
    }

    @Override
    public BlockEntity createPistonBlockEntity(BlockPos pos, BlockState state, BlockState pushedBlock,
                                               Direction facing, boolean extending, boolean source) {
        return new SlipperyPistonBlockEntity(pos, state, pushedBlock, facing, extending, source,
                ModBlocks.SLIPPERY_MOVING_PISTON);
    }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World w, BlockState state, BlockEntityType<T> t) {
        return checkType(t, ModBlockEntities.SLIPPERY_PISTON_BLOCK_ENTITY, SlipperyPistonBlockEntity::tick);
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock()) && !world.isClient && world.getBlockEntity(pos) == null)
            world.createAndScheduleBlockTick(pos, this, DELAY);
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!world.isClient()) {
            if (neighborState.isIn(ModTags.SLIPPERY_BLOCKS)) {
                world.createAndScheduleBlockTick(pos, this, SLIP_DELAY);
            } else {
                world.createAndScheduleBlockTick(pos, this, DELAY);
            }
        }
        return state;
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int i = AbstractSlipperyBlock.calculateDistance(world, pos);
        BlockState blockState = state.with(SLIPPERY_DISTANCE, i);
        if (blockState.get(SLIPPERY_DISTANCE) == MAX_DISTANCE) {
            FallingBlockEntity.spawnFromBlock(world, pos, blockState);
        } else if (state != blockState) {
            //world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
        }
    }

    //public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    //    return AbstractSlipperyBlock.calculateDistance(world, pos) < MAX_DISTANCE;
    //}

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, SLIPPERY_DISTANCE);
    }
}
