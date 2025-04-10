package engine.gameManager;

import engine.objects.*;

import java.util.ArrayList;

public class TransmutationHandler {

    public static int tokenId = 680162;

    public static void ExchangeRuneForToken(PlayerCharacter pc, Item item){
        pc.getCharItemManager().delete(item);
        AddTokensToInventory(pc,1);
    }

    public static void ExchangeTokensForRune(PlayerCharacter pc, ItemBase runeBase){

        int tokenCount = 0;
        for(Item i : pc.getCharItemManager().getInventory()){
            if(i.getItemBaseID() == tokenId){
                tokenCount += i.getNumOfItems();
            }
        }

        //TODO system to get rune token cost
        int cost = getCost(runeBase.getUUID());


        if (tokenCount + cost < 0) {
            ChatManager.chatSystemInfo(pc, "You Do Not Have Enough Tokens For This Rune.");
            return;
        }

        AddTokensToInventory(pc,cost);
        MobLoot rune = new MobLoot(pc,runeBase,1, false);
        Item promoted = rune.promoteToItem(pc);
        pc.getCharItemManager().addItemToInventory(promoted);
        pc.getCharItemManager().updateInventory();
    }

    public static void AddTokensToInventory(PlayerCharacter pc, int amount){
        int tokenCount = 0;
        for(Item i : pc.getCharItemManager().getInventory()){
            if(i.getItemBaseID() == tokenId){
                tokenCount += i.getNumOfItems();
                pc.getCharItemManager().delete(i);
                pc.getCharItemManager().updateInventory();
            }
        }
        tokenCount += amount;
        MobLoot tokens = new MobLoot(pc,ItemBase.getItemBase(tokenId),1, false);
        Item promoted = tokens.promoteToItem(pc);
        promoted.setNumOfItems(tokenCount);
        pc.getCharItemManager().addItemToInventory(promoted);
        pc.getCharItemManager().updateInventory();
        DbManager.ItemQueries.UPDATE_NUM_ITEMS(promoted,tokenCount);
        for(Item i : pc.getCharItemManager().getInventory()){
            if(i.getItemBaseID() == tokenId){
                if(i.getNumOfItems() <= 0){
                    pc.getCharItemManager().delete(i);
                    pc.getCharItemManager().updateInventory();
                }
            }
        }
    }

    public static int getCost(int i) {
        switch (i) {
            case 971070:
                return -3;
            case 971012:
                return -1;
            case 250001: //5 stats
            case 250010:
            case 250019:
            case 250028:
            case 250037:
            case 250002: //10 stats
            case 250011:
            case 250020:
            case 250029:
            case 250038:
            case 250003: //15 stats
            case 250012:
            case 250021:
            case 250030:
            case 250039:
            case 250004: //20 stats
            case 250013:
            case 250022:
            case 250031:
            case 250040:
            case 250005: //25 stats
            case 250014:
            case 250023:
            case 250032:
            case 250041:
            case 250006: //30 stats
            case 250015:
            case 250024:
            case 250033:
            case 250042:
            case 250007: //35 stats
            case 250016:
            case 250025:
            case 250034:
            case 250043:
                return -5;//000;
            case 250008: //40 stats
            case 250017:
            case 250026:
            case 250035:
            case 250044:
                return -10;//000;
            case 252127:
                return -5;//000;
            case 3040: //prospector
                return -1;
            default:
                return -5;//000;
        }
    }
}
