// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.net.client.msg;


import engine.Dungeons.Dungeon;
import engine.net.AbstractConnection;
import engine.net.AbstractNetMsg;
import engine.net.ByteBufferReader;
import engine.net.ByteBufferWriter;
import engine.net.client.Protocol;
import engine.objects.City;
import engine.objects.Mine;
import engine.objects.PlayerCharacter;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;


public class TeleportRepledgeListMsg extends ClientNetMsg {

    ArrayList<City> cities;
    ArrayList<Mine> mines;
    private PlayerCharacter player;
    private boolean isTeleport;

    /**
     * This is the general purpose constructor.
     */
    public TeleportRepledgeListMsg(PlayerCharacter player, boolean isTeleport) {
        super(Protocol.SENDCITYENTRY);
        this.player = player;
        this.isTeleport = isTeleport;
    }

    /**
     * This constructor is used by NetMsgFactory. It attempts to deserialize the
     * ByteBuffer into a message. If a BufferUnderflow occurs (based on reading
     * past the limit) then this constructor Throws that Exception to the
     * caller.
     */
    public TeleportRepledgeListMsg(AbstractConnection origin, ByteBufferReader reader) {
        super(Protocol.SENDCITYENTRY, origin, reader);
    }

    /**
     * Copy constructor
     */
    public TeleportRepledgeListMsg(TeleportRepledgeListMsg msg) {
        super(Protocol.SENDCITYENTRY);
        this.player = msg.player;
        this.isTeleport = msg.isTeleport;
    }

    /**
     * @see AbstractNetMsg#getPowerOfTwoBufferSize()
     */
    @Override
    protected int getPowerOfTwoBufferSize() {
        // Larger size for historically larger opcodes
        return (16);
    }

    /**
     * Deserializes the subclass specific items from the supplied ByteBufferReader.
     */
    @Override
    protected void _deserialize(ByteBufferReader reader) {
        //Do we even want to try this?
    }

    // Pre-caches and configures message so data is avaiable
    // when we serialize.

    public void configure() {

        if (isTeleport) {
            cities = City.getCitiesToTeleportTo(player);
            try {
                mines = Mine.getMinesToTeleportTo(player);
                if(mines == null)
                    mines = new ArrayList<>();
            }catch(Exception e){
                Logger.error("Unable To Load Mines For Teleport: " + e.getMessage());
            }
        }else {
            cities = City.getCitiesToRepledgeTo(player);
            mines = new ArrayList<>();
        }
    }

    /**
     * Serializes the subclass specific items to the supplied ByteBufferWriter.
     */
    @Override
    protected void _serialize(ByteBufferWriter writer) {
        if (isTeleport)
            writer.putInt(2); //teleport?
        else
            writer.putInt(1); //repledge?

        for (int i = 0; i < 3; i++)
            writer.putInt(0);

        writer.putInt(cities.size() + mines.size() + 1);

        for (City city : cities)
            City.serializeForClientMsg(city, writer);

        for(Mine mine : mines)
            Mine.serializeForClientMsgTeleport(mine, writer);

        Dungeon.serializeForClientMsgTeleport(writer);
    }

    public PlayerCharacter getPlayer() {
        return this.player;
    }

    public boolean isTeleport() {
        return this.isTeleport;
    }

}
