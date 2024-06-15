// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.gameManager;

import engine.Enum;
import engine.db.archive.CityRecord;
import engine.db.archive.DataWarehouse;
import engine.math.Bounds;
import engine.math.Vector2f;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.objects.Building;
import engine.objects.City;
import engine.objects.Zone;
import engine.server.MBServerStatics;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Class contains methods and structures which
 * track in-game Zones
 */
public enum ZoneManager {

    ZONEMANAGER;

    public static final Set<Zone> macroZones = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<Integer, Zone> zonesByID = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD);
    private static final ConcurrentHashMap<Integer, Zone> zonesByUUID = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD);
    private static final ConcurrentHashMap<String, Zone> zonesByName = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD);
    private static final Set<Zone> npcCityZones = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Zone> playerCityZones = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static Instant hotZoneLastUpdate;
    public static Zone hotZone = null;
    public static int hotZoneCycle = 0;  // Used with HOTZONE_DURATION from config.
    public static HashMap<Integer, Vector2f> _zone_size_data = new HashMap<>();
    /* Instance variables */
    private static Zone seaFloor = null;

    // Find all zones coordinates fit into, starting with Sea Floor

    public static ArrayList<Zone> getAllZonesIn(final Vector3fImmutable loc) {

        ArrayList<Zone> allIn = new ArrayList<>();
        Zone zone;

        zone = ZoneManager.findSmallestZone(loc);

        if (zone != null) {
            allIn.add(zone);
            while (zone.getParent() != null) {
                zone = zone.getParent();
                allIn.add(zone);
            }
        }
        return allIn;
    }

    // Find smallest zone coordinates fit into.

    public static final Zone findSmallestZone(final Vector3fImmutable loc) {

        Zone zone = ZoneManager.seaFloor;

        if (zone == null)
            return null;

        boolean childFound = true;

        while (childFound) {

            childFound = false;

            ArrayList<Zone> nodes = zone.getNodes();

            // Logger.info("soze", "" + nodes.size());
            if (nodes != null)
                for (Zone child : nodes) {

                    if (Bounds.collide(loc, child.getBounds()) == true) {
                        zone = child;
                        childFound = true;
                        break;
                    }
                }
        }
        return zone;
    }

    public static void addZone(final int zoneID, final Zone zone) {

        ZoneManager.zonesByID.put(zoneID, zone);

        ZoneManager.zonesByUUID.put(zone.getObjectUUID(), zone);

        ZoneManager.zonesByName.put(zone.getName().toLowerCase(), zone);

    }

    // Resets the  availability of hotZones
    // for this cycle

    public static void resetHotZones() {

        for (Zone zone : ZoneManager.macroZones)
            if (zone.hasBeenHotzone)
                zone.hasBeenHotzone = false;

    }

    public static Zone getZoneByUUID(final int zoneUUID) {
        return ZoneManager.zonesByUUID.get(zoneUUID);
    }

    public static Zone getZoneByZoneID(final int zoneID) {

        return ZoneManager.zonesByID.get(zoneID);
    }

    public static Zone getZoneByName(final String zoneName) {
        return ZoneManager.zonesByName.get(zoneName);
    }

    public static final Collection<Zone> getAllZones() {
        return ZoneManager.zonesByUUID.values();
    }

    public static final void setHotZone(final Zone zone) {

        if (!zone.isMacroZone())
            return;

        ZoneManager.hotZone = zone;
        ZoneManager.hotZoneCycle = 1;  // Used with HOTZONE_DURATION from config.
        zone.hasBeenHotzone = true;
        ZoneManager.hotZoneLastUpdate = LocalDateTime.now().withMinute(0).withSecond(0).atZone(ZoneId.systemDefault()).toInstant();

    }

    public static boolean inHotZone(final Vector3fImmutable loc) {

        if (ZoneManager.hotZone == null)
            return false;

        return (Bounds.collide(loc, ZoneManager.hotZone.getBounds()) == true);
    }

    public static Zone getSeaFloor() {
        return ZoneManager.seaFloor;
    }

    public static void setSeaFloor(final Zone value) {
        ZoneManager.seaFloor = value;
    }

    public static final void populateWorldZones(final Zone zone) {

        int loadNum = zone.getLoadNum();

        // Zones are added to separate
        // collections for quick access
        // based upon their type.

        if (zone.isMacroZone()) {
            addMacroZone(zone);
            return;
        }


        if (zone.isPlayerCity()) {
            addPlayerCityZone(zone);
            return;
        }

        if (zone.isNPCCity())
            addNPCCityZone(zone);

    }

    private static void addMacroZone(final Zone zone) {
        ZoneManager.macroZones.add(zone);
    }

    private static void addNPCCityZone(final Zone zone) {
        zone.setNPCCity(true);
        ZoneManager.npcCityZones.add(zone);
    }

    public static final void addPlayerCityZone(final Zone zone) {
        zone.setPlayerCity(true);
        ZoneManager.playerCityZones.add(zone);
    }

    // Converts world coordinates to coordinates local to a given zone.

    public static Vector3fImmutable worldToLocal(Vector3fImmutable worldVector,
                                                 Zone serverZone) {

        Vector3fImmutable localCoords;

        localCoords = new Vector3fImmutable(worldVector.x - serverZone.absX,
                worldVector.y - serverZone.absY, worldVector.z
                - serverZone.absZ);

        return localCoords;
    }

    public static Vector2f worldToZoneSpace(Vector3fImmutable worldVector,
                                            Zone serverZone) {

        Vector2f localCoords;
        Vector2f zoneOrigin;

        // Top left corner of zone is calculated in world space by the center and it's extents.

        zoneOrigin = new Vector2f(serverZone.getLoc().x, serverZone.getLoc().z);
        zoneOrigin = zoneOrigin.subtract(new Vector2f(serverZone.getBounds().getHalfExtents().x, serverZone.getBounds().getHalfExtents().y));

        // Local coordinate in world space translated to an offset from the calculated zone origin.

        localCoords = new Vector2f(worldVector.x, worldVector.z);
        localCoords = localCoords.subtract(zoneOrigin);

        localCoords.setY((serverZone.getBounds().getHalfExtents().y * 2) - localCoords.y);


        // TODO : Make sure this value does not go outside the zone's bounds.

        return localCoords;
    }

    // Converts local zone coordinates to world coordinates

    public static Vector3fImmutable localToWorld(Vector3fImmutable worldVector,
                                                 Zone serverZone) {

        Vector3fImmutable worldCoords;

        worldCoords = new Vector3fImmutable(worldVector.x + serverZone.absX,
                worldVector.y + serverZone.absY, worldVector.z
                + serverZone.absZ);

        return worldCoords;
    }


    /**
     * Converts from local (relative to this building) to world.
     *
     * @param localPos position in local reference (relative to this building)
     * @return position relative to world
     */

    public static Vector3fImmutable convertLocalToWorld(Building building, Vector3fImmutable localPos) {

        // convert from SB rotation value to radians

        if (building.getBounds().getQuaternion() == null)
            return building.getLoc();

        Vector3fImmutable rotatedLocal = Vector3fImmutable.rotateAroundPoint(Vector3fImmutable.ZERO, localPos, building.getBounds().getQuaternion());

        // handle building rotation
        // handle building translation

        return building.getLoc().add(rotatedLocal.x, rotatedLocal.y, rotatedLocal.z);
    }


    //used for regions, Building bounds not set yet.
    public static Vector3f convertLocalToWorld(Building building, Vector3f localPos, Bounds bounds) {

        // convert from SB rotation value to radians


        Vector3f rotatedLocal = Vector3f.rotateAroundPoint(Vector3f.ZERO, localPos, bounds.getQuaternion());

        // handle building rotation
        // handle building translation

        return new Vector3f(building.getLoc().add(rotatedLocal.x, rotatedLocal.y, rotatedLocal.z));
    }

    public static Vector3fImmutable convertWorldToLocal(Building building, Vector3fImmutable WorldPos) {
        Vector3fImmutable convertLoc = Vector3fImmutable.rotateAroundPoint(building.getLoc(), WorldPos, -building.getBounds().getQuaternion().angleY);

        convertLoc = convertLoc.subtract(building.getLoc());

        // convert from SB rotation value to radians

        return convertLoc;

    }

    // Method returns a city if the given location is within
    // a city zone.

    public static City getCityAtLocation(Vector3fImmutable worldLoc) {

        Zone currentZone;
        ArrayList<Zone> zoneList;
        City city;

        currentZone = ZoneManager.findSmallestZone(worldLoc);

        if (currentZone.isPlayerCity())
            return City.getCity(currentZone.getPlayerCityUUID());

        return null;
    }

    /* Method is called when creating a new player city to
     * validate that the new zone does not overlap any other
     * zone that might currently exist
     */

    public static boolean validTreePlacementLoc(Zone currentZone, float positionX, float positionZ) {

        // Member Variable declaration

        ArrayList<Zone> zoneList;
        boolean validLocation = true;
        Bounds treeBounds;

        if (currentZone.isContinent() == false)
            return false;


        treeBounds = Bounds.borrow();
        treeBounds.setBounds(new Vector2f(positionX, positionZ), new Vector2f(Enum.CityBoundsType.PLACEMENT.extents, Enum.CityBoundsType.PLACEMENT.extents), 0.0f);

        zoneList = currentZone.getNodes();

        for (Zone zone : zoneList) {

            if (zone.isContinent())
                continue;

            if (Bounds.collide(treeBounds, zone.getBounds(), 0.0f))
                validLocation = false;
        }

        treeBounds.release();
        return validLocation;
    }

    public static void loadCities(Zone zone) {

        ArrayList<City> cities = DbManager.CityQueries.GET_CITIES_BY_ZONE(zone.getObjectUUID());

        for (City city : cities) {

            city.setParent(zone);
            city.setObjectTypeMask(MBServerStatics.MASK_CITY);
            city.setLoc(city.getLoc()); // huh?

//not player city, must be npc city..

            if (!zone.isPlayerCity())
                zone.setNPCCity(true);

            if ((ConfigManager.serverType.equals(Enum.ServerType.WORLDSERVER)) && (city.getHash() == null)) {

                city.setHash();

                if (DataWarehouse.recordExists(Enum.DataRecordType.CITY, city.getObjectUUID()) == false) {
                    CityRecord cityRecord = CityRecord.borrow(city, Enum.RecordEventType.CREATE);
                    DataWarehouse.pushToWarehouse(cityRecord);
                }
            }
        }
    }
}
