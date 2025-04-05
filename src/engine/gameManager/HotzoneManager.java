package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.math.Vector3fImmutable;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.HotzoneChangeMsg;
import engine.objects.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class HotzoneManager {

    public static Zone hotzone = null;
    public static Mob currentBoss = null;
    public static ArrayList<Mob> minions = null;

    public static int bossID = 14180;
    public static int minionID = 2112;

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
        HotzoneManager.hotzone = null;
        if(HotzoneManager.currentBoss != null) {
            DbManager.MobQueries.DELETE_MOB(HotzoneManager.currentBoss);
            WorldGrid.removeObject(HotzoneManager.currentBoss);
            HotzoneManager.currentBoss.removeFromCache();
        }
        if(HotzoneManager.minions != null) {
            for (Mob minion : HotzoneManager.minions) {
                if (minion != null) {
                    DbManager.MobQueries.DELETE_MOB(minion);
                    WorldGrid.removeObject(minion);
                    minion.removeFromCache();
                }
            }
        }
    }

    public static void create_random_hotzone(){

        wipe_hotzone();

        Zone newHot = null;
        while (newHot == null){
            int roll = ThreadLocalRandom.current().nextInt(ZoneManager.macroZones.size() + 1);
            if(ZoneManager.macroZones.toArray()[roll] != null){
                Zone potential = (Zone) ZoneManager.macroZones.toArray()[roll];
                if(potential.getSafeZone() == 0) {
                    HotzoneManager.hotzone = potential;
                    break;
                }
            }
        }

        HotzoneManager.currentBoss = create_epic();
        HotzoneManager.currentBoss.equipmentSetID = 9713;
        HotzoneManager.currentBoss.runAfterLoad();
        HotzoneManager.currentBoss.setLoc(hotzone.getLoc());
        HotzoneManager.currentBoss.spawnTime = 999999999;
        HotzoneManager.currentBoss.BehaviourType = Enum.MobBehaviourType.Aggro;
        HotzoneManager.currentBoss.setResists(new Resists());
        HotzoneManager.minions = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            Mob minion = create_minion();
            minion.equipmentSetID = 6329;
            minion.runAfterLoad();
            minion.setLoc(Vector3fImmutable.getRandomPointOnCircle(currentBoss.loc,32f));
            HotzoneManager.minions.add(minion);
            minion.spawnTime = 999999999;
            minion.BehaviourType = Enum.MobBehaviourType.Aggro;
        }
        generate_epic_loot();

        generate_minion_loot();

        ZoneManager.hotZoneLastUpdate = LocalDateTime.now().withMinute(0).withSecond(0).atZone(ZoneId.systemDefault()).toInstant();

        for(PlayerCharacter player : SessionManager.getAllActivePlayerCharacters()) {
            int zoneType = Enum.GameObjectType.Zone.ordinal();
            int zoneUUID = HotzoneManager.hotzone.getObjectUUID();
            HotzoneChangeMsg hcm = new HotzoneChangeMsg(zoneType, zoneUUID);
            Dispatch dispatch = Dispatch.borrow(player, hcm);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
        }
    }

    public static Mob create_epic(){
        Mob epic = Mob.createMob(253014,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Hotzone Commander",85);
        return epic;
    }

    public static Mob create_minion(){
        Mob minion = Mob.createMob(253006,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Hotzone Minion",65);
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
        if(ThreadLocalRandom.current().nextInt(100) > 35)
            return;
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

    public static void end_hotzone(){
        wipe_hotzone();
        for(PlayerCharacter player : SessionManager.getAllActivePlayerCharacters()) {
            int zoneType = Enum.GameObjectType.Zone.ordinal();
            HotzoneChangeMsg hcm = new HotzoneChangeMsg(zoneType, 0);
            Dispatch dispatch = Dispatch.borrow(player, hcm);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
        }
    }
}
