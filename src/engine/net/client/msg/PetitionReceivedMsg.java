// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.net.client.msg;


import engine.net.AbstractConnection;
import engine.net.ByteBufferReader;
import engine.net.ByteBufferWriter;
import engine.net.client.Protocol;


public class PetitionReceivedMsg extends ClientNetMsg {

    // TODO pull these statics out into SBEmuStatics.java
    public static final int PETITION_NEW = 1;
    public static final int PETITION_CANCEL = 2;

    public int petition;
    public int unknown01;
    public int unknown02;
    public byte unknownByte01;
    public int unknown03;
    public int unknown04;
    public int unknown05;
    public int unknown06;
    public int type;
    public int subType;
    public String compType;
    public String language;
    public int unknown07;
    public String message;

    /**
     * This constructor is used by NetMsgFactory. It attempts to deserialize the
     * ByteBuffer into a message. If a BufferUnderflow occurs (based on reading
     * past the limit) then this constructor Throws that Exception to the
     * caller.
     */
    public PetitionReceivedMsg(AbstractConnection origin, ByteBufferReader reader) {
        super(Protocol.CUSTOMERPETITION, origin, reader);
    }

    /**
     * Serializes the subclass specific items to the supplied NetMsgWriter.
     */
    @Override
    protected void _serialize(ByteBufferWriter writer) {
        writer.putInt(this.petition);

        if (this.petition == PETITION_NEW) {
            writer.putInt(this.unknown01);
            writer.putInt(this.unknown02);
            writer.put(this.unknownByte01);
            writer.putInt(this.unknown03);
            writer.putInt(this.unknown04);
            writer.putInt(this.unknown05);
            writer.putInt(this.unknown06);
            writer.putInt(this.type);
            writer.putInt(this.subType);
            writer.putString(this.compType);
            writer.putString(this.language);
            writer.putInt(this.unknown07);
            writer.putUnicodeString(message);
        } else {
            writer.putInt(this.unknown01);
            writer.putInt(this.unknown02);
            writer.put(this.unknownByte01);
            writer.putInt(this.unknown03);
            writer.putInt(this.unknown04);
        }
    }

    /**
     * Deserializes the subclass specific items from the supplied NetMsgReader.
     */
    @Override
    protected void _deserialize(ByteBufferReader reader) {
        petition = reader.getInt();
        if (petition == PETITION_NEW) {
            this.unknown01 = reader.getInt();
            this.unknown02 = reader.getInt();
            this.unknownByte01 = reader.get();
            this.unknown03 = reader.getInt();
            this.unknown04 = reader.getInt();
            this.unknown05 = reader.getInt();
            this.unknown06 = reader.getInt();
            this.type = reader.getInt();
            this.subType = reader.getInt();
            this.compType = reader.getString();
            this.language = reader.getString();
            this.unknown07 = reader.getInt();
            this.message = reader.getUnicodeString();
        } else if (petition == PETITION_CANCEL) {
            this.unknown01 = reader.getInt();
            this.unknown02 = reader.getInt();
            this.unknownByte01 = reader.get();
            this.unknown03 = reader.getInt();
            this.unknown04 = reader.getInt();
        }
    }

}
