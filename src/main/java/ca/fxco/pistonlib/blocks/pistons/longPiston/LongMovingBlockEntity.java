package ca.fxco.pistonlib.blocks.pistons.longPiston;

import ca.fxco.pistonlib.base.ModBlocks;
import ca.fxco.pistonlib.blocks.pistons.basePiston.BasicMovingBlockEntity;
import ca.fxco.pistonlib.pistonLogic.families.PistonFamily;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LongMovingBlockEntity extends BasicMovingBlockEntity {

    public static final int MAX_ARM_LENGTH = 11;

    public final int maxLength;
    public int length;
    public boolean isArm;
    public boolean skipCheck = false;

    public LongMovingBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);

        maxLength = 0;
    }

    public LongMovingBlockEntity(PistonFamily family, BlockPos pos, BlockState state, BlockState movedState,
                                 BlockEntity movedBlockEntity, Direction facing, boolean extending, boolean isSourcePiston) {
        this(family, pos, state, movedState, movedBlockEntity, facing, extending, isSourcePiston, MAX_ARM_LENGTH, 0, false);
    }

    public LongMovingBlockEntity(PistonFamily family, BlockPos pos, BlockState state, BlockState movedState,
                                 BlockEntity movedBlockEntity, Direction facing, boolean extending, boolean isSourcePiston,
                                 int maxLength, int length, boolean isArm) {
        super(family, pos, state, movedState, movedBlockEntity, facing, extending, isSourcePiston);

        this.maxLength = maxLength;
        this.length = length;
        this.isArm = isArm;
    }

    public boolean isArm() {
        return this.isArm;
    }

    public void skipCheck() {
        this.skipCheck = true;
    }

    @Override
    protected BlockState getStateForMovingEntities() {
        if (!this.extending) {
            return this.isSourcePiston && this.movedState.is(this.getFamily().getBase(this.type)) ?
                this.getFamily().getHead().defaultBlockState()
                    .setValue(BlockStateProperties.SHORT, this.progress > 0.25F)
                    .setValue(BlockStateProperties.FACING, this.movedState.getValue(BlockStateProperties.FACING)) :
                this.movedState;
        } else {
            return this.isArm && this.movedState.is(this.getFamily().getBase(this.type)) ?
                this.getFamily().getArm().defaultBlockState()
                    .setValue(BlockStateProperties.SHORT, this.isSourcePiston && this.progress > 0.25F)
                    .setValue(BlockStateProperties.FACING, this.movedState.getValue(BlockStateProperties.FACING)) :
                this.movedState;
        }
    }

    @Override
    protected void finishMovement(boolean removeSource) {
        if (this.finishArmMovement()) {
            super.finishMovement(removeSource);
        }
    }

    protected boolean finishArmMovement() {
        BlockPos arriveAt = this.worldPosition.relative(this.direction);
        BlockEntity blockEntity = this.level.getBlockEntity(arriveAt);
        //Should tick before the other piston block entities because it was created first. But not really V
        // Compensate for mojang's terrible code - Carpet-Fixes Rule: `reloadUpdateOrderFix` is the proper fix
        if (blockEntity instanceof LongMovingBlockEntity bpbe && ((bpbe.length == 0) != bpbe.isArm()) &&
                bpbe.length == this.length-1 && bpbe.direction == this.direction) bpbe.finalTick();
        /*if (be.skipCheck || (be.length < be.maxLength-1 &&
                be.getPistonHandler(be.world, arriveAt, be.facing, be.extending)
                        .calculatePullPushWithBE(!be.extending, LongPistonBlockEntity::skipCheck))) {
            BlockState blockState = be.world.getBlockState(be.pos);
            be.world.setBlockState(arriveAt, blockState, Block.FORCE_STATE);
            be.world.addBlockEntity(((LongPistonExtensionBlock) be.EXTENSION_BLOCK).createLongPistonBlockEntity(
                    arriveAt,
                    blockState,
                    be.movedState,
                    be.facing,
                    be.extending,
                    false,
                    be.maxLength - 1,
                    be.length,
                    be.arm
            ));
            if (be.isSource()) {
                BlockPos pos = be.pos.offset(be.facing);
                // TODO: Make more configurable
                BlockState armState = ModBlocks.LONG_PISTON_ARM.defaultBlockState
                        .setValue(BlockStateProperties.SHORT, false)
                        .setValue(BlockStateProperties.FACING, be.facing)
                        .setValue(BlockStateProperties.PISTON_TYPE, blockState.get(BlockStateProperties.PISTON_TYPE));
                be.world.setBlockState(pos, armState, Block.FORCE_STATE);
                be.world.addBlockEntity(((LongPistonExtensionBlock) be.EXTENSION_BLOCK).createLongPistonBlockEntity(
                        pos,
                        Blocks.EMERALD_BLOCK.defaultBlockState,
                        Blocks.EMERALD_BLOCK.defaultBlockState,
                        be.facing,
                        be.extending,
                        true,
                        be.maxLength,
                        be.length + 1,
                        true
                ));
            } else {
                be.world.removeBlockEntity(be.pos);
                be.setRemoved();
            }
            return false;
        }*/
        // TODO: Make more configurable          V
        ModBlocks.LONG_PISTON.moveBlocks(this.level, this.worldPosition, this.direction, this.extending);
        return true;
    }

    @Override
    protected BlockState getMovingStateForCollisionShape() {
        if (this.isSourcePiston) {
            if (this.isArm) {
                return this.getFamily().getArm().defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, this.direction)
                    .setValue(PistonHeadBlock.SHORT, this.extending != 1.0F - this.progress < 0.25F);
            } else {
                return this.getFamily().getHead().defaultBlockState()
                    .setValue(PistonHeadBlock.FACING, this.direction)
                    .setValue(PistonHeadBlock.SHORT, this.extending != 1.0F - this.progress < 0.25F);
            }
        } else {
            return this.movedState;
        }
    }
}
