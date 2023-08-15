// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.net.client.msg;

import engine.Enum.GameObjectType;
import engine.Enum.MinionType;
import engine.Enum.ProtectionState;
import engine.gameManager.NPCManager;
import engine.gameManager.PowersManager;
import engine.net.ByteBufferReader;
import engine.net.ByteBufferWriter;
import engine.net.client.Protocol;
import engine.objects.*;
import engine.powers.EffectsBase;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.Seconds;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order NPC
 */
public class ManageNPCMsg extends ClientNetMsg {

    private int targetType;
    private int targetID;
    private int unknown03;
    private int unknown04;
    private int unknown05;
    private int unknown06;
    private int messageType;
    private int unknown01;
    private int buildingID;
    private int unknown20;
    private int unknown26;
    private int unknown83;

    /**
     * This is the general purpose constructor
     */
    public ManageNPCMsg(AbstractCharacter ac) {
        super(Protocol.MANAGENPC);
        this.targetType = ac.getObjectType().ordinal();
        this.targetID = ac.getObjectUUID();
        this.messageType = 1; //This seems to be the "update Hireling window" value flag

        //Unknown defaults...

        this.unknown20 = 0;
        this.unknown26 = 0;
        this.unknown83 = 0;
    }

    //Serialize lists for Bulwarks

    private static void serializeBulwarkList(ByteBufferWriter writer, int minion) {

        writer.putInt(0);

        for (int i = 0; i < 3; i++)
            writer.putInt(0); //static

        writer.putInt(9);
        writer.putInt(5);
        writer.putInt(9);
        writer.putInt(5);
        writer.put((byte) 0);

        writer.putInt((minion == 1) ? 900 : 600); //roll time
        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(0); //Array
        writer.put((byte) 0);

        if (minion == 1)
            writer.putString("Trebuchet");
        else if (minion == 2)
            writer.putString("Ballista");
        else
            writer.putString("Mangonel");

        writer.put((byte) 1);
        writer.putString("A weapon suited to laying siege");
    }

    private static void serializeGuardList(ByteBufferWriter writer, int minion) {

        writer.putInt(1);

        for (int i = 0; i < 3; i++)
            writer.putInt(0); //static

        writer.putInt(minion);
        writer.putInt(1);
        writer.putInt(minion);
        writer.putInt(1);
        writer.put((byte) 0);

        writer.putInt(600); //roll time
        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(0); //Array
        writer.put((byte) 0);

        MinionType minionType = MinionType.ContractToMinionMap.get(minion);
        writer.putString(minionType != null ? minionType.getRace() + " " + minionType.getName() : "Minion Guard");
        writer.put((byte) 1);
        writer.putString("A Guard To Protect Your City.");
    }

    @Override
    protected int getPowerOfTwoBufferSize() {
        return (19); // 2^10 == 1024
    }

    /**
     * Deserializes the subclass specific items to the supplied NetMsgWriter.
     */
    @Override
    protected void _deserialize(ByteBufferReader reader) {
        //TODO do we need to do anything here? Does the client ever send this message to the server?
    }

    /**
     * Serializes the subclass specific items from the supplied NetMsgReader.
     */
    @Override
    protected void _serialize(ByteBufferWriter writer) {

        Period upgradePeriod;
        int upgradePeriodInSeconds;

        try {


            writer.putInt(messageType); //1
            if (messageType == 5) {
                writer.putInt(unknown20);//0
                writer.putInt(targetType);
                writer.putInt(targetID);

                writer.putInt(GameObjectType.Building.ordinal());
                writer.putInt(buildingID);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);
                writer.putInt(0);

            } else if (messageType == 1) {
                NPC npc = null;
                Mob mobA = null;

                if (this.targetType == GameObjectType.NPC.ordinal()) {

                    npc = NPC.getFromCache(this.targetID);

                    if (npc == null) {
                        Logger.error("Missing NPC of ID " + this.targetID);
                        return;
                    }

                    Contract contract = null;
                    contract = npc.getContract();

                    if (contract == null) {
                        Logger.error("Missing contract for NPC " + this.targetID);
                        return;
                    }

                    writer.putInt(0); //anything other than 0 seems to mess up the client
                    writer.putInt(targetType);
                    writer.putInt(targetID);
                    writer.putInt(0); //static....
                    writer.putInt(0);//static....
                    writer.putInt(Blueprint.getNpcMaintCost(npc.getRank()));  // salary

                    writer.putInt(npc.getUpgradeCost());

                    if (npc.isRanking() && npc.getUpgradeDateTime().isAfter(DateTime.now()))
                        upgradePeriod = new Period(DateTime.now(), npc.getUpgradeDateTime());
                    else
                        upgradePeriod = new Period(0);

                    writer.put((byte) upgradePeriod.getDays());    //for timer
                    writer.put((byte) unknown26);//unknown
                    writer.putInt(100); //unknown

                    writer.put((byte) upgradePeriod.getHours());    //for timer
                    writer.put((byte) upgradePeriod.getMinutes());  //for timer
                    writer.put((byte) upgradePeriod.getSeconds());  //for timer

                    if (npc.isRanking() && npc.getUpgradeDateTime().isAfter(DateTime.now()))
                        upgradePeriodInSeconds = Seconds.secondsBetween(DateTime.now(), npc.getUpgradeDateTime()).getSeconds();
                    else
                        upgradePeriodInSeconds = 0;

                    writer.putInt(upgradePeriodInSeconds);

                    writer.put((byte) 0);
                    writer.put((byte) (npc.getRank() == 7 ? 0 : 1));  //0 will make the upgrade field show "N/A"
                    writer.put((byte) 0);
                    writer.put((byte) 0);
                    writer.putInt(0);
                    writer.putInt(10000);  //no idea...
                    writer.put((byte) 0);
                    writer.put((byte) 0);
                    writer.put((byte) 0);
                    writer.putInt(0);

                    NPCProfits profit = NPC.GetNPCProfits(npc);

                    if (profit == null)
                        profit = NPCProfits.defaultProfits;
                    //adding .000000001 to match client.
                    int buyNormal = (int) ((profit.buyNormal + .000001f) * 100);
                    int buyGuild = (int) ((profit.buyGuild + .000001f) * 100);
                    int buyNation = (int) ((profit.buyNation + .000001f) * 100);

                    int sellNormal = (int) ((profit.sellNormal + .000001f) * 100);
                    int sellGuild = (int) ((profit.sellGuild + .000001f) * 100);
                    int sellNation = (int) ((profit.sellNation + .000001f) * 100);

                    writer.putInt(buyNormal);
                    writer.putInt(buyGuild);
                    writer.putInt(buyNation);
                    writer.putInt(sellNormal);
                    writer.putInt(sellGuild);
                    writer.putInt(sellNation);

                    if (contract.isRuneMaster()) {
                        writer.putInt(0); //vendor slots
                        writer.putInt(0); //artillery slots

                        //figure out number of protection slots based on building rank
                        int runemasterSlots = (2 * npc.getRank()) + 6;

                        writer.putInt(runemasterSlots);

                        for (int i = 0; i < 13; i++) {
                            writer.putInt(0); //statics
                        }
                        //some unknown list
                        writer.putInt(4); //list count
                        writer.putInt(17);
                        writer.putInt(2);
                        writer.putInt(12);
                        writer.putInt(23);

                        writer.putInt(0); //static
                        writer.putInt(0); //static

                        //TODO add runemaster list here

                        ArrayList<Building> buildingList = NPCManager.getProtectedBuildings(npc);

                        writer.putInt(buildingList.size());

                        for (Building b : buildingList) {
                            writer.putInt(3);
                            writer.putInt(b.getObjectType().ordinal());
                            writer.putInt(b.getObjectUUID());

                            writer.putInt(npc.getParentZone().getObjectType().ordinal());
                            writer.putInt(npc.getParentZone().getObjectUUID());

                            writer.putLong(0); //TODO Identify what Comp this is suppose to be.
                            if (b.getProtectionState() == ProtectionState.PENDING)
                                writer.put((byte) 1);
                            else
                                writer.put((byte) 0);
                            writer.put((byte) 0);
                            writer.putString(b.getName());
                            writer.putInt(1);//what?
                            writer.putInt(1);//what?
                            //taxType = b.getTaxType()
                            switch (b.taxType) {
                                case NONE:
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    break;
                                case WEEKLY:
                                    writer.putInt(b.taxAmount);
                                    writer.putInt(0);
                                    break;
                                case PROFIT:
                                    writer.putInt(0);
                                    writer.putInt(b.taxAmount);
                                    break;

                            }
                            writer.put(b.enforceKOS ? (byte) 1 : 0); //ENFORCE KOS
                            writer.put((byte) 0); //??
                            writer.putInt(1);
                        }

                        writer.putInt(0); //artillery captain list

                    } else if (contract.isArtilleryCaptain()) {
                        int slots = 1;
                        if (contract.getContractID() == 839)
                            slots = 3;


                        writer.putInt(0); //vendor slots
                        writer.putInt(slots); //artillery slots
                        writer.putInt(0); //runemaster slots

                        for (int i = 0; i < 13; i++) {
                            writer.putInt(0); //statics
                        }
                        //some unknown list
                        writer.putInt(1); //list count
                        writer.putInt(16);

                        writer.putInt(0); //static
                        writer.putInt(0); //static
                        writer.putInt(0); //runemaster list

                        //artillery captain list
                        ConcurrentHashMap<Mob, Integer> siegeMinions = npc.getSiegeMinionMap();
                        writer.putInt(1 + siegeMinions.size());
                        serializeBulwarkList(writer, 1); //Trebuchet
                        //serializeBulwarkList(writer, 2); //Ballista

                        if (siegeMinions != null && siegeMinions.size() > 0)

                            for (Mob mob : siegeMinions.keySet()) {
                                this.unknown83 = mob.getObjectUUID();
                                writer.putInt(2);
                                writer.putInt(mob.getObjectType().ordinal());
                                writer.putInt(this.unknown83);
                                writer.putInt(0);
                                writer.putInt(10);
                                writer.putInt(0);
                                writer.putInt(1);
                                writer.putInt(1);

                                long curTime = System.currentTimeMillis() / 1000;
                                long upgradeTime = (mob.deathTime + (mob.spawnTime * 1000)) / 1000;
                                long timeLife = upgradeTime - curTime;
                                if (upgradeTime * 1000 > System.currentTimeMillis()) {
                                    if (mob.npcOwner.isAlive()) {
                                        writer.put((byte) 0);//shows respawning timer
                                        writer.putInt(mob.spawnTime);
                                        writer.putInt(mob.spawnTime);
                                        writer.putInt((int) timeLife); //time remaining for mob that is dead
                                        writer.putInt(0);
                                        writer.put((byte) 0);
                                        writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                        writer.put((byte) 0);
                                    } else {
                                        writer.put((byte) 0);//shows respawning timer
                                        writer.putInt(0);
                                        writer.putInt(0);
                                        writer.putInt(0); //time remaining for mob that is dead
                                        writer.putInt(0);
                                        writer.put((byte) 0);
                                        writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                        writer.put((byte) 0);
                                    }
                                } else {
                                    //nothing required for countdown for a mob that is alive
                                    writer.put((byte) 1);//shows "Standing By"
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.put((byte) 0);
                                    writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                    writer.put((byte) 0);
                                }
                            }
                        return;

                    } else {

                        if (Contract.NoSlots(npc.getContract()))
                            writer.putInt(0);
                        else
                            writer.putInt(npc.getRank()); //vendor slots
                        writer.putInt(0); //artilerist slots
                        writer.putInt(0); //runemaster slots

                        writer.putInt(1); //is this static?
                        for (int i = 0; i < 4; i++) {
                            writer.putInt(0); //statics
                        }

                        HashSet<Integer> rollableSet = npc.getCanRoll();

                        //Begin Item list for creation.
                        writer.putInt(rollableSet.size());

                        for (Integer ib : rollableSet) {
                            ItemBase item = ItemBase.getItemBase(ib);
                            writer.put((byte) 1);
                            writer.putInt(0);
                            writer.putInt(ib); //itemID
                            writer.putInt(item.getBaseValue());
                            writer.putInt(600);
                            writer.put((byte) 1);
                            writer.put((byte) item.getModTable());
                            writer.put((byte) item.getModTable());
                            writer.put((byte) item.getModTable());
                            writer.put((byte) item.getModTable());//EffectItemType
                        }
                        ArrayList<MobLoot> itemList = npc.getRolling();

                        if (itemList.isEmpty())
                            writer.putInt(0);
                        else {
                            if (itemList.size() < npc.getRank())
                                writer.putInt(itemList.size());
                            else
                                writer.putInt(npc.getRank());

                            for (Item i : itemList) {

                                if (itemList.indexOf(i) >= npc.getRank())
                                    break;

                                ItemBase ib = i.getItemBase();

                                writer.put((byte) 0); // ? Unknown45
                                writer.putInt(i.getObjectType().ordinal());
                                writer.putInt(i.getObjectUUID());

                                writer.putInt(0);
                                writer.putInt(i.getItemBaseID());
                                writer.putInt(ib.getBaseValue());
                                long curTime = System.currentTimeMillis() / 1000;
                                long upgradeTime = i.getDateToUpgrade() / 1000;
                                long timeLife = i.getDateToUpgrade() - System.currentTimeMillis();

                                timeLife /= 1000;
                                writer.putInt((int) timeLife);
                                writer.putInt(npc.getRollingTimeInSeconds(i.getItemBaseID()));
                                writer.putInt(1);

                                if (i.isComplete())
                                    writer.put((byte) 1);
                                else
                                    writer.put((byte) 0);

                                ArrayList<String> effectsList = i.getEffectNames();
                                EffectsBase prefix = null;
                                EffectsBase suffix = null;

                                for (String effectName : effectsList) {

                                    if (effectName.contains("PRE"))
                                        prefix = PowersManager.getEffectByIDString(effectName);

                                    if (effectName.contains("SUF"))
                                        suffix = PowersManager.getEffectByIDString(effectName);
                                }

                                if ((prefix == null && suffix == null))
                                    writer.putInt(0);
                                else
                                    writer.putInt(-1497023830);

                                if ((prefix != null && !i.isRandom()) || (prefix != null && i.isComplete()))
                                    writer.putInt(prefix.getToken());
                                else
                                    writer.putInt(0);

                                if ((suffix != null && !i.isRandom()) || (suffix != null && i.isComplete()))
                                    writer.putInt(suffix.getToken());
                                else
                                    writer.putInt(0);

                                writer.putString(i.getCustomName());
                            }
                        }

                        writer.putInt(0);
                        writer.putInt(0);
                        writer.putInt(1);
                        writer.putInt(0);
                        writer.putInt(3);
                        writer.putInt(3);
                        writer.putInt(0);
                        writer.putString("Repair items");
                        writer.putString("percent");
                        writer.putInt(npc.getRepairCost()); //cost for repair
                        writer.putInt(0);

                        ArrayList<Integer> modPrefixList = npc.getModTypeTable();
                        Integer mod = modPrefixList.get(0);

                        if (mod != 0) {
                            writer.putInt(npc.getModTypeTable().size()); //Effects size

                            for (Integer mtp : npc.getModTypeTable()) {

                                int imt = modPrefixList.indexOf(mtp);
                                writer.putInt(npc.getItemModTable().get(imt)); //?
                                writer.putInt(0);
                                writer.putInt(0);
                                writer.putFloat(2);
                                writer.putInt(0);
                                writer.putInt(1);
                                writer.putInt(2);
                                writer.putInt(0);
                                writer.putInt(1);
                                writer.put(npc.getItemModTable().get(imt));
                                writer.put(npc.getItemModTable().get(imt));
                                writer.put(npc.getItemModTable().get(imt));
                                writer.put(npc.getItemModTable().get(imt));//writer.putInt(-916801465); effectItemType
                                writer.putInt(mtp); //prefix
                                int mts = modPrefixList.indexOf(mtp);
                                writer.putInt(npc.getModSuffixTable().get(mts)); //suffix
                            }
                        } else
                            writer.putInt(0);

                        ArrayList<Item> inventory = npc.getInventory();
                        writer.putInt(inventory.size()); //placeholder for item cnt

                        for (Item i : inventory)
                            Item.serializeForClientMsgWithoutSlot(i, writer);

                        writer.putInt(0);
                        writer.putInt(5);
                        writer.putInt(1);
                        writer.putInt(2);
                        writer.putInt(15);
                        writer.putInt(3);
                        writer.putInt(18);

                        writer.putInt(0);
                        writer.putInt(0);
                        writer.putInt(0);
                        writer.putInt(0);
                    }

                } else if (this.targetType == GameObjectType.Mob.ordinal()) {

                    mobA = Mob.getFromCacheDBID(this.targetID);

                    if (mobA == null) {
                        Logger.error("Missing Mob of ID " + this.targetID);
                        return;
                    }

                    if (mobA != null) {

                        Contract con = mobA.getContract();

                        if (con == null) {
                            Logger.error("Missing contract for NPC " + this.targetID);
                            return;
                        }

                        int maxSlots = 1;

                        switch (mobA.getRank()) {
                            case 1:
                            case 2:
                                maxSlots = 1;
                                break;
                            case 3:
                                maxSlots = 2;
                                break;
                            case 4:
                            case 5:
                                maxSlots = 3;
                                break;
                            case 6:
                                maxSlots = 4;
                                break;
                            case 7:
                                maxSlots = 5;
                                break;
                            default:
                                maxSlots = 1;

                        }

                        if (NPC.ISGuardCaptain(mobA.getContract().getContractID()) == false)
                            maxSlots = 0;

                        writer.putInt(0); //anything other than 0 seems to mess up the client
                        writer.putInt(targetType);
                        writer.putInt(targetID);
                        writer.putInt(0); //static....
                        writer.putInt(0);//static....
                        writer.putInt(Blueprint.getNpcMaintCost(mobA.getRank()));  // salary

                        writer.putInt(Mob.getUpgradeCost(mobA));

                        if (mobA.isRanking() && mobA.getUpgradeDateTime().isAfter(DateTime.now()))
                            upgradePeriod = new Period(DateTime.now(), mobA.getUpgradeDateTime());
                        else
                            upgradePeriod = new Period(0);

                        writer.put((byte) upgradePeriod.getDays());    //for timer
                        writer.put((byte) unknown26);//unknown
                        writer.putInt(100); //unknown

                        writer.put((byte) upgradePeriod.getHours());    //for timer
                        writer.put((byte) upgradePeriod.getMinutes());  //for timer
                        writer.put((byte) upgradePeriod.getSeconds());  //for timer

                        if (mobA.isRanking() && mobA.getUpgradeDateTime().isAfter(DateTime.now()))
                            upgradePeriodInSeconds = Seconds.secondsBetween(DateTime.now(), mobA.getUpgradeDateTime()).getSeconds();
                        else
                            upgradePeriodInSeconds = 0;

                        writer.putInt(upgradePeriodInSeconds);

                        writer.put((byte) 0);
                        writer.put((byte) (mobA.getRank() == 7 ? 0 : 1));  //0 will make the upgrade field show "N/A"
                        writer.put((byte) 0);
                        writer.put((byte) 0);
                        writer.putInt(0);
                        writer.putInt(10000);  //no idea...
                        writer.put((byte) 0);
                        writer.put((byte) 0);
                        writer.put((byte) 0);
                        writer.putInt(0);

                        NPCProfits profit = NPCProfits.defaultProfits;

                        writer.putInt((int) (profit.buyNormal * 100));
                        writer.putInt((int) (profit.buyGuild * 100));
                        writer.putInt((int) (profit.buyNation * 100));
                        writer.putInt((int) (profit.sellNormal * 100));
                        writer.putInt((int) (profit.sellGuild * 100));
                        writer.putInt((int) (profit.sellNation * 100));

                        writer.putInt(0); //vendor slots
                        writer.putInt(maxSlots); //artillery slots
                        writer.putInt(0); //runemaster slots

                        for (int i = 0; i < 13; i++)
                            writer.putInt(0); //statics

                        //some unknown list
                        writer.putInt(1); //list count
                        writer.putInt(16);

                        writer.putInt(0); //static
                        writer.putInt(0); //static
                        writer.putInt(0); //runemaster list

                        //artillery captain list
                        ConcurrentHashMap<Mob, Integer> siegeMinions = mobA.getSiegeMinionMap();

                        writer.putInt(siegeMinions.size() + 1);
                        serializeGuardList(writer, mobA.getContract().getContractID()); //Guard

                        if (siegeMinions != null && siegeMinions.size() > 0)

                            for (Mob mob : siegeMinions.keySet()) {
                                this.unknown83 = mob.getObjectUUID();
                                writer.putInt(2);
                                writer.putInt(mob.getObjectType().ordinal());
                                writer.putInt(this.unknown83);
                                writer.putInt(0);
                                writer.putInt(10);
                                writer.putInt(0);
                                writer.putInt(1);
                                writer.putInt(1);

                                long curTime = System.currentTimeMillis() / 1000;
                                long upgradeTime = (mob.deathTime + (mob.spawnTime * 1000)) / 1000;
                                long timeLife = upgradeTime - curTime;

                                if (upgradeTime * 1000 > System.currentTimeMillis()) {
                                    if (mob.npcOwner.isAlive()) {
                                        writer.put((byte) 0);//shows respawning timer
                                        writer.putInt(mob.spawnTime);
                                        writer.putInt(mob.spawnTime);
                                        writer.putInt((int) timeLife); //time remaining for mob that is dead
                                        writer.putInt(0);
                                        writer.put((byte) 0);
                                        writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                        writer.put((byte) 0);
                                    } else {
                                        writer.put((byte) 0);//shows respawning timer
                                        writer.putInt(0);
                                        writer.putInt(0);
                                        writer.putInt(0); //time remaining for mob that is dead
                                        writer.putInt(0);
                                        writer.put((byte) 0);
                                        writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                        writer.put((byte) 0);
                                    }
                                } else {
                                    //nothing required for countdown for a mob that is alive
                                    writer.put((byte) 1);//shows "Standing By"
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.putInt(0);
                                    writer.put((byte) 0);
                                    writer.putString(mob.getNameOverride().isEmpty() ? mob.getName() : mob.getNameOverride());
                                    writer.put((byte) 0);
                                }
                            }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getUnknown01() {
        return unknown01;
    }

    public void setUnknown01(int unknown01) {
        this.unknown01 = unknown01;
    }

    public int getUnknown03() {
        return unknown03;
    }

    public void setUnknown03(int unknown03) {
        this.unknown03 = unknown03;
    }

    public int getUnknown04() {
        return unknown04;
    }

    public void setUnknown04(int unknown04) {
        this.unknown04 = unknown04;
    }

    public int getUnknown05() {
        return unknown05;
    }

    public void setUnknown05(int unknown05) {
        this.unknown05 = unknown05;
    }

    public int getUnknown06() {
        return unknown06;
    }

    public void setUnknown06(int unknown06) {
        this.unknown06 = unknown06;
    }
    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(int buildingID) {
        this.buildingID = buildingID;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public int getTargetID() {
        return targetID;
    }

    public void setTargetID(int targetID) {
        this.targetID = targetID;
    }


}
