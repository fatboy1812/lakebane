package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.math.Vector3fImmutable;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.HotzoneChangeMsg;
import engine.objects.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class HotzoneManager {

    public static Zone hotzone;
    public static Mob currentBoss;
    public static ArrayList<Mob> minions;

    public static int bossID = 0;
    public static int minionID = 0;

    public static final ArrayList<Integer> int_rune_ids = new ArrayList<>(Arrays.asList(
            250028, 250029, 250030, 250031, 250032, 250033, 250034, 250035
    ));
    public static final ArrayList<Integer> dex_rune_ids = new ArrayList<>(Arrays.asList(
            250010, 250011, 250012, 250013, 250014, 250015, 250016, 250017
    ));
    public static final ArrayList<Integer> con_rune_ids = new ArrayList<>(Arrays.asList(
            250019, 250020, 250021, 250022, 250023, 250024, 250025, 250026
    ));

    public static void wipe_hotzone(){
        hotzone = null;
        DbManager.MobQueries.DELETE_MOB(currentBoss);
        WorldGrid.removeObject(currentBoss);
        for(Mob minion : minions){
            DbManager.MobQueries.DELETE_MOB(minion);
            WorldGrid.removeObject(minion);
        }
    }

    public static void create_random_hotzone(){

        wipe_hotzone();

        //TODO create and assign a new zone to be hotzone
        hotzone = null;

        currentBoss = create_epic();
        minions = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            Mob minion = create_minion();
            minion.setLoc(Vector3fImmutable.getRandomPointOnCircle(currentBoss.loc,32f));
            minions.add(minion);
        }
        generate_epic_loot();

        generate_minion_loot();

        for(PlayerCharacter player : SessionManager.getAllActivePlayerCharacters()) {
            HotzoneChangeMsg hcm = new HotzoneChangeMsg(Enum.GameObjectType.Zone.ordinal(), hotzone.getObjectUUID());
            Dispatch dispatch = Dispatch.borrow(player, hcm);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
        }
    }

    public static Mob create_epic(){
        Mob epic = Mob.createStrongholdMob(bossID,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Hotzone Commander",85);
        return epic;
    }

    public static Mob create_minion(){
        Mob minion = Mob.createStrongholdMob(minionID,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Hotzone Minion",65);
        return minion;
    }

    public static void generate_epic_loot(){
        currentBoss.getCharItemManager().clearInventory();
        Random random = new Random();
        ItemBase runeBase;
        int tableRoll = random.nextInt(3);
        switch(tableRoll){
            case 1:
                runeBase = ItemBase.getItemBase(dex_rune_ids.get(random.nextInt(2) + 6));
                break;
            case 2:
                runeBase = ItemBase.getItemBase(con_rune_ids.get(random.nextInt(2) + 6));
                break;
            default:
                runeBase = ItemBase.getItemBase(int_rune_ids.get(random.nextInt(2) + 6));
                break;
        }
        if(runeBase != null){
            MobLoot loot = new MobLoot(currentBoss,runeBase,true);
            currentBoss.getCharItemManager().addItemToInventory(loot);
        }
    }

    public static void generate_minion_loot(){
        for(Mob minion : minions){
            minion.getCharItemManager().clearInventory();
            Random random = new Random();
            ItemBase runeBase;
            int tableRoll = random.nextInt(3);
            switch(tableRoll){
                case 1:
                    runeBase = ItemBase.getItemBase(dex_rune_ids.get(random.nextInt(6)));
                    break;
                case 2:
                    runeBase = ItemBase.getItemBase(con_rune_ids.get(random.nextInt(6)));
                    break;
                default:
                    runeBase = ItemBase.getItemBase(int_rune_ids.get(random.nextInt(6)));
                    break;
            }
            if(runeBase != null){
                MobLoot loot = new MobLoot(minion,runeBase,true);
                minion.getCharItemManager().addItemToInventory(loot);
            }
        }
    }

    public static Zone get_new_hotzone(){
        Zone newHot = null;

        return newHot;
    }
}
