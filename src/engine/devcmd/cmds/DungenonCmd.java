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
import engine.gameManager.ChatManager;
import engine.gameManager.DbManager;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import org.pmw.tinylog.Logger;

/**
 * @author Eighty
 */
public class DungenonCmd extends AbstractDevCmd {

    public DungenonCmd() {
        super("dungeon");
    }

    @Override
    protected void _doCmd(PlayerCharacter pc, String[] words,
                          AbstractGameObject target) {
        if (words.length != 1) {
            this.sendUsage(pc);
            return;
        }
        if(words.length < 1) {
            throwbackInfo(pc, this._getHelpString());
            return;
        }
        Zone parent = ZoneManager.findSmallestZone(pc.loc);
        if(parent == null)
            return;

        switch(words[0]){
            case "mob":
                int mobbase = Integer.parseInt(words[1]);
                int level = Integer.parseInt(words[2]);
                Mob.createStrongholdMob(mobbase,pc.loc,Guild.getErrantGuild(),true,parent,null,0,"",level);
                break;
            case "building":
                int blueprint = Integer.parseInt(words[1]);
                int rank = Integer.parseInt(words[2]);
                int rot = Integer.parseInt(words[3]);

                break;
        }
    }

    @Override
    protected String _getHelpString() {
        return "indicate mob or building followed by an id and a level";
    }

    @Override
    protected String _getUsageString() {
        return "'/dungeon mob 2001 10'";
    }

}
