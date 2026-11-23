package com.iawolf.captureprobe;

/**
 * Lab10: route two-frame CONFIRMED enemy detections to A/B/C advice.
 * No image inference, Android capture, champion skill recognition or automated controls.
 * Each instance is match-scoped; call reset at match end.
 */
public final class EnemyPlanPipeline {
    private final EnemyObservationGate gate = new EnemyObservationGate();

    public synchronized TacticalPlanCoordinator.Result ingest(
            EnemyObservationGate.Detection frame,
            int ownHealthPercent, int knownNearbyAllies,
            TacticalPlanCoordinator.Intent playerIntent) {
        EnemyObservationGate.Confirmation confirmation = gate.observe(frame);
        if (!confirmation.confirmed) return TacticalPlanCoordinator.plan(null);
        TacticalDecisionEngine.State state = new TacticalDecisionEngine.State(
            ownHealthPercent, 1, knownNearbyAllies, true, true,
            false, true, false);
        return TacticalPlanCoordinator.plan(new TacticalPlanCoordinator.Context(
            state, playerIntent, false, false, false));
    }
    public synchronized void reset() { gate.reset(); }
}
