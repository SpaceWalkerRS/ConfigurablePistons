package ca.fxco.pistonlib.pistonLogic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

public class StickyGroup {

    public static final Set<StickyGroup> STICKY_GROUPS = new HashSet<>();

    public static final BiPredicate<StickyGroup, StickyGroup> STRICT_SAME = Object::equals;
    public static final BiPredicate<StickyGroup, StickyGroup> NOT_STRICT_SAME = (group, adjGroup) -> !group.equals(adjGroup);
    public static final BiPredicate<StickyGroup, StickyGroup> INHERIT_SAME = (group, adjGroup) -> {
        if (adjGroup.equals(group)) return true;
        while(adjGroup.inherits != null) {
            if (adjGroup.inherits.equals(group)) return true;
            adjGroup = adjGroup.inherits;
        }
        return false;
    };
    public static final BiPredicate<StickyGroup, StickyGroup> NOT_INHERIT_SAME = (group, adjGroup) -> {
        if (adjGroup.equals(group)) return false;
        while(adjGroup.inherits != null) {
            if (adjGroup.inherits.equals(group)) return false;
            adjGroup = adjGroup.inherits;
        }
        return true;
    };

    public static final StickyGroup SLIME = create(new ResourceLocation("slime"), STRICT_SAME, null);
    public static final StickyGroup HONEY = create(new ResourceLocation("honey"), STRICT_SAME, null);

    private final ResourceLocation id;
    private final BiPredicate<StickyGroup, StickyGroup> stickRule;
    private final @Nullable StickyGroup inherits;

    StickyGroup(ResourceLocation id, BiPredicate<StickyGroup, StickyGroup> stickRule, @Nullable StickyGroup inherits) {
        this.id = id;
        this.stickRule = stickRule;
        this.inherits = inherits;
        while (inherits != null) {
            if (inherits == this) {
                throw new IllegalStateException("StickyGroup: `" + id + "` cannot inherit itself!");
            }
            inherits = inherits.inherits;
        }
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((StickyGroup)o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static boolean canStick(@Nullable StickyGroup stickyGroup, @Nullable StickyGroup adjStickyGroup) {
        if (stickyGroup == null || adjStickyGroup == null) return true;
        return stickyGroup.stickRule.test(stickyGroup, adjStickyGroup) &&
                adjStickyGroup.stickRule.test(adjStickyGroup, stickyGroup);
    }

    public static StickyGroup create(ResourceLocation id, BiPredicate<StickyGroup, StickyGroup> stickRule, @Nullable StickyGroup inherits) {
        StickyGroup group = new StickyGroup(id, stickRule, inherits);
        STICKY_GROUPS.add(group);
        return group;
    }
}