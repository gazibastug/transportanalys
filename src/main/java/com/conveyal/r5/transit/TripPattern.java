package com.conveyal.r5.transit;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * This is like a transmodel JourneyPattern.
 * All the trips on the same Route that have the same sequence of stops, with the same pickup/dropoff options.
 */
public class TripPattern implements Serializable {

    private static Logger LOG = LoggerFactory.getLogger(TripPattern.class);

    public String routeId;
    public int directionId = Integer.MIN_VALUE;
    public int[] stops;
    // Could be compacted into 2 bits each or a bunch of flags, but is it even worth it?
    public PickDropType[] pickups;
    public PickDropType[] dropoffs;
    public BitSet wheelchairAccessible; // One bit per stop
    public List<TripSchedule> tripSchedules = new ArrayList<>();

    /** does this trip pattern have any frequency trips */
    public boolean hasFrequencies;

    /** does this trip pattern have any scheduled trips */
    public boolean hasSchedules;

    // This set includes the numeric codes for all services on which at least one trip in this pattern is active.
    public BitSet servicesActive = new BitSet();

    public TripPattern (TripPatternKey tripPatternKey) {
        int nStops = tripPatternKey.stops.size();
        stops = new int[nStops];
        pickups = new PickDropType[nStops];
        dropoffs = new PickDropType[nStops];
        wheelchairAccessible = new BitSet(nStops);
        for (int s = 0; s < nStops; s++) {
            stops[s] = tripPatternKey.stops.get(s);
            pickups[s] = PickDropType.forGtfsCode(tripPatternKey.pickupTypes.get(s));
            dropoffs[s] = PickDropType.forGtfsCode(tripPatternKey.dropoffTypes.get(s));
        }
        routeId = tripPatternKey.routeId;
    }

    public void addTrip (TripSchedule tripSchedule) {
        tripSchedules.add(tripSchedule);
        hasFrequencies = hasFrequencies || tripSchedule.headwaySeconds != null;
        hasSchedules = hasSchedules || tripSchedule.headwaySeconds == null;
        servicesActive.set(tripSchedule.serviceCode);
    }

    public void setOrVerifyDirection (int directionId) {
        if (this.directionId != directionId) {
            if (this.directionId == Integer.MIN_VALUE) {
                this.directionId = directionId;
                LOG.debug("Pattern has route_id {} and direction_id {}", routeId, directionId);
            } else {
                LOG.warn("Trips with different direction IDs are in the same pattern.");
            }
        }
    }

    // Simply write "graph builder annotations" to a log file alongside the graphs.
    // function in gtfs-lib getOrderedStopTimes(string tripId)
    // Test GTFS loading on NL large data set.

    /**
     * Linear search.
     * @return null if no departure is possible.
     */
    TripSchedule findNextDeparture (int time, int stopOffset) {
        TripSchedule bestSchedule = null;
        int bestTime = Integer.MAX_VALUE;
        for (TripSchedule schedule : tripSchedules) {
            boolean active = servicesActive.get(schedule.serviceCode);
            // LOG.info("Trip with service {} active: {}.", schedule.serviceCode, active);
            if (servicesActive.get(schedule.serviceCode)) {
                int departureTime = schedule.departures[stopOffset];
                if (departureTime > time && departureTime < bestTime) {
                    bestTime = departureTime;
                    bestSchedule = schedule;
                }
            }
        }
        return bestSchedule;
    }
}
