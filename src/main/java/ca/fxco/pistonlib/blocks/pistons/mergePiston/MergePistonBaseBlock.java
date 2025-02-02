package ca.fxco.pistonlib.blocks.pistons.mergePiston;

import ca.fxco.pistonlib.base.ModBlocks;
import ca.fxco.pistonlib.blocks.pistons.basePiston.BasicMovingBlock;
import ca.fxco.pistonlib.blocks.pistons.basePiston.BasicPistonBaseBlock;
import ca.fxco.pistonlib.blocks.pistons.basePiston.BasicPistonHeadBlock;
import ca.fxco.pistonlib.impl.BlockEntityMerging;
import ca.fxco.pistonlib.pistonLogic.families.PistonFamily;
import ca.fxco.pistonlib.pistonLogic.structureResolvers.BasicStructureResolver;
import ca.fxco.pistonlib.pistonLogic.structureResolvers.MergingPistonStructureResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MergePistonBaseBlock extends BasicPistonBaseBlock {

	public MergePistonBaseBlock(PistonFamily family, PistonType type) {
        super(family, type);
    }

    @Override
    public BasicStructureResolver newStructureResolver(Level level, BlockPos pos, Direction facing, boolean extend) {
        return new MergingPistonStructureResolver(this, level, pos, facing, extend);
    }

    @Override
    public boolean moveBlocks(Level level, BlockPos pos, Direction facing, boolean extend, BasicStructureResolver.Factory<? extends BasicStructureResolver> structureProvider) {
        if (!extend) {
            BlockPos headPos = pos.relative(facing);
            BlockState headState = level.getBlockState(headPos);

            if (headState.is(this.family.getHead())) {
                level.setBlock(headPos, Blocks.AIR.defaultBlockState(), UPDATE_KNOWN_SHAPE | UPDATE_INVISIBLE);
            }
        }

        MergingPistonStructureResolver structure = (MergingPistonStructureResolver) structureProvider.create(level, pos, facing, extend);

        if (!structure.resolve()) {
            return false;
        }

        Map<BlockPos, BlockState> toRemove = new LinkedHashMap<>();
        List<BlockPos> toMove = structure.getToPush();
        List<BlockPos> toDestroy = structure.getToDestroy();
        List<BlockPos> toMerge = structure.getToMerge();
        List<BlockState> statesToMove = new ArrayList<>();
        List<BlockEntity> blockEntitiesToMove = new ArrayList<>();

        // collect blocks to move
        for (BlockPos posToMove : toMove) {
            BlockState stateToMove = level.getBlockState(posToMove);
            BlockEntity blockEntityToMove = level.getBlockEntity(posToMove);

            if (blockEntityToMove != null) {
                level.removeBlockEntity(posToMove);
                blockEntityToMove.setChanged();
            }

            statesToMove.add(stateToMove);
            blockEntitiesToMove.add(blockEntityToMove);
            toRemove.put(posToMove, stateToMove);
        }

        BlockState[] affectedStates = new BlockState[toMove.size() + toDestroy.size()];
        int affectedIndex = 0;

        Direction moveDir = extend ? facing : facing.getOpposite();

        // Merge Blocks
        for (int i = toMerge.size() - 1; i >= 0; i--) {
            BlockPos posToMerge = toMerge.get(i);
            BlockState stateToMerge = level.getBlockState(posToMerge);

            BlockPos mergeIntoPos = posToMerge.relative(moveDir);
            BlockState mergeIntoState = level.getBlockState(mergeIntoPos);

            if (mergeIntoState.getBlock() instanceof MergeBlock) { // MultiMerge
                if (level.getBlockEntity(mergeIntoPos) instanceof MergeBlockEntity mergeBlockEntity) {
                    if (mergeBlockEntity.initialBlockEntity != null) {
                        BlockEntity blockEntityToMerge = level.getBlockEntity(posToMerge);
                        if (blockEntityToMerge instanceof BlockEntityMerging bem2 &&
                                bem2.shouldStoreSelf(mergeBlockEntity)) {
                            bem2.onMerge(mergeBlockEntity, moveDir);
                            mergeBlockEntity.doMerge(stateToMerge, blockEntityToMerge, moveDir, 1); //TODO: Add speed
                        } else {
                            mergeBlockEntity.doMerge(stateToMerge, moveDir, 1); //TODO: Add speed
                        }
                    }
                    mergeBlockEntity.doMerge(stateToMerge, moveDir, 1); //TODO: Add speed
                }
            } else {
                BlockState mergeBlockState = ModBlocks.MERGE_BLOCK.defaultBlockState();
                MergeBlockEntity mergeBlockEntity;
                BlockEntity mergeIntoBlockEntity = level.getBlockEntity(mergeIntoPos);
                if (mergeIntoBlockEntity instanceof BlockEntityMerging bem && bem.doMerging()) {
                    mergeBlockEntity = new MergeBlockEntity(mergeIntoPos, mergeBlockState, mergeIntoState, mergeIntoBlockEntity);
                    bem.onMerge(mergeBlockEntity, moveDir); // Call onMerge for the base block entity

                    BlockEntity blockEntityToMerge = level.getBlockEntity(posToMerge);
                    if (blockEntityToMerge instanceof BlockEntityMerging bem2 &&
                            bem2.shouldStoreSelf(mergeBlockEntity)) {
                        bem2.onMerge(mergeBlockEntity, moveDir);
                        mergeBlockEntity.doMerge(stateToMerge, blockEntityToMerge, moveDir, 1); //TODO: Add speed
                    } else {
                        mergeBlockEntity.doMerge(stateToMerge, moveDir, 1); //TODO: Add speed
                    }
                } else {
                    mergeBlockEntity = new MergeBlockEntity(mergeIntoPos, mergeBlockState, mergeIntoState);
                    mergeBlockEntity.doMerge(stateToMerge, moveDir, 1); //TODO: Add speed
                }

                level.setBlock(posToMerge, Blocks.AIR.defaultBlockState(), UPDATE_KNOWN_SHAPE | UPDATE_CLIENTS);

                level.setBlock(mergeIntoPos, mergeBlockState, UPDATE_MOVE_BY_PISTON | UPDATE_INVISIBLE);
                level.setBlockEntity(mergeBlockEntity);
            }
        }

        // destroy blocks
        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockPos posToDestroy = toDestroy.get(i);
            BlockState stateToDestroy = level.getBlockState(posToDestroy);
            BlockEntity blockEntityToDestroy = level.getBlockEntity(posToDestroy);

            dropResources(stateToDestroy, level, posToDestroy, blockEntityToDestroy);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_KNOWN_SHAPE | UPDATE_CLIENTS);
            if (!stateToDestroy.is(BlockTags.FIRE)) {
                level.addDestroyBlockEffect(posToDestroy, stateToDestroy);
            }

            affectedStates[affectedIndex++] = stateToDestroy;
        }

        // move blocks
        for (int i = toMove.size() - 1; i >= 0; i--) {
            BlockPos posToMove = toMove.get(i);
            BlockPos dstPos = posToMove.relative(moveDir);
            BlockState stateToMove = statesToMove.get(i);
            BlockEntity blockEntityToMove = blockEntitiesToMove.get(i);

            toRemove.remove(dstPos);

            BlockState movingBlock = this.family.getMoving().defaultBlockState()
                    .setValue(BasicMovingBlock.FACING, facing);
            BlockEntity movingBlockEntity = this.family
                    .newMovingBlockEntity(dstPos, movingBlock, stateToMove, blockEntityToMove, facing, extend, false);

            level.setBlock(dstPos, movingBlock, UPDATE_MOVE_BY_PISTON | UPDATE_INVISIBLE);
            level.setBlockEntity(movingBlockEntity);

            affectedStates[affectedIndex++] = stateToMove;
        }

        // place extending head
        if (extend) {
            BlockPos headPos = pos.relative(facing);
            BlockState headState = this.family.getHead().defaultBlockState()
                    .setValue(BasicPistonHeadBlock.FACING, facing);

            toRemove.remove(headPos);

            BlockState movingBlock = this.family.getMoving().defaultBlockState()
                    .setValue(BasicMovingBlock.TYPE, this.type)
                    .setValue(BasicMovingBlock.FACING, facing);
            BlockEntity movingBlockEntity = this.family
                    .newMovingBlockEntity(headPos, movingBlock, headState, null, facing, extend, true);

            level.setBlock(headPos, movingBlock, UPDATE_MOVE_BY_PISTON | UPDATE_INVISIBLE);
            level.setBlockEntity(movingBlockEntity);
        }

        // remove left over blocks
        BlockState air = Blocks.AIR.defaultBlockState();

        for (BlockPos posToRemove : toRemove.keySet()) {
            level.setBlock(posToRemove, air, UPDATE_MOVE_BY_PISTON | UPDATE_KNOWN_SHAPE | UPDATE_CLIENTS);
        }

        // do neighbor updates
        for (Map.Entry<BlockPos, BlockState> entry : toRemove.entrySet()) {
            BlockPos removedPos = entry.getKey();
            BlockState removedState = entry.getValue();

            removedState.updateIndirectNeighbourShapes(level, removedPos, UPDATE_CLIENTS);
            air.updateNeighbourShapes(level, removedPos, UPDATE_CLIENTS);
            air.updateIndirectNeighbourShapes(level, removedPos, UPDATE_CLIENTS);
        }

        affectedIndex = 0;

        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockPos destroyedPos = toDestroy.get(i);
            BlockState destroyedState = affectedStates[affectedIndex++];

            destroyedState.updateIndirectNeighbourShapes(level, destroyedPos, UPDATE_CLIENTS);
            level.updateNeighborsAt(destroyedPos, destroyedState.getBlock());
        }
        for (int i = toMove.size() - 1; i >= 0; i--) {
            BlockPos movedPos = toMove.get(i);
            BlockState movedState = affectedStates[affectedIndex++];

            level.updateNeighborsAt(movedPos, movedState.getBlock());
        }
        if (extend) {
            level.updateNeighborsAt(pos.relative(facing), this.family.getHead());
        }

        return true;
    }

    @Override
    public boolean canMoveBlock(BlockState state) {
        return true;
    }
}
