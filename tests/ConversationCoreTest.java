import com.iawolf.captureprobe.ConversationCore;
import java.util.ArrayList;
import java.util.List;

public final class ConversationCoreTest {
    private static int checks = 0;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ConversationCore.VisionStatus none = new ConversationCore.VisionStatus(false,false,false);
        ConversationCore absent = new ConversationCore(null);
        check(!absent.respond("coach", "hola", none).ready, "missing model must not pretend to reply");

        List<List<ConversationCore.Message>> captured = new ArrayList<>();
        ConversationCore core = new ConversationCore(messages -> {
            captured.add(new ArrayList<>(messages));
            return "Respuesta generada " + captured.size();
        });
        check(core.respond("companero", "¿Cómo estás?", none).ready, "generated reply");
        check(core.respond("companero", "¿Qué te pregunté?", none).ready, "second reply");
        check(captured.get(1).stream().anyMatch(m -> m.role.equals("user") && m.content.contains("Cómo")), "memory carries prior question");
        check(captured.get(1).stream().anyMatch(m -> m.role.equals("assistant") && m.content.equals("Respuesta generada 1")), "memory carries prior answer");
        check(core.rememberedPairs("companero") == 2, "pair count");
        check(core.rememberedPairs("coach") == 0, "coach and companion are isolated");
        check(core.respond("coach", "¿Ves mi pantalla?", new ConversationCore.VisionStatus(true,true,false)).ready, "coach reply");
        check(captured.get(2).get(0).content.contains("NO reconoce campeones"), "do not fabricate vision");
        check(!captured.get(2).get(0).content.contains("Modo Compañero"), "mode isolation");
        for(int i=0;i<8;i++)core.respond("companero", "nueva pregunta "+i, none);
        check(core.rememberedPairs("companero") == 6, "memory remains bounded");
        core.clear("companero");
        check(core.rememberedPairs("companero") == 0, "clear memory");
        ConversationCore failing = new ConversationCore(messages -> { throw new IllegalStateException("no runtime"); });
        check(!failing.respond("coach", "hola", none).ready, "backend failure must be explicit");
        check(failing.rememberedPairs("coach") == 0, "failed call does not enter history");
        check(!core.respond("coach", "  ", none).ready, "blank is ignored");
        System.out.println("ConversationCoreTest: " + checks + " assertions OK");
    }
}
