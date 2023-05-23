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
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class dbHandlerBase {

    protected final ThreadLocal<CallableStatement> callableStatement = new ThreadLocal<>();
    protected final ThreadLocal<Connection> connection = new ThreadLocal<>();
    /*
     * CallableStatements handled below this line!
     */
    protected Class<? extends AbstractGameObject> localClass = null;
    protected GameObjectType localObjectType;

    protected final void prepareCallable(final String sql) {
        try {
            this.connection.set(DbManager.getConnection());
            this.callableStatement.set(this.connection.get().prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        } catch (SQLException e) {
            Logger.error("DbManager.getConn", e);
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setInt(int parameterIndex, int value) {
        try {
            this.callableStatement.get().setInt(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setLong(int parameterIndex, long value) {
        try {
            this.callableStatement.get().setLong(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setFloat(int parameterIndex, float value) {
        try {
            this.callableStatement.get().setFloat(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setShort(int parameterIndex, short value) {
        try {
            this.callableStatement.get().setShort(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setString(int parameterIndex, String value) {
        try {
            this.callableStatement.get().setString(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setByte(int parameterIndex, byte value) {
        try {
            this.callableStatement.get().setByte(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setBoolean(int parameterIndex, boolean value) {
        try {
            this.callableStatement.get().setBoolean(parameterIndex, value);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setNULL(int parameterIndex, int type) {
        try {
            this.callableStatement.get().setNull(parameterIndex, type);
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setLocalDateTime(int parameterIndex, LocalDateTime localDateTime) {

        try {
            this.callableStatement.get().setTimestamp(parameterIndex, Timestamp.valueOf(localDateTime));
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final void setTimeStamp(int parameterIndex, long time) {
        try {
            this.callableStatement.get().setTimestamp(parameterIndex, new java.sql.Timestamp(time));
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        }
    }

    protected final boolean execute() {
        try {
            return this.callableStatement.get().execute();
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
            logSQLCommand();
        }
        return false;
    }

    protected final ResultSet executeQuery() {
        try {
            return this.callableStatement.get().executeQuery();
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
            logSQLCommand();
        }
        return null;
    }

    protected final int executeUpdate() {
        return executeUpdate(true);
    }

    protected final int executeUpdate(boolean close) {
        try {
            return this.callableStatement.get().executeUpdate();
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
            logSQLCommand();
        } finally {
            if (close)
                closeCallable();
        }
        return 0;
    }

    protected final void logSQLCommand() {
        try {
            Logger.error("Failed SQL Command: " + this.callableStatement.get().toString());
        } catch (Exception e) {

        }
    }

    // Common return values from the database when calling stored procedures, abstracted to this layer
    protected final String getResult() {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next() && !isError(rs))
                return rs.getString("result");
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return null;
    }

    // Used for Stored procedures that return true when they succeed.
    protected final boolean worked() {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next() && !isError(rs))
                return rs.getBoolean("result");
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return false;
    }

    // Common return values from the database when calling stored procedures, abstracted to this layer
    protected final long getUUID() {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next() && !isError(rs))
                return rs.getLong("UID");
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return -1;
    }

    protected final String getString(String field) {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next())
                return rs.getString(field);
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return "";
    }

    protected final long getLong(String field) {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next())
                return rs.getLong(field);
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return 0L;
    }

    protected final int getInt(String field) {
        try {
            ResultSet rs = this.executeQuery();
            if (rs.next())
                return rs.getInt(field);
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return 0;
    }

    protected final int insertGetUUID() {
        int key = 0;
        try {
            this.callableStatement.get().executeUpdate();
            ResultSet rs = this.callableStatement.get().getGeneratedKeys();
            if (rs.next())
                key = rs.getInt(1);
        } catch (SQLException e) {
            Logger.error(e);
            logSQLCommand();
        } finally {
            closeCallable();
        }
        return key;
    }

    protected final boolean isError(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        if (rsmd.getColumnCount() > 0 && !rsmd.getColumnName(1).equals("errorno"))
            return false;
        printError(rs);
        return true;
    }

    protected final void printError(ResultSet rs) {
        try {
            int errorNum = rs.getInt("errorno");
            String errorMsg = rs.getString("errormsg");
            Logger.error("SQLError: errorNum: " + errorNum + ", errorMsg: " + errorMsg);
            logSQLCommand();
        } catch (SQLException e) {
        }
    }

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

        return abstractGameObject;
    }

    protected <T extends AbstractGameObject> ArrayList<T> getObjectsFromRs(ResultSet rs, int listSize) {

        ArrayList<T> objectList = new ArrayList<>(listSize);

        try {
            while (rs.next()) {

                int id = rs.getInt(1);

                if (DbManager.inCache(localObjectType, id)) {
                    objectList.add((T) DbManager.getFromCache(localObjectType, id));
                } else {
                    AbstractGameObject toAdd = localClass.getConstructor(ResultSet.class).newInstance(rs);
                    DbManager.addToCache(toAdd);
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

    protected <T extends AbstractGameObject> AbstractGameObject getObjectSingle(int id) {
        return getObjectSingle(id, false, true);
    }

    protected <T extends AbstractGameObject> AbstractGameObject getObjectSingle(int id, boolean forceFromDB, boolean storeInCache) {

        if (callableStatement.get() == null) {
            return null;
        }

        if (!forceFromDB) {
            if (DbManager.inCache(localObjectType, id)) {
                closeCallable();
                return DbManager.getFromCache(localObjectType, id);
            }
        }

        AbstractGameObject out = null;

        try {
            if (MBServerStatics.DB_ENABLE_QUERY_OUTPUT)
                Logger.info("[GetObjectList] Executing query:" + callableStatement.get().toString());

            ResultSet rs = callableStatement.get().executeQuery();

            if (rs.next()) {
                out = localClass.getConstructor(ResultSet.class).newInstance(rs);

                if (storeInCache)
                    DbManager.addToCache(out);
            }

            rs.close();

        } catch (Exception e) {
            Logger.error("AbstractGameObject", e);
            out = null;
        } finally {
            closeCallable();
        }

        // Only call runAfterLoad() for objects instanced on the world server

        if ((out != null && out instanceof AbstractWorldObject) &&
                (ConfigManager.serverType.equals(Enum.ServerType.WORLDSERVER) ||
                        (out.getObjectType() == GameObjectType.Guild)))
            ((AbstractWorldObject) out).runAfterLoad();

        return out;
    }

    protected void closeCallable() {
        try {
            if (this.callableStatement.get() != null)
                this.callableStatement.get().close();

            this.connection.get().close();

        } catch (SQLException e) {
        }
    }

    protected <T extends AbstractGameObject> ArrayList<T> getObjectList() {
        return getObjectList(20, false);
    }

    protected <T extends AbstractGameObject> ArrayList<T> getLargeObjectList() {
        return getObjectList(2000, false);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractGameObject> ArrayList<T> getObjectList(int listSize, boolean forceFromDB) {

        String query = "No Callable Statement accessable.";

        ArrayList<T> out = new ArrayList<>(listSize);

        if (this.callableStatement.get() == null)
            return out;

        try {

            CallableStatement css = this.callableStatement.get();

            if (css != null)
                query = this.callableStatement.get().toString();

            if (MBServerStatics.DB_ENABLE_QUERY_OUTPUT)
                Logger.info("[GetObjectList] Executing query:" + query);

            ResultSet rs = this.callableStatement.get().executeQuery();

            while (rs.next()) {

                int id = rs.getInt(1);

                if (!forceFromDB && DbManager.inCache(localObjectType, id)) {
                    out.add((T) DbManager.getFromCache(localObjectType, id));
                } else {
                    AbstractGameObject toAdd = localClass.getConstructor(ResultSet.class).newInstance(rs);
                    DbManager.addToCache(toAdd);
                    out.add((T) toAdd);

                    if (toAdd != null && toAdd instanceof AbstractWorldObject)
                        ((AbstractWorldObject) toAdd).runAfterLoad();

                }
            }
            rs.close();
        } catch (Exception e) {
            Logger.error(localClass.getCanonicalName(), "List Failure: " + query, e);
            e.printStackTrace();
            return new ArrayList<>(); // Do we want a null return on error?
        } finally {
            closeCallable();
        }

        return out;
    }

    /* Prepared Statements handled below this line */

    protected HashSet<Integer> getIntegerList(final int columnNumber) {

        if (MBServerStatics.DB_ENABLE_QUERY_OUTPUT)
            Logger.info("[GetIntegerList] Executing query:" + this.callableStatement);

        HashSet<Integer> out = new HashSet<>();

        try {
            ResultSet rs = executeQuery();

            while (rs.next()) {
                out.add(rs.getInt(columnNumber));
            }
            rs.close();
        } catch (SQLException e) {
            Logger.error("SQL Error number: " + e.getErrorCode());
        } finally {
            closeCallable();
        }
        return out;
    }
}
