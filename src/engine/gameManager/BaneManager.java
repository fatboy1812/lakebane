package engine.gameManager;

import engine.Enum;
import engine.objects.Bane;
import org.joda.time.DateTime;

public class BaneManager {

    public static void default_check(){
        for(Bane bane : Bane.banes.values()){
            if(!bane.daySet || !bane.timeSet || !bane.capSet){
                DateTime placementDate = bane.getPlacementDate();
                if(DateTime.now().isAfter(placementDate.plusHours(48))){
                    //set bane for default time
                    if(!bane.daySet){
                        bane.daySet = true;
                        DbManager.BaneQueries.SET_BANE_DAY_NEW(3,bane.getCityUUID());
                    }

                    if(!bane.timeSet){
                        bane.timeSet = true;
                        DbManager.BaneQueries.SET_BANE_TIME_NEW(9,bane.getCityUUID());
                    }

                    if(!bane.capSet){
                        bane.capSet = true;
                        bane.capSize = 20;
                        DbManager.BaneQueries.SET_BANE_CAP_NEW(20,bane.getCityUUID());
                    }
                    bane.setLiveDate(DbManager.BaneQueries.getLiveDate(bane.getCityUUID()));
                }
            }
        }
    }

    public static void pulse_banes(){
        //used to pulse the anti-zerg mechanic of active banes
        for(Bane bane : Bane.banes.values()){
            if(bane.getSiegePhase().equals(Enum.SiegePhase.WAR)){
                // bane is live, handle bane anti-zerg mechanic stuff
                bane.applyZergBuffs();
            }
        }
    }
}
