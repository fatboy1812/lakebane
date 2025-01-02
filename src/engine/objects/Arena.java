package engine.objects;

import engine.InterestManagement.WorldGrid;
import engine.gameManager.ArenaManager;
import engine.gameManager.ChatManager;
import engine.gameManager.MovementManager;
import engine.math.Vector3fImmutable;
import engine.server.MBServerStatics;

import java.util.HashSet;

public class Arena {
    public PlayerCharacter player1;
    public PlayerCharacter player2;
    public Long startTime;
    public Vector3fImmutable loc;
    public Building arenaCircle;

    public Arena(){

    }
    public Boolean disqualify() {
        HashSet<AbstractWorldObject> inRange = WorldGrid.getObjectsInRangePartial(this.loc, 250f, MBServerStatics.MASK_PLAYER);
        HashSet<AbstractWorldObject> warningRange = WorldGrid.getObjectsInRangePartial(this.loc, 500f, MBServerStatics.MASK_PLAYER);
        for(AbstractWorldObject obj : warningRange){
            PlayerCharacter pc = (PlayerCharacter)obj;
            if(pc.equals(this.player1) || pc.equals(this.player2))
                continue;

            ChatManager.chatSystemInfo(pc, "WARNING!! You are entering an arena zone!");
        }
        //boot out all non competitors
        for(AbstractWorldObject obj : inRange){
            if(obj.equals(this.player1))
                continue;

            if(obj.equals(this.player2))
                continue;

            PlayerCharacter intruder = (PlayerCharacter)obj;
            MovementManager.translocate(intruder,new Vector3fImmutable(88853,32,45079),Regions.GetRegionForTeleport(new Vector3fImmutable(88853,32,45079)));
        }

        if (!inRange.contains(this.player1) && inRange.contains(this.player2)) {
            ArenaManager.endArena(this,this.player2,this.player1,"Player Has Left Arena");
            return true;
        } else if (!inRange.contains(this.player2) && inRange.contains(this.player1)) {
            ArenaManager.endArena(this,this.player1,this.player2,"Player Has Left Arena");
            return true;
        }else if (!inRange.contains(this.player2) && !inRange.contains(this.player1)) {
            ArenaManager.endArena(this,null,null,"Both Parties Have Left The Arena");
            return true;
        }
        return false;
    }

    public Boolean checkToComplete(){

        if(this.startTime == null)
            this.startTime = System.currentTimeMillis();

        if(System.currentTimeMillis() - this.startTime < 10000L)
            return false;

        if(this.disqualify())
            return true;

        if(!this.player1.isAlive() && this.player2.isAlive()){
            ArenaManager.endArena(this,this.player2,this.player1,"Player Has Died");
            return true;
        } else if(this.player1.isAlive() && !this.player2.isAlive()){
            ArenaManager.endArena(this,this.player1,this.player2,"Player Has Died");
            return true;
        } else if(!this.player1.isAlive() && !this.player2.isAlive()){
            ArenaManager.endArena(this,null,null,"Both Players Have Died");
            return true;
        } else if(this.startTime + 300000L < System.currentTimeMillis()){
            ArenaManager.endArena(this,null,null,"Time Has Elapsed");
            return true;
        }
        return false;
    }
}
