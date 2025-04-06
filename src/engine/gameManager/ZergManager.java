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

        if(count > 6)
            return 0.0f;

        switch(count){
            case 1: return 2.0f;
            case 2: return 1.5f;
            case 4: return 0.62f;
            case 5: return 0.4f;
            case 6: return 0.25f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier5Man(int count) {
        if(count > 10)
            return 0.0f;

        switch(count){
            case 1:
            case 2:
                return 2.0f;
            case 3: return 1.67f;
            case 4: return 1.25f;
            case 6: return 0.75f;
            case 7: return 0.57f;
            case 8: return 0.44f;
            case 9: return 0.33f;
            case 10: return 0.25f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier10Man(int count) {

        if(count > 20)
            return 0.0f;

        switch(count){
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return 2.0f;
            case 6: return 1.67f;
            case 7: return 1.43f;
            case 8: return 1.25f;
            case 9: return 1.11f;
            case 11: return 0.87f;
            case 12: return 0.77f;
            case 13: return 0.68f;
            case 14: return 0.60f;
            case 15: return 0.53f;
            case 16: return 0.48f;
            case 17: return 0.42f;
            case 18: return 0.38f;
            case 19: return 0.34f;
            case 20: return 0.30f;
            default: return 1.0f;
        }
    }
    public static float getMultiplier20Man(int count) {

        if(count > 40)
            return 0.0f;

        switch (count)
        {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                return 2.0f;
            case 11: return 1.82f;
            case 12: return 1.67f;
            case 13: return 1.54f;
            case 14: return 1.43f;
            case 15: return 1.33f;
            case 16: return 1.25f;
            case 17: return 1.18f;
            case 18: return 1.11f;
            case 19: return 1.05f;
            case 21: return 0.94f;
            case 22: return 0.88f;
            case 23: return 0.82f;
            case 24: return 0.78f;
            case 25: return 0.73f;
            case 26: return 0.69f;
            case 27: return 0.65f;
            case 28: return 0.61f;
            case 29: return 0.58f;
            case 30: return 0.55f;
            case 31: return 0.52f;
            case 32: return 0.49f;
            case 33: return 0.47f;
            case 34: return 0.44f;
            case 35: return 0.42f;
            case 36: return 0.40f;
            case 37: return 0.38f;
            case 38: return 0.36f;
            case 39: return 0.34f;
            case 40: return 0.33f;
            default: return 1.0f;
        }

    }
    public static float getMultiplier30Man(int count) {

        if(count > 60)
            return 0.0f;

        switch (count)
        {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return 2.0f;
            case 16: return 1.88f;
            case 17: return 1.76f;
            case 18: return 1.67f;
            case 19: return 1.58f;
            case 21: return 1.43f;
            case 22: return 1.36f;
            case 23: return 1.30f;
            case 24: return 1.25f;
            case 25: return 1.20f;
            case 26: return 1.15f;
            case 27: return 1.11f;
            case 28: return 1.07f;
            case 29: return 1.03f;
            case 31: return 0.96f;
            case 32: return 0.92f;
            case 33: return 0.88f;
            case 34: return 0.85f;
            case 35: return 0.81f;
            case 36: return 0.78f;
            case 37: return 0.75f;
            case 38: return 0.73f;
            case 39: return 0.70f;
            case 40: return 0.68f;
            case 41: return 0.65f;
            case 42: return 0.63f;
            case 43: return 0.61f;
            case 44: return 0.59f;
            case 45: return 0.57f;
            case 46: return 0.55f;
            case 47: return 0.53f;
            case 48: return 0.51f;
            case 49: return 0.50f;
            case 50: return 0.48f;
            case 51: return 0.46f;
            case 52: return 0.45f;
            case 53: return 0.44f;
            case 54: return 0.42f;
            case 55: return 0.41f;
            case 56: return 0.40f;
            case 57: return 0.38f;
            case 58: return 0.37f;
            case 59: return 0.36f;
            case 60: return 0.35f;
            default: return 1.0f;
        }

    }
    public static float getMultiplier40Man(int count) {

        if(count > 80)
            return 0.0f;

        switch (count)
        {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
                return 2.0f;
            case 21: return 1.90f;
            case 22: return 1.82f;
            case 23: return 1.74f;
            case 24: return 1.67f;
            case 25: return 1.60f;
            case 26: return 1.54f;
            case 27: return 1.48f;
            case 28: return 1.43f;
            case 29: return 1.38f;
            case 30: return 1.33f;
            case 31: return 1.29f;
            case 32: return 1.25f;
            case 33: return 1.21f;
            case 34: return 1.18f;
            case 35: return 1.14f;
            case 36: return 1.11f;
            case 37: return 1.08f;
            case 38: return 1.05f;
            case 39: return 1.03f;
            case 41: return 0.97f;
            case 42: return 0.94f;
            case 43: return 0.91f;
            case 44: return 0.89f;
            case 45: return 0.86f;
            case 46: return 0.84f;
            case 47: return 0.81f;
            case 48: return 0.79f;
            case 49: return 0.77f;
            case 50: return 0.75f;
            case 51: return 0.73f;
            case 52: return 0.71f;
            case 53: return 0.69f;
            case 54: return 0.68f;
            case 55: return 0.66f;
            case 56: return 0.64f;
            case 57: return 0.63f;
            case 58: return 0.61f;
            case 59: return 0.60f;
            case 60: return 0.58f;
            case 61: return 0.57f;
            case 62: return 0.56f;
            case 63: return 0.54f;
            case 64: return 0.53f;
            case 65: return 0.52f;
            case 66: return 0.51f;
            case 67: return 0.50f;
            case 68: return 0.49f;
            case 69: return 0.47f;
            case 70: return 0.46f;
            case 71: return 0.45f;
            case 72: return 0.44f;
            case 73:
            case 74:
                return 0.43f;
            case 75: return 0.42f;
            case 76: return 0.41f;
            case 77: return 0.40f;
            case 78: return 0.39f;
            case 79:
            case 80:
                return 0.38f;
            default: return 1.0f;
        }
    }
}
