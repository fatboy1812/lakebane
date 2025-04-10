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
        int cost = -5;

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
}
