package ca.fxco.pistonlib.pistonLogic.structureResolvers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.fxco.pistonlib.blocks.pistons.basePiston.BasicPistonBaseBlock;
import ca.fxco.pistonlib.pistonLogic.accessible.ConfigurablePistonBehavior;
import ca.fxco.pistonlib.pistonLogic.accessible.ConfigurablePistonStickiness;
import ca.fxco.pistonlib.pistonLogic.families.PistonFamily;
import ca.fxco.pistonlib.pistonLogic.internal.BlockStateBaseExpandedSticky;
import ca.fxco.pistonlib.pistonLogic.sticky.StickRules;
import ca.fxco.pistonlib.pistonLogic.sticky.StickyType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class BasicStructureResolver extends PistonStructureResolver {

    protected final PistonFamily family;
    protected final BasicPistonBaseBlock piston;
    protected final int maxMovableBlocks;

    public BasicStructureResolver(BasicPistonBaseBlock piston, Level level, BlockPos pos,
                                  Direction facing, boolean extend) {
        super(level, pos, facing, extend );

        this.family = piston.family;
        this.piston = piston;
        this.maxMovableBlocks = this.family.getPushLimit();
    }

    @Override
    public boolean resolve() {
        this.toPush.clear();
        this.toDestroy.clear();
        BlockState state = this.level.getBlockState(this.startPos);
        if (!this.piston.canMoveBlock(state, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending) {
                ConfigurablePistonBehavior pistonBehavior = (ConfigurablePistonBehavior)state.getBlock();
                if (pistonBehavior.usesConfigurablePistonBehavior()) {
                    if (pistonBehavior.canDestroy(state)) {
                        this.toDestroy.add(this.startPos);
                        return true;
                    }
                } else if (state.getPistonPushReaction() == PushReaction.DESTROY) {
                    this.toDestroy.add(this.startPos);
                    return true;
                }
                return false;
            }
            return false;
        } else {
            if (this.cantMove(this.startPos, !this.extending ? this.pushDirection.getOpposite() : this.pushDirection))
                return false;
        }
        for (int i = 0; i < this.toPush.size(); ++i) {
            BlockPos blockPos = this.toPush.get(i);
            state = this.level.getBlockState(blockPos);
            ConfigurablePistonStickiness stick = (ConfigurablePistonStickiness) state.getBlock();
            if (stick.usesConfigurablePistonStickiness()) {
                if (stick.isSticky(state) && cantMoveAdjacentStickyBlocks(stick.stickySides(state), blockPos))
                    return false;
            } else {
                if (stick.hasStickyGroup() && this.cantMoveAdjacentBlocks(blockPos))
                    return false;
            }
        }
        return true;
    }

    protected boolean cantMoveAdjacentBlocks(BlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != this.pushDirection.getAxis()) {
                BlockPos blockPos = pos.relative(direction);
                BlockState blockState2 = this.level.getBlockState(blockPos);
                if (canAdjacentBlockStick(direction, blockState, blockState2) && this.cantMove(blockPos, direction))
                    return true;
            }
        }
        return false;
    }

    protected boolean cantMoveAdjacentStickyBlocks(Map<Direction, StickyType> sides, BlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        for (Map.Entry<Direction, StickyType> sideData : sides.entrySet()) {
            StickyType stickyType = sideData.getValue();
            if (stickyType == StickyType.NO_STICK) continue;
            Direction dir = sideData.getKey();
            if (dir.getAxis() != this.pushDirection.getAxis()) {
                BlockPos blockPos = pos.relative(dir);
                BlockState adjState = this.level.getBlockState(blockPos);
                if (stickyType == StickyType.CONDITIONAL && !stickyType.canStick(blockState, adjState, dir)) {
                    continue;
                }
                if (canAdjacentBlockStick(dir, blockState, adjState) && this.cantMove(blockPos, dir))
                    return true;
            }
        }
        return false;
    }


    // Stickiness checks
    protected static boolean canAdjacentBlockStick(Direction dir, BlockState state, BlockState adjState) {
        BlockStateBaseExpandedSticky stick = (BlockStateBaseExpandedSticky)adjState;
        if (stick.usesConfigurablePistonStickiness()) {
            if (!stick.isSticky()) return true;
            StickyType type = stick.sideStickiness(dir.getOpposite());
            if (type == StickyType.CONDITIONAL && !type.canStick(state, adjState, dir)) {
                return true;
            }
            return type != StickyType.NO_STICK;
        }
        return StickRules.test(((BlockStateBaseExpandedSticky)state).getStickyGroup(), stick.getStickyGroup());
    }

    protected boolean cantMove(BlockPos pos, Direction dir) {
        BlockState state = this.level.getBlockState(pos);
        if (state.isAir() || pos.equals(this.pistonPos) || this.toPush.contains(pos)) return false;
        if (!this.piston.canMoveBlock(state, this.level, pos, this.pushDirection, false, dir)) return false;
        int i = 1;
        if (i + this.toPush.size() > this.maxMovableBlocks) return true;
        Direction dir2 = this.pushDirection.getOpposite();
        ConfigurablePistonStickiness stick = (ConfigurablePistonStickiness)state.getBlock();
        boolean isSticky = stick.usesConfigurablePistonStickiness() ?
                (stick.isSticky(state) && stick.sideStickiness(state, dir2).ordinal() >= StickyType.STICKY.ordinal()) :
                stick.hasStickyGroup();
        while (isSticky) {
            BlockPos blockPos = pos.relative(dir2, i);
            BlockState blockState2 = state;
            state = this.level.getBlockState(blockPos);
            stick = (ConfigurablePistonStickiness)state.getBlock();
            if (state.isAir() ||
                    !canAdjacentBlockStick(dir2, blockState2, state) ||
                    blockPos.equals(this.pistonPos) ||
                    !this.piston.canMoveBlock(state, this.level, blockPos, this.pushDirection, false, dir2))
                break;
            if (++i + this.toPush.size() > this.maxMovableBlocks) return true;
            if (stick.usesConfigurablePistonStickiness()) {
                boolean StickyStick = stick.isSticky(state);
                if (StickyStick && stick.sideStickiness(state, dir2).ordinal() < StickyType.STICKY.ordinal())
                    break;
                isSticky = StickyStick;
            } else {
                isSticky = stick.hasStickyGroup();
            }
        }
        int j = 0, k;
        for(k = i - 1; k >= 0; --k) {
            this.toPush.add(pos.relative(dir2, k));
            ++j;
        }
        k = 1;
        while(true) {
            BlockPos pos2 = pos.relative(this.pushDirection, k);
            int l = this.toPush.indexOf(pos2);
            if (l > -1) {
                this.setMovedBlocks(j, l);
                for(int m = 0; m <= l + j; ++m) {
                    BlockPos pos3 = this.toPush.get(m);
                    state = this.level.getBlockState(pos3);
                    stick = (ConfigurablePistonStickiness)state.getBlock();
                    if (stick.usesConfigurablePistonStickiness()) {
                        if (stick.isSticky(state) && this.cantMoveAdjacentStickyBlocks(stick.stickySides(state),pos3))
                            return true;
                    } else {
                        if (stick.hasStickyGroup() && this.cantMoveAdjacentBlocks(pos3))
                            return true;
                    }
                }
                return false;
            }
            state = this.level.getBlockState(pos2);
            if (state.isAir())
                return false;
            if (pos2.equals(this.pistonPos))
                return true;
            if (!piston.canMoveBlock(state, this.level, pos2, this.pushDirection, true, this.pushDirection))
                return true;
            ConfigurablePistonBehavior pistonBehavior = (ConfigurablePistonBehavior)state.getBlock();
            if (pistonBehavior.usesConfigurablePistonBehavior()) {
                if (pistonBehavior.canDestroy(state)) {
                    this.toDestroy.add(pos2);
                    return false;
                }
            } else if (state.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(pos2);
                return false;
            }
            if (this.toPush.size() >= this.maxMovableBlocks) return true;
            this.toPush.add(pos2);
            ++j;
            ++k;
        }
    }

    protected void setMovedBlocks(int from, int to) {
        List<BlockPos> list = new ArrayList<>();
        List<BlockPos> list2 = new ArrayList<>();
        List<BlockPos> list3 = new ArrayList<>();
        list.addAll(this.toPush.subList(0, to));
        list2.addAll(this.toPush.subList(this.toPush.size() - from, this.toPush.size()));
        list3.addAll(this.toPush.subList(to, this.toPush.size() - from));
        this.toPush.clear();
        this.toPush.addAll(list);
        this.toPush.addAll(list2);
        this.toPush.addAll(list3);
    }

    public int getMoveLimit() {
        return this.maxMovableBlocks;
    }

    @FunctionalInterface
    public interface Factory<T extends BasicStructureResolver> {

        T create(Level level, BlockPos pos, Direction facing, boolean extend);

    }
}
