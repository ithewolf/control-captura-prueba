package com.iawolf.captureprobe;

/**
 * Pure-Java bridge from detector claims to tactics. No Android / model imports.
 * The image detector is a required future dependency; this class does NOT inspect pixels.
 */
public final class EnemyCoachPipeline {
    private final EnemyObservationGate gate=new EnemyObservationGate();
    public synchronized TacticalDecisionEngine.Decision ingest(EnemyObservationGate.Detection detection,
                                                               int ownHealthPercent,
                                                               int knownNearbyAllies) {
        EnemyObservationGate.Confirmation c=gate.observe(detection);
        if (!c.confirmed) return TacticalDecisionEngine.evaluate(null);
        TacticalDecisionEngine.State s=new TacticalDecisionEngine.State(
            ownHealthPercent,1,knownNearbyAllies,true,true,false,true,false);
        return TacticalDecisionEngine.evaluate(s);
    }
    public synchronized void reset(){gate.reset();}
}
