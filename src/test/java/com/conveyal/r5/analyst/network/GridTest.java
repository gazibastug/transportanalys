package com.conveyal.r5.analyst.network;

import com.conveyal.r5.OneOriginResult;
import com.conveyal.r5.analyst.TravelTimeComputer;
import com.conveyal.r5.analyst.WebMercatorGridPointSet;
import com.conveyal.r5.analyst.cluster.AnalysisWorkerTask;
import com.conveyal.r5.analyst.cluster.TimeGridWriter;
import com.conveyal.r5.transit.TransportNetwork;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;

import java.io.FileOutputStream;

/**
 * Created by abyrd on 2020-11-20
 *
 * TODO option to align grid exactly with sample points in WebMercatorGridPointSet to eliminate walking time
 *      and check splitting
 */
public class GridTest {

    public static final Coordinate SIMPSON_DESERT_CORNER = new CoordinateXY(136.5, -25.5);

    @Test
    public void testGridScheduled () throws Exception {
        GridLayout gridLayout = new GridLayout(SIMPSON_DESERT_CORNER, 100);
        gridLayout.addHorizontalRoute(20, 20);
        gridLayout.addHorizontalRoute(40, 20);
        gridLayout.addHorizontalRoute(60, 20);
        gridLayout.addVerticalRoute(40, 20);
        TransportNetwork network = gridLayout.generateNetwork();

        // Logging and file export for debugging:
        // System.out.println("Grid envelope: " + gridLayout.gridEnvelope());
        // gridLayout.exportFiles("test");

        AnalysisWorkerTask task = gridLayout.newTaskBuilder()
                .morningPeak()
                .setOrigin(20, 20)
                .uniformOpportunityDensity(10)
                .build();

        TravelTimeComputer computer = new TravelTimeComputer(task, network);
        OneOriginResult oneOriginResult = computer.computeTravelTimes();

        // Write travel times to Geotiff for debugging visualization in desktop GIS:
        // toGeotiff(oneOriginResult, task);

        // TODO move this into reproducible method, perhaps on GridLayout
        // Now to verify the results. We have only our 5 percentiles here, not the full list of travel times.
        // They are also arranged on a grid. This grid does not match the full extents of the network, rather it
        // matches the extents set in the task, which must exactly match those of the opportunity grid.
        Coordinate destLatLon = gridLayout.getIntersectionLatLon(40, 40);
        // Here is a bit of awkwardness where WebMercatorGridPointSet and Grid both extend PointSet, but don't share
        // their grid referencing code, so one would have to be converted to the other to get the point index.
        int pointIndex = new WebMercatorGridPointSet(task.getWebMercatorExtents()).getPointIndexContaining(destLatLon);

        int[] travelTimePercentiles = oneOriginResult.travelTimes.getTarget(pointIndex);

        // Transit takes 30 seconds per block. Mean wait time is 10 minutes. Any trip takes one transfer.
        // 20+20 blocks at 30 seconds each = 20 minutes. Two waits at 0-20 minutes each, mean is 20 minutes.
        // Board slack is 2 minutes. With pure frequency routes total should be 24 to 64 minutes, median 44.
        // However, these are not pure frequencies, but synchronized such that the transfer wait is always 10 minutes.
        // So scheduled range is expected to be 2 minutes slack, 0-20 minutes wait, 10 minutes ride, 10 minutes wait,
        // 10 minutes ride, giving 32 to 52 minutes.
        // Maybe codify this estimation logic as a TravelTimeEstimate.waitWithHeadaway(20) etc.
        DistributionTester.assertUniformlyDistributed(travelTimePercentiles, 32, 52);

//        for (int p = 0; p < 5; p++) {
//            int travelTimeMinutes = oneOriginResult.travelTimes.getValues()[p][pointIndex];
//            System.out.printf(
//                "percentile %d %s\n",
//                p,
//                (travelTimeMinutes == UNREACHED) ? "NONE" : Integer.toString(travelTimeMinutes) + "minutes"
//            );
//        }

    }

    /**
     * Similar to above, but using frequency routes which should increase uncertainty waiting for second ride.
     * The availability of multiple alternative paths should also reduce the variance of the distribution.
     */
    @Test
    public void testGridFrequency () throws Exception {
        GridLayout gridLayout = new GridLayout(SIMPSON_DESERT_CORNER, 100);
        gridLayout.addHorizontalFrequencyRoute(20, 20);
        // The next two horizontal routes are not expected to contribute to travel time
        gridLayout.addHorizontalFrequencyRoute(40, 20);
        gridLayout.addHorizontalFrequencyRoute(60, 20);
        gridLayout.addVerticalFrequencyRoute(40, 20);
        TransportNetwork network = gridLayout.generateNetwork();

        AnalysisWorkerTask task = gridLayout.newTaskBuilder()
                .morningPeak()
                .setOrigin(20, 20)
                .uniformOpportunityDensity(10)
                .build();

        TravelTimeComputer computer = new TravelTimeComputer(task, network);
        OneOriginResult oneOriginResult = computer.computeTravelTimes();

        // This should be factored out into a method eventually
        Coordinate destLatLon = gridLayout.getIntersectionLatLon(40, 40);
        int pointIndex = new WebMercatorGridPointSet(task.getWebMercatorExtents()).getPointIndexContaining(destLatLon);
        int[] travelTimePercentiles = oneOriginResult.travelTimes.getTarget(pointIndex);

        // Frequency travel time reasoning is similar to scheduled test method.
        // But transfer time is variable from 0...20 minutes.
        // Frequency range is expected to be 2x 2 minutes slack, 2x 0-20 minutes wait, 2x 10 minutes ride,
        // giving 24 to 64 minutes.
        Distribution ride = new Distribution(2, 20);
        Distribution expected = Distribution.convolution(ride, ride).delay(20);

        DistributionTester.assertExpectedDistribution(expected, travelTimePercentiles);
    }


    /** Write travel times to GeoTiff. Convenience method to help visualize results in GIS while developing tests. */
    private static void toGeotiff (OneOriginResult oneOriginResult, AnalysisWorkerTask task) {
        try {
            TimeGridWriter timeGridWriter = new TimeGridWriter(oneOriginResult.travelTimes, task);
            timeGridWriter.writeGeotiff(new FileOutputStream("traveltime.tiff"));
        } catch (Exception e) {
            throw new RuntimeException("Could not write geotiff.", e);
        }
    }

}
