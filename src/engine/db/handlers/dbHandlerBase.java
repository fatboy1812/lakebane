// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.Enum.GameObjectType;
import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.objects.AbstractGameObject;
import engine.objects.AbstractWorldObject;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;

public abstract class dbHandlerBase {

    protected Class<? extends AbstractGameObject> localClass = null;
    protected GameObjectType localObjectType;

    protected <T extends AbstractGameObject> AbstractGameObject getObjectFromRs(ResultSet rs) {

        AbstractGameObject abstractGameObject = null;

        try {
            if (rs.next()) {
                abstractGameObject = localClass.getConstructor(ResultSet.class).newInstance(rs);
                DbManager.addToCache(abstractGameObject);
            }
        } catch (Exception e) {
            Logger.error(e);
        }

        // Only call runAfterLoad() for objects instanced on the world server

        if ((abstractGameObject != null && abstractGameObject instanceof AbstractWorldObject) &&
                (ConfigManager.serverType.equals(Enum.ServerType.WORLDSERVER) ||
                        (abstractGameObject.getObjectType() == GameObjectType.Guild)))
            ((AbstractWorldObject) abstractGameObject).runAfterLoad();

        return abstractGameObject;
    }

    protected <T extends AbstractGameObject> ArrayList<T> getObjectsFromRs(ResultSet rs, int listSize) {

        ArrayList<T> objectList = new ArrayList<>(listSize);

        try {
            while (rs.next()) {

                int id = rs.getInt(1);
                try {
                    if (rs.getInt("capSize") == 0) {
                        continue;
                    }
                }catch(Exception e){
                    //not a mine
                }

                if (DbManager.inCache(localObjectType, id)) {
                    objectList.add((T) DbManager.getFromCache(localObjectType, id));
                } else {
                    try{
                        if(rs.getInt("mineLiveHour") == 1)
                            continue;
                    }catch(Exception e){
                        //not a mine
                    }
                    AbstractGameObject toAdd = localClass.getConstructor(ResultSet.class).newInstance(rs);
                    DbManager.addToCache(toAdd);
                    if(toAdd.getObjectType().equals(GameObjectType.Zone) && rs.getInt("canLoad") == 0){
                        continue;
                    }
                    objectList.add((T) toAdd);

                    if (toAdd != null && toAdd instanceof AbstractWorldObject)
                        ((AbstractWorldObject) toAdd).runAfterLoad();
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return objectList;
    }
}
