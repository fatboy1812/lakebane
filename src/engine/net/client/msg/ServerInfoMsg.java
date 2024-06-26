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
        int TotalTrees = 275;
        int currentTrees = DbManager.CityQueries.GET_CITY_COUNT();
        if (currentTrees < TotalTrees * 0.2f)
            writer.putInt(0); //Land Rush
        else if (currentTrees < TotalTrees * 0.4f)
            writer.putInt(1); //Low pop
        else if (currentTrees < TotalTrees * 0.6f)
            writer.putInt(2); //Normal pop
        else if (currentTrees < TotalTrees * 0.8f)
            writer.putInt(3); //High Pop
        else if (currentTrees < TotalTrees)
            writer.putInt(4); //Very overpopulated pop
        else
            writer.putInt(5); //Full pop
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
