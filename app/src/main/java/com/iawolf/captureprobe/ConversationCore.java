package com.iawolf.captureprobe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LAB6: conversation and bounded short-term memory only. No fake language model:
 * an actual on-device inference backend must implement LocalTextModel.
 * This class is intentionally Android-independent so it can be tested on a JVM.
 */
public final class ConversationCore {
    public interface LocalTextModel {
        String generate(List<Message> messages) throws Exception;
    }

    public static final class Message {
        public final String role;
        public final String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static final class VisionStatus {
        public final boolean capturing;
        public final boolean recentFrame;
        public final boolean darkFrame;
        public VisionStatus(boolean capturing, boolean recentFrame, boolean darkFrame) {
            this.capturing = capturing;
            this.recentFrame = recentFrame;
            this.darkFrame = darkFrame;
        }
    }

    public static final class Answer {
        public final boolean ready;
        public final String text;
        public final String detail;
        private Answer(boolean ready, String text, String detail) {
            this.ready = ready;
            this.text = text;
            this.detail = detail;
        }
        private static Answer ok(String text) { return new Answer(true, text, "generated"); }
        private static Answer unavailable(String detail) { return new Answer(false, "", detail); }
    }

    private static final int MAX_PAIRS = 6;
    // Keep six pairs on disk, send only the latest three to reduce prompt work.
    private static final int CONTEXT_PAIRS = 3;
    private static final int MAX_INPUT_CHARS = 600;
    private static final int MAX_OUTPUT_CHARS = 1100;
    private final LocalTextModel model;
    private final Map<String, ArrayDeque<Message>> history = new HashMap<>();

    public ConversationCore(LocalTextModel model) { this.model = model; }

    public synchronized Answer respond(String mode, String heard, VisionStatus vision) {
        if (model == null) return Answer.unavailable("Modelo conversacional local no instalado");
        String input = clean(heard, MAX_INPUT_CHARS);
        if (input.isEmpty()) return Answer.unavailable("No se recibio una pregunta transcrita");
        String m = normalizeMode(mode);
        ArrayDeque<Message> turns = history.computeIfAbsent(m, key -> new ArrayDeque<>());
        ArrayList<Message> prompt = new ArrayList<>();
        prompt.add(new Message("system", systemPrompt(m, vision)));
        int skip = Math.max(0, turns.size() - CONTEXT_PAIRS * 2);
        int index = 0;
        for (Message turn : turns) if (index++ >= skip) prompt.add(turn);
        prompt.add(new Message("user", input));
        try {
            String reply = clean(model.generate(Collections.unmodifiableList(prompt)), MAX_OUTPUT_CHARS);
            if (reply.isEmpty()) return Answer.unavailable("El modelo no genero una respuesta valida");
            turns.addLast(new Message("user", input));
            turns.addLast(new Message("assistant", reply));
            while (turns.size() > MAX_PAIRS * 2) { turns.removeFirst(); turns.removeFirst(); }
            return Answer.ok(reply);
        } catch (Exception error) {
            // Do not store failed conversations and do not pretend a canned line is AI.
            return Answer.unavailable("Fallo del modelo local: " + error.getClass().getSimpleName());
        }
    }

    /** Persist the bounded conversation locally, never including the system prompt. */
    public synchronized String exportHistory(String mode) {
        ArrayDeque<Message> turns = history.get(normalizeMode(mode));
        if (turns == null || turns.isEmpty()) return "";
        StringBuilder out = new StringBuilder("CONTROL_MEMORY_V1\n");
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        for (Message turn : turns) {
            if (!"user".equals(turn.role) && !"assistant".equals(turn.role)) continue;
            out.append(turn.role).append(':')
               .append(encoder.encodeToString(turn.content.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
               .append('\n');
        }
        return out.toString();
    }

    /** Defensive restore: malformed, oversized or unpaired history is ignored. */
    public synchronized boolean importHistory(String mode, String saved) {
        String key = normalizeMode(mode);
        if (saved == null || saved.isEmpty()) { history.remove(key); return true; }
        if (saved.length() > 40000 || !saved.startsWith("CONTROL_MEMORY_V1\n")) return false;
        String[] lines = saved.split("\\n", -1);
        if (lines.length < 3 || lines.length > MAX_PAIRS * 2 + 2) return false;
        ArrayDeque<Message> restored = new ArrayDeque<>();
        try {
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].isEmpty()) {
                    // Only the terminal newline is allowed; blank turns are corruption.
                    if (i == lines.length - 1) continue;
                    return false;
                }
                String expected = restored.size() % 2 == 0 ? "user:" : "assistant:";
                if (!lines[i].startsWith(expected)) return false;
                byte[] bytes = java.util.Base64.getDecoder().decode(lines[i].substring(expected.length()));
                String decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (!java.util.Arrays.equals(bytes, decoded.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return false;
                int limit = expected.equals("user:") ? MAX_INPUT_CHARS : MAX_OUTPUT_CHARS;
                if (decoded.isEmpty() || decoded.length() > limit) return false;
                restored.addLast(new Message(expected.substring(0,expected.length()-1), decoded));
            }
        } catch (IllegalArgumentException ex) { return false; }
        if (restored.size() % 2 != 0 || restored.size() > MAX_PAIRS * 2) return false;
        history.put(key, restored);
        return true;
    }

    public synchronized int rememberedPairs(String mode) {
        ArrayDeque<Message> turns = history.get(normalizeMode(mode));
        return turns == null ? 0 : turns.size() / 2;
    }

    public synchronized void clear(String mode) {
        history.remove(normalizeMode(mode));
    }

    private static String normalizeMode(String mode) {
        return "companero".equalsIgnoreCase(mode) ? "companero" : "coach";
    }

    private static String clean(String text, int max) {
        if (text == null) return "";
        String normalized = text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ")
                .replaceAll("\\s+", " ").trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private static String systemPrompt(String mode, VisionStatus vision) {
        String personality = "Sos CONTROL, compañero original de Wolf. Hablás en español rioplatense natural, "
                + "con voz conceptual masculina, segura, serena, calculadora y cálida cuando corresponde. "
                + "Respondé de forma breve y específica a lo que se pregunta; no repitas saludos ni muletillas. "
                + "No afirmes saber algo que no sabés. No te presentes como ChatGPT, Luna ni como otro personaje. ";
        String focus = "companero".equals(mode)
                ? "Modo Compañero: conversá sobre temas cotidianos y seguí el contexto de la charla. "
                : "Modo Coach Wild Rift: orientá decisiones con lenguaje correcto del juego, "
                + "sin inventar información del mapa, campeón, builds ni tiempos de enfriamiento. ";
        String visual;
        if (vision == null || !vision.capturing) {
            visual = "No hay captura de pantalla activa. No digas que estás mirando el juego. ";
        } else if (!vision.recentFrame) {
            visual = "La captura está activa, pero no hay fotograma reciente verificable. "
                    + "No digas qué sucede en el juego. ";
        } else if (vision.darkFrame) {
            visual = "Hay un fotograma oscuro sin identificación fiable. No inventes detalles. ";
        } else {
            visual = "Hay fotograma reciente pero el sistema aún NO reconoce campeones, enemigos ni eventos. "
                    + "No describas elementos visuales no identificados. ";
        }
        return personality + focus + visual
                + "Si necesitás datos para recomendar, pedilos brevemente. No afirmes conciencia ni sentimientos propios.";
    }
}
