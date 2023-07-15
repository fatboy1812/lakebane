// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.powers.effectmodifiers;

import engine.Enum.ModType;
import engine.Enum.SourceType;
import engine.jobs.AbstractEffectJob;
import engine.objects.AbstractCharacter;
import engine.objects.AbstractWorldObject;
import engine.objects.Building;
import engine.objects.Item;
import engine.powers.EffectsBase;

import java.sql.ResultSet;
import java.sql.SQLException;


public abstract class AbstractEffectModifier {

    public float minMod;
    public SourceType sourceType;
    public ModType modType;
    protected EffectsBase parent;
    protected int UUID;
    protected String IDString;
    protected String effectType;
    protected float maxMod;
    protected float percentMod;
    protected float ramp;
    protected boolean useRampAdd;
    protected String type;
    protected String string1;
    protected String string2;

    public AbstractEffectModifier(ResultSet rs) throws SQLException {

        this.UUID = rs.getInt("ID");
        this.IDString = rs.getString("IDString");
        this.effectType = rs.getString("modType");
        this.modType = ModType.GetModType(this.effectType);
        this.type = rs.getString("type").replace("\"", "");
        this.sourceType = SourceType.GetSourceType(this.type.replace(" ", "").replace("-", ""));
        this.minMod = rs.getFloat("minMod");
        this.maxMod = rs.getFloat("maxMod");
        this.percentMod = rs.getFloat("percentMod");
        this.ramp = rs.getFloat("ramp");
        this.useRampAdd = (rs.getInt("useRampAdd") == 1) ? true : false;

        this.string1 = rs.getString("string1");
        this.string2 = rs.getString("string2");
    }


    public int getUUID() {
        return this.UUID;
    }

    // public String getIDString() {
    // return this.IDString;
    // }

    public String getmodType() {
        return this.effectType;
    }

    public float getMinMod() {
        return this.minMod;
    }

    public float getMaxMod() {
        return this.maxMod;
    }

    public float getPercentMod() {
        return this.percentMod;
    }

    public float getRamp() {
        return this.ramp;
    }

    public String getType() {
        return this.type;
    }

    public String getString1() {
        return this.string1;
    }

    public String getString2() {
        return this.string2;
    }

    public EffectsBase getParent() {
        return this.parent;
    }

    public void setParent(EffectsBase value) {
        this.parent = value;
    }

    public void applyEffectModifier(AbstractCharacter source, AbstractWorldObject awo, int trains, AbstractEffectJob effect) {

        _applyEffectModifier(source, awo, trains, effect);
    }

    protected abstract void _applyEffectModifier(AbstractCharacter source, AbstractWorldObject awo, int trains, AbstractEffectJob effect);

    public abstract void applyBonus(AbstractCharacter ac, int trains);

    public abstract void applyBonus(Item item, int trains);

    public abstract void applyBonus(Building building, int trains);
}
