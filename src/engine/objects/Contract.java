// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import ch.claude_martin.enumbitset.EnumBitSet;
import engine.Enum;
import engine.gameManager.*;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.CityDataMsg;
import engine.net.client.msg.ErrorPopupMsg;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Contract extends AbstractGameObject {

    private final int contractID;
    private final String name;
    private final int mobbaseID;
    private final int classID;
    private final int extraRune;
    private final int iconID;
    public int equipmentSet = 0;
    public int inventorySet = 0;
    private int vendorID;
    private boolean isTrainer;
    private VendorDialog vendorDialog;
    private ArrayList<Integer> npcMenuOptions = new ArrayList<>();
    private ArrayList<Integer> npcModTypeTable = new ArrayList<>();
    private ArrayList<Integer> npcModSuffixTable = new ArrayList<>();
    private ArrayList<Byte> itemModTable = new ArrayList<>();
    private ArrayList<MobEquipment> sellInventory = new ArrayList<>();
    private EnumBitSet<Enum.BuildingGroup> allowedBuildings;
    private ArrayList<Integer> buyItemType = new ArrayList<>();
    private ArrayList<Integer> buySkillToken = new ArrayList<>();
    private ArrayList<Integer> buyUnknownToken = new ArrayList<>();

    /**
     * No Table ID Constructor
     */
    public Contract(int contractID, String name, int mobbaseID, int classID, int dialogID, int iconID, int extraRune) {
        super();
        this.contractID = contractID;
        this.name = name;
        this.mobbaseID = mobbaseID;
        this.classID = classID;
        this.iconID = iconID;
        this.extraRune = extraRune;
        this.vendorDialog = VendorDialog.getVendorDialog(dialogID);
        setBools();
    }

    /**
     * Normal Constructor
     */
    public Contract(int contractID, String name, int mobbaseID, int classID, int dialogID, int iconID, int extraRune, int newUUID) {
        super(newUUID);
        this.contractID = contractID;
        this.name = name;
        this.mobbaseID = mobbaseID;
        this.classID = classID;
        this.iconID = iconID;
        this.extraRune = extraRune;
        this.vendorDialog = VendorDialog.getVendorDialog(dialogID);
        setBools();
    }


    /**
     * ResultSet Constructor
     */
    public Contract(ResultSet rs) throws SQLException {
        super(rs);
        this.contractID = rs.getInt("contractID");
        this.name = rs.getString("name");
        this.mobbaseID = rs.getInt("mobbaseID");
        this.classID = rs.getInt("classID");
        this.extraRune = rs.getInt("extraRune");
        this.vendorDialog = VendorDialog.getVendorDialog(rs.getInt("dialogID"));
        this.iconID = rs.getInt("iconID");
        this.vendorID = rs.getInt("vendorID");
        this.allowedBuildings = EnumBitSet.asEnumBitSet(rs.getLong("allowedBuildingTypeID"), Enum.BuildingGroup.class);
        switch(this.contractID){
            case 866: //banker
            case 865: //siege engineer
            case 899: //alchemist
                this.allowedBuildings.add(Enum.BuildingGroup.TOL);
        }
        if(this.getName().toLowerCase().contains("sage")){
            this.allowedBuildings.add(Enum.BuildingGroup.TOL);
        }
        this.equipmentSet = rs.getInt("equipSetID");
        this.inventorySet = rs.getInt("inventorySet");

        try {
            String menuoptions = rs.getString("menuoptions");

            if (!menuoptions.isEmpty()) {
                String[] data = menuoptions.split(" ");
                for (String data1 : data) {
                    this.npcMenuOptions.add(Integer.parseInt(data1));
                }
            }

            String modtypetable = rs.getString("pTable");
            if (!modtypetable.isEmpty()) {
                String[] data = modtypetable.split(" ");
                for (String data1 : data) {
                    this.npcModTypeTable.add(Integer.parseInt(data1));
                }
            }

            String suffix = rs.getString("sTable");

            if (!suffix.isEmpty()) {
                String[] data1 = suffix.split(" ");

                for (String data11 : data1) {
                    this.npcModSuffixTable.add(Integer.parseInt(data11));
                }
            }

            String itemMod = rs.getString("itemModTable");

            if (!itemMod.isEmpty()) {
                String[] data2 = itemMod.split(" ");
                for (byte i = 0; i < data2.length; i++) {
                    this.itemModTable.add(Byte.parseByte(data2[i]));
                }

            }

        } catch (SQLException | NumberFormatException e) {
            Logger.error("Error when parsing mod tables");
        }
        setBools();
    }

    //Specify if trainer, merchant, banker, etc via classID
    private void setBools() {
        DbManager.ContractQueries.LOAD_CONTRACT_INVENTORY(this);
        DbManager.ContractQueries.LOAD_SELL_LIST_FOR_CONTRACT(this);

        this.isTrainer = this.classID > 2499 && this.classID < 3050 || this.classID == 2028;

    }

    /*
     * Getters
     */
    public int getContractID() {
        return this.contractID;
    }

    public String getName() {
        return this.name;
    }

    public int getMobbaseID() {
        return this.mobbaseID;
    }

    public int getClassID() {
        return this.classID;
    }

    public int getExtraRune() {
        return this.extraRune;
    }

    public boolean isTrainer() {
        return this.isTrainer;
    }

    public int getIconID() {
        return this.iconID;
    }

    public int getVendorID() {
        return this.vendorID;
    }

    public VendorDialog getVendorDialog() {
        return this.vendorDialog;
    }

    public static VendorDialog HandleArenaMaster(int optionId, NPC npc, PlayerCharacter pc){
        //1502043
        pc.setLastNPCDialog(npc);
        VendorDialog vd = new VendorDialog(VendorDialog.getHostileVendorDialog().getDialogType(),VendorDialog.getHostileVendorDialog().getIntro(),-1);//VendorDialog.getHostileVendorDialog();
        vd.getOptions().clear();

        switch(optionId){
            case 15020431:
                if(pc.isBoxed){
                    ChatManager.chatSystemInfo(pc, "You Cannot Join The Que, You Are Boxed");
                }else {
                    if (ArenaManager.playerQueue.contains(pc)) {
                        ChatManager.chatSystemInfo(pc, "You Are Already In The Arena Que");
                    } else {
                        ArenaManager.joinQueue(pc);
                        ChatManager.chatSystemInfo(pc, "You Have Joined The Arena Que");
                    }
                }
                break;
            case 15020432:
                if(ArenaManager.playerQueue.contains(pc)) {
                    ArenaManager.leaveQueue(pc);
                    ChatManager.chatSystemInfo(pc, "You Have Left The Arena Que");
                }else{
                    ChatManager.chatSystemInfo(pc, "You Are Not In The Arena Que");
                }
                break;
        }
        MenuOption option1 = new MenuOption(15020431, "Join Arena Que", 15020431);
        vd.getOptions().add(option1);
        MenuOption option2 = new MenuOption(15020432, "Leave Arena Que", 15020432);
        vd.getOptions().add(option2);
        return vd;
    }
    public static VendorDialog HandleEnrollmentOfficer(int optionId, NPC npc, PlayerCharacter pc){
        pc.setLastNPCDialog(npc);
        //VendorDialog vd = new VendorDialog(npc.contract.getVendorDialog().getDialogType(),npc.contract.getVendorDialog().getIntro(),-1);//VendorDialog.getHostileVendorDialog();
        VendorDialog vd = new VendorDialog(npc.contract.getVendorDialog().getDialogType(),npc.contract.getVendorDialog().getIntro(),-1);//VendorDialog.getHostileVendorDialog();
        vd.getOptions().clear();
        switch(optionId) {
            default:
            if (pc.isBoxed) {
                MenuOption option1 = new MenuOption(15020401, "Unbox Character", 15020401);
                vd.getOptions().add(option1);
            }
            break;
            case 15020401:
                PlayerCharacter.unboxPlayer(pc);
                vd.getOptions().clear();
                break;
        }
        return vd;
    }
    public static VendorDialog HandleBaneCommanderOptions(int optionId, NPC npc, PlayerCharacter pc){
        pc.setLastNPCDialog(npc);
        VendorDialog vd = new VendorDialog(VendorDialog.getHostileVendorDialog().getDialogType(),VendorDialog.getHostileVendorDialog().getIntro(),-1);//VendorDialog.getHostileVendorDialog();
        vd.getOptions().clear();
        Building building = npc.building;
        Bane bane = null;
        int updateBaneTime = 0;
        int updateBaneDay = 0;
        int updateBaneCap = 0;

        int treesInNation = 0;
        if(building != null)
        {
            City city = ZoneManager.getCityAtLocation(building.loc);
            if(city != null){
                bane = city.getBane();
                if(!city.getGuild().equals(pc.guild))
                    return vd;

                if(!GuildStatusController.isInnerCouncil(pc.getGuildStatus()) && !GuildStatusController.isGuildLeader(pc.getGuildStatus())){
                    return vd;
                }
                for(Guild sub : city.getGuild().getNation().getSubGuildList()){
                    if(sub.getOwnedCity() != null){
                        treesInNation += 1;
                    }
                }
            }
        }
        if(bane == null){
            return VendorDialog.getHostileVendorDialog();
        }
        if(bane.timeSet && bane.capSet && bane.daySet){
            vd.getOptions().clear();
            return vd;
        }

        DateTime placement = bane.getPlacementDate();
        vd.getOptions().clear();
        switch(optionId){
            default:
                if(!bane.daySet) {
                    MenuOption option1 = new MenuOption(796, "Set Bane Day", 796);
                    vd.getOptions().add(option1);
                }
                if(!bane.timeSet) {
                    MenuOption option2 = new MenuOption(797, "Set Bane Time", 797);
                    vd.getOptions().add(option2);
                }
                if(!bane.capSet) {
                    MenuOption option3 = new MenuOption(797, "Set Bane Cap", 798);
                    vd.getOptions().add(option3);
                }
                break;
            case 796: // set bane day
                DateTime dayOption1Date = placement.plusDays(3);
                MenuOption dayOption1 = new MenuOption(7961, dayOption1Date.toString("yyyy-MM-dd"), 7961);
                vd.getOptions().add(dayOption1);

                DateTime dayOption2Date = placement.plusDays(4);
                MenuOption dayOption2 = new MenuOption(7962, dayOption2Date.toString("yyyy-MM-dd"), 7962);
                vd.getOptions().add(dayOption2);

                DateTime dayOption3Date = placement.plusDays(5);
                MenuOption dayOption3 = new MenuOption(7963, dayOption3Date.toString("yyyy-MM-dd"), 7963);
                vd.getOptions().add(dayOption3);

                DateTime dayOption4Date = placement.plusDays(6);
                MenuOption dayOption4 = new MenuOption(7964, dayOption4Date.toString("yyyy-MM-dd"), 7964);
                vd.getOptions().add(dayOption4);

                DateTime dayOption5Date = placement.plusDays(7);
                MenuOption dayOption5 = new MenuOption(7965, dayOption5Date.toString("yyyy-MM-dd"), 7965);
                vd.getOptions().add(dayOption5);
                break;
            case 797: // set bane time
                MenuOption timeOption1 = new MenuOption(7971, "6:00 pm CST", 7971);
                vd.getOptions().add(timeOption1);

                MenuOption timeOption2 = new MenuOption(7972, "7:00 pm CST", 7972);
                vd.getOptions().add(timeOption2);

                MenuOption timeOption3 = new MenuOption(7973, "8:00 pm CST", 7973);
                vd.getOptions().add(timeOption3);

                MenuOption timeOption4 = new MenuOption(7974, "9:00 pm CST", 7974);
                vd.getOptions().add(timeOption4);

                MenuOption timeOption5 = new MenuOption(7975, "10:00 pm CST", 7975);
                vd.getOptions().add(timeOption5);
                break;
            case 798: // set bane cap
                if(treesInNation < 6) {
                    MenuOption capOption1 = new MenuOption(7981, "10 Maximum Players", 7981);
                    vd.getOptions().add(capOption1);
                }

                if(treesInNation < 11) {
                    MenuOption capOption2 = new MenuOption(7982, "20 Maximum Players", 7982);
                    vd.getOptions().add(capOption2);
                }

                MenuOption capOption3 = new MenuOption(7983, "30 Maximum Players", 7983);
                vd.getOptions().add(capOption3);

                MenuOption capOption4 = new MenuOption(7984, "40 Maximum Players", 7984);
                vd.getOptions().add(capOption4);

                MenuOption capOption5 = new MenuOption(7985, "Unlimited Players", 7985);
                vd.getOptions().add(capOption5);

                break;

            case 7961: //3 days after placement
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set 3 Days From Placement Date");
                updateBaneDay = 3;
                break;
            case 7962: //4 days after placement
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set 4 Days From Placement Date");
                updateBaneDay = 4;
                break;
            case 7963: //5 days after placement
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set 5 Days From Placement Date");
                updateBaneDay = 5;
                break;
            case 7964: //6 days after placement
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set 6 Days From Placement Date");
                updateBaneDay = 6;
                break;
            case 7965: //7 days after placement
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set 7 Days From Placement Date");
                updateBaneDay = 7;
                break;
            case 7971: //6:00pm CST
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set For 6:00 pm CST");
                updateBaneTime = 6;
                break;
            case 7972: //7:00pm CST
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set For 7:00 pm CST");
                updateBaneTime = 7;
                break;
            case 7973: //8:00pm CST
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set For 8:00 pm CST");
                updateBaneTime = 8;
                break;
            case 7974: //9:00pm CST
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set For 9:00 pm CST");
                updateBaneTime = 9;
                break;
            case 7975: //10:00pm CST
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Set For 10:00 pm CST");
                updateBaneTime = 10;
                break;
            case 7981: //cap = 10
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Cap Set To 10 Players On Each Side");
                updateBaneCap = 10;
                break;
            case 7982: //cap = 20
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Cap Set To 20 Players On Each Side");
                updateBaneCap = 20;
                break;
            case 7983: //cap = 30
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Cap Set To 30 Players On Each Side");
                updateBaneCap = 30;
                break;
            case 7984: //cap = 40
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Cap Set To 40 Players On Each Side");
                updateBaneCap = 40;
                break;
            case 7985: //cap = Unlimited
                ErrorPopupMsg.sendErrorMsg(pc, "Bane Cap Set To Unlimited Players On Each Side");
                updateBaneCap = 9999;
                break;
        }
        if (updateBaneDay > 0) {
            if(DbManager.BaneQueries.SET_BANE_DAY_NEW(updateBaneDay,bane.getCityUUID())){
                bane.daySet = true;
                if(bane.getLiveDate() == null) {
                    bane.setLiveDate_NEW(bane.getPlacementDate().plusDays(updateBaneDay));
                }else{
                    bane.setLiveDate_NEW(bane.getLiveDate().plusDays(updateBaneDay));
                }
            }
        }
        if (updateBaneTime > 0) {
            if(DbManager.BaneQueries.SET_BANE_TIME_NEW(updateBaneTime,bane.getCityUUID())){
                bane.timeSet = true;
                if(bane.getLiveDate() == null) {
                    bane.setLiveDate_NEW(bane.getPlacementDate().withHourOfDay(12 + updateBaneTime));
                }else{
                    bane.setLiveDate_NEW(bane.getLiveDate().withHourOfDay(12 + updateBaneTime));
                }
            }
            bane.setLiveDate(DbManager.BaneQueries.getLiveDate(bane.getCityUUID()));
        }
        if (updateBaneCap > 0) {
            if(DbManager.BaneQueries.SET_BANE_CAP_NEW(updateBaneCap,bane.getCityUUID())){
                bane.capSet = true;
                bane.capSize = updateBaneCap;
            }
        }

        if(updateBaneCap > 0 ||  updateBaneTime > 0 || updateBaneDay > 0) {
            bane.getSiegePhase();
            for (PlayerCharacter playerCharacter : SessionManager.getAllActivePlayerCharacters()) {
                CityDataMsg cityDataMsg = new CityDataMsg(SessionManager.getSession(playerCharacter), false);
                cityDataMsg.updateMines(true);
                cityDataMsg.updateCities(true);
                Dispatch dispatch = Dispatch.borrow(playerCharacter, cityDataMsg);
                DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
            }
            vd.getOptions().clear();
            if(!bane.daySet) {
                MenuOption option1 = new MenuOption(796, "Set Bane Day", 796);
                vd.getOptions().add(option1);
            }
            if(!bane.timeSet) {
                MenuOption option2 = new MenuOption(797, "Set Bane Time", 797);
                vd.getOptions().add(option2);
            }
            if(!bane.capSet) {
                MenuOption option3 = new MenuOption(797, "Set Bane Cap", 798);
                vd.getOptions().add(option3);
            }
        }

        return vd;
    }

    public ArrayList<Integer> getNPCMenuOptions() {
        return this.npcMenuOptions;
    }

    public ArrayList<Integer> getNPCModTypeTable() {
        return this.npcModTypeTable;
    }

    public ArrayList<Integer> getNpcModSuffixTable() {
        return npcModSuffixTable;
    }

    public ArrayList<Byte> getItemModTable() {
        return itemModTable;
    }

    public ArrayList<MobEquipment> getSellInventory() {
        if(this.getObjectUUID() == 900){ //resource merchant
            for(MobEquipment me : this.sellInventory){
                if(me.getItemBase().getType().equals(Enum.ItemType.RESOURCE)){
                    int amountResource = Warehouse.getSellStackSize(me.getItemBase().getUUID());
                    me.magicValue = amountResource * me.getItemBase().getBaseValue();
                } else{
                    me.magicValue = 1000000;
                }
            }
        }

        if(this.getObjectUUID() == 1202){ //rune merchant
            for(MobEquipment me : this.sellInventory){
                switch(me.getItemBase().getUUID()){
                    case 250001: //5 stats
                    case 250010:
                    case 250019:
                    case 250028:
                    case 250037:
                        me.magicValue = 3000000;
                        break;
                    case 250002: //10 stats
                    case 250011:
                    case 250020:
                    case 250029:
                    case 250038:
                        me.magicValue = 4000000;
                        break;
                    case 250003: //15 stats
                    case 250012:
                    case 250021:
                    case 250030:
                    case 250039:
                        me.magicValue = 5000000;
                        break;
                    case 250004: //20 stats
                    case 250013:
                    case 250022:
                    case 250031:
                    case 250040:
                        me.magicValue = 6000000;
                        break;
                    case 250005: //25 stats
                    case 250014:
                    case 250023:
                    case 250032:
                    case 250041:
                        me.magicValue = 7000000;
                        break;
                    case 250006: //30 stats
                    case 250015:
                    case 250024:
                    case 250033:
                    case 250042:
                        me.magicValue = 8000000;
                        break;
                    case 250007: //35 stats
                    case 250016:
                    case 250025:
                    case 250034:
                    case 250043:
                        me.magicValue = 9000000;
                        break;
                    default:
                        me.magicValue = 10000000;
                        break;
                }
            }
        }

        if(this.getObjectUUID() == 1201){ //disc merchant
            for(MobEquipment me : this.sellInventory){
                if(me.getItemBase().getName().equals("Prospector")){
                    me.magicValue = 500000;
                }else{
                    me.magicValue = 10000000;
                }
            }
        }

        if(this.getObjectUUID() == 1502041) {//noob helper{
            for(MobEquipment me : this.sellInventory){
                me.magicValue = 1;
            }
        }

        return this.sellInventory;
    }

    public int getPromotionClass() {
        if (this.classID < 2504 || this.classID > 2526)
            return 0;
        return this.classID;
    }

    public boolean isRuneMaster() {
        return (this.classID == 850);
    }

    public boolean isArtilleryCaptain() {
        return this.contractID == 839 || this.contractID == 842;
    }

    @Override
    public void updateDatabase() {
        DbManager.ContractQueries.updateDatabase(this);
    }

    public EnumBitSet<Enum.BuildingGroup> getAllowedBuildings() {
        return allowedBuildings;
    }

    public ArrayList<Integer> getBuyItemType() {
        return this.buyItemType;
    }

    public ArrayList<Integer> getBuySkillToken() {
        return this.buySkillToken;
    }

    public ArrayList<Integer> getBuyUnknownToken() {
        return this.buyUnknownToken;
    }

    public boolean canSlotinBuilding(Building building) {

        // Need a building to slot in a building!
        if (building == null)
            return false;

        // Can't slot in anything but a blueprinted building
        if (building.getBlueprintUUID() == 0)
            return false;

        // No buildings no slotting
        if (this.allowedBuildings.size() == 0)
            return false;

        // Binary match
        return (building.getBlueprint().getBuildingGroup().elementOf(this.allowedBuildings));
    }

    public int getEquipmentSet() {
        return equipmentSet;
    }
}
