// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.jobs;

import engine.Enum;
import engine.gameManager.CombatManager;
import engine.job.AbstractJob;
import engine.objects.AbstractCharacter;
import engine.objects.PlayerCharacter;

public class AttackJob extends AbstractJob {

    private final AbstractCharacter source;
    private final int slot;
    private final boolean success;

    public AttackJob(AbstractCharacter source, int slot, boolean success) {
        super();
        this.source = source;
        this.slot = slot;
        this.success = success;
    }

    @Override
    public void doJob() {

        if(this.source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
            PlayerCharacter pc = (PlayerCharacter)source;
            if(pc.combatTarget != null && pc.combatTarget.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
                PlayerCharacter target = (PlayerCharacter)pc.combatTarget;
                if(!pc.canSee(target)) {
                    return;
                }
            }
        }
        CombatManager.doCombat(this.source, slot);
    }

    public boolean success() {
        return this.success;
    }

    protected void _cancelJob() {
    }
}