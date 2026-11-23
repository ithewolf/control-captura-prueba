package com.iawolf.captureprobe;

/**
 * Lab10: bounded, inspectable plan selection over CONFIRMED observations.
 * Does not process images, track players, trigger inputs or claim abilities ready.
 * Plans are advice only; actual user action is inferred by an external observer.
 */
public final class TacticalPlanCoordinator {
    private TacticalPlanCoordinator() {}
    public enum Intent { ATTACK, RETREAT, FARM, UNKNOWN }
    public enum Plan { DEFEND_AND_RETREAT, SUPPORT_RETREAT, OBSERVE_AND_FARM,
        HOLD_POSITION, REASSESS, ATTACK_WITH_ESCAPE }
    public static final class Context {
        public final TacticalDecisionEngine.State observed;
        public final Intent observedPlayerIntent;
        public final boolean allyInDanger, escapeKnown, objectiveContested;
        public Context(TacticalDecisionEngine.State observed, Intent observedPlayerIntent,
                       boolean allyInDanger, boolean escapeKnown, boolean objectiveContested) {
            this.observed=observed;
            this.observedPlayerIntent=observedPlayerIntent == null ? Intent.UNKNOWN : observedPlayerIntent;
            this.allyInDanger=allyInDanger; this.escapeKnown=escapeKnown;
            this.objectiveContested=objectiveContested;
        }
    }
    public static final class Result {
        public final Plan primary, alternative, contingency;
        public final boolean actionable;
        public final String evidence;
        private Result(Plan p,Plan a,Plan c,boolean ok,String evidence) {
            this.primary=p;this.alternative=a;this.contingency=c;
            this.actionable=ok;this.evidence=evidence;
        }
    }
    public static Result plan(Context c) {
        if (c == null || c.observed == null || !c.observed.fresh || !c.observed.reliable)
            return new Result(Plan.REASSESS,Plan.HOLD_POSITION,Plan.REASSESS,false,
                    "Observacion no validada o desactualizada.");
        TacticalDecisionEngine.Decision core=TacticalDecisionEngine.evaluate(c.observed);
        if (!core.sufficientEvidence) 
            return new Result(Plan.REASSESS,Plan.HOLD_POSITION,Plan.REASSESS,false,
                    core.rationale);
        if (core.action==TacticalDecisionEngine.Action.RETREAT
                || core.action==TacticalDecisionEngine.Action.RECOVER) {
            // A player attacking despite retreat advice gets a safe escape contingency.
            return new Result(Plan.DEFEND_AND_RETREAT,Plan.SUPPORT_RETREAT,
                Plan.DEFEND_AND_RETREAT,true,"Riesgo validado; observar la accion real del jugador.");
        }
        if (c.allyInDanger && c.escapeKnown && c.observed.nearbyAllies >= 0)
            return new Result(Plan.SUPPORT_RETREAT,Plan.HOLD_POSITION,
                Plan.DEFEND_AND_RETREAT,true,"Aliado observado en peligro; ruta de salida conocida.");
        if (core.action==TacticalDecisionEngine.Action.FARM_SAFELY)
            return new Result(Plan.OBSERVE_AND_FARM,Plan.HOLD_POSITION,
                Plan.DEFEND_AND_RETREAT,true,"No hay enemigo observado en el area examinada.");
        if (core.action==TacticalDecisionEngine.Action.IDENTIFY_ENEMY)
            return new Result(Plan.HOLD_POSITION,Plan.DEFEND_AND_RETREAT,
                Plan.REASSESS,true,"Enemigo confirmado; falta evidencia para ordenar una ofensiva.");
        return new Result(Plan.REASSESS,Plan.HOLD_POSITION,Plan.REASSESS,false,"Sin evidencia tactica.");
    }
}
