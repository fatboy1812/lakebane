package engine.jobs;

import engine.gameManager.ZoneManager;
import engine.job.AbstractScheduleJob;
import engine.objects.Building;
import engine.objects.City;
import org.pmw.tinylog.Logger;

/*
 * This class handles upgrading of buildings, swapping the
 * appropriate mesh according to the building's blueprint.
 * @Author
 */
public class UpgradeBuildingJob extends AbstractScheduleJob {

    private final Building rankingBuilding;

    public UpgradeBuildingJob(Building building) {
        super();
        this.rankingBuilding = building;

    }

    @Override
    public void doJob() {


        // Must have a building to rank!

        if (rankingBuilding == null) {
            Logger.error("Attempting to rank null building");
            return;
        }

        // Make sure the building is currently set to upgrade
        // (Duplicate job sanity check)

        if (rankingBuilding.isRanking() == false)
            return;

        // SetCurrentRank also changes the mesh and maxhp
        // accordingly for buildings with blueprints

        rankingBuilding.setRank(rankingBuilding.getRank() + 1);

        if(rankingBuilding.getBlueprint().isWallPiece()){
            City cityObject = ZoneManager.getCityAtLocation(rankingBuilding.loc);
            if(cityObject.getTOL().getRank() == 8) {
                if (rankingBuilding.getBlueprint() != null && rankingBuilding.getBlueprint().getBuildingGroup() != null && rankingBuilding.getBlueprint().isWallPiece()) {
                    float currentHealthRatio = rankingBuilding.getCurrentHitpoints() / rankingBuilding.healthMax;
                    float newMax = rankingBuilding.healthMax * 1.1f;
                    rankingBuilding.setMaxHitPoints(newMax);
                    rankingBuilding.setHealth(rankingBuilding.healthMax * currentHealthRatio);
                }
            }
        }

        // Reload the object


    }

    @Override
    protected void _cancelJob() {
    }

}
