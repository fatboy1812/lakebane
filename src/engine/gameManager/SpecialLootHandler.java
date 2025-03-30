package engine.gameManager;
import engine.InterestManagement.WorldGrid;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.server.MBServerStatics;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
public class SpecialLootHandler {

    public static final ArrayList<Integer> static_rune_ids_low = new ArrayList<>(Arrays.asList(
            250001, 250002, 250003, 250004, 250005, 250006, 250010, 250011,
            250012, 250013, 250014, 250015, 250019, 250020, 250021, 250022,
            250023, 250024, 250028, 250029, 250030, 250031, 250032, 250033,
            250037, 250038, 250039, 250040, 250041, 250042
    ));
    public static final ArrayList<Integer> static_rune_ids_mid = new ArrayList<>(Arrays.asList(
            250006, 250007, 250008,
            250015, 250016, 250017,
            250024, 250025, 250026,
            250033, 250034, 250035,
            250042, 250043, 250044
    ));
    public static final ArrayList<Integer> static_rune_ids_high = new ArrayList<>(Arrays.asList(
            250007, 250008,
            250016, 250017,
            250025, 250026,
            250034, 250035,
            250043, 250044
    ));
    public static final List<Integer> DWARVEN_contracts = Arrays.asList(
            35250, 35251, 35252, 35253, 35254, 35255, 35256, 35257, 35258, 35259, 35260, 35261, 35262, 35263
    );
    public static final List<Integer> INVORRI_contracts = Arrays.asList(
            35050, 35051, 35052, 35053, 35054, 35055, 35056, 35057, 35058, 35059, 35060, 35061, 35062, 35063
    );
    public static final List<Integer> SHADE_contracts = Arrays.asList(
            35400, 35401, 35402, 35403, 35404, 35405, 35406, 35407, 35408, 35409, 35410, 35411, 35412, 35413,
            35414, 35415, 35416, 35417, 35418, 35419, 35420, 35421, 35422, 35423, 35424, 35425, 35426, 35427
    );
    public static final List<Integer> VAMPIRE_contracts = Arrays.asList(
            35545, 35546, 35547, 35548, 35549, 35550, 35551, 35552, 35553, 35554, 35555, 35556, 35557, 35558,
            35559, 35560, 35561, 35562, 35563, 35564, 35565, 35566, 35567, 35568, 35569, 35570
    );
    public static final List<Integer> IREKEI_contracts = Arrays.asList(
            35100, 35101, 35102, 35103, 35104, 35105, 35106, 35107, 35108, 35109, 35110, 35111, 35112, 35113,
            35114, 35115, 35116, 35117, 35118, 35119, 35120, 35121, 35122, 35123, 35124, 35125, 35126, 35127
    );
    public static final List<Integer> AMAZON_contracts = Arrays.asList(
            35200, 35201, 35202, 35203, 35204, 35205, 35206, 35207, 35208, 35209, 35210, 35211, 35212, 35213
    );
    public static final List<Integer> GWENDANNEN_contracts = Arrays.asList(
            35350, 35351, 35352, 35353, 35354, 35355, 35356, 35357, 35358, 35359, 35360, 35361, 35362, 35363,
            35364, 35365, 35366, 35367, 35368, 35369, 35370, 35371, 35372, 35373, 35374, 35375, 35376, 35377
    );
    public static final List<Integer> NEPHILIM_contracts = Arrays.asList(
            35500, 35501, 35502, 35503, 35504, 35505, 35506, 35507, 35508, 35509, 35510, 35511, 35512, 35513,
            35514, 35515, 35516, 35517, 35518, 35519, 35520, 35521, 35522, 35523, 35524, 35525
    );
    public static final List<Integer> ELVEN_contracts = Arrays.asList(
            35000, 35001, 35002, 35003, 35004, 35005, 35006, 35007, 35008, 35009, 35010, 35011, 35012, 35013,
            35014, 35015, 35016, 35017, 35018, 35019, 35020, 35021, 35022, 35023, 35024, 35025, 35026, 35027
    );
    public static final List<Integer> CENTAUR_contracts = Arrays.asList(
            35150, 35151, 35152, 35153, 35154, 35155, 35156, 35157, 35158, 35159, 35160, 35161, 35162, 35163,
            35164, 35165, 35166, 35167, 35168, 35169, 35170, 35171, 35172, 35173, 35174, 35175, 35176, 35177
    );
    public static final List<Integer> LIZARDMAN_contracts = Arrays.asList(
            35300, 35301, 35302, 35303, 35304, 35305, 35306, 35307, 35308, 35309, 35310, 35311, 35312, 35313
    );
    public static final List<Integer> GLASS_ITEMS = Arrays.asList(
            7000100, 7000110, 7000120, 7000130, 7000140, 7000150, 7000160, 7000170, 7000180, 7000190,
            7000200, 7000210, 7000220, 7000230, 7000240, 7000250, 7000270, 7000280
    );
    public static final List<Integer> STAT_RUNES = Arrays.asList(
            250001, 250002, 250003, 250004, 250005, 250006, 250007, 250008, 250010, 250011,
            250012, 250013, 250014, 250015, 250016, 250017, 250019, 250020, 250021, 250022,
            250023, 250024, 250025, 250026, 250028, 250029, 250030, 250031, 250032, 250033,
            250034, 250035, 250037, 250038, 250039, 250040, 250041, 250042, 250043, 250044
    );
    public static final List<Integer> racial_guard = Arrays.asList(
            841,951,952,1050,1052,1180,1182,1250,1252,1350,1352,1450,
            1452,1500,1502,1525,1527,1550,1552,1575,1577,1600,1602,1650,1652,1700,980100,
            980102
    );

    public static final List<Integer> gold_resources = Arrays.asList(
            1580000, 1580008, 1580009, 1580010, 1580011, 1580017
    );
    public static final List<Integer> lumber_resources = Arrays.asList(
            1580004, 1580005, 1580006, 1580007, 1580018
    );
    public static final List<Integer> ore_resources = Arrays.asList(
            1580000, 1580001, 1580002, 1580003, 1580019
    );
    public static final List<Integer> magic_resources = Arrays.asList(
            1580012, 1580013, 1580014, 1580015, 1580016, 1580020
    );

    public static void RollContract(Mob mob){
        Zone zone = getMacroZone(mob);
        if(zone == null)
            return;
        int contactId = 0;
        int roll = ThreadLocalRandom.current().nextInt(250);
        if(roll == 125){
            contactId = getContractForZone(zone);
        }
        if(contactId == 0)
            return;
        ItemBase contractBase = ItemBase.getItemBase(contactId);
        if(contractBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot contract = new MobLoot(mob,contractBase,false);
        mob.getCharItemManager().addItemToInventory(contract);
    }
    public static Zone getMacroZone(Mob mob){
        Zone parentZone = mob.parentZone;
        if(parentZone == null)
            return null;
        while(!parentZone.isMacroZone() && !parentZone.equals(ZoneManager.getSeaFloor())){
            parentZone = parentZone.getParent();
        }
        return parentZone;
    }
    public static int getContractForZone(Zone zone){
        Random random = new Random();
        switch (zone.getObjectUUID())
        {
            case 178:
                // Kralgaar Holm
                return DWARVEN_contracts.get(random.nextInt(DWARVEN_contracts.size()));
            case 122:
                // Aurrochs Skrae
                return INVORRI_contracts.get(random.nextInt(INVORRI_contracts.size()));
            case 197:
                // Ymur's Crown
                return INVORRI_contracts.get(random.nextInt(INVORRI_contracts.size()));
            case 234:
                // Ecklund Wilds
                return INVORRI_contracts.get(random.nextInt(INVORRI_contracts.size()));
            case 313:
                // Greensward Pyre
                return SHADE_contracts.get(random.nextInt(SHADE_contracts.size()));
            case 331:
                // The Doomplain
                return VAMPIRE_contracts.get(random.nextInt(VAMPIRE_contracts.size()));
            case 353:
                // Thollok Marsh
                return LIZARDMAN_contracts.get(random.nextInt(LIZARDMAN_contracts.size()));
            case 371:
                // The Black Bog
                return LIZARDMAN_contracts.get(random.nextInt(LIZARDMAN_contracts.size()));
            case 388:
                // Sevaath Mere
                return LIZARDMAN_contracts.get(random.nextInt(LIZARDMAN_contracts.size()));
            case 418:
                // Valkos Wilds
                return GWENDANNEN_contracts.get(random.nextInt(GWENDANNEN_contracts.size()));
            case 437:
                // Grimscairne
                return SHADE_contracts.get(random.nextInt(SHADE_contracts.size()));
            case 475:
                // Phaedra's Prize
                return AMAZON_contracts.get(random.nextInt(AMAZON_contracts.size()));
            case 491:
                // Holloch Forest
                return GWENDANNEN_contracts.get(random.nextInt(GWENDANNEN_contracts.size()));
            case 508:
                // Aeran Belendor
                return ELVEN_contracts.get(random.nextInt(ELVEN_contracts.size()));
            case 532:
                // Tainted Swamp
                return NEPHILIM_contracts.get(random.nextInt(NEPHILIM_contracts.size()));
            case 550:
                // Aerath Hellendroth
                return ELVEN_contracts.get(random.nextInt(ELVEN_contracts.size()));
            case 569:
                // Aedroch Highlands
                return GWENDANNEN_contracts.get(random.nextInt(GWENDANNEN_contracts.size()));
            case 590:
                // Fellgrim Forest
                return GWENDANNEN_contracts.get(random.nextInt(GWENDANNEN_contracts.size()));
            case 616:
                // Derros Plains
                return CENTAUR_contracts.get(random.nextInt(CENTAUR_contracts.size()));
            case 632:
                // Ashfell Plain
                return SHADE_contracts.get(random.nextInt(SHADE_contracts.size()));
            case 717:
                // Kharsoom
                return IREKEI_contracts.get(random.nextInt(IREKEI_contracts.size()));
            case 737:
                // Leth'khalivar Desert
                return IREKEI_contracts.get(random.nextInt(IREKEI_contracts.size()));
            case 761:
                // The Blood Sands
                return IREKEI_contracts.get(random.nextInt(IREKEI_contracts.size()));
            case 785:
                // Vale of Nar Addad
                return IREKEI_contracts.get(random.nextInt(IREKEI_contracts.size()));
            case 824:
                // Western Battleground
                return NEPHILIM_contracts.get(random.nextInt(NEPHILIM_contracts.size()));
            case 842:
                // Pandemonium
                return NEPHILIM_contracts.get(random.nextInt(NEPHILIM_contracts.size()));
            case 951:
                //Bone Marches
                return VAMPIRE_contracts.get(random.nextInt(VAMPIRE_contracts.size()));
            case 952:
                //plain of ashes
                return VAMPIRE_contracts.get(random.nextInt(VAMPIRE_contracts.size()));
        }
        return 0;
    }
    public static void RollGlass(Mob mob){
        int roll = ThreadLocalRandom.current().nextInt(10000);
        if(roll != 5000)
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
    public static void RollRune(Mob mob){
        int runeID = 0;
        int roll = ThreadLocalRandom.current().nextInt(250);
        Random random = new Random();
        if(roll == 125){
            runeID = STAT_RUNES.get(random.nextInt(STAT_RUNES.size()));
        }
        if(runeID == 0)
            return;
        ItemBase runeBase = ItemBase.getItemBase(runeID);
        if(runeBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot rune = new MobLoot(mob,runeBase,false);
        mob.getCharItemManager().addItemToInventory(rune);
    }
    public static void RollRacialGuard(Mob mob){
        int roll = ThreadLocalRandom.current().nextInt(5000);
        if(roll != 2500)
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

    public static void ResourceDrop(Mob mob){
        Zone zone = getMacroZone(mob);
        if(zone == null)
            return;
        int resourceId = 0;
        int roll = ThreadLocalRandom.current().nextInt(125);
        if(roll == 75){
            resourceId = getResourceForZone(zone);
        }
        if(resourceId == 0)
            return;
        ItemBase resourceBase = ItemBase.getItemBase(resourceId);
        if(resourceBase == null)
            return;
        if(mob.getCharItemManager() == null)
            return;
        MobLoot resource = new MobLoot(mob,resourceBase,false);

        int stackMax = (int)(Warehouse.maxResources.get(resourceId) * 0.02f);
        if(stackMax > 100)
            stackMax = 100;

        resource.setNumOfItems(ThreadLocalRandom.current().nextInt(stackMax));

        mob.getCharItemManager().addItemToInventory(resource);
    }

    public static int getResourceForZone(Zone zone){
        Random random = new Random();
        switch (zone.getObjectUUID())
        {
            case 178:
            case 717:
            case 632:
            case 952:
            case 475:
            case 371:
            case 313:
            case 234:
                // Ecklund Wilds // ORE MINE
                // Greensward Pyre // ORE MINE
                // The Black Bog // ORE MINE
                // Phaedra's Prize // ORE MINE
                //plain of ashes ORE MINE
                // Ashfell Plain // ORE MINE
                // Kharsoom // ORE MINE
                // Kralgaar Holm // ORE MINE
                return ore_resources.get(random.nextInt(ore_resources.size()));
            case 122:
            case 824:
            case 737:
            case 569:
            case 590:
            case 437:
            case 388:
                // Sevaath Mere // LUMBER MINE
                // Grimscairne // LUMBER MINE
                // Fellgrim Forest // LUMBER MINE
                // Aedroch Highlands // LUMBER MINE
                // Leth'khalivar Desert // LUMBER MINE
                // Western Battleground // LUMBER MINE
                // Aurrochs Skrae // LUMBER MINE
                return lumber_resources.get(random.nextInt(lumber_resources.size()));
            case 197:
            case 761:
            case 616:
            case 951:
            case 532:
            case 491:
            case 353:
            case 331:
                // The Doomplain // GOLD MINE
                // Thollok Marsh // GOLD MINE
                // Holloch Forest // GOLD MINE
                // Tainted Swamp // GOLD MINE
                //Bone Marches // GOLD MINE
                // Derros Plains // GOLD MINE
                // The Blood Sands // GOLD MINE
                // Ymur's Crown // GOLD MINE
                return gold_resources.get(random.nextInt(gold_resources.size()));
            case 418:
            case 842:
            case 785:
            case 550:
            case 508:
                // Aeran Belendor // MAGIC MINE
                // Aerath Hellendroth // MAGIC MINE
                // Vale of Nar Addad // MAGIC MINE
                // Pandemonium // MAGIC MINE
                // Valkos Wilds // MAGIC MINE
                return magic_resources.get(random.nextInt(magic_resources.size()));
        }
        return 0;
    }
}
