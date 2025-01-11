// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.net.client.msg;

import engine.Enum;
import engine.gameManager.ChatManager;
import engine.gameManager.DbManager;
import engine.net.*;
import engine.net.client.ClientConnection;
import engine.net.client.Protocol;
import engine.objects.CharacterRune;
import engine.objects.PlayerCharacter;
import engine.objects.RuneBase;
import engine.objects.RuneBaseAttribute;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ApplyRuneMsg extends ClientNetMsg {

    private int targetType;
    private int targetID;
    private int removeRuneBase;
    private int runeBase;
    private int runeType;
    private int runeID;
    private Boolean isPromo;

    /**
     * This is the general purpose constructor.
     */
    public ApplyRuneMsg(int targetType, int targetID, int runeBase, int runeType, int runeID, Boolean isPromo) {
        super(Protocol.SETRUNE);
        this.targetType = targetType;
        this.targetID = targetID;
        this.runeBase = runeBase;
        this.runeType = runeType;
        this.runeID = runeID;
        this.isPromo = isPromo;
        this.removeRuneBase = 0;
    }

    public ApplyRuneMsg(int targetType, int targetID, int removeRuneBase) {
        super(Protocol.SETRUNE);
        this.targetType = targetType;
        this.targetID = targetID;
        this.runeBase = 0;
        this.runeType = 0;
        this.runeID = 0;
        this.isPromo = false;
        this.removeRuneBase = removeRuneBase;
    }

    /**
     * This constructor is used by NetMsgFactory. It attempts to deserialize the
     * ByteBuffer into a message. If a BufferUnderflow occurs (based on reading
     * past the limit) then this constructor Throws that Exception to the
     * caller.
     */
    public ApplyRuneMsg(AbstractConnection origin, ByteBufferReader reader) {
        super(Protocol.SETRUNE, origin, reader);
    }

    public static boolean applyRune(int runeID, ClientConnection origin, PlayerCharacter playerCharacter) {
        RuneBase rb = RuneBase.getRuneBase(runeID);
        Dispatch dispatch;
        if (playerCharacter == null || origin == null || rb == null) {
            return false;
        }
        int raceID = playerCharacter.getRaceID();
        //Check race is met
        ConcurrentHashMap<Integer, Boolean> races = rb.getRace();
        if(runeID != 3007 && runeID != 3014) {//bounty hunter and huntsman
            if (races.size() > 0) {
                boolean valid = false;
                for (int validID : races.keySet()) {
                    if (validID == raceID) {
                        valid = true;
                        break;
                    }
                }
                if(runeID == 3040)
                    valid = true;

                if(runeID == 3049)
                    valid = true;

                if(raceID == 1999){
                    switch(runeID){
                        case 2514:
                        case 3036:
                        case 3033:
                        case 3001:
                        case 3002:
                        case 3003:
                        case 3004:
                        case 3008:
                        case 3009:
                        case 3013:
                        case 3016:
                        case 3017:
                        case 3018:
                        case 3020:
                        case 3021:
                        case 3030:
                        case 3031:
                        case 3037:
                        case 3045:
                        case 3046:
                        case 3047:
                        case 3048:
                        case 3049:
                            valid = true;
                            break;
                    }
                }
                if (!valid) {
                    return false;
                }
            }
            //Check base class is met
            ConcurrentHashMap<Integer, Boolean> baseClasses = rb.getBaseClass();
            if (baseClasses.size() > 0) {
                int baseClassID = playerCharacter.getBaseClassID();
                boolean valid = false;
                for (int validID : baseClasses.keySet()) {
                    if (validID == baseClassID) {
                        valid = true;
                        break;
                    }
                }
                if(runeID == 3040)
                    valid = true;
                if(runeID == 3036 && raceID == 1999)
                    valid = true;
                if(runeID == 3033 && raceID == 1999)
                    valid = true;
                if(runeID == 3035 && baseClassID == 2501)
                    valid = true;
                if(runeID == 3028 && baseClassID == 2501 && playerCharacter.getRace().getName().contains("Irekei"))
                    valid = true;
                if (!valid) {
                    return false;
                }
            }
            //Check promotion class is met
            ConcurrentHashMap<Integer, Boolean> promotionClasses = rb.getPromotionClass();
            if (promotionClasses.size() > 0) {
                int promotionClassID = playerCharacter.getPromotionClassID();
                int baseClassID = playerCharacter.getBaseClassID();
                boolean valid = false;
                for (int validID : promotionClasses.keySet()) {
                    if (validID == promotionClassID) {
                        valid = true;
                        break;
                    }
                }
                if(runeID == 3040)
                    valid = true;
                if(runeID == 3004 && (playerCharacter.getPromotionClassID() == 2505 || playerCharacter.getPromotionClassID() == 2510))
                    valid = true;
                if(runeID == 3036 && raceID == 1999)
                    valid = true;
                if(runeID == 3033 && raceID == 1999)
                    valid = true;
                if(runeID == 3028 && (raceID == 2013 || raceID == 2014) && playerCharacter.getBaseClassID() == 2501)
                    valid = true;
                if(runeID == 3035 && baseClassID == 2501)
                    valid = true;
                if (!valid) {
                    return false;
                }
            }
        } else{
            if(playerCharacter.getPromotionClassID() == 2519){//priest
                return false;
            }
        }
        //Check disciplines are met
        ArrayList<CharacterRune> runes = playerCharacter.getRunes();
        ConcurrentHashMap<Integer, Boolean> disciplines = rb.getDiscipline();
        if (disciplines.size() > 0) {
            for (CharacterRune cr : runes) {
                int runeBaseID = cr.getRuneBaseID();
                for (Integer prohID : disciplines.keySet()) {
                    if (runeBaseID == prohID) {
                        return false; //Prohibited rune
                    }
                }
            }
        }
        int discCount = 0;
        for (CharacterRune cr : runes) {
            int runeBaseID = cr.getRuneBaseID();
            //count number of discipline runes
            if(isDiscipline(runeBaseID)){
                discCount++;
            }
            //see if rune is already applied
            if (runeBaseID == runeID) {
                return false;
            }
        }
        //Check level is met
        if (playerCharacter.getLevel() < rb.getLevelRequired()) {
            return false;
        }
        int strTotal = 0;
        int dexTotal = 0;
        int conTotal = 0;
        int intTotal = 0;
        int spiTotal = 0;
        int cost = 0;
        //Check any attributes are met
        ArrayList<RuneBaseAttribute> attrs = rb.getAttrs();
        if (rb.getAttrs() != null)
            for (RuneBaseAttribute rba : attrs) {
                int attrID = rba.getAttributeID();
                int mod = rba.getModValue();
                switch (attrID) {
                    case MBServerStatics.RUNE_COST_ATTRIBUTE_ID:
                        switch (rb.getName()) {
                            case "Born of the Ethyri":
                            case "Born of the Taripontor":
                            case "Born of the Gwendannen":
                            case "Born of the Invorri":
                            case "Born of the Irydnu":
                            case "Scion of the Dar Khelegeur":
                            case "Scion of the Gwaridorn":
                            case "Scion of the Twathedilion":
                                mod = 0;
                        }
                        if (mod > playerCharacter.getUnusedStatPoints()) {
                            return false;
                        }
                        cost = mod;
                        break;
                    case MBServerStatics.RUNE_STR_MIN_NEEDED_ATTRIBUTE_ID:
                        if ((int) playerCharacter.statStrBase < mod) {
                            return false;
                        }
                        strTotal = mod;
                        break;
                    case MBServerStatics.RUNE_DEX_MIN_NEEDED_ATTRIBUTE_ID:
                        if ((int) playerCharacter.statDexBase < mod) {
                            return false;
                        }
                        dexTotal = mod;
                        break;
                    case MBServerStatics.RUNE_CON_MIN_NEEDED_ATTRIBUTE_ID:
                        if ((int) playerCharacter.statConBase < mod) {
                            return false;
                        }
                        conTotal = mod;
                        break;
                    case MBServerStatics.RUNE_INT_MIN_NEEDED_ATTRIBUTE_ID:
                        if ((int) playerCharacter.statIntBase < mod) {
                            return false;
                        }
                        intTotal = mod;
                        break;
                    case MBServerStatics.RUNE_SPI_MIN_NEEDED_ATTRIBUTE_ID:
                        if ((int) playerCharacter.statSpiBase < mod) {
                            return false;
                        }
                        spiTotal = mod;
                        break;
                    case MBServerStatics.RUNE_STR_ATTRIBUTE_ID:
                        strTotal += mod;
                        break;
                    case MBServerStatics.RUNE_DEX_ATTRIBUTE_ID:
                        dexTotal += mod;
                        break;
                    case MBServerStatics.RUNE_CON_ATTRIBUTE_ID:
                        conTotal += mod;
                        break;
                    case MBServerStatics.RUNE_INT_ATTRIBUTE_ID:
                        intTotal += mod;
                        break;
                    case MBServerStatics.RUNE_SPI_ATTRIBUTE_ID:
                        spiTotal += mod;
                        break;
                }
            }
        //Check if max number runes already reached
        if (runes.size() > 12) {
            ChatManager.chatSystemInfo(playerCharacter,"You Have Too Many Runes Applied");
            return false;
        }
        switch (rb.getName()) {
            case "Born of the Ethyri":
            case "Born of the Taripontor":
            case "Born of the Gwendannen":
            case "Born of the Invorri":
            case "Born of the Irydnu":
                for (CharacterRune charRune : playerCharacter.getRunes()) {
                    RuneBase rb2 = charRune.getRuneBase();
                    switch (rb2.getName()) {
                        case "Born of the Ethyri":
                        case "Born of the Taripontor":
                        case "Born of the Gwendannen":
                        case "Born of the Invorri":
                        case "Born of the Irydnu":
                            ChatManager.chatSystemError(playerCharacter, "You Have Already Applied A Blood Rune");
                            return false;
                    }
                }
                break;
            case "Scion of the Dar Khelegeur":
            case "Scion of the Gwaridorn":
            case "Scion of the Twathedilion":
                for (CharacterRune charRune : playerCharacter.getRunes()) {
                    RuneBase rb2 = charRune.getRuneBase();
                    switch (rb2.getName()) {
                        case "Scion of the Dar Khelegeur":
                        case "Scion of the Gwaridorn":
                        case "Scion of the Twathedilion":
                            ChatManager.chatSystemError(playerCharacter, "You Have Already Applied A Blood Rune");
                            return false;
                    }
                }
                break;
        }
        //if discipline, check number applied
        int discAllowed = 0;
        if (isDiscipline(runeID)) {
            switch(playerCharacter.getRank()){
                case 1:
                    discAllowed = 0;
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    discAllowed = 3;
                    break;
                case 7:
                    discAllowed = 4;
                    break;
                case 8:
                    discAllowed = 5;
                    break;
            }

            if(discCount >= discAllowed)
                return false;
        }
        //Everything succeeded. Let's apply the rune
        //Attempt add rune to database
        CharacterRune runeWithoutID = new CharacterRune(rb, playerCharacter.getObjectUUID());
        CharacterRune cr;
        try {
            cr = DbManager.CharacterRuneQueries.ADD_CHARACTER_RUNE(runeWithoutID);
        } catch (Exception e) {
            cr = null;
            Logger.error(e);
        }
        if (cr == null) {
            return false;
        }
        //remove any overridden runes from player
        ArrayList<Integer> overwrite = rb.getOverwrite();
        CharacterRune toRemove = null;
        if (overwrite.size() > 0) {
            for (int overwriteID : overwrite) {
                toRemove = playerCharacter.removeRune(overwriteID);
            }
        }
        //add rune to player
        playerCharacter.addRune(cr);
        // recalculate all bonuses/formulas/skills/powers
        playerCharacter.recalculate();
        //if overwriting a stat rune, add any amount granted from previous rune.
        if (toRemove != null) {
            RuneBase rbs = toRemove.getRuneBase();
            if (rbs != null && rbs.getObjectUUID() > 249999 && rbs.getObjectUUID() < 250045) {
                //add any additional stats to match old amount
                int dif = strTotal - (int) playerCharacter.statStrBase;
                if (dif > 0 && strTotal < (int) playerCharacter.statStrMax) {
                    playerCharacter.addStr(dif);
                }
                dif = dexTotal - (int) playerCharacter.statDexBase;
                if (dif > 0 && dexTotal < (int) playerCharacter.statDexMax) {
                    playerCharacter.addDex(dif);
                }
                dif = conTotal - (int) playerCharacter.statConBase;
                if (dif > 0 && conTotal < (int) playerCharacter.statConMax) {
                    playerCharacter.addCon(dif);
                }
                dif = intTotal - (int) playerCharacter.statIntBase;
                if (dif > 0 && intTotal < (int) playerCharacter.statIntMax) {
                    playerCharacter.addInt(dif);
                }
                dif = spiTotal - (int) playerCharacter.statSpiBase;
                if (dif > 0 && spiTotal < (int) playerCharacter.statSpiMax) {
                    playerCharacter.addSpi(dif);
                }
                // recalculate all bonuses/formulas/skills/powers
                playerCharacter.recalculate();
            }
        }
        switch (rb.getName()) {
            case "Born of the Ethyri":
            case "Born of the Taripontor":
            case "Born of the Gwendannen":
            case "Born of the Invorri":
            case "Born of the Irydnu":
            case "Scion of the Dar Khelegeur":
            case "Scion of the Gwaridorn":
            case "Scion of the Twathedilion":
                cost = 0;
                break;
        }
        if (cost > 0) {
            ModifyStatMsg msm = new ModifyStatMsg((0 - cost), 0, 3);
            dispatch = Dispatch.borrow(playerCharacter, msm);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
        }
        //send apply rune message to client
        ApplyRuneMsg arm = new ApplyRuneMsg(playerCharacter.getObjectType().ordinal(), playerCharacter.getObjectUUID(), runeID, cr.getObjectType().ordinal(), cr.getObjectUUID(), false);
        dispatch = Dispatch.borrow(playerCharacter, arm);
        DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
        //alert them of success
        ErrorPopupMsg.sendErrorPopup(playerCharacter, 160);
        //reapply bonuses
        playerCharacter.applyBonuses();
        return true;
    }

    public static boolean isDiscipline(int runeID) {

        return runeID > 3000 && runeID < 3050;
    }

    /**
     * Serializes the subclass specific items to the supplied NetMsgWriter.
     */
    @Override
    protected void _serialize(ByteBufferWriter writer) {
        writer.putInt(this.targetType);
        writer.putInt(this.targetID);
        writer.putInt(0);
        writer.putInt(this.removeRuneBase);
        writer.putInt(0);
        writer.putInt(this.runeBase);
        writer.putInt(this.runeType);
        writer.putInt(this.runeID);
        if (this.isPromo) {
            writer.put((byte) 1);
        } else {
            writer.put((byte) 0);
        }
    }

    /**
     * Deserializes the subclass specific items from the supplied NetMsgReader.
     */
    @Override
    protected void _deserialize(ByteBufferReader reader) {
        this.targetType = reader.getInt();
        this.targetID = reader.getInt();
        reader.getInt();
        this.removeRuneBase = reader.getInt();
        reader.getInt();
        this.runeBase = reader.getInt();
        this.runeType = reader.getInt();
        this.runeID = reader.getInt();
        this.isPromo = (reader.get() == 1) ? true : false;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setTargetType(int value) {
        this.targetType = value;
    }

    public int getTargetID() {
        return targetID;
    }

    public void setTargetID(int value) {
        this.targetID = value;
    }

    public int getRuneID() {
        return runeID;
    }

    public void setRuneID(int value) {
        this.runeID = value;
    }
}
