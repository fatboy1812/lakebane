// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.Enum.ItemContainerType;
import engine.Enum.ItemType;
import engine.Enum.OwnerType;
import engine.gameManager.*;
import engine.loot.ModTableEntry;
import engine.loot.ModTypeTableEntry;
import engine.net.ItemProductionManager;
import engine.net.ItemQueue;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ErrorPopupMsg;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ItemFactory {

    public static void fillInventory(PlayerCharacter pc, int objectID, int count) {

        if (pc == null)
            return;

        int max = 20;
        CharacterItemManager itemManager = pc.getCharItemManager();
        ItemBase ib = ItemBase.getItemBase(objectID);
        if (count > max)
            count = max;

        ClientConnection cc = pc.getClientConnection();

        if (itemManager == null || ib == null || cc == null)
            return;

        boolean worked;
        for (int i = 0; i < count; i++) {
            worked = false;

            if (!itemManager.hasRoomInventory(ib.getWeight())) {
                if (pc != null)
                    ChatManager.chatSystemInfo(pc, "You can not carry any more of that item.");
                break;
            }

            Item item = new Item(ib, pc.getObjectUUID(), OwnerType.PlayerCharacter, (byte) 0, (byte) 0,
                    (short) 1, (short) 1, true, false, ItemContainerType.INVENTORY, (byte) 0,
                    new ArrayList<>(), "");
            try {
                item = DbManager.ItemQueries.ADD_ITEM(item);
                worked = true;
            } catch (Exception e) {
                Logger.error(e);
            }
            if (worked) {
                itemManager.addItemToInventory(item);
            }
        }
        itemManager.updateInventory();
    }

    public static Item ResourceRoll(NPC npc, PlayerCharacter pc, int itemsToRoll, int itemID, int pToken, int sToken, String customName) {

        String prefixString = "";
        String suffixString = "";
        if (npc == null)
            return null;

        boolean useWarehouse = false;

        ItemBase ib = ItemBase.getItemBase(itemID);

        if (ib == null)
            return null;

        Building forge = npc.getBuilding();

        if (forge == null)
            return null;


        if (!npc.getCharItemManager().hasRoomInventory(ib.getWeight())) {
            if (pc != null)
                ErrorPopupMsg.sendErrorPopup(pc, 21);
            return null;
        }

        Zone zone = npc.getBuilding().getParentZone();

        if (zone == null)
            return null;

        City city = City.getCity(zone.getPlayerCityUUID());

        if (city == null)
            return null;
        MobLoot ml = null;
        city.transactionLock.writeLock().lock();

        try {
            Warehouse cityWarehouse = city.getWarehouse();

            if (cityWarehouse != null && forge.assetIsProtected())
                useWarehouse = true;
            // ROLL BANE SCROLL.

            if (ib.getUUID() > 910010 && ib.getUUID() < 910019) {
                ConcurrentHashMap<ItemBase, Integer> resources = cityWarehouse.getResources();


                int buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, ib.getBaseValue());
                int overdraft = BuildingManager.GetOverdraft(forge, ib.getBaseValue());

                if (overdraft > 0 && !useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + " " + ib.getName());
                    return null;
                }

                if (overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + " " + ib.getName());
                    return null;
                }

                if (overdraft > resources.get(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + " " + ib.getName());
                    return null;
                }

                //All checks passed, lets withdraw from building first.

                //			if (pc != null){
                //				ChatManager.chatGuildInfo(pc.getGuild(), "Building withdraw = " + buildingWithdraw);
                //				ChatManager.chatGuildInfo(pc.getGuild(), "Warehouse overdraft withdraw = " + overdraft);
                //
                //				ChatManager.chatGuildInfo(pc.getGuild(), "total withdraw = " + (overdraft + buildingWithdraw));
                //			}

                if (!forge.transferGold(-buildingWithdraw, false)) {
                    overdraft += buildingWithdraw;

                    if (!useWarehouse) {
                        ErrorPopupMsg.sendErrorMsg(pc, "Building does not have enough gold to produce this item." + ib.getName());
                        return null;
                    } else {
                        if (overdraft > resources.get(ItemBase.GOLD_ITEM_BASE)) {
                            ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse to produce this item." + ib.getName());
                            return null;
                        }
                    }
                }

                if (overdraft > 0)
                    if (!cityWarehouse.withdraw(npc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                        //ChatManager.chatGuildError(pc, "Failed to create Item");
                        Logger.error("Warehouse With UID of " + cityWarehouse.getUID() + " Failed to Create Item." + ib.getName());
                        return null;
                    }


                ml = new MobLoot(npc, ib, false);


                ml.containerType = Enum.ItemContainerType.FORGE;
                ml.setValue(0);
                ml.loadEnchantments();

                float time;
                float rank = npc.getBuilding().getRank() - 1;
                float rate = (float) (2.5 * rank);
                time = (20 - rate);
                time *= MBServerStatics.ONE_MINUTE;

                if (ml.getItemBase().getUUID() > 910010 && ml.getItemBase().getUUID() < 910019) {
                    rank = ml.getItemBaseID() - 910010;
                    time = rank * 60 * 60 * 3 * 1000;

                }

                // No job is submitted, as object's upgradetime field
                // is used to determin whether or not an object has
                // compelted rolling.  The game object exists previously
                // to this, not when 'compelte' is pressed.
                long upgradeTime = System.currentTimeMillis() + (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue()));

                DateTime dateTime = new DateTime();
                dateTime = dateTime.withMillis(upgradeTime);
                ml.setDateToUpgrade(upgradeTime);

                npc.addItemToForge(ml);

                int playerID = 0;

                if (pc != null)
                    playerID = pc.getObjectUUID();
                DbManager.NPCQueries.ADD_TO_PRODUCTION_LIST(ml.getObjectUUID(), npc.getObjectUUID(), ml.getItemBaseID(), dateTime, "", "", "", false, playerID);
                ProducedItem pi = new ProducedItem(ml.getObjectUUID(), npc.getObjectUUID(), ml.getItemBaseID(), dateTime, false, "", "", "", playerID);
                pi.setProducedItemID(ml.getObjectUUID());
                pi.setAmount(itemsToRoll);
                pi.setRandom(false);

                ItemQueue produced = ItemQueue.borrow(pi, (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue())));
                ItemProductionManager.send(produced);

                return ml;
            }


            int galvorAmount = 0;
            int wormwoodAmount = 0;
            int prefixCost = 0;
            int suffixCost = 0;


            if (ib.getType() == ItemType.WEAPON && ib.getPercentRequired() == 110) {
                switch (ib.getSkillRequired()) {
                    case "Bow":
                    case "Crossbow":
                    case "Spear":
                    case "Pole Arm":
                    case "Staff":
                        wormwoodAmount = 20;
                        break;
                    case "Axe":
                    case "Dagger":
                    case "Sword":
                    case "Hammer":
                    case "Unarmed Combat":

                        if (ib.isTwoHanded())
                            galvorAmount = 20;
                        else
                            galvorAmount = 10;
                        break;
                }
            }

            ItemBase galvor = ItemBase.getItemBase(1580017);
            ItemBase wormwood = ItemBase.getItemBase(1580018);

            if (galvorAmount > 0 || wormwoodAmount > 0)
                if (!useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "This item requires resources to roll! Please make sure the forge is protected to access the warehouse." + ib.getName());
                    return null;

                }

            if (galvorAmount > 0) {
                if (cityWarehouse.isResourceLocked(galvor)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Galvor is locked." + ib.getName());
                    return null;
                }

                if (cityWarehouse.getResources().get(galvor) < galvorAmount) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Galvor in warehouse to roll this item." + ib.getName());
                    return null;
                }
            }

            if (wormwoodAmount > 0) {
                if (cityWarehouse.isResourceLocked(wormwood)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Wormwood is locked." + ib.getName());
                    return null;
                }

                if (cityWarehouse.getResources().get(wormwood) < wormwoodAmount) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Wormwood in warehouse to roll this item." + ib.getName());
                    return null;
                }
            }
            ConcurrentHashMap<ItemBase, Integer> suffixResourceCosts = null;
            ConcurrentHashMap<ItemBase, Integer> prefixResourceCosts = null;
            EffectsBase prefix = null;
            if (pToken != 0) {

                if (!useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Forge cannot access warehouse! Check to make sure forge is protected." + ib.getName());
                    return null;
                }
                prefix = PowersManager.getEffectByToken(pToken);
                if (prefix == null)
                    return null;
                EffectsBase prefixValue = PowersManager.getEffectByIDString(prefix.getIDString() + 'A');
                if (prefixValue == null)
                    return null;

                int baseCost = ib.getBaseValue();
                int effectCost = (int) prefixValue.getValue();
                int total = baseCost * 10 + effectCost;

                prefixCost = effectCost;
                int buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, total);
                int overdraft = BuildingManager.GetOverdraft(forge, total);

                if (overdraft > 0 && !useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                    return null;
                }

                if (overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                    return null;
                }

                if (overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                    return null;
                }
                prefixResourceCosts = prefix.getResourcesForEffect();
                for (ItemBase ibResources : prefixResourceCosts.keySet()) {
                    int warehouseAmount = cityWarehouse.getResources().get(ibResources);
                    int creationAmount = prefixResourceCosts.get(ibResources);
                    //ChatManager.chatInfoError(pc, "Prefix : " + ibResources.getName() + " / " + creationAmount);
                    if (warehouseAmount < creationAmount) {
                        //ChatManager.chatInfoError(pc, "You need at least " + creationAmount + " " + ibResources.getName() + " to Create this item.");
                        return null;
                    }

                }
            }

            EffectsBase suffix = null;
            if (sToken != 0) {

                if (!useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Forge cannot access warehouse! Check to make sure forge is protected." + ib.getName());
                    return null;
                }
                suffix = PowersManager.getEffectByToken(sToken);
                if (suffix == null)
                    return null;
                EffectsBase suffixValue = PowersManager.getEffectByIDString(suffix.getIDString() + 'A');
                if (suffixValue == null)
                    return null;
                suffixResourceCosts = suffix.getResourcesForEffect();
                int baseCost = ib.getBaseValue();
                int effectCost = (int) suffixValue.getValue();
                suffixCost = effectCost;
                int total = baseCost * 10 + effectCost;


                //	int buildingWithdraw = Building.GetWithdrawAmountForRolling(forge, total);
                int overdraft = BuildingManager.GetOverdraft(forge, total);

                if (overdraft > 0 && !useWarehouse) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                    return null;
                }

                if (overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                    return null;
                }

                if (overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                    return null;
                }


                for (ItemBase ibResources : suffixResourceCosts.keySet()) {
                    int warehouseAmount = cityWarehouse.getResources().get(ibResources);
                    int creationAmount = suffixResourceCosts.get(ibResources);
                    if (warehouseAmount < creationAmount) {
                        //					if (pc != null)
                        //						ChatManager.chatInfoError(pc, "You need at least " + creationAmount + " " + ibResources.getName() + " to Create this item.");
                        return null;
                    }


                }

            }


            //Check if Total suffix and prefix costs + itemCost can be withdrawn.
            int costToCreate = suffixCost + prefixCost + (ib.getBaseValue());
            int buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, costToCreate);

            int overdraft = BuildingManager.GetOverdraft(forge, costToCreate);

            if (overdraft > 0 && !useWarehouse) {
                if (pc != null)
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                return null;
            }

            if (overdraft > 0 && useWarehouse && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                if (pc != null)
                    ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                return null;
            }

            if (useWarehouse && overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {
                if (pc != null)
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                return null;
            }

            //		if (pc != null){
            //			ChatManager.chatGuildInfo(pc.getGuild(), "Building withdraw = " + buildingWithdraw);
            //			ChatManager.chatGuildInfo(pc.getGuild(), "Warehouse overdraft withdraw = " + overdraft);
            //
            //			ChatManager.chatGuildInfo(pc.getGuild(), "total withdraw = " + (overdraft + buildingWithdraw));
            //		}

            if (!forge.transferGold(-buildingWithdraw, false)) {
                overdraft += buildingWithdraw;

                if (!useWarehouse) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Building does not have enough gold to produce this item." + ib.getName());
                    return null;
                } else {
                    if (overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse to produce this item." + ib.getName());
                        return null;
                    }
                }
            }

            if (overdraft > 0 && useWarehouse)
                if (!cityWarehouse.withdraw(npc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                    //ChatManager.chatGuildError(pc, "Failed to create Item");
                    Logger.error("Warehouse With UID of " + cityWarehouse.getUID() + " Failed to Create Item." + ib.getName());
                    return null;
                }


            if (prefix != null) {

                if (!useWarehouse) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Cannot Resource Roll without access to the warehouse! Make sure the forge is currently protected." + ib.getName());
                    return null;
                }


                for (ItemBase ibResources : prefixResourceCosts.keySet()) {

                    int creationAmount = prefixResourceCosts.get(ibResources);

                    if (cityWarehouse.isResourceLocked(ibResources) == true)
                        return null;

                    int oldAmount = cityWarehouse.getResources().get(ibResources);
                    int amount = creationAmount;

                    if (oldAmount < amount)
                        amount = oldAmount;

                    if (!cityWarehouse.withdraw(npc, ibResources, amount, false, true)) {
                        //ChatManager.chatGuildError(pc, "Failed to create Item");
                        Logger.error("Warehouse With UID of " + cityWarehouse.getUID() + " Failed to Create Item." + ib.getName());
                        return null;
                    }
                }
            }

            if (suffix != null) {

                for (ItemBase ibResources : suffixResourceCosts.keySet()) {
                    int creationAmount = suffixResourceCosts.get(ibResources);

                    if (cityWarehouse.isResourceLocked(ibResources) == true) {
                        ChatManager.chatSystemError(pc, ibResources.getName() + " is locked!" + ib.getName());
                        return null;
                    }

                    int oldAmount = cityWarehouse.getResources().get(ibResources);
                    int amount = creationAmount;
                    if (oldAmount < amount)
                        amount = oldAmount;
                    if (!cityWarehouse.withdraw(npc, ibResources, amount, false, true)) {
                        //ChatManager.chatGuildError(pc, "Failed to create Item");
                        Logger.error("Warehouse With UID of " + cityWarehouse.getUID() + " Failed to Create Item." + ib.getName());
                        return null;
                    }
                }
            }

            if (prefix == null && suffix == null) {

                int baseCost = ib.getBaseValue();
                int total = (int) (baseCost + baseCost * (float) .10);

                buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, total);

                overdraft = BuildingManager.GetOverdraft(forge, total);

                if (overdraft > 0 && !useWarehouse) {

                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                    return null;
                }

                if (overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {

                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                    return null;
                }

                if (useWarehouse && overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {

                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                    return null;
                }
            }

            if (!forge.transferGold(-buildingWithdraw, false)) {
                overdraft += buildingWithdraw;

                if (!useWarehouse) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Building does not have enough gold to produce this item." + ib.getName());
                    return null;
                } else {
                    if (overdraft > cityWarehouse.getResources().get(ItemBase.GOLD_ITEM_BASE)) {
                        ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse to produce this item." + ib.getName());
                        return null;
                    }
                }
            }

            if (overdraft > 0)
                if (!cityWarehouse.withdraw(npc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                    //ChatManager.chatGuildError(pc, "Failed to create Item");
                    Logger.error("Warehouse With UID of " + cityWarehouse.getUID() + " Failed to Create Item." + ib.getName());
                    return null;
                }

            //	ChatManager.chatGuildInfo(pc, "Gold Cost = " + total);

            if (galvorAmount > 0) {
                if (!cityWarehouse.withdraw(npc, galvor, galvorAmount, false, true)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Failed to withdraw Galvor from warehouse!" + ib.getName());
                    Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                    return null;
                }
            }

            if (wormwoodAmount > 0) {
                if (!cityWarehouse.withdraw(npc, wormwood, wormwoodAmount, false, true)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Failed to withdraw Wormwood from warehouse!" + ib.getName());
                    Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                    return null;
                }
            }

            ml = new MobLoot(npc, ib, false);

            ml.containerType = Enum.ItemContainerType.FORGE;
            ml.setName(customName);

            if (prefix != null) {
                ml.addPermanentEnchantment(prefix.getIDString(), 0, 0, true);
                ml.setPrefix(prefix.getIDString());
                prefixString = prefix.getIDString();
            }

            if (suffix != null) {
                ml.addPermanentEnchantment(suffix.getIDString(), 0, 0, false);
                ml.setSuffix(suffix.getIDString());
                suffixString = suffix.getIDString();
            }

            ml.loadEnchantments();
            //set value to 0 so magicvalue can be recalculated in getValue.
            ml.setValue(0);


            float time;
            float rank = npc.getBuilding().getRank() - 1;
            float rate = (float) (2.5 * rank);
            time = (20 - rate);
            time *= MBServerStatics.ONE_MINUTE;

            if (ml.getItemBase().getUUID() > 910010 && ml.getItemBase().getUUID() < 910019) {
                rank = ml.getItemBaseID() - 910010;
                time = rank * 60 * 60 * 3 * 1000;
            }


            // No job is submitted, as object's upgradetime field
            // is used to determin whether or not an object has
            // compelted rolling.  The game object exists previously
            // to this, not when 'compelte' is pressed.
            long upgradeTime = System.currentTimeMillis() + (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue()));

            DateTime dateTime = new DateTime();
            dateTime = dateTime.withMillis(upgradeTime);
            ml.setDateToUpgrade(upgradeTime);

            npc.addItemToForge(ml);
            int playerID = 0;

            if (pc != null)
                playerID = pc.getObjectUUID();

            DbManager.NPCQueries.ADD_TO_PRODUCTION_LIST(ml.getObjectUUID(), npc.getObjectUUID(), ml.getItemBaseID(), dateTime, prefixString, suffixString, ml.getCustomName(), false, playerID);
            ProducedItem pi = new ProducedItem(npc.getRolling().size(), npc.getObjectUUID(), ml.getItemBaseID(), dateTime, false, prefixString, suffixString, ml.getCustomName(), playerID);
            pi.setProducedItemID(ml.getObjectUUID());
            pi.setAmount(itemsToRoll);

            ItemQueue produced = ItemQueue.borrow(pi, (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue())));
            ItemProductionManager.send(produced);
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            city.transactionLock.writeLock().unlock();
        }


        //		npc.addItemToForge(item);
        return ml;

    }


    public static Item randomRoll(NPC vendor, PlayerCharacter playerCharacter, int itemsToRoll, int itemBaseID) {
        byte itemModTable;
        int prefixMod = 0;
        int suffixMod = 0;
        String prefix = "";
        String suffix = "";

        ModTableEntry prefixEntry = null;
        ModTableEntry suffixEntry = null;

        ItemBase ib = ItemBase.getItemBase(itemBaseID);

        if (ib == null)
            return null;

        if (!vendor.getCharItemManager().hasRoomInventory(ib.getWeight())) {

            if (playerCharacter != null)
                ChatManager.chatSystemInfo(playerCharacter, vendor.getName() + " " + vendor.getContract().getName() + " Inventory is full.");

            return null;
        }

        itemModTable = (byte) ib.getModTable();

        if (!vendor.getItemModTable().contains(itemModTable)) {

            if (playerCharacter != null)
                ErrorPopupMsg.sendErrorPopup(playerCharacter, 59);

            return null;
        }

        for (byte temp : vendor.getItemModTable()) {

            if (itemModTable != temp)
                continue;

            prefixMod = vendor.getModTypeTable().get(vendor.getItemModTable().indexOf(temp));
            suffixMod = vendor.getModSuffixTable().get(vendor.getItemModTable().indexOf(temp));
        }

        if (prefixMod == 0 && suffixMod == 0) {
            Logger.info("Failed to find modTables for item " + ib.getName());
            return null;
        }

        // Roll on the tables for this vendor

        ModTypeTableEntry prefixTypeTable = ModTypeTableEntry.rollTable(prefixMod, ThreadLocalRandom.current().nextInt(1, 100 + 1));
        ModTypeTableEntry suffixTypeTable = ModTypeTableEntry.rollTable(suffixMod, ThreadLocalRandom.current().nextInt(1, 100 + 1));

        // Sanity check.

        if (prefixTypeTable == null || suffixTypeTable == null)
            return null;

        int rollPrefix = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        if (rollPrefix < 80) {

            int randomPrefix = TableRoll(vendor.getLevel());
            prefixEntry = ModTableEntry.rollTable(prefixTypeTable.modTableID, randomPrefix);

            if (prefixEntry != null)
                prefix = prefixEntry.action;

        }

        int rollSuffix = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        // Always have at least one mod on a magic rolled item.
        // Suffix will be our backup plan.

        if (rollSuffix < 80 || prefixEntry == null) {

            int randomSuffix = TableRoll(vendor.getLevel());
            suffixEntry = ModTableEntry.rollTable(suffixTypeTable.modTableID, randomSuffix);

            if (suffixEntry != null)
                suffix = suffixEntry.action;
        }

        MobLoot toRoll = ItemFactory.produceRandomRoll(vendor, playerCharacter, prefix, suffix, itemBaseID);

        if (toRoll == null)
            return null;

        toRoll.setValue(0);

        float time;
        float rank = vendor.getBuilding().getRank() - 1;
        float rate = (float) (2.5 * rank);
        time = (20 - rate);
        time *= MBServerStatics.ONE_MINUTE;

        if (toRoll.getItemBase().getUUID() > 910010 && toRoll.getItemBase().getUUID() < 910019) {
            rank = toRoll.getItemBaseID() - 910010;
            time = rank * 60 * 60 * 3 * 1000;
        }

        // No job is submitted, as object's upgradetime field
        // is used to determine whether an object has
        // completed rolling.  The game object exists previously
        // to this, not when 'complete' is pressed.

        long upgradeTime = System.currentTimeMillis() + (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue()));

        DateTime dateTime = new DateTime();
        dateTime = dateTime.withMillis(upgradeTime);
        toRoll.setDateToUpgrade(upgradeTime);

        int playerID = 0;

        if (playerCharacter != null)
            playerID = playerCharacter.getObjectUUID();

        DbManager.NPCQueries.ADD_TO_PRODUCTION_LIST(toRoll.getObjectUUID(), vendor.getObjectUUID(), toRoll.getItemBaseID(), dateTime, prefix, suffix, toRoll.getCustomName(), true, playerID);

        ProducedItem pi = new ProducedItem(toRoll.getObjectUUID(), vendor.getObjectUUID(), toRoll.getItemBaseID(), dateTime, true, prefix, suffix, toRoll.getCustomName(), playerID);
        pi.setProducedItemID(toRoll.getObjectUUID());
        pi.setAmount(itemsToRoll);
        ItemQueue produced = ItemQueue.borrow(pi, (long) (time * Float.parseFloat(ConfigManager.MB_PRODUCTION_RATE.getValue())));
        ItemProductionManager.send(produced);

        return toRoll;
    }

    public static int TableRoll(int vendorLevel) {
        // Calculate min and max based on mobLevel
        int min = 60;
        int max = 120;
        switch(vendorLevel){
            case 20:
                min = 70;
                max = 140;
                break;
            case 30:
                min = 80;
                max = 160;
                break;
            case 40:
                min = 90;
                max = 180;
                break;
            case 50:
                min = 100;
                max = 200;
                break;
            case 60:
                min = 175;
                max = 260;
                break;
            case 70:
                min = 220;
                max = 320;
                break;
        }

        int roll = ThreadLocalRandom.current().nextInt(min, max + 1);

        return roll;
    }


    public static MobLoot produceRandomRoll(NPC npc, PlayerCharacter pc, String prefixString, String suffixString, int itemID) {

        boolean useWarehouse = false;

        if (npc == null)
            return null;

        ItemBase ib = ItemBase.getItemBase(itemID);

        if (ib == null)
            return null;

        Building forge = npc.getBuilding();

        if (forge == null)
            return null;

        Zone zone = npc.getBuilding().getParentZone();

        if (zone == null)
            return null;

        City city = City.getCity(zone.getPlayerCityUUID());

        if (city == null)
            return null;

        MobLoot ml = null;
        city.transactionLock.writeLock().lock();

        try {


            Warehouse cityWarehouse = city.getWarehouse();

            if (cityWarehouse != null && forge.assetIsProtected())
                useWarehouse = true;

            ConcurrentHashMap<ItemBase, Integer> resources = null;

            if (useWarehouse)
                resources = cityWarehouse.getResources();

            int galvorAmount = 0;
            int wormwoodAmount = 0;

            if (ib.getType() == ItemType.WEAPON && ib.getPercentRequired() == 110) {
                switch (ib.getSkillRequired()) {
                    case "Bow":
                    case "Crossbow":
                    case "Spear":
                    case "Pole Arm":
                    case "Staff":
                        wormwoodAmount = 22;
                        break;
                    case "Axe":
                    case "Dagger":
                    case "Sword":
                    case "Hammer":
                    case "Unarmed Combat":

                        if (ib.isTwoHanded())
                            galvorAmount = 22;
                        else
                            galvorAmount = 11;
                        break;
                }
            }

            ItemBase galvor = ItemBase.getItemBase(1580017);
            ItemBase wormwood = ItemBase.getItemBase(1580018);

            //Cant roll 110% weapons that require resources if not allowed to use warehouse.
            if (galvorAmount > 0 || wormwoodAmount > 0)
                if (!useWarehouse)
                    return null;

            if (galvorAmount > 0) {
                if (cityWarehouse.isResourceLocked(galvor)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Galvor is locked." + ib.getName());
                    return null;
                }

                if (cityWarehouse.getResources().get(galvor) < galvorAmount) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough Galvor in warehouse to roll this item." + ib.getName());
                    return null;
                }
            }

            if (wormwoodAmount > 0) {
                if (cityWarehouse.isResourceLocked(wormwood)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Galvor is locked." + ib.getName());
                    return null;
                }

                if (cityWarehouse.getResources().get(wormwood) < wormwoodAmount) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough Galvor in warehouse to roll this item." + ib.getName());
                    return null;
                }
            }

            EffectsBase prefix = null;

            if (!prefixString.isEmpty()) {
                prefix = PowersManager.getEffectByIDString(prefixString);
                if (prefix == null)
                    return null;
            }

            ItemBase goldIB = ItemBase.getGoldItemBase();

            //int baseCost = ib.getBaseValue();
            int total = ib.getBaseValue();

            EffectsBase suffix = null;

            if (!suffixString.isEmpty()) {
                suffix = PowersManager.getEffectByIDString(suffixString);

                if (suffix == null)
                    return null;
            }

            //calculate gold costs and remove from the warehouse
            if (prefix != null || suffix != null) {
                int costToCreate = (int) (ib.getBaseValue() + ib.getBaseValue() * .10f);
                int buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, costToCreate);
                int overdraft = BuildingManager.GetOverdraft(forge, costToCreate);

                if (overdraft > 0 && !useWarehouse) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                    return null;
                }

                if (useWarehouse && overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                    return null;
                }

                if (useWarehouse && overdraft > resources.get(goldIB)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                    return null;
                }

                if (!forge.transferGold(-buildingWithdraw, false)) {
                    overdraft += buildingWithdraw;

                    if (!useWarehouse) {
                        ErrorPopupMsg.sendErrorMsg(pc, "Building does not have enough gold to produce this item." + ib.getName());
                        return null;
                    } else {
                        if (overdraft > resources.get(goldIB)) {
                            ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse to produce this item." + ib.getName());
                            return null;
                        }
                    }
                }

                // there was an overdraft, withdraw the rest from warehouse.
                if (overdraft > 0) {
                    if (pc != null) {
                        if (!cityWarehouse.withdraw(pc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                            Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                            return null;
                        }
                    } else {
                        if (!cityWarehouse.withdraw(npc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                            Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                            return null;
                        }
                    }
                }
            }

            if (prefix == null && suffix == null) {

                int buildingWithdraw = BuildingManager.GetWithdrawAmountForRolling(forge, total);
                int overdraft = BuildingManager.GetOverdraft(forge, total);

                if (overdraft > 0 && !useWarehouse) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough gold in building strongbox." + ib.getName());
                    return null;
                }

                if (useWarehouse && overdraft > 0 && cityWarehouse.isResourceLocked(ItemBase.GOLD_ITEM_BASE)) {
                    if (pc != null)
                        ErrorPopupMsg.sendErrorMsg(pc, "Warehouse gold is barred! Overdraft cannot be withdrawn from warehouse." + ib.getName());
                    return null;
                }

                if (useWarehouse && overdraft > resources.get(goldIB)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse for overdraft." + ib.getName());
                    return null;
                }

                if (!forge.transferGold(-buildingWithdraw, false)) {
                    overdraft += buildingWithdraw;

                    if (!useWarehouse) {
                        ErrorPopupMsg.sendErrorMsg(pc, "Building does not have enough gold to produce this item." + ib.getName());
                        return null;
                    } else {
                        if (overdraft > resources.get(goldIB)) {
                            ErrorPopupMsg.sendErrorMsg(pc, "Not enough Gold in Warehouse to produce this item." + ib.getName());
                            return null;
                        }
                    }
                }

                if (overdraft > 0 && useWarehouse) {

                    if (pc != null) {
                        if (!cityWarehouse.withdraw(pc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                            Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                            return null;
                        }
                    } else {
                        if (!cityWarehouse.withdraw(npc, ItemBase.GOLD_ITEM_BASE, overdraft, false, true)) {
                            Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                            return null;
                        }
                    }
                }
            }

            if (galvorAmount > 0 && useWarehouse) {
                //ChatManager.chatGuildInfo(pc, "Withdrawing " + galvorAmount + " galvor from warehouse");
                if (!cityWarehouse.withdraw(npc, galvor, galvorAmount, false, true)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Failed to withdraw Galvor from warehouse!" + ib.getName());
                    Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");
                    return null;
                }
            }

            if (wormwoodAmount > 0 && useWarehouse) {
                //ChatManager.chatGuildInfo(pc, "Withdrawing " + wormwoodAmount + " wormwood from warehouse");
                if (!cityWarehouse.withdraw(npc, wormwood, wormwoodAmount, false, true)) {
                    ErrorPopupMsg.sendErrorMsg(pc, "Failed to withdraw Wormwood from warehouse for " + ib.getName());
                    Logger.error("Warehouse with UID of" + cityWarehouse.getObjectUUID() + "Failed to Withdrawl ");

                    return null;
                }
            }

            ml = new MobLoot(npc, ib, false);

            ml.containerType = Enum.ItemContainerType.FORGE;

            if (prefix != null) {
                ml.addPermanentEnchantment(prefix.getIDString(), 0, 0, true);
                ml.setPrefix(prefix.getIDString());
            }

            if (suffix != null) {
                ml.addPermanentEnchantment(suffix.getIDString(), 0, 0, false);
                ml.setSuffix(suffix.getIDString());
            }

            ml.loadEnchantments();


            ml.setValue(0);
            ml.setRandom(true);
            npc.addItemToForge(ml);
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            city.transactionLock.writeLock().unlock();
        }
        return ml;

    }
}
