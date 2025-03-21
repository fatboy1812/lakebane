// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.server;

import engine.Enum;
import engine.math.Vector3fImmutable;

public class MBServerStatics {

    public static final int revisionNumber = 1;
    public static final String CMDLINE_ARGS_EXE_NAME_DELIMITER = "-name";
    public static final String CMDLINE_ARGS_CONFIG_FILE_PATH_DELIMITER = "-config";
    public static final String CMDLINE_ARGS_CALLER_DELIMITER = "-caller";
    public static final String CMDLINE_ARGS_REASON_DELIMITER = "-reason";
    public static final String EXISTING_CONNECTION_CLOSED = "An existing connection was forcibly closed by the remote host";
    public static final String RESET_BY_PEER = "Connection reset by peer";
    /*
     * ####Debugging Flags####
     */
    public static final boolean POWERS_DEBUG = false;
    public static final boolean MOVEMENT_SYNC_DEBUG = false;
    public static final boolean BONUS_TRAINS_ENABLED = false;
    public static final boolean REGENS_DEBUG = false;
    public static final boolean SHOW_SAFE_MODE_CHANGE = false;
    public static final boolean COMBAT_TARGET_HITBOX_DEBUG = false; // output
    // hit box
    // calcs
    public static final boolean PRINT_INCOMING_OPCODES = false; // print
    public static final int BANK_GOLD_LIMIT = 100000000;
    // incoming
    // opcodes to
    // console
    public static final int PLAYER_GOLD_LIMIT = 50000000;
    // buildings, npcs
    /*
     * Login cache flags
     */
    public static final boolean SKIP_CACHE_LOGIN = false; // skip caching														// login server
    public static final boolean SKIP_CACHE_LOGIN_PLAYER = false; // skip caching															// on login
    public static final boolean SKIP_CACHE_LOGIN_ITEM = false; // skip caching
    /*
     * ChatManager related
     */
    public static final int SHOUT_PERCEPTION_RADIUS_MOD = 2;
    /*
     * DevCmd related
     */
    public static final String DEV_CMD_PREFIX = "./";
    // The number of elements in INITIAL_WORKERS defines the initial number of
    // job pools
    public static final int[] INITIAL_JOBPOOL_WORKERS = {4, 2, 1};

    /*
     * JobManager related
     */
    public static final int JOBMANAGER_INTERNAL_MONITORING_INTERVAL_MS = 1000;
    public static final int JOB_STALL_THRESHOLD_MS = 120 * 1000;
    public static final int SCHEDULER_INITIAL_CAPACITY = 1000;
    public static final int SCHEDULER_EXECUTION_TIME_COMPENSATION = 16;
    public static final int CHM_INIT_CAP = 10;

    /*
     * Concurrent Hash Map - Defaults
     */
    public static final float CHM_LOAD = 0.75f;
    public static final int CHM_THREAD_HIGH = 4;
    public static final int CHM_THREAD_MED = 2;
    public static final int CHM_THREAD_LOW = 1;
    /*
     * LoginErrorMsg related
     */
    public static final int LOGINERROR_INVALID_USERNAME_PASSWORD = 1;
    /*
     * Message is Version:
     */
    public static final int LOGINERROR_INCORRECT_CLIENT_VERSION = 3;
    public static final int LOGINERROR_NOT_ALLOWED_TO_LOGIN_YET = 4;
    /*
     * Message is 'Error ='
     */
    public static final int LOGINERROR_LOGINSERVER_IS_UNAVAILABLE = 5;
    public static final int LOGINERROR_INVALID_ADMIN_USERNAME_PASSWORD = 6;
    public static final int LOGINERROR_NO_MORE_PLAYTIME_ON_ACCOUNT = 7;
    public static final int LOGINERROR_ACCOUNT_DOESNT_HAVE_SUBSCRIPTION = 8;
    public static final int LOGINERROR_ACCOUNT_INSECURE_CHANGE_PASSWORD = 9;
    public static final int LOGINERROR_TOO_MANY_LOGIN_TRIES = 10;
    /*
     * Message is 'Error ='
     */
    public static final int LOGINERROR_NOMOREPLAYTIME = 7;
    public static final int LOGINERROR_INACTIVE = 8;
    public static final int LOGINERROR_UNABLE_TO_LOGIN = 11;
    public static final int LOGINERROR_LOGINSERVER_BUSY = 12;
    public static final int LOGINERROR_BLANK = 13;
    /*
     * Name Validation Related
     */
    public static final int INVALIDNAME_FIRSTNAME_MUST_BE_LONGER = 1;
    public static final int INVALIDNAME_FIRSTANDLAST_MUST_BE_SHORTER = 2;
    public static final int INVALIDNAME_FIRSTNAME_MUST_NOT_HAVE_SPACES = 3;
    public static final int INVALIDNAME_FIRSTNAME_INVALID_CHARACTERS = 4;
    public static final int INVALIDNAME_PLEASE_CHOOSE_ANOTHER_FIRSTNAME = 5;
    public static final int INVALIDNAME_PLEASE_CHOOSE_ANOTHER_LASTNAME = 7;
    public static final int INVALIDNAME_LASTNAME_UNAVAILABLE = 8;
    public static final int INVALIDNAME_FIRSTNAME_UNAVAILABLE = 9;
    public static final int INVALIDNAME_WRONG_WORLD_ID = 10;
    public static final int INVALIDNAME_GENERIC = 11;
    /*
     * 1: A first name of at least 3 character(s) must be entered 2: Your first
     * and last name cannot be more than 15 characters each. 3: Your first name
     * may not contain spaces 4: There are invalid characters in the first name.
     * 5: Please choose another first name 7: Please choose another last name 8:
     * That last name is unavailable 9: That first name is unavailable 10: Your
     * client sent an invalid world id 11: Invalid name. Choose another
     */
    public static final int MIN_NAME_LENGTH = 3;
    public static final int MAX_NAME_LENGTH = 15;
    /*
     * ClientConnection related
     */
    public static final boolean TCP_NO_DELAY_DEFAULT = true;
    public static final byte MAX_CRYPTO_INIT_TRIES = 10;
    public static final byte MAX_LOGIN_ATTEMPTS = 5;
    public static final int RESET_LOGIN_ATTEMPTS_AFTER = (15 * 60 * 1000); // in
    /*
     * Account Related
     */
    public static final int MAX_ACTIVE_GAME_ACCOUNTS_PER_DISCORD_ACCOUNT = 4; // 0
    public static final byte MAX_NUM_OF_CHARACTERS = 7;
    public static final int STAT_STR_ID = 0x8AC3C0E6;
    public static final int STAT_SPI_ID = 0xACB82E33;
    public static final int STAT_CON_ID = 0xB15DC77E;
    public static final int STAT_DEX_ID = 0xE07B3336;
    public static final int STAT_INT_ID = 0xFF665EC3;
    public static final int SLOT_UNEQUIPPED = 0;
    public static final int SLOT_MAINHAND = 1;
    public static final int SLOT_OFFHAND = 2;
    public static final int SLOT_HELMET = 3;
    public static final int SLOT_CHEST = 4;
    public static final int SLOT_ARMS = 5;
    public static final int SLOT_GLOVES = 6;
    public static final int SLOT_RING1 = 7;
    public static final int SLOT_RING2 = 8;
    public static final int SLOT_NECKLACE = 9;
    public static final int SLOT_LEGGINGS = 10;
    public static final int SLOT_FEET = 11;
    public static final int SLOT_HAIRSTYLE = 18; // 17 & 18? Weird.
    public static final int SLOT_BEARDSTYLE = 17; // 17 & 18? Weird.
    /*
     * Group Formation Names
     */
    public static final String[] FORMATION_NAMES = {"Column", "Line", "Box",
            "Triangle", "Circle", "Ranks", "Wedge", "Inverse Wedge", "T"};
    public static final int RUNE_COST_ATTRIBUTE_ID = 0;

    // Equip[0] = Slot1 = Weapon MainHand
    // Equip[1] = Slot2 = OffHand
    // Equip[2] = Slot3 = Helmet
    // Equip[3] = Slot4 = Chest
    // Equip[4] = Slot5 = Arms
    // Equip[5] = Slot6 = Gloves
    // Equip[6] = Slot7 = Ring1
    // Equip[7] = Slot8 = Ring2
    // Equip[8] = Slot9 = Necklace
    // Equip[9] = Slot10 = Leggings
    // Equip[10] = Slot11 = Feet
    // Equip[11] = Slot17 = HairStyle
    // Equip[12] = Slot18 = BeardStyle
    public static final int RUNE_STR_ATTRIBUTE_ID = 1;
    public static final int RUNE_DEX_ATTRIBUTE_ID = 2;
    public static final int RUNE_CON_ATTRIBUTE_ID = 3;
    public static final int RUNE_INT_ATTRIBUTE_ID = 4;
    public static final int RUNE_SPI_ATTRIBUTE_ID = 5;
    public static final int RUNE_STR_MAX_ATTRIBUTE_ID = 6;
    public static final int RUNE_DEX_MAX_ATTRIBUTE_ID = 7;
    public static final int RUNE_CON_MAX_ATTRIBUTE_ID = 8;
    public static final int RUNE_INT_MAX_ATTRIBUTE_ID = 9;
    public static final int RUNE_SPI_MAX_ATTRIBUTE_ID = 10;
    public static final int RUNE_STR_MIN_NEEDED_ATTRIBUTE_ID = 11;
    public static final int RUNE_DEX_MIN_NEEDED_ATTRIBUTE_ID = 12;
    public static final int RUNE_CON_MIN_NEEDED_ATTRIBUTE_ID = 13;
    public static final int RUNE_INT_MIN_NEEDED_ATTRIBUTE_ID = 14;
    public static final int RUNE_SPI_MIN_NEEDED_ATTRIBUTE_ID = 15;
    /*
     * DBMan
     */
    public static final int NO_DB_ROW_ASSIGNED_YET = Integer.MAX_VALUE;
    public static final boolean DB_DEBUGGING_ON_BY_DEFAULT = false; // warning:
    public static final boolean ENABLE_QUERY_TIME_WARNING = true;
    public static final boolean ENABLE_UPDATE_TIME_WARNING = true;
    public static final boolean ENABLE_EXECUTION_TIME_WARNING = true;
    public static final int AFK_TIMEOUT_MS = (30 * 60 * 1000) * 100; // Added
    public static final int TIMEOUT_CHECKS_TIMER_MS = (60 * 1000);
    public static final int MASK_PLAYER = 1;
    public static final int MASK_MOB = 2;

    /*
     * Masks for Quad Tree. Masks should be multiple of 2.
     */
    public static final int MASK_PET = 4;
    public static final int MASK_CORPSE = 8;
    public static final int MASK_BUILDING = 16;
    public static final int MASK_UNDEAD = 64;
    public static final int MASK_BEAST = 128;
    public static final int MASK_HUMANOID = 256;
    public static final int MASK_NPC = 512;
    public static final int MASK_IAGENT = 2048;
    public static final int MASK_DRAGON = 4096;
    public static final int MASK_RAT = 8192;
    public static final int MASK_SIEGE = 16384;
    public static final int MASK_CITY = 32768;
    public static final int MASK_ZONE = 65536;
    public static final int MASK_AGGRO = 5; // Player, Pet
    public static final int MASK_MOBILE = 7; // Player, Mob, Pet

    /*
     * Combined QT Masks. For convenience
     */
    public static final int MASK_STATIC = 568; // Corpse, Building, Trigger, NPC
    /*
     * World Coordinate Data
     */
    public static final double MAX_WORLD_HEIGHT = -98304.0;
    public static final double MAX_WORLD_WIDTH = 131072.0;
    public static final float SEA_FLOOR_ALTITUDE = -1000f;
    /*
     * Ranges
     */
    public static final int CHARACTER_LOAD_RANGE = 400; // load range of mobile objects
    // (default: 300)
    public static final int STRUCTURE_LOAD_RANGE = 700; // load range of
    public static final int EXP_RANGE = 400;
    public static final int GOLD_SPLIT_RANGE = 600;
    // non-moving objects
    public static final int SAY_RANGE = 200;
    public static final int SHOUT_RANGE = 300;
    public static final int STATIC_THRESHOLD = 75; // Range must travel before
    // reloading statics
    public static final int FORMATION_RANGE = 75; // Max Distance a player can
    // (default: 600)
    // be from group lead on
    // formation move
    public static final int OPENCLOSEDOORDISTANCE = 128; // Max distance a
    public static final int DOOR_CLOSE_TIMER = 30000; // 30 seconds
    // player can be from a door in order to toggle its state
    public static final int TRADE_RANGE = 10; // Max distance a player can be
    // from another player to trade
    public static final int NPC_TALK_RANGE = 20; // Range player can be to talk
    // to npc
    public static final int MAX_TELEPORT_RANGE = 1020; // Max range teleports
    // will work at
    public static final int RANGED_WEAPON_RANGE = 35; // any weapon attack
    // range beyond this
    // is ranged.
    public static final int CALL_FOR_HELP_RADIUS = 100; // Range mobs will
    public static final int TREE_TELEPORT_RADIUS = 30;
    public static final int[] DEFAULTGRID = {-1, 1};
    public static final float startX = 19128;// 70149f; //19318.0f;
    public static final float startY = 94f; // 94f;
    public static final float startZ = -73553; // -73661.0f;
    public static final Vector3fImmutable DEFAULT_START = new Vector3fImmutable(
            MBServerStatics.startX, MBServerStatics.startY,
            MBServerStatics.startZ);
    /*
     * Base movement speeds. Do NOT modify these. They must match the client
     */
    public static final float FLYWALKSPEED = 6.33f;
    // respond to calls
    // for help
    public static final float FLYRUNSPEED = 18.38f;
    public static final float SWIMSPEED = 6.5f;
    public static final float WALKSPEED = 6.5f;
    public static final float RUNSPEED = 14.67f;
    public static final float COMBATWALKSPEED = 4.44f;


    /*
     * Noob Island Start Location for new players
     */
    public static final float COMBATRUNSPEED = 14.67f;
    public static final float RUNSPEED_MOB = 15.4f;
    public static final float MOVEMENT_DESYNC_TOLERANCE = 2f; // Distance out of
    public static final float NO_WEAPON_RANGE = 8f; // Range for attack with no
    public static final float REGEN_IDLE = .06f;
    /*
     * Base regen rates. Do NOT modify these. They must match the client %per
     * second for health/mana. x per second for stamina.
     */
    public static final float HEALTH_REGEN_SIT = 0.0033333f; // 100% in 3
    // minutes
    public static final float HEALTH_REGEN_IDLE = 0.000666667f; // 100% in 25
    // minutes
    public static final float HEALTH_REGEN_WALK = 0.0005f; // 100% in 33.33
    // minutes
    public static final float HEALTH_REGEN_RUN = 0f;
    public static final float HEALTH_REGEN_SWIM_NOSTAMINA = -.03f; // 100% in
    public static final float HEALTH_REGEN_SIT_STATIC = 0.33333f; // 100% in 3
    // minutes
    public static final float HEALTH_REGEN_IDLE_STATIC = 0.0666667f; // 100% in
    // 25
    // minutes
    public static final float HEALTH_REGEN_WALK_STATIC = 0.05f; // 100% in 33.33
    // minutes
    public static final float HEALTH_REGEN_RUN_STATIC = 0f;
    public static final float HEALTH_REGEN_SWIM_NOSTAMINA_STATIC = 0f; // 100%
    // weapon
    public static final float MANA_REGEN_STATIC = 0.16666666666666666666666666666667f;
    public static final float MANA_REGEN_SIT = 0.8333333f; // 1% every 1.2 seconds
    public static final float MANA_REGEN_IDLE = 0.1666667f; // 1% every 6 seconds
    public static final float MANA_REGEN_WALK = 0.125f; // 1% every 8 seconds
    public static final float MANA_REGEN_RUN = 0.0f; // No regeneration while running
    public static final float STAMINA_REGEN_SIT = 2f; // 2 per second
    public static final float STAMINA_REGEN_IDLE = 0.2f; // 1 per 5 seconds
    public static final float STAMINA_REGEN_WALK = 0f;
    public static final float STAMINA_REGEN_RUN_COMBAT = -0.6499999762f;
    public static final float STAMINA_REGEN_RUN_NONCOMBAT = -0.400000006f;
    public static final float STAMINA_REGEN_SWIM = -1f; // -1 per second
    public static final int REGEN_SENSITIVITY_PLAYER = 250; // calc regen ever X
    public static final int REGEN_SENSITIVITY_MOB = 1000; // calc regen ever X
    public static final int TOMBSTONE = 2024;
    public static final int LOGOUT_TIMER_MS = 1000; // logout delay applied
    public static final int CORPSE_CLEANUP_TIMER_MS = 10 * 60 * 1000; // Cleanup
    public static final int DEFAULT_SPAWN_TIME_MS = 3 * 60 * 1000; // 3 minute
    public static final int SESSION_CLEANUP_TIMER_MS = 30 * 1000; // cleanup
    public static final int MOVEMENT_FREQUENCY_MS = 1000; // Update movement
    // once every X ms
    public static final int FLY_FREQUENCY_MS = 1000; // Update flight once every
    public static final float FLY_RATE = .0078f;
    // x ms
    public static final int HEIGHT_CHANGE_TIMER_MS = 125; // Time in ms to fly
    // up or down 1 unit
    public static final long OPCODE_HANDLE_TIME_WARNING_MS = 250L;
    public static final long DB_QUERY_WARNING_TIME_MS = 250L;
    public static final long DB_UPDATE_WARNING_TIME_MS = 250L;
    public static final long DB_EXECUTION_WARNING_TIME_MS = 250L;
    // summons
    public static final int THIRTY_SECONDS = 30000;
    public static final int FOURTYFIVE_SECONDS = 45000;
    public static final int ONE_MINUTE = 60000;
    public static final int FIVE_MINUTES = 300000;
    public static final int FIFTEEN_MINUTES = 900000;
    public static final int THIRTY_MINUTES = 1800000;
    public static final long TWENTY_FOUR_HOURS = 86400000;
    public static final int LOAD_OBJECT_DELAY = 500; // long to wait to update
    public static final int TELEPORT_TIME_IN_SECONDS = 10;
    public static final int REPLEDGE_TIME_IN_SECONDS = 0;
    public static final int RUNEGATE_CLOSE_TIME = 30000; // runegate close timer
    public static final long PLAYER_KILL_XP_TIMER = 60 * 60 * 1000; // 60
    public static final int UPDATE_GROUP_RATE = 10000; // Update group info
    public static final int COMBAT_SEND_DODGE = 20;
    public static final int COMBAT_SEND_BLOCK = 21;
    public static final int COMBAT_SEND_PARRY = 22;
    public static final short LEVELCAP = 80;
    public static final int LEVEL_CON_WHITE = 7;
    public static final int RESPAWN_TIMER = 90 * 1000;
    public static final int DESPAWN_TIMER = 12 * 1000;
    public static final int DESPAWN_TIMER_WITH_LOOT = 60 * 1000;
    public static final int DESPAWN_TIMER_ONCE_LOOTED = 5 * 1000;
    public static final int MAX_COMBAT_HITBOX_RADIUS = 80;
    public static final int PROC_CHANCE = 5; // %chance to proc
    public static final float TRACK_ARROW_FAST_RANGE = 50f; // Range to go from
    public static final int TRACK_ARROW_SENSITIVITY = 1000; // Refresh track
    // arrows every X ms
    public static final int TRACK_ARROW_SENSITIVITY_FAST = 250; // Refresh track
    public static final int LOW_POPULATION = 100;
    public static final int NORMAL_POPULATION = 500;
    public static final int HIGH_POPULATION = 1000;
    public static final int VERY_OVERPOPULATED_POPULATION = 3000;
    public static final int FULL_POPULATION = 5000;
    public static final int TRACK_WINDOW_THRESHOLD = 1000; // max refresh once
    public static final int WHO_WINDOW_THRESHOLD = 3000; // max refresh once
    // Mine related
    public static final int MINE_EARLY_WINDOW = 16; // 4pm
    public static final int MINE_LATE_WINDOW = 0; // Midnight
    public static final Long THREE_MINUTES = 180000L;
    public static boolean DEBUG_PROTOCOL = false;
    public static int SPATIAL_HASH_BUCKETSX = 16384;
    public static int SPATIAL_HASH_BUCKETSY = 12288;
    public static float MAX_PLAYER_X_LOC = 129999;
    public static float MAX_PLAYER_Y_LOC = -97000;
    public static float LOOT_RANGE = 100;
    public static float MOB_SPEED_WALK = 6.5f;
    public static float MOB_SPEED_WALKCOMBAT = 4.4f;
    public static float MOB_SPEED_RUN = 14.67f;
    public static float MOB_SPEED_RUNCOMBAT = 14.67f;
    public static float STAMINA_REGEN_FLY_IDLE = -2f; // needs verifying
    public static float STAMINA_REGEN_FLY_WALK = -1f; // needs verifying
    public static float STAMINA_REGEN_FLY_RUN = -1.400000006f; // needs verifying
    public static float STAMINA_REGEN_FLY_RUN_COMBAT = -1.6499999762f; // needs verifying
    public static boolean DB_ENABLE_QUERY_OUTPUT = false;
    public static float PLAYER_HATE_DELIMITER = 50; // reduces 50 hate a second
    public static float PLAYER_COMBAT_HATE_MODIFIER = 2;
    // DO NOT FINAL THESE FIELD!
    public static Enum.AccountStatus accessLevel; // Min account level to login to server
    public static boolean blockLogin = false;
    public static boolean ENABLE_VAULT_FILL = false;
    public static boolean ENABLE_MOB_LOOT = true;
    public static boolean ENABLE_AUDIT_JOB_WORKERS = true;
    public static boolean ENABLE_COMBAT_TARGET_HITBOX = true;
    public static String JUNIOR = "Junior";
    public static String VETERAN = "Veteran";
    public static String ELITE = "Elite";

    public static String getEmulatorVersion() {
        return Integer.toString(revisionNumber);
    }

}
