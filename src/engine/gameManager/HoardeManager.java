package engine.gameManager;

import engine.Enum;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import engine.server.MBServerStatics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
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

    public static void pulse_horde(){
        if(nextPulse < System.currentTimeMillis()){
            for(Mob boss : bosses){
                if(boss.isAlive())
                    return;
            }
            for(Mob minion : minions){
                if(minion.isAlive())
                    return;
            }
            nextPulse = System.currentTimeMillis() + 10000;
        }

        if(nextHorde < System.currentTimeMillis())
            StartHorde();

    }

    public static void StartHorde(){

        if(bosses != null){
            for(Mob boss : bosses){
                DbManager.MobQueries.DELETE_MOB(boss);
            }
        }

        if(minions != null){
            for(Mob minion : minions){
                if(minion.isAlive())
                    DbManager.MobQueries.DELETE_MOB(minion);
            }
        }

        minions = new ArrayList<>();
        bosses = new ArrayList<>();
        HordeLevel ++;

        Zone khar = ZoneManager.getZoneByUUID(806);
        hotzone = khar;

        if(HordeLevel >= 11){
            HordeLevel = 1;
            ChatSystemMsg chatMsg = new ChatSystemMsg(null, "The Horde Attacking " + khar.getName() + " Has Been Defeated!");
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
}
