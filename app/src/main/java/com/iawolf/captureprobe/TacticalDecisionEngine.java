package com.iawolf.captureprobe;

/**
 * Offline decision policy. Consumes validated observations, NEVER raw pixels.
 * UNKNOWN is not FALSE: missing data cannot justify an attack.
 */
public final class TacticalDecisionEngine {
    private TacticalDecisionEngine() {}
    public enum Action { RETREAT, RECOVER, FARM_SAFELY, IDENTIFY_ENEMY, INSUFFICIENT_EVIDENCE }
    public static final int UNKNOWN = -1;
    public static final class State {
        public final int healthPercent, nearbyEnemies, nearbyAllies;
        public final boolean reliable, fresh, laneWaveVisible, enemyChampionConfirmed, abilitiesReady;
        public State(int healthPercent, int nearbyEnemies, int nearbyAllies,
                     boolean reliable, boolean fresh,
                     boolean laneWaveVisible, boolean enemyChampionConfirmed, boolean abilitiesReady) {
            this.healthPercent=healthPercent; this.nearbyEnemies=nearbyEnemies;
            this.nearbyAllies=nearbyAllies;
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
                + "No inventes datos del minimapa, cooldowns ni adversarios. " + "No requieras porcentajes de HP ni mana enemigos. "
                + "Es una sugerencia, no una orden automatica.";
        }
    }
    public static Decision evaluate(State s) {
        if (s == null || !s.reliable || !s.fresh)
            return unknown("No hay observaciones recientes y verificadas.");
        if (s.nearbyEnemies < UNKNOWN || s.nearbyAllies < UNKNOWN)
            return unknown("Cantidad de personajes invalida.");
        if (s.healthPercent < UNKNOWN || s.healthPercent > 100)
            return unknown("Vida propia invalida.");
        // An identified enemy does NOT require enemy HP/MP, or even player HP.
        // Do not equate unknown enemy count with zero enemies.
        if (s.enemyChampionConfirmed && s.nearbyEnemies == 0)
            return unknown("Contradiccion entre identidad enemiga y recuento.");
        if (s.nearbyEnemies >= 1 && s.enemyChampionConfirmed) {
            if (s.healthPercent != UNKNOWN && s.healthPercent <= 30)
                return new Decision(Action.RETREAT, "Enemigo identificado y vida propia baja.", true);
            if (s.nearbyAllies != UNKNOWN && s.nearbyEnemies > s.nearbyAllies + 1)
                return new Decision(Action.RETREAT, "Enemigo identificado y desventaja numerica.", true);
            return new Decision(Action.IDENTIFY_ENEMY,
                "Campeon enemigo visible e identificado: describir ubicacion verificada sin inventar HP ni mana.",
                true);
        }
        // Unknown own HP may not block reporting an enemy, but does block health advice.
        if (s.healthPercent != UNKNOWN && s.healthPercent <= 25)
            return new Decision(Action.RECOVER, "Vida propia critica.", true);
        if (s.healthPercent != UNKNOWN && s.healthPercent >= 50
                && s.nearbyEnemies == 0 && s.laneWaveVisible)
            return new Decision(Action.FARM_SAFELY,
                "Oleada observada y sin enemigo detectado en el area examinada; no significa mapa seguro.", true);
        return unknown("No hay datos suficientes para un consejo tactico.");
    }
    private static Decision unknown(String why) {
        return new Decision(Action.INSUFFICIENT_EVIDENCE,why,false);
    }
}
