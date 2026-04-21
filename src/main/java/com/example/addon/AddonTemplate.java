package your.package.here;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class AddonTemplate extends MeteorAddon {
    @Override
    public void onInitialize() {
        // Register the debug hud
        Hud.get().register(PlayerDebugHud.INFO);
    }

    @Override public void onRegisterCategories() {}
    @Override public String getPackage() { return "your.package.here"; }

    public static class PlayerDebugHud extends HudElement {
        public static final HudElementInfo<PlayerDebugHud> INFO = new HudElementInfo<>(
            HudElement.GROUPS.PLAYER, "player-debug", "Displays Pos and Vel.", PlayerDebugHud::new
        );

        public PlayerDebugHud() { super(INFO); }

        @Override
        public void render(HudRenderer renderer) {
            if (mc.player == null) return;
            
            String pos = String.format("Pos: %.2f, %.2f, %.2f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            String vel = String.format("Vel: %.3f, %.3f, %.3f", mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);

            setSize(Math.max(renderer.textWidth(pos), renderer.textWidth(vel)), (renderer.textHeight() * 2) + 2);

            renderer.text(pos, x, y, Color.WHITE, true);
            renderer.text(vel, x, y + renderer.textHeight() + 2, Color.WHITE, true);
        }
    }
}
