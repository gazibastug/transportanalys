package com.conveyal.analysis.models;

import com.mongodb.client.model.geojson.LineString;

/**
 * Represents a single segment of an added trip pattern (between two user-specified points)
 */
public class Segment {
    /** Is there a stop at the start of this segment */
    public boolean stopAtStart;

    /** Is there a stop at the end of this segment */
    public boolean stopAtEnd;

    /** If this segment starts at an existing stop, the feed-scoped stop ID (feed:stop_id). */
    public String fromStopId;

    /** If this segment ends at an existing stop, the feed-scoped stop ID (feed:stop_id). */
    public String toStopId;

    /** spacing between stops in this segment, meters */
    public int spacing;

    /**
     * Geometry of this segment
     * Generally speaking, this will be a LineString, but the first segment may be a Point
     * if there are no more segments. This is used when someone first starts drawing a line and
     * they have only drawn one stop so far. Of course a transit line with only one stop would
     * not be particularly useful.
     */
    public LineString geometry;
}
