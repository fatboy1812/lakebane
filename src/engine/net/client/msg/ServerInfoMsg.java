// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.net.client.msg;


import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.net.AbstractConnection;
import engine.net.ByteBufferReader;
import engine.net.ByteBufferWriter;
import engine.net.client.Protocol;
import engine.server.MBServerStatics;
import engine.server.login.LoginServer;
import engine.server.world.WorldServer;


public class ServerInfoMsg extends ClientNetMsg {


    /**
     * This is the general purpose constructor.
     */
    public ServerInfoMsg() {
        super(Protocol.SELECTSERVER);
    }

    /**
     * This constructor is used by NetMsgFactory. It attempts to deserialize the
     * ByteBuffer into a message. If a BufferUnderflow occurs (based on reading
     * past the limit) then this constructor Throws that Exception to the
     * caller.
     */
    public ServerInfoMsg(AbstractConnection origin, ByteBufferReader reader) {
        super(Protocol.SELECTSERVER, origin, reader);
    }

    /**
     * Serializes the subclass specific items to the supplied NetMsgWriter.
     */
    @Override
    protected void _serialize(ByteBufferWriter writer) {
        // writer.putInt(this.servers.size());
        // for (WorldServerInfoSnapshot wsis : this.servers) {
        // wsis.serializeForClientMsg(writer);
        // }
        writer.putInt(1);

        writer.putInt(WorldServer.worldMapID);
        writer.putString(ConfigManager.MB_WORLD_NAME.getValue());
        int TotalTrees = 21;
        int currentR8Trees = DbManager.CityQueries.GET_CAPITAL_CITY_COUNT();

        switch(currentR8Trees){
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                writer.putInt(0); //Land Rush
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                writer.putInt(1); //Low pop
                break;
            case 9:
            case 10:
            case 11:
            case 12:
                writer.putInt(2); //Normal pop
                break;
            case 13:
            case 14:
            case 15:
            case 16:
                writer.putInt(3); //High Pop
                break;
            case 17:
            case 18:
            case 19:
            case 20:
                writer.putInt(4); //Very overpopulated pop
                break;
            default:
                writer.putInt(5); //Full pop
                break;
        }
    }

    /**
     * Deserializes the subclass specific items from the supplied NetMsgReader.
     */
    @Override
    protected void _deserialize(ByteBufferReader reader) {
        int size = reader.getInt();
        for (int i = 0; i < size; i++) {
            int ID = reader.getInt();
            String name = reader.getString();
            int pop = reader.getInt();
        }
    }


}
