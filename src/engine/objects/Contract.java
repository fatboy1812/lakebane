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
import engine.gameManager.DbManager;
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
                    me.magicValue = amountResource * me.getItemBase().getBaseValue() * 2;
                } else{
                    me.magicValue = 100000;
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
