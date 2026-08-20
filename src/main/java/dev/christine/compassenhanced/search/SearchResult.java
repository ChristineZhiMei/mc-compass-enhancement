package dev.christine.compassenhanced.search;

import net.minecraft.util.math.Vec3d;

public record SearchResult(
        Vec3d target,
        SearchSource source,
        double distanceSquared
) {
}
