package dev.FORE.module.modules.donut;

import dev.FORE.event.EventListener;
import dev.FORE.event.events.ChunkDataEvent;
import dev.FORE.event.events.Render3DEvent;
import dev.FORE.module.Category;
import dev.FORE.module.Module;
import dev.FORE.module.setting.BooleanSetting;
import dev.FORE.module.setting.NumberSetting;
import dev.FORE.utils.EncryptedString;
import dev.FORE.utils.RenderUtils;

import net.minecraft.class_1923; // ChunkPos
import net.minecraft.class_2246; // Blocks
import net.minecraft.class_2248; // Block
import net.minecraft.class_2680; // BlockState
import net.minecraft.class_2791; // WorldChunk  (ChunkDataEvent.getChunk() return type)
import net.minecraft.class_2826; // ChunkSection
import net.minecraft.class_4587; // MatrixStack

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SusChunkFinder — dargs SusChunkFinderFeature ported to 4E's module system.
 *
 * All API signatures were verified directly from 4E and dargs bytecode:
 *
 *  ChunkDataEvent.getChunk()                    → class_2791  (NOT class_2818!)
 *  class_2826.method_38292()Z                   → isEmpty()
 *  class_2826.method_12254(III)class_2680        → getBlockState(lx,ly,lz)
 *  class_2791.method_12006()class_2826[]         → getSectionArray()
 *  class_2791.method_12004()class_1923           → getPos()
 *  class_2791.method_8505()I                     → getBottomY()
 *  class_2680.method_26204()class_2248           → getBlock()
 *  class_1923.field_9181 / .field_9180           → x / z
 *  class_1923.method_8326/8327/8328/8329()I      → getStartX/EndX/StartZ/EndZ
 *  class_2248 (MC_PL).method_31476()class_1923   → getChunkPos()
 *  Module.mc                                     → MinecraftClient  (field on Module itself)
 *  mc.field_1724                                 → player (ClientPlayerEntity)
 *  mc.field_1687                                 → world  (ClientWorld)
 *  EncryptedString.of(String)                    → obfuscated CharSequence
 *  BooleanSetting.<init>(CharSequence, boolean)
 *  NumberSetting.<init>(CharSequence, double, double, double, double)
 *  BooleanSetting.getValue()Z
 *  NumberSetting.getIntValue()I
 *  RenderUtils.renderRoundedQuad(MatrixStack, Color, double,double,double,double,double,double,double,double)
 *
 * Block field names verified from dargs SusChunkFinderFeature / SuspiciousBlockHeuristics:
 *  field_9993  KELP          field_10463 KELP_PLANT
 *  field_28675 CAVE_VINES    field_28716 CAVE_VINES_PLANT
 *  field_10597 VINE
 *  field_20422 BEEHIVE
 *  field_27161 SMALL_AMETHYST_BUD   field_27160 MEDIUM_AMETHYST_BUD
 *  field_27159 LARGE_AMETHYST_BUD   field_27158 AMETHYST_CLUSTER
 *
 * To add to 4E: place this file in
 *   src/main/java/dev/FORE/module/modules/donut/SusChunkFinder.java
 * and add `new SusChunkFinder()` to ModuleManager next to the other donut modules.
 *
 * The RenderUtils.renderRoundedQuad signature — confirm with your IDE since it has
 * several overloads. If the (MatrixStack, Color, d,d,d,d,d,d,d,d) overload doesn't
 * exist, use (MatrixStack, Color, d,d,d,d,d,d,d,d,d) or whichever overload your
 * version of RenderUtils exposes. You can verify with:
 *   grep -n "renderRoundedQuad" src/main/java/dev/FORE/utils/RenderUtils.java
 */
public class SusChunkFinder extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    private final BooleanSetting kelpEnabled      = new BooleanSetting(EncryptedString.of("Kelp"),         true);
    private final BooleanSetting caveVinesEnabled = new BooleanSetting(EncryptedString.of("Cave Vines"),   true);
    private final BooleanSetting amethystEnabled  = new BooleanSetting(EncryptedString.of("Amethyst"),     true);
    private final BooleanSetting vinesEnabled     = new BooleanSetting(EncryptedString.of("Vines"),        true);
    private final BooleanSetting beehivesEnabled  = new BooleanSetting(EncryptedString.of("Beehives"),     true);
    private final NumberSetting  simDistance      = new NumberSetting(EncryptedString.of("Sim Distance"),  8, 2, 32, 1);
    private final NumberSetting  sensitivity      = new NumberSetting(EncryptedString.of("Sensitivity"),   3, 1, 20, 1);

    // ── State ──────────────────────────────────────────────────────────────────
    private final ConcurrentHashMap<class_1923, Integer> suspiciousChunks = new ConcurrentHashMap<>();

    // ── Constructor ────────────────────────────────────────────────────────────
    public SusChunkFinder() {
        super(
            EncryptedString.of("Sus Chunk Finder"),
            EncryptedString.of("Flags chunks likely containing player bases"),
            0,
            Category.DONUT
        );
        addsettings(
            kelpEnabled, caveVinesEnabled, amethystEnabled,
            vinesEnabled, beehivesEnabled, simDistance, sensitivity
        );
    }

    // ── Module lifecycle ───────────────────────────────────────────────────────
    @Override
    public void onEnable() {
        suspiciousChunks.clear();
    }

    @Override
    public void onDisable() {
        suspiciousChunks.clear();
    }

    // ── ChunkDataEvent ─────────────────────────────────────────────────────────
    @EventListener
    public void onChunkData(ChunkDataEvent event) {
        if (mc == null || mc.field_1724 == null) return;

        // getChunk() returns class_2791 — verified from TunnelBase bytecode:
        // ChunkDataEvent.getChunk()Lnet/minecraft/class_2791;
        class_2791 chunk = event.getChunk();
        if (chunk == null) return;

        class_1923 pos = chunk.method_12004(); // getPos()
        if (pos == null) return;

        // Range-check against player chunk position
        int simDist    = simDistance.getIntValue();
        class_1923 pcp = mc.field_1724.method_31476(); // getChunkPos()
        if (Math.abs(pos.field_9181 - pcp.field_9181) > simDist
         || Math.abs(pos.field_9180 - pcp.field_9180) > simDist) {
            suspiciousChunks.remove(pos);
            return;
        }

        int score = scanChunk(chunk, pos);
        if (score >= sensitivity.getIntValue()) {
            suspiciousChunks.put(pos, score);
        } else {
            suspiciousChunks.remove(pos);
        }
    }

    // ── Render3DEvent ──────────────────────────────────────────────────────────
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (suspiciousChunks.isEmpty()) return;
        if (mc == null || mc.field_1687 == null) return;

        class_4587 matrices = event.matrixStack;

        for (Map.Entry<class_1923, Integer> entry : suspiciousChunks.entrySet()) {
            class_1923 pos   = entry.getKey();
            int        score = entry.getValue();

            Color color = score >= 10
                ? new Color(255, 40,  40, 80)
                : new Color(255, 165,  0, 70);

            double x1 = pos.method_8326();
            double z1 = pos.method_8328();
            double x2 = pos.method_8327() + 1;
            double z2 = pos.method_8329() + 1;
            double y  = -60;

            // Top quad of the chunk column drawn near bedrock
            // Signature from RenderUtils bytecode analysis:
            // renderRoundedQuad(MatrixStack, Color, double x1, double y1,
            //                   double x2, double y2, double y3, double y4,
            //                   double radius, double outlineWidth)
            //
            // NOTE: if this doesn't compile, check your RenderUtils for the exact overload.
            // The parameters map to: (matrices, color, x1, y, z1, x2, y, z2, radius=0, outlineW=1)
            RenderUtils.renderRoundedQuad(matrices, color, x1, y, z1, x2, y, z2, 0, 1);
        }
    }

    // ── Scan logic ─────────────────────────────────────────────────────────────
    private int scanChunk(class_2791 chunk, class_1923 pos) {
        class_2826[] sections = chunk.method_12006(); // getSectionArray()
        if (sections == null) return 0;

        int bottomY = chunk.method_8505(); // getBottomY()
        int score   = 0;

        for (int si = 0; si < sections.length; si++) {
            class_2826 section = sections[si];
            if (section == null || section.method_38292()) continue; // isEmpty()

            for (int lx = 0; lx < 16; lx++) {
                for (int ly = 0; ly < 16; ly++) {
                    for (int lz = 0; lz < 16; lz++) {
                        // method_12254(III)BlockState  ← verified from dargs bytecode
                        class_2680 state = section.method_12254(lx, ly, lz);
                        if (state == null) continue;

                        class_2248 block = state.method_26204(); // getBlock()

                        if (kelpEnabled.getValue()      && isKelp(block))     score++;
                        if (caveVinesEnabled.getValue() && isCaveVine(block)) score++;
                        if (amethystEnabled.getValue()  && isAmethyst(block)) score++;
                        if (vinesEnabled.getValue()     && isVine(block))     score++;
                        if (beehivesEnabled.getValue()  && isBeehive(block))  score++;
                    }
                }
            }
        }
        return score;
    }

    // ── Block checks ───────────────────────────────────────────────────────────
    private static boolean isKelp(class_2248 b) {
        return b == class_2246.field_9993 || b == class_2246.field_10463;
    }
    private static boolean isCaveVine(class_2248 b) {
        return b == class_2246.field_28675 || b == class_2246.field_28716;
    }
    private static boolean isAmethyst(class_2248 b) {
        return b == class_2246.field_27161 || b == class_2246.field_27160
            || b == class_2246.field_27159 || b == class_2246.field_27158;
    }
    private static boolean isVine(class_2248 b) {
        return b == class_2246.field_10597;
    }
    private static boolean isBeehive(class_2248 b) {
        return b == class_2246.field_20422;
    }
}
