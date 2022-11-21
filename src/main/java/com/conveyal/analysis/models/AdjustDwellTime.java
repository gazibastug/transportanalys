package com.conveyal.analysis.models;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.List;

/**
 * Created by matthewc on 3/3/16.
 */
@BsonDiscriminator(key = "type", value = "adjust-dwell-time")
public class AdjustDwellTime extends Modification {
    @Override
    public String getType() {
        return "adjust-dwell-time";
    }

    public String feed;

    public List<String> routes;

    public List<String> trips;

    public List<String> stops;

    /** are we scaling existing times (true) or replacing them with a brand new time (false) */
    public boolean scale;

    /** the factor by which to scale, OR the new time, depending on the value of above */
    public double value;

    public com.conveyal.r5.analyst.scenario.AdjustDwellTime toR5 () {
        com.conveyal.r5.analyst.scenario.AdjustDwellTime adt = new com.conveyal.r5.analyst.scenario.AdjustDwellTime();
        adt.comment = name;

        if (trips == null) {
            adt.routes = feedScopedIdSet(feed, routes);
        } else {
            adt.patterns = feedScopedIdSet(feed, trips);
        }

        adt.stops = feedScopedIdSet(feed, stops);

        if (scale) {
            adt.scale = value;
        } else {
            adt.dwellSecs = (int) value;
        }

        return adt;
    }
}
