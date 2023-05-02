// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.gameManager.BuildingManager;
import engine.gameManager.DbManager;
import engine.math.Quaternion;
import engine.math.Vector3fImmutable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;


public class BuildingLocation extends AbstractGameObject {

	private final int buildingUUID;
	private final int type;
	private final int slot;
	private final int unknown;
	private final Vector3fImmutable location;
	private final Quaternion rotation;


	/**
	 * ResultSet Constructor
	 */
	public BuildingLocation(ResultSet rs) throws SQLException {
		super(rs);
		this.buildingUUID = rs.getInt("BuildingID");
		this.type = rs.getInt("type");
		this.slot = rs.getInt("slot");
		this.unknown = rs.getInt("unknown");
		this.location = new Vector3fImmutable(rs.getFloat("locX"), rs.getFloat("locY"), rs.getFloat("locZ"));
		this.rotation = new Quaternion(rs.getFloat("rotX"), rs.getFloat("rotY"), rs.getFloat("rotZ"), rs.getFloat("w"));
	}

	/*
	 * Getters
	 */

	public int getBuildingUUID() {
		return this.buildingUUID;
	}

	public int getType() {
		return this.type;
	}

	public int getSlot() {
		return this.slot;
	}

	public int getUnknown() {
		return this.unknown;
	}

	public float getLocX() {
		return this.location.x;
	}

	public float getLocY() {
		return this.location.y;
	}


	public Vector3fImmutable getLocation() {
		return this.location;
	}

	public Quaternion getRotation() {
		return this.rotation;
	}

	@Override
	public void updateDatabase() {
	}

	public static void loadBuildingLocations() {

		ArrayList<BuildingLocation> buildingLocations = DbManager.BuildingLocationQueries.LOAD_BUILDING_LOCATIONS();
		HashMap<Integer, ArrayList<BuildingLocation>> locationCollection = new HashMap<>();

		// Only slot locations and stuck locations are currently loaded.

		for (BuildingLocation buildingLocation : buildingLocations) {

			switch (buildingLocation.type) {
				case 6:
					locationCollection = BuildingManager._slotLocations;
					break;
				case 8:
					locationCollection = BuildingManager._stuckLocations;
					break;
			}

			// Add location to collection in BuildingManager

			if (locationCollection.containsKey(buildingLocation.buildingUUID))
				locationCollection.get(buildingLocation.buildingUUID).add(buildingLocation);
				else {
				locationCollection.put(buildingLocation.buildingUUID, new ArrayList<>());
				locationCollection.get(buildingLocation.buildingUUID).add(buildingLocation);
				}

		}
	}

}
