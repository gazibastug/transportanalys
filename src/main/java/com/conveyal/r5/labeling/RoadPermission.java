package com.conveyal.r5.labeling;

import com.conveyal.r5.streets.EdgeFlag;

import java.util.EnumSet;

/**
 * Class providing a return type for functions in the TraversalPermissionLabeler, which need to return a set of
 * permissions in both the forward and backward direction on a single road segment.
 */
public class RoadPermission {
    public final EnumSet<EdgeFlag> forward;
    public final EnumSet<EdgeFlag> backward;

    public RoadPermission(EnumSet<EdgeFlag> forward,
        EnumSet<EdgeFlag> backward) {
        this.forward = forward;
        this.backward = backward;

    }
}
