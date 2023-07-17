// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.devcmd.AbstractDevCmd;
import engine.gameManager.ZoneManager;
import engine.objects.*;

public class ZoneSetCmd extends AbstractDevCmd {

    public ZoneSetCmd() {
        super("zoneset");
    }

    @Override
    protected void _doCmd(PlayerCharacter playerCharacter, String[] words,
                          AbstractGameObject target) {
        Zone zone;

        if (playerCharacter == null)
            return;

        if (words.length == 0) {
            throwbackError(playerCharacter, "Usage: zoneset npc/mob");
            return;
        }

        if (!words[0].equalsIgnoreCase("npc") &&
                !words[0].equalsIgnoreCase("mob")) {
            throwbackError(playerCharacter, "Usage: zoneset npc/mob");
            return;
        }

        zone = ZoneManager.findSmallestZone(playerCharacter.getLoc());

        if (zone == null) {
            throwbackError(playerCharacter, "Error:  can't find the zone you are in.");
            return;
        }

        //get all mobs for the zone

        throwbackInfo(playerCharacter, zone.getName() + " (" + zone.getLoadNum() + ") " + zone.getObjectUUID());

        if (words[0].equalsIgnoreCase("mob")) {

            for (Mob mob : zone.zoneMobSet) {

                String out = mob.getName() + '(' + mob.getDBID() + "): ";

                if (mob.isAlive())
                    out += mob.getLoc().x + "x" + mob.getLoc().z + "; isAlive: " + mob.isAlive();
                else
                    out += " isAlive: " + mob.isAlive();

                throwbackInfo(playerCharacter, out);
                return;
            }
        }

        // NPC

        for (NPC npc : zone.zoneNPCSet) {
            String out = npc.getName() + '(' + npc.getDBID() + "): ";
            throwbackInfo(playerCharacter, out);
        }

    }

    @Override
    protected String _getUsageString() {
        return "' /zoneset npc|mob'";
    }

    @Override
    protected String _getHelpString() {
        return "lists entries in zone npc set";
    }

}
