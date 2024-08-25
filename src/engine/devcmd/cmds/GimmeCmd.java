// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.Enum.ItemContainerType;
import engine.Enum.ItemType;
import engine.Enum.OwnerType;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.ChatManager;
import engine.gameManager.DbManager;
import engine.objects.*;
import engine.powers.EffectsBase;

import java.util.ArrayList;

/**
 * @author Eighty
 */
public class GimmeCmd extends AbstractDevCmd {

    public GimmeCmd() {
        super("gimme");
    }

    @Override
    protected void _doCmd(PlayerCharacter pc, String[] words,
                          AbstractGameObject target) {
        int amt = 0;
        int currentGold = pc.getCharItemManager().getGoldInventory().getNumOfItems();
        amt = 10000000 - currentGold;
        if (!pc.getCharItemManager().addGoldToInventory(amt, true)) {
            throwbackError(pc, "Failed to add gold to inventory");
            return;
        }

        ChatManager.chatSayInfo(pc, amt + " gold added to inventory");
        pc.getCharItemManager().updateInventory();

    }

    @Override
    protected String _getHelpString() {
        return "Round up current gold in inventory to 10,000,000";
    }

    @Override
    protected String _getUsageString() {
        return "'./gimme";
    }

}
