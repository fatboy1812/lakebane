package engine.gameManager;
import engine.objects.Guild;
public class ZergManager {

    public static float getCurrentMultiplier(int count, int maxCount){
        switch(maxCount) {
            case 3: return getMultiplier3Man(count);
            case 5: return getMultiplier5Man(count);
            case 10: return getMultiplier10Man(count);
            case 20: return getMultiplier20Man(count);
            case 30: return getMultiplier30Man(count);
            case 40: return getMultiplier40Man(count);
            default: return 1.0f; //unlimited
        }
    }
    public static float getMultiplier3Man(int count) {
        if(count < 4)
            return 1.0f;

        if(count > 6)
            return 0.0f;

        switch(count){
            case 4: return 0.50f;
            case 5: return 0.0f;
            case 6: return 0.0f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier5Man(int count) {
        if(count < 6)
            return 1.0f;

        if(count > 10)
            return 0.0f;

        switch(count){
            case 6: return 0.75f;
            case 7: return 0.57f;
            case 8: return 0.44f;
            case 9: return 0.33f;
            case 10: return 0.25f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier10Man(int count) {
        if(count < 11)
            return 1.0f;

        if(count > 20)
            return 0.0f;

        switch(count){
            case 11: return 0.86f;
            case 12: return 0.75f;
            case 13: return 0.65f;
            case 14: return 0.57f;
            case 15: return 0.50f;
            case 16: return 0.44f;
            case 17: return 0.38f;
            case 18: return 0.33f;
            case 19: return 0.29f;
            case 20: return 0.25f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier20Man(int count) {
        if(count < 21)
            return 1.0f;

        if(count > 40)
            return 0.0f;

        switch (count)
        {
            case 21: return 0.93f;
            case 22: return 0.86f;
            case 23: return 0.80f;
            case 24: return 0.75f;
            case 25: return 0.70f;
            case 26: return 0.65f;
            case 27: return 0.61f;
            case 28: return 0.57f;
            case 29: return 0.53f;
            case 30: return 0.50f;
            case 31: return 0.47f;
            case 32: return 0.44f;
            case 33: return 0.41f;
            case 34: return 0.38f;
            case 35: return 0.36f;
            case 36: return 0.33f;
            case 37: return 0.31f;
            case 38: return 0.29f;
            case 39: return 0.27f;
            case 40: return 0.25f;
            default: return 1.0f;
        }

    }
    public static float getMultiplier30Man(int count) {
        if(count < 31)
            return 1.0f;

        if(count > 60)
            return 0.0f;

        switch (count)
        {
            case 31: return 0.95f;
            case 32: return 0.91f;
            case 33: return 0.86f;
            case 34: return 0.82f;
            case 35: return 0.79f;
            case 36: return 0.75f;
            case 37: return 0.72f;
            case 38: return 0.68f;
            case 39: return 0.65f;
            case 40: return 0.63f;
            case 41: return 0.60f;
            case 42: return 0.57f;
            case 43: return 0.55f;
            case 44: return 0.52f;
            case 45: return 0.50f;
            case 46: return 0.48f;
            case 47: return 0.46f;
            case 48: return 0.44f;
            case 49: return 0.42f;
            case 50: return 0.40f;
            case 51: return 0.38f;
            case 52: return 0.37f;
            case 53: return 0.35f;
            case 54: return 0.33f;
            case 55: return 0.32f;
            case 56: return 0.30f;
            case 57: return 0.29f;
            case 58: return 0.28f;
            case 59: return 0.26f;
            case 60: return 0.25f;
            default: return 1.0f;
        }

    }
    public static float getMultiplier40Man(int count) {
        if(count < 41)
            return 1.0f;

        if(count > 80)
            return 0.0f;

        switch (count)
        {
            case 41: return 0.96f;
            case 42: return 0.93f;
            case 43: return 0.90f;
            case 44: return 0.86f;
            case 45: return 0.83f;
            case 46: return 0.80f;
            case 47: return 0.78f;
            case 48: return 0.75f;
            case 49: return 0.72f;
            case 50: return 0.70f;
            case 51: return 0.68f;
            case 52: return 0.65f;
            case 53: return 0.63f;
            case 54: return 0.61f;
            case 55: return 0.59f;
            case 56: return 0.57f;
            case 57: return 0.55f;
            case 58: return 0.53f;
            case 59: return 0.52f;
            case 60: return 0.50f;
            case 61: return 0.48f;
            case 62: return 0.47f;
            case 63: return 0.45f;
            case 64: return 0.44f;
            case 65: return 0.42f;
            case 66: return 0.41f;
            case 67: return 0.40f;
            case 68: return 0.38f;
            case 69: return 0.37f;
            case 70: return 0.36f;
            case 71: return 0.35f;
            case 72: return 0.33f;
            case 73: return 0.32f;
            case 74: return 0.31f;
            case 75: return 0.30f;
            case 76: return 0.29f;
            case 77: return 0.28f;
            case 78: return 0.27f;
            case 79: return 0.26f;
            case 80: return 0.25f;
            default: return 1.0f;
        }
    }
}
