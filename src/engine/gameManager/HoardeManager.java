package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import engine.server.MBServerStatics;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class HoardeManager {
    public static Zone hotzone = null;
    public static ArrayList<Mob> minions = null;
    public static ArrayList<Mob> bosses = null;

    public static int HordeLevel = 0;
    public static long nextPulse = 0;
    public static long nextHorde = 0;
    public static final ArrayList<Integer> int_rune_ids = new ArrayList<>(Arrays.asList(
            250028, 250029, 250030, 250031, 250032, 250033, 250034, 250035
    ));
    public static final ArrayList<Integer> dex_rune_ids = new ArrayList<>(Arrays.asList(
            250010, 250011, 250012, 250013, 250014, 250015, 250016, 250017
    ));
    public static final ArrayList<Integer> con_rune_ids = new ArrayList<>(Arrays.asList(
            250019, 250020, 250021, 250022, 250023, 250024, 250025, 250026
    ));
    public static final List<Integer> racial_guard = Arrays.asList(
            841,951,952,1050,1052,1180,1182,1250,1252,1350,1352,1450,
            1452,1500,1502,1525,1527,1550,1552,1575,1577,1600,1602,1650,1652,1700,980100,
            980102
    );

    public static final List<Integer> GLASS_ITEMS = Arrays.asList(
            7000100, 7000110, 7000120, 7000130, 7000140, 7000150, 7000160, 7000170, 7000180, 7000190,
            7000200, 7000210, 7000220, 7000230, 7000240, 7000250, 7000270, 7000280
    );

    public static void pulse_horde(){

        if(nextHorde == 0){
            nextHorde = System.currentTimeMillis() + MBServerStatics.THREE_MINUTES;
            return;
        }

        if(nextHorde > System.currentTimeMillis())
            return;

        if(nextPulse < System.currentTimeMillis()){
            nextPulse = System.currentTimeMillis() + 1000;
            if(bosses != null) {
                for (Mob boss : bosses) {
                    if (boss.isAlive())
                        return;
                }
            }
            if(minions != null) {
                for (Mob minion : minions) {
                    if (minion.isAlive())
                        return;
                }
            }

            int secondsRemaining = (int) ((nextHorde - System.currentTimeMillis()) * 0.001f);

            if(nextHorde < System.currentTimeMillis()) {
                StartHorde();
            }
        }

    }

    public static void StartHorde(){

        minions = new ArrayList<>();
        bosses = new ArrayList<>();
        HordeLevel ++;

        Zone khar = ZoneManager.getZoneByUUID(806);
        HashSet<AbstractWorldObject> mobs = WorldGrid.getObjectsInRangePartial(khar.getLoc(),700,MBServerStatics.MASK_MOB);
        for(AbstractWorldObject awo : mobs){
            try {
                DbManager.MobQueries.DELETE_MOB((Mob)awo);
            }catch(Exception ignored){

            }
            try {
                WorldGrid.removeObject(awo);
            }catch(Exception ignored){

            }
            try {
                DbManager.removeFromCache(awo);
            }catch(Exception ignored){

            }
            try {
                ((Mob)awo).killCharacter("no raid");
                ((Mob)awo).despawn();
            }catch(Exception ignored){

            }

        }
        hotzone = khar;

        if(HordeLevel >= 4){
            HordeLevel = 0;
            ChatSystemMsg chatMsg = new ChatSystemMsg(null, "The Horde Attacking " + khar.getName() + " Has Been Defeated! Next Wave In 30 Minutes!");
            chatMsg.setMessageType(10);
            chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
            DispatchMessage.dispatchMsgToAll(chatMsg);
            nextHorde = System.currentTimeMillis() + MBServerStatics.THIRTY_MINUTES;
            return;
        }

        for(int i = 0; i < HordeLevel; i++){
            spawnInvaders();
        }

        generate_epic_loot();
        generate_minion_loot();

        ChatSystemMsg chatMsg = new ChatSystemMsg(null, "A Horde Is Attacking " + khar.getName() + ", Glory and Riches Await Those Who Would Defend!" + " Level: " + HordeLevel);
        chatMsg.setMessageType(10);
        chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
        DispatchMessage.dispatchMsgToAll(chatMsg);
    }

    public static void spawnInvaders(){
        Mob epic = create_epic();
        if(epic != null){
            epic.bindLoc = hotzone.getLoc();
            epic.equipmentSetID = 9713;
            epic.runAfterLoad();
            epic.setLoc(hotzone.getLoc());
            epic.spawnTime = 999999999;
            epic.BehaviourType = Enum.MobBehaviourType.Aggro;
            epic.setResists(new Resists());
            bosses.add(epic);
        }
        for(int i = 0; i< 5; i++){
            Mob minion = create_minion();
            if(epic != null){
                minion.bindLoc = hotzone.getLoc();
                minion.equipmentSetID = 6329;
                minion.runAfterLoad();
                minion.setLoc(hotzone.getLoc());
                minion.spawnTime = 999999999;
                minion.BehaviourType = Enum.MobBehaviourType.Aggro;
                minion.setResists(new Resists());
                minions.add(minion);
            }
        }
    }

    public static Mob create_epic(){
        Mob epic = Mob.createMob(253014,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Raid Commander",85);
        return epic;
    }

    public static Mob create_minion(){
        Mob minion = Mob.createMob(253006,hotzone.getLoc(), Guild.getErrantGuild(),true,hotzone,null,0,"Raid Minion",65);
        return minion;
    }

    public static void generate_epic_loot(){
        for(Mob currentBoss : bosses) {
            currentBoss.getCharItemManager().clearInventory();
            Random random = new Random();
            ItemBase runeBase;
            int tableRoll = random.nextInt(3);
            switch (tableRoll) {
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
            if (runeBase != null) {
                MobLoot loot = new MobLoot(currentBoss, runeBase, true);
                currentBoss.getCharItemManager().addItemToInventory(loot);
            }
            RollRacialGuard(currentBoss);
            RollBaneStone(currentBoss);
            RollGlass(currentBoss);
        }
    }

    public static void RollRacialGuard(Mob mob){
        int roll = ThreadLocalRandom.current().nextInt(10);
        if(roll != 5)
            return;
        Random random = new Random();
        int guardId = racial_guard.get(random.nextInt(racial_guard.size()));
        ItemBase guardBase = ItemBase.getItemBase(guardId);
        if(guardBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot guard = new MobLoot(mob,guardBase,false);
        mob.getCharItemManager().addItemToInventory(guard);
    }

    public static void RollBaneStone(Mob mob){
        int roll = ThreadLocalRandom.current().nextInt(25);
        if(roll != 5)
            return;
        ItemBase baneStoneBase = ItemBase.getItemBase(910018);//r8 banescroll
        if(baneStoneBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot baneStone = new MobLoot(mob,baneStoneBase,false);
        mob.getCharItemManager().addItemToInventory(baneStone);
    }

    public static void RollGlass(Mob mob){
        int roll = ThreadLocalRandom.current().nextInt(100);
        if(roll != 50)
            return;
        Random random = new Random();
        int glassId = GLASS_ITEMS.get(random.nextInt(GLASS_ITEMS.size()));
        ItemBase glassBase = ItemBase.getItemBase(glassId);
        if(glassBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot glass = new MobLoot(mob,glassBase,false);
        mob.getCharItemManager().addItemToInventory(glass);
    }

    public static void generate_minion_loot(){

        for (Mob minion : minions) {
            int roll = ThreadLocalRandom.current().nextInt(25);
            if (roll == 5) {
                minion.getCharItemManager().clearInventory();
                Random random = new Random();
                ItemBase runeBase;
                int tableRoll = random.nextInt(3);
                switch (tableRoll) {
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
                if (runeBase != null) {
                    MobLoot loot = new MobLoot(minion, runeBase, true);
                    minion.getCharItemManager().addItemToInventory(loot);
                }
            }
            minion.getCharItemManager().addGoldToInventory(ThreadLocalRandom.current().nextInt(25000,75000),false);
        }
    }
}
