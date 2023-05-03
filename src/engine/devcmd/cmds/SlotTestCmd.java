// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com



package engine.devcmd.cmds;

import engine.Enum.GameObjectType;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.BuildingManager;
import engine.gameManager.ChatManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;

import java.util.ArrayList;

public class SlotTestCmd extends AbstractDevCmd {

	public SlotTestCmd() {
        super("slottest");
    }

	@Override
	protected void _doCmd(PlayerCharacter playerCharacter, String[] args,
			AbstractGameObject target) {

		ArrayList<BuildingLocation> buildingLocations;
		String outString = "Available Slots\r\n";

		if (target == null)
			return;

		if (target.getObjectType() != GameObjectType.Building)
			return;

		Building building = (Building) target;

		buildingLocations = BuildingManager._slotLocations.get(building.meshUUID);

		if (buildingLocations == null) {
			outString = "No slot information for mesh: " + building.meshUUID;
			ChatManager.chatSystemInfo(playerCharacter, outString);
			return;
		}

		// Goto slot location

		if (args.length == 1) {

			int slot = Integer.parseInt(args[0]);
			Vector3fImmutable slotPosition;
			BuildingLocation slotLocation = BuildingManager._slotLocations.get(building.meshUUID).get(slot);
			slotPosition = building.getLoc().add(slotLocation.getLocation());
			slotPosition = Vector3fImmutable.rotateAroundPoint(building.getLoc(), slotPosition, building.getBounds().getQuaternion().angleY);
			playerCharacter.teleport(slotPosition);
			return;
		}


		for (BuildingLocation buildingLocation : BuildingManager._slotLocations.get(building.meshUUID))
			outString += buildingLocation.getSlot() + buildingLocation.getLocation().toString() + "\r\n";

		outString += "\r\nNext Available Slot: " + BuildingManager.getAvailableSlot(building);

		if (building.getHirelings().isEmpty() == false) {

			outString += "\r\n\r\n";
			outString += "Hirelings List:";

			for (AbstractCharacter hireling : building.getHirelings().keySet())
				outString += "\r\n" + hireling.getName() + " slot : " + building.getHirelings().get(hireling);

		}

		ChatManager.chatSystemInfo(playerCharacter,outString);

	}

	@Override
	protected String _getHelpString() {
		return "Displays slot information for building";
	}

	@Override
	protected String _getUsageString() {
		return "./slottest <target builing> n";

	}

}
