package com.conveyal.r5.labeling;

import com.conveyal.osmlib.OSMEntity;
import com.conveyal.osmlib.Way;
import com.conveyal.r5.streets.EdgeFlag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by mabu on 26.11.2015.
 */
public class TraversalPermissionLabelerTest {

    static TraversalPermissionLabeler traversalPermissionLabeler;

    public static final EnumSet<EdgeFlag> ALL = EnumSet
        .of(EdgeFlag.ALLOWS_BIKE, EdgeFlag.ALLOWS_CAR,
            EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_WHEELCHAIR);
    public static final EnumSet<EdgeFlag> ALLPERMISSIONS = EnumSet
        .of(EdgeFlag.ALLOWS_BIKE, EdgeFlag.ALLOWS_CAR,
            EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_WHEELCHAIR,
            EdgeFlag.NO_THRU_TRAFFIC,
            EdgeFlag.NO_THRU_TRAFFIC_BIKE, EdgeFlag.NO_THRU_TRAFFIC_PEDESTRIAN,
            EdgeFlag.NO_THRU_TRAFFIC_CAR);
    public static final EnumSet<EdgeFlag> PEDESTRIAN_AND_BICYCLE = EnumSet.of(
        EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_WHEELCHAIR, EdgeFlag.ALLOWS_BIKE);
    public static final EnumSet<EdgeFlag> PEDESTRIAN_AND_CAR = EnumSet.of(
        EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_WHEELCHAIR, EdgeFlag.ALLOWS_CAR );
    public static final EnumSet<EdgeFlag> BICYCLE_AND_CAR = EnumSet.of(EdgeFlag.ALLOWS_BIKE,
        EdgeFlag.ALLOWS_CAR);
    public static final EnumSet<EdgeFlag> NONE = EnumSet.noneOf(EdgeFlag.class);

    public static final EnumSet<EdgeFlag> PEDESTRIAN = EnumSet.of(EdgeFlag.ALLOWS_PEDESTRIAN,
        EdgeFlag.ALLOWS_WHEELCHAIR);

    public static final EnumSet<EdgeFlag> PEDESTRIAN_ONLY = EnumSet.of(EdgeFlag.ALLOWS_PEDESTRIAN);

    public static final EnumSet<EdgeFlag> BICYCLE = EnumSet.of(EdgeFlag.ALLOWS_BIKE);

    public static final EnumSet<EdgeFlag> CAR = EnumSet.of(EdgeFlag.ALLOWS_CAR);

    @BeforeAll
    public static void setUpClass() {
        traversalPermissionLabeler = new TestPermissionsLabeler();
    }

    @Test
    public void testCyclewayPermissions() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=cycleway");
        roadFlagComparision(osmWay, PEDESTRIAN_AND_BICYCLE, PEDESTRIAN_AND_BICYCLE);
        roadFlagComparision(osmWay, "access", "destination",
            EnumSet.of(EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_BIKE,
                EdgeFlag.ALLOWS_WHEELCHAIR, EdgeFlag.NO_THRU_TRAFFIC_CAR),
            EnumSet.of(EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_BIKE,
                EdgeFlag.ALLOWS_WHEELCHAIR, EdgeFlag.NO_THRU_TRAFFIC_CAR));

    }

    @Test
    public void testOnewayPermissions() {
        Way osmWay = new Way();
        osmWay.addTag("highway", "residential");
        osmWay.addTag("oneway", "true");
        osmWay.addTag("oneway:bicycle", "no");
        roadFlagComparision(osmWay, ALL, PEDESTRIAN_AND_BICYCLE);
    }

    @Test
    public void testPath() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=path;access=private");
        EnumSet<EdgeFlag> expectedPermissions = EnumSet.of(EdgeFlag.ALLOWS_BIKE,
            EdgeFlag.ALLOWS_PEDESTRIAN, EdgeFlag.ALLOWS_WHEELCHAIR,
            EdgeFlag.NO_THRU_TRAFFIC_CAR);
        roadFlagComparision(osmWay, expectedPermissions, expectedPermissions);
    }

    @Test
    public void testPlatform() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=platform;public_transport=platform");

        roadFlagComparision(osmWay, PEDESTRIAN, PEDESTRIAN);

        roadFlagComparision(osmWay, "wheelchair", "no", PEDESTRIAN_ONLY, PEDESTRIAN_ONLY);
    }

    @Disabled("specific tagging isn't supported yet in specific permissions")
    @Test
    public void testSidewalk() throws Exception {
        Way osmWay = new Way();
        osmWay.addTag("highway", "footway");
        roadFlagComparision(osmWay, PEDESTRIAN_AND_BICYCLE, PEDESTRIAN_AND_BICYCLE);

        //TODO: this had special permissions in OTP
        osmWay = makeOSMWayFromTags("footway=sidewalk;highway=footway");

        roadFlagComparision(osmWay, PEDESTRIAN, PEDESTRIAN);
    }

    //Sidewalks are assumed to be bidirectional so it shouldn't matter on which side of the street they are
    @Test
    public void testRoadWithSidewalk() {

        Way osmWay = makeOSMWayFromTags("highway=nobikenoped");

        roadFlagComparision(osmWay, CAR, CAR);

        roadFlagComparision(osmWay, "sidewalk", "right", PEDESTRIAN_AND_CAR, PEDESTRIAN_AND_CAR);
        roadFlagComparision(osmWay, "sidewalk", "left", PEDESTRIAN_AND_CAR, PEDESTRIAN_AND_CAR);
        roadFlagComparision(osmWay, "sidewalk", "both", PEDESTRIAN_AND_CAR, PEDESTRIAN_AND_CAR);
        roadFlagComparision(osmWay, "sidewalk", "none", CAR, CAR);
        roadFlagComparision(osmWay, "sidewalk", "no", CAR, CAR);
        osmWay = makeOSMWayFromTags("highway=residential");
        roadFlagComparision(osmWay, ALL, ALL);

        //This shouldn't remove WALK permissions
        roadFlagComparision(osmWay, "sidewalk", "no", ALL, ALL);
        roadFlagComparision(osmWay, "sidewalk", "none", ALL, ALL);
    }


    @Test
    public void testRoadWithBidirectionalCycleway() {

        Way osmWay = makeOSMWayFromTags("highway=nobikenoped");

        roadFlagComparision(osmWay, CAR, CAR);

        roadFlagComparision(osmWay, "cycleway", "lane", BICYCLE_AND_CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway", "track", BICYCLE_AND_CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway:both", "lane", BICYCLE_AND_CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway:both", "track", BICYCLE_AND_CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway", "share_busway", BICYCLE_AND_CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway", "shared_lane", BICYCLE_AND_CAR, BICYCLE_AND_CAR);
    }

    @Test
    public void testPrivateRoadWithFootBicyclePermissions() {
        //Private road which can be only used as destination for motor vehicles but can be used normally for pedestrian and bicycle traffic
        Way osmWay = makeOSMWayFromTags("access=private;bicycle=designated;foot=yes;highway=service;motor_vehicle=private");

        EnumSet<EdgeFlag> NO_THRU_CAR_PEDESTRIAN_BICYCLE = EnumSet.copyOf(PEDESTRIAN_AND_BICYCLE);
        NO_THRU_CAR_PEDESTRIAN_BICYCLE.add(EdgeFlag.NO_THRU_TRAFFIC_CAR);


        RoadPermission roadPermission = roadFlagComparision(osmWay, NO_THRU_CAR_PEDESTRIAN_BICYCLE, NO_THRU_CAR_PEDESTRIAN_BICYCLE);

        //Doesn't insert edges which don't have any permissions forward and backward
        assertFalse(
            Collections.disjoint(roadPermission.forward, ALLPERMISSIONS) && Collections
                .disjoint(roadPermission.backward, ALLPERMISSIONS));
    }

    @Test
    public void testSkippingRoadsWithNoPermissions() throws Exception {
        Way osmWay = makeOSMWayFromTags("bicycle=no;foot=no;highway=primary;lanes=2;maxspeed=70;oneway=yes;ref=1");
        RoadPermission roadPermission = roadFlagComparision(osmWay, CAR, NONE);

        //Doesn't insert edges which don't have any permissions forward and backward
        assertFalse(
            Collections.disjoint(roadPermission.forward, ALLPERMISSIONS) && Collections
                .disjoint(roadPermission.backward, ALLPERMISSIONS));

        assertTrue(
            Collections.disjoint(NONE, ALLPERMISSIONS) && Collections
                .disjoint(NONE, ALLPERMISSIONS));

    }

    @Test
    public void testRoadWithMonodirectionalCycleway() {
        Way osmWay = makeOSMWayFromTags("highway=nobikenoped");

        roadFlagComparision(osmWay, "cycleway:right", "lane", BICYCLE_AND_CAR, CAR);

        roadFlagComparision(osmWay, "cycleway:right", "track", BICYCLE_AND_CAR, CAR);

        roadFlagComparision(osmWay, "cycleway:left", "lane", CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "cycleway:left", "track", CAR, BICYCLE_AND_CAR);

        osmWay = makeOSMWayFromTags("highway=residential;foot=no");

        roadFlagComparision(osmWay, "bicycle:forward", "use_sidepath", CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "bicycle:forward", "no", CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "bicycle:forward", "dismount", CAR, BICYCLE_AND_CAR);

        roadFlagComparision(osmWay, "bicycle:backward", "use_sidepath", BICYCLE_AND_CAR, CAR);

        roadFlagComparision(osmWay, "bicycle:backward", "no", BICYCLE_AND_CAR, CAR);

        roadFlagComparision(osmWay, "bicycle:backward", "dismount", BICYCLE_AND_CAR, CAR);

        osmWay = makeOSMWayFromTags("cycleway:right=lane;highway=residential;cycleway:left=opposite_lane;oneway=yes");

        roadFlagComparision(osmWay, ALL, PEDESTRIAN_AND_BICYCLE);

        roadFlagComparision(osmWay, "oneway:bicycle", "no", ALL, PEDESTRIAN_AND_BICYCLE);

        osmWay = makeOSMWayFromTags("highway=tertiary;cycleway:left=lane;bicycle:forward=use_sidepath");
        roadFlagComparision(osmWay, PEDESTRIAN_AND_CAR, ALL);

        osmWay = makeOSMWayFromTags("highway=nobikenoped;cycleway:left=lane;bicycle:forward=use_sidepath");
        roadFlagComparision(osmWay, CAR, BICYCLE_AND_CAR);

        osmWay = makeOSMWayFromTags("highway=nobikenoped;foot=yes;oneway=-1;cycleway:left=opposite_lane");
        roadFlagComparision(osmWay, PEDESTRIAN_AND_BICYCLE, PEDESTRIAN_AND_CAR);
    }

    @Test
    public void testCyclewayNo() throws Exception {
        Way osmWay = makeOSMWayFromTags("oneway=no;highway=residential;cycleway=no");
        roadFlagComparision(osmWay, ALL, ALL);

    }

    private RoadPermission roadFlagComparision(Way osmWay, EnumSet<EdgeFlag> forwardExpected,
        EnumSet<EdgeFlag> backwardExpected) {
        return roadFlagComparision(osmWay, null, null, forwardExpected, backwardExpected);
    }

    /**
     * Makes comparision of way with osmWay tags and newTag with newValue and compares forward and backward permissions with expected permissions
     *
     * Copy of osmWay is made since otherwise tags would be changed
     *  @param iosmWay
     * @param newTag
     * @param newValue
     * @param forwardExpected
     * @param backwardExpected
     */
    private static RoadPermission roadFlagComparision(Way iosmWay, String newTag, String newValue, EnumSet<EdgeFlag> forwardExpected, EnumSet<EdgeFlag> backwardExpected) {
        Way osmWay = new Way();

        StringJoiner stringJoiner = new StringJoiner(";");

        for (OSMEntity.Tag tag: iosmWay.tags) {
            osmWay.addTag(tag.key, tag.value);
            stringJoiner.add(tag.key+"="+tag.value);
        }
        if (newTag != null && newValue != null) {
            osmWay.addTag(newTag, newValue);
            stringJoiner.add(newTag+"="+newValue);
        }
        Set<EdgeFlag> forwardFiltered;
        Set<EdgeFlag> backwardFiltered;

        RoadPermission roadPermission = traversalPermissionLabeler.getPermissions(osmWay);

        forwardFiltered = filterFlags(roadPermission.forward);
        backwardFiltered = filterFlags(roadPermission.backward);

        String tags = "Tags: " + stringJoiner.toString();

        assertEquals(forwardExpected, forwardFiltered, tags);
        assertEquals(backwardExpected, backwardFiltered, tags);
        return roadPermission;
    }

    @Test
    public void testSteps() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=steps");

        roadFlagComparision(osmWay, PEDESTRIAN_ONLY, PEDESTRIAN_ONLY);

        roadFlagComparision(osmWay, "wheelchair", "yes", PEDESTRIAN, PEDESTRIAN);

        roadFlagComparision(osmWay, "wheelchair", "limited", PEDESTRIAN_ONLY, PEDESTRIAN_ONLY);

        roadFlagComparision(osmWay, "ramp:wheelchair", "yes", PEDESTRIAN, PEDESTRIAN);
    }

    @Test
    public void testSidepath() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=tertiary;bicycle=use_sidepath");

        roadFlagComparision(osmWay, PEDESTRIAN_AND_CAR, PEDESTRIAN_AND_CAR);
    }

    @Test
    public void testSpecificPermission() throws Exception {
        Way osmWay = makeOSMWayFromTags("highway=primary;bicycle=use_sidepath;foot=no;junction=roundabout");

        roadFlagComparision(osmWay, CAR, NONE);
    }

    /**
     * Removes all flags except permissions
     * @param permissions
     * @return
     */
    private static Set<EdgeFlag> filterFlags(EnumSet<EdgeFlag> permissions) {
        return permissions.stream()
            .filter(ALLPERMISSIONS::contains)
            .collect(Collectors.toSet());
    }

    /**
     * Creates osmway based on provided tags
     *
     * For example: footway=sidewalk;highway=footway
     * This adds two tags footway=sidewalk and highway=footway. Order doesn't matter.
     * @param tags with tags separated with ; and tag and value separated with =
     * @return
     */
    protected static Way makeOSMWayFromTags(String tags) {
        Way osmWay = new Way();
        String[] pairs = tags.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            osmWay.addTag(kv[0], kv[1]);
        }
        return osmWay;
    }
}
