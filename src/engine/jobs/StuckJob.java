// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.jobs;

import engine.InterestManagement.WorldGrid;
import engine.job.AbstractScheduleJob;
import engine.math.Bounds;
import engine.math.Vector3fImmutable;
import engine.net.client.msg.ErrorPopupMsg;
import engine.objects.AbstractWorldObject;
import engine.objects.Building;
import engine.objects.PlayerCharacter;
import engine.server.MBServerStatics;

import java.util.HashSet;

public class StuckJob extends AbstractScheduleJob {

    private final PlayerCharacter player;

    public StuckJob(PlayerCharacter player) {
        super();
        this.player = player;
    }

    @Override
    protected void doJob() {

        Vector3fImmutable stuckLoc;
        Building building = null;

        if (player == null)
            return;

        if (player.getClientConnection() == null)
            return;

        HashSet<AbstractWorldObject> awoList = WorldGrid.getObjectsInRangePartial(player, 150, MBServerStatics.MASK_BUILDING);

        for (AbstractWorldObject awo : awoList) {

            Building buildingEntry = (Building) awo;

            if (buildingEntry.getStuckLocation() == null)
                continue;

            if (Bounds.collide(player.getLoc(), buildingEntry)) {
                building = buildingEntry;
                break;

            }
        }

        if (building == null) {
            ErrorPopupMsg.sendErrorMsg(player, "Unable to find desired location");
            return;
        }

        stuckLoc = building.getStuckLocation();

        if (stuckLoc == null) {
            ErrorPopupMsg.sendErrorMsg(player, "Unable to find desired location");
            return;
        }

        player.teleport(stuckLoc);

    }

    @Override
    protected void _cancelJob() {
    }

}

