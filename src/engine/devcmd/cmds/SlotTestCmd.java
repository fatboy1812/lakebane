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
import engine.objects.*;

public class SlotTestCmd extends AbstractDevCmd {

	public SlotTestCmd() {
        super("slottest");
    }

	@Override
	protected void _doCmd(PlayerCharacter playerCharacter, String[] args,
			AbstractGameObject target) {

		String outString = "Available Slots\r\n";

		if (target == null)
			return;

		if (target.getObjectType() != GameObjectType.Building)
			return;

		Building building = (Building)target;

		for (BuildingLocation buildingLocation : BuildingManager._slotLocations.get(building.meshUUID))
			outString += buildingLocation.getSlot() + buildingLocation.getLoc().toString() + "\r\n";

		outString += "\r\nNeext Available Slot: " + BuildingManager.getAvailableSlot(building);

		if (building.getHirelings().isEmpty() == false) {

			outString += "\r\n\r\n";
			outString += "Hirelings List: name / slot / floor";

			for (AbstractCharacter hireling : building.getHirelings().keySet()) {

				outString += "\r\n" + hireling.getName() + " slot " + building.getHirelings().get(hireling);
				outString += "\r\n" + "location " + hireling.getLoc();
			}
		}

		ChatManager.chatSystemInfo(playerCharacter,outString);

	}

	@Override
	protected String _getHelpString() {
		return "Temporarily Changes SubRace";
	}

	@Override
	protected String _getUsageString() {
		return "' /setBuildingCollidables add/remove 'add creates a collision line.' needs 4 integers. startX, endX, startY, endY";

	}

}
