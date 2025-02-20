// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.Enum.GameObjectType;
import engine.Enum.ModType;
import engine.Enum.SourceType;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.mobileAI.Threads.MobAIThread;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.PetMsg;
import engine.server.MBServerStatics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public abstract class AbstractIntelligenceAgent extends AbstractCharacter {
    protected Vector3fImmutable lastBindLoc;
    public boolean assist = false;
    public Enum.AIAgentType agentType = Enum.AIAgentType.MOBILE;


    public AbstractIntelligenceAgent(ResultSet rs) throws SQLException {
        super(rs);
    }

    public AbstractIntelligenceAgent(String firstName,
                                     String lastName, short statStrCurrent, short statDexCurrent,
                                     short statConCurrent, short statIntCurrent, short statSpiCurrent,
                                     short level, int exp, boolean sit, boolean walk, boolean combat,
                                     Vector3fImmutable bindLoc, Vector3fImmutable currentLoc, Vector3fImmutable faceDir,
                                     short healthCurrent, short manaCurrent, short stamCurrent,
                                     Guild guild, byte runningTrains) {
        super(firstName, lastName, statStrCurrent, statDexCurrent, statConCurrent,
                statIntCurrent, statSpiCurrent, level, exp, bindLoc,
                faceDir, guild,
                runningTrains);
    }

    public AbstractIntelligenceAgent(String firstName,
                                     String lastName, short statStrCurrent, short statDexCurrent,
                                     short statConCurrent, short statIntCurrent, short statSpiCurrent,
                                     short level, int exp, boolean sit, boolean walk, boolean combat,
                                     Vector3fImmutable bindLoc, Vector3fImmutable currentLoc, Vector3fImmutable faceDir,
                                     short healthCurrent, short manaCurrent, short stamCurrent,
                                     Guild guild, byte runningTrains, int newUUID) {
        super(firstName, lastName, statStrCurrent, statDexCurrent, statConCurrent,
                statIntCurrent, statSpiCurrent, level, exp, bindLoc,
                currentLoc, faceDir, guild,
                runningTrains, newUUID);
    }

    @Override
    public void setObjectTypeMask(int mask) {
        mask |= MBServerStatics.MASK_IAGENT;
        super.setObjectTypeMask(mask);
    }

    /* AI Job Management */

    public MobBase getMobBase() {

        if (this.getObjectType().equals(GameObjectType.Mob))
            return this.getMobBase();

        return null;
    }

    public void setPet(PlayerCharacter owner, boolean summoned) {

        if (summoned)
            this.agentType = Enum.AIAgentType.PET; //summoned
        else
            this.agentType = Enum.AIAgentType.CHARMED;

        if (this.getObjectType().equals(GameObjectType.Mob)) {
            ((Mob) this).setOwner(owner);
        }
    }


    public boolean isPet() {

        return (this.agentType.equals(Enum.AIAgentType.PET) ||
                this.agentType.equals(Enum.AIAgentType.CHARMED));
    }

    public void toggleAssist() {
        this.assist = !this.assist;
    }

    public int getDBID() {

        if (this.getObjectType().equals(GameObjectType.Mob))
            return this.getDBID();

        return 0;
    }

    public PlayerCharacter getOwner() {

        if (this.getObjectType().equals(GameObjectType.Mob))
            return this.getOwner();

        return null;
    }

    public boolean getSafeZone() {

        ArrayList<Zone> allIn = ZoneManager.getAllZonesIn(this.getLoc());

        for (Zone zone : allIn)
            if (zone.getSafeZone() == (byte) 1)
                return true;

        return false;
    }

    public float getAggroRange() {

        float ret = MobAIThread.AI_BASE_AGGRO_RANGE;

        if (this.bonuses != null)
            ret *= (1 + this.bonuses.getFloatPercentAll(ModType.ScanRange, SourceType.None, null));

        return ret;
    }

    public void dismiss() {

        if (this.isPet()) {

            if ((this.agentType.equals(Enum.AIAgentType.PET))) { //delete summoned pet

                WorldGrid.RemoveWorldObject(this);

                if (this.getObjectType() == GameObjectType.Mob)
                    if (((Mob) this).getParentZone() != null)
                        ((Mob) this).getParentZone().zoneMobSet.remove(this);

            } else { //revert charmed pet
                this.agentType = Enum.AIAgentType.MOBILE;
                this.setCombatTarget(null);
            }

            //clear owner

            PlayerCharacter owner = this.getOwner();

            //close pet window

            if (owner != null) {

                Mob pet = owner.getPet();

                PetMsg petMsg = new PetMsg(5, null);
                Dispatch dispatch = Dispatch.borrow(owner, petMsg);
                DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);

                if (pet != null && pet.getObjectUUID() == this.getObjectUUID())
                    owner.setPet(null);

                if (this.getObjectType().equals(GameObjectType.Mob))
                    ((Mob) this).setOwner(null);
            }


        }
    }

}

