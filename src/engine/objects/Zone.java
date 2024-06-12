// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.InterestManagement.HeightMap;
import engine.db.archive.DataWarehouse;
import engine.gameManager.DbManager;
import engine.gameManager.ZoneManager;
import engine.math.Bounds;
import engine.math.Vector2f;
import engine.math.Vector3fImmutable;
import engine.net.ByteBufferWriter;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Zone extends AbstractGameObject {

    public final Set<Building> zoneBuildingSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Set<NPC> zoneNPCSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public final Set<Mob> zoneMobSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final int playerCityID;
    private final String zoneName;
    private final float xCoord;
    private final float zCoord;
    private final float yCoord;
    private final int loadNum;
    private final byte safeZone;
    private final String Icon1;
    private final String Icon2;
    private final String Icon3;
    public float absX = 0.0f;
    public float absY = 0.0f;
    public float absZ = 0.0f;
    public int minLvl;
    public int maxLvl;
    public boolean hasBeenHotzone = false;
    private ArrayList<Zone> nodes = null;
    private int parentZoneID;
    private Zone parent = null;
    private Bounds bounds;
    private boolean isNPCCity = false;
    private boolean isPlayerCity = false;
    private String hash;
    private float worldAltitude = 0;
    private float seaLevel = 0;
    //public static ArrayList<Mob> respawnQue = new ArrayList<>();
    public static final Set<Mob> respawnQue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static long lastRespawn = 0;
    public int canLoad;
    /**
     * ResultSet Constructor
     */
    public Zone(ResultSet rs) throws SQLException {
        super(rs);
        this.parentZoneID = rs.getInt("parent");
        this.playerCityID = rs.getInt("isPlayerCity");
        this.isPlayerCity = this.playerCityID != 0;
        this.zoneName = rs.getString("Name");
        this.xCoord = rs.getFloat("XCoord");
        this.zCoord = rs.getFloat("ZCoord");
        this.yCoord = rs.getFloat("YOffset");
        this.loadNum = rs.getInt("LoadNum");
        this.safeZone = rs.getByte("SafeZone");
        this.Icon1 = rs.getString("Icon1");
        this.Icon2 = rs.getString("Icon2");
        this.Icon3 = rs.getString("Icon3");
        this.hash = rs.getString("hash");

        this.minLvl = rs.getInt("minLvl");
        this.maxLvl = rs.getInt("maxLvl");

        //this needs to be here specifically for new zones created after server boot (e.g. player city zones)
        Zone parentZone = ZoneManager.getZoneByUUID(parentZoneID);

        this.setParent(parentZone);

        if (this.minLvl == 0 && parentZone != null) {
            this.minLvl = parentZone.minLvl;
            this.maxLvl = parentZone.maxLvl;
        }

        if (parentZone != null)
            parentZone.addNode(this);

        // If zone doesn't yet hava a hash then write it back to the zone table

        if (hash == null)
            setHash();

        this.canLoad = rs.getInt("canLoad");

    }

    public static void serializeForClientMsg(Zone zone, ByteBufferWriter writer) {

        if (zone.loadNum == 0 && zone.playerCityID == 0)
            Logger.warn("Warning! WorldServerMap with ID " + zone.getObjectUUID() + " has a loadnum of 0 (player city) and no city linked. This will probably crash the client!");

        // Player City Terraform values serialized here.

        if (zone.playerCityID > 0) {
            writer.put((byte) 1); // Player City - True
            writer.putFloat(Enum.CityBoundsType.ZONE.extents);
            writer.putFloat(Enum.CityBoundsType.ZONE.extents);
        } else
            writer.put((byte) 0); // Player City - False

        writer.putFloat(zone.xCoord);
        writer.putFloat(zone.zCoord);
        writer.putFloat(zone.yCoord);

        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(zone.loadNum);

        if (zone.playerCityID > 0) {
            City k = City.getCity(zone.playerCityID);

            if (k != null) {
                writer.putInt(k.getObjectType().ordinal());
                writer.putInt(k.getObjectUUID());
            } else
                writer.putLong(0x0);
        } else {
            writer.putInt(zone.getObjectType().ordinal());
            writer.putInt(zone.getObjectUUID());
        }
        writer.putInt(zone.nodes.size());

        City city = City.getCity(zone.playerCityID);

        if (city != null)
            writer.putString(city.getCityName());
        else
            writer.putString(zone.zoneName);
        writer.put(zone.safeZone);
        writer.putString(zone.Icon1);
        writer.putString(zone.Icon2);
        writer.putString(zone.Icon3);
        writer.put((byte) 0); // Pad

        for (Zone child : zone.nodes) {
            Zone.serializeForClientMsg(child, writer);
        }
    }

    /* Method sets a default value for player cities
     * otherwise using values derived from the loadnum
     * field in the obj_zone database table.
     */
    public void setBounds() {

        float halfExtentX;
        float halfExtentY;

        // Set initial bounds object

        this.bounds = Bounds.borrow();

        // Player cities are assigned default value

        if (this.loadNum == 0) {
            bounds.setBounds(new Vector2f(this.absX, this.absZ), new Vector2f(Enum.CityBoundsType.ZONE.extents, Enum.CityBoundsType.ZONE.extents), 0.0f);
            return;
        }

        Vector2f zoneSize = ZoneManager._zone_size_data.get(this.loadNum);

        // Default to player zone size on error? Maybe log this

        if (zoneSize != null)
            this.bounds.setBounds(new Vector2f(this.absX, this.absZ), zoneSize, 0.0f);
        else
            bounds.setBounds(new Vector2f(this.absX, this.absZ), new Vector2f(Enum.CityBoundsType.ZONE.extents, Enum.CityBoundsType.ZONE.extents), 0.0f);

    }

    public int getPlayerCityUUID() {
        return this.playerCityID;
    }

    public String getName() {
        return zoneName;
    }

    public float getXCoord() {
        return xCoord;
    }

    public float getYCoord() {
        return yCoord;
    }

    public float getZCoord() {
        return zCoord;
    }

    public int getLoadNum() {
        return loadNum;
    }

    public byte getSafeZone() {
        return safeZone;
    }

    public String getIcon1() {
        return Icon1;
    }

    public void generateWorldAltitude() {

        if (ZoneManager.getSeaFloor().getObjectUUID() == this.getObjectUUID()) {
            this.worldAltitude = MBServerStatics.SEA_FLOOR_ALTITUDE;
            return;
        }

        Zone parentZone = this.parent;

        Zone currentZone = this;
        float altitude = this.absY;

        //seafloor only zone with null parent;

        while (parentZone != ZoneManager.getSeaFloor()) {

            if (parentZone.getHeightMap() != null) {

                Vector2f zoneLoc = ZoneManager.worldToZoneSpace(currentZone.getLoc(), parentZone);
                altitude += parentZone.getHeightMap().getInterpolatedTerrainHeight(zoneLoc);

            }
            currentZone = parentZone;
            parentZone = parentZone.parent;

        }

        this.worldAltitude = altitude;

        if (ZoneManager.getSeaFloor().equals(this))
            this.seaLevel = 0;
        else if
        (this.getHeightMap() != null && this.getHeightMap().getSeaLevel() == 0) {
            this.seaLevel = this.parent.seaLevel;

        } else if (this.getHeightMap() != null) {
            this.seaLevel = this.worldAltitude + this.getHeightMap().getSeaLevel();
        } else {
            this.seaLevel = this.parent.seaLevel;
        }

    }

    public Zone getParent() {
        return this.parent;
    }

    public void setParent(final Zone value) {

        this.parent = value;
        this.parentZoneID = (this.parent != null) ? this.parent.getObjectUUID() : 0;

        if (this.parent != null) {
            this.absX = this.xCoord + parent.absX;
            this.absY = this.yCoord + parent.absY;
            this.absZ = this.zCoord + parent.absZ;

            if (this.minLvl == 0 || this.maxLvl == 0) {
                this.minLvl = this.parent.minLvl;
                this.maxLvl = this.parent.maxLvl;
            }
        } else {  //only the Sea Floor zone does not have a parent
            this.absX = this.xCoord;
            this.absY = MBServerStatics.SEA_FLOOR_ALTITUDE;
            this.absZ = this.zCoord;
        }

        // Zone AABB is set here as it's coordinate space is world requiring a parent.
        this.setBounds();

        if (this.getHeightMap() != null && this.getHeightMap().getSeaLevel() != 0)
            this.seaLevel = this.getHeightMap().getSeaLevel();

    }

    public float getAbsX() {
        return this.absX;
    }

    public float getAbsY() {
        return this.absY;
    }

    public float getAbsZ() {
        return this.absZ;
    }

    public boolean isMacroZone() {

        // Macro zones have icons.

        if (this.isPlayerCity == true)
            return false;

        if (this.parent == null)
            return false;

        return !this.getIcon1().equals("");
    }

    public boolean isNPCCity() {
        return this.isNPCCity;
    }

    public void setNPCCity(boolean value) {
        this.isNPCCity = value;
    }

    public boolean isPlayerCity() {
        return this.isPlayerCity;
    }

    public void setPlayerCity(boolean value) {
        this.isPlayerCity = value;
    }

    public Vector3fImmutable getLoc() {
        return new Vector3fImmutable(this.absX, this.absY, this.absZ);
    }

    public int getParentZoneID() {
        return this.parentZoneID;
    }

    public ArrayList<Zone> getNodes() {
        if (this.nodes == null) {
            this.nodes = DbManager.ZoneQueries.GET_MAP_NODES(super.getObjectUUID());

            //Add reverse lookup for child->parent
            if (this.nodes != null)
                for (Zone zone : this.nodes) {
                    zone.setParent(this);
                }
        }

        return nodes;
    }

    /*
     * Serializing
     */

    public void addNode(Zone child) {
        this.nodes.add(child);
    }

    @Override
    public void updateDatabase() {
        // TODO Auto-generated method stub
    }

    public boolean isContinent() {

        if (this.equals(ZoneManager.getSeaFloor()))
            return false;

        if (this.getNodes().isEmpty())
            return false;

        if (this.getNodes().get(0).isMacroZone())
            return true;

        return this.getParent().equals(ZoneManager.getSeaFloor());

    }

    /**
     * @return the bounds
     */
    public Bounds getBounds() {
        return bounds;
    }

    public String getHash() {
        return hash;
    }

    public void setHash() {

        this.hash = DataWarehouse.hasher.encrypt(this.getObjectUUID());

        // Write hash to player character table

        DataWarehouse.writeHash(Enum.DataRecordType.ZONE, this.getObjectUUID());
    }

    // Return heightmap for this Zone.

    public HeightMap getHeightMap() {

        if (this.isPlayerCity)
            return HeightMap.PlayerCityHeightMap;

        return HeightMap.heightmapByLoadNum.get(this.loadNum);
    }

    public float getSeaLevel() {
        return seaLevel;
    }

    public float getWorldAltitude() {
        return worldAltitude;
    }

}
