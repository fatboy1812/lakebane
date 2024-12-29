package engine.gameManager;
import engine.objects.Guild;
public class ZergManager {

    public static float getCurrentMultiplier(int count, int maxCount){
        switch(maxCount) {
            case 3:
                return getMultiplier3Man(count);
            case 5:
                return getMultiplier5Man(count);
            case 10:
                return getMultiplier10Man(count);
            default:
                return getMultiplier20Man(count);
        }
    }
    public static float getMultiplier3Man(int count) {
        if(count < 4)
            return 1.0f;

        if(count > 6)
            return 0.0f;

        switch(count){
            case 4:
                return 0.63f;
            case 5:
                return 0.40f;
            case 6:
                return 0.25f;

        }
        return 1.0f;
    }
    public static float getMultiplier5Man(int count) {
        if(count < 6)
            return 1.0f;

        if(count > 10)
            return 0.0f;

        switch(count){
            case 6:
                return 0.75f;
            case 7:
                return 0.57f;
            case 8:
                return 0.44f;
            case 9:
                return 0.33f;
            case 10:
                return 0.25f;

        }
        return 1.0f;
    }
    public static float getMultiplier10Man(int count) {
        if(count < 11)
            return 1.0f;

        if(count > 20)
            return 0.0f;

        switch(count){
            case 11:
                return 0.86f;
            case 12:
                return 0.75f;
            case 13:
                return 0.65f;
            case 14:
                return 0.57f;
            case 15:
                return 0.50f;
            case 16:
                return 0.44f;
            case 17:
                return 0.38f;
            case 18:
                return 0.33f;
            case 19:
                return 0.29f;
            case 20:
                return 0.25f;

        }
        return 1.0f;
    }
    public static float getMultiplier20Man(int count) {
        return getMultiplier10Man(count * 2);
    }
}
