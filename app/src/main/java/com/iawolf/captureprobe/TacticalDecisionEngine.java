package com.iawolf.captureprobe;

/**
 * Offline decision policy. Consumes validated observations, NEVER raw pixels.
 * UNKNOWN is not FALSE: missing data cannot justify an attack.
 */
public final class TacticalDecisionEngine {
    private TacticalDecisionEngine() {}
    public enum Action { RETREAT, RECOVER, FARM_SAFELY, CONSIDER_ATTACK, INSUFFICIENT_EVIDENCE }
    public static final int UNKNOWN = -1;
    public static final class State {
        public final int healthPercent, nearbyEnemies, nearbyAllies, enemyHealthPercent;
        public final boolean reliable, fresh, laneWaveVisible, enemyChampionConfirmed, abilitiesReady;
        public State(int healthPercent, int nearbyEnemies, int nearbyAllies,
                     int enemyHealthPercent, boolean reliable, boolean fresh,
                     boolean laneWaveVisible, boolean enemyChampionConfirmed, boolean abilitiesReady) {
            this.healthPercent=healthPercent; this.nearbyEnemies=nearbyEnemies;
            this.nearbyAllies=nearbyAllies; this.enemyHealthPercent=enemyHealthPercent;
            this.reliable=reliable; this.fresh=fresh; this.laneWaveVisible=laneWaveVisible;
            this.enemyChampionConfirmed=enemyChampionConfirmed; this.abilitiesReady=abilitiesReady;
        }
    }
    public static final class Decision {
        public final Action action;
        public final String rationale;
        public final boolean sufficientEvidence;
        Decision(Action action, String rationale, boolean sufficientEvidence) {
            this.action=action; this.rationale=rationale; this.sufficientEvidence=sufficientEvidence;
        }
        public String promptContext() {
            return "Evaluacion tactica de observaciones validadas: " + action.name()
                + ". Motivo: " + rationale + ". "
                + "No inventes datos del minimapa, cooldowns ni adversarios. "
                + "Es una sugerencia, no una orden automatica.";
        }
    }
    public static Decision evaluate(State s) {
        if(s==null || !s.reliable || !s.fresh)
            return unknown("Sin observaciones visuales fiables y recientes.");
        if(s.healthPercent < 0 || s.healthPercent > 100)
            return unknown("Vida propia desconocida o invalida.");
        if(s.nearbyEnemies < 0 || s.nearbyAllies < 0)
            return unknown("No hay recuento confirmado de aliados y enemigos cercanos.");
        if(s.healthPercent <= 30 && s.nearbyEnemies > 0)
            return new Decision(Action.RETREAT,"Vida baja y amenaza enemiga observada.",true);
        if(s.healthPercent <= 25)
            return new Decision(Action.RECOVER,"Vida critica; evitar exponerse.",true);
        if(s.nearbyEnemies > s.nearbyAllies + 1)
            return new Decision(Action.RETREAT,"Desventaja numerica observada.",true);
        if(s.enemyChampionConfirmed && s.nearbyEnemies == 1 && s.nearbyAllies >= 0
           && s.healthPercent >= 70 && s.enemyHealthPercent >= 0
           && s.enemyHealthPercent <= 25 && s.abilitiesReady)
            return new Decision(Action.CONSIDER_ATTACK,
                "Posible oportunidad: rival confirmado con poca vida, pero faltan riesgos no observados.",true);
        if(s.laneWaveVisible && s.healthPercent >= 50 && s.nearbyEnemies == 0)
            return new Decision(Action.FARM_SAFELY,
                "Oleada visible y ningun enemigo cercano detectado; ausencia visible no prueba seguridad total.",true);
        return unknown("Datos insuficientes para seleccionar una accion tactica.");
    }
    private static Decision unknown(String why) {
        return new Decision(Action.INSUFFICIENT_EVIDENCE,why,false);
    }
}
