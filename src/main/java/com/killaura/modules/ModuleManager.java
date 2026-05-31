package com.killaura.modules;

import com.killaura.KillAuraClient;
import com.killaura.gui.ClickGUI;
import com.killaura.modules.combat.*;
import com.killaura.modules.misc.*;
import com.killaura.modules.movement.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    private final KeyBinding guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.killaura-client.open_gui", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, "category.killaura-client.modules"));

    public void init() {
        register(new KillAura());
        register(new Criticals());
        register(new AntiKnockback());
        register(new AutoClicker());
        register(new Reach());
        register(new Sprint());
        register(new Velocity());
        register(new NoFall());
        register(new ESP());
        register(new NameTags());
        register(new Timer());
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void register(Module module) { modules.add(module); }

    private void onTick(MinecraftClient mc) {
        if (guiKey.wasPressed() && mc.currentScreen == null) { mc.setScreen(new ClickGUI()); return; }
        if (mc.player == null) return;
        for (Module module : modules) {
            if (module.getKeyBinding().wasPressed()) {
                module.toggle();
                String status = module.isEnabled() ? "§aEnabled" : "§cDisabled";
                mc.player.sendMessage(Text.literal("§7[§6KA§7] §f" + module.getName() + " " + status), true);
            }
        }
        for (Module module : modules) {
            if (module.isEnabled()) {
                try { module.onTick(); }
                catch (Exception e) { KillAuraClient.LOGGER.error("[{}] Tick error: {}", module.getName(), e.getMessage()); }
            }
        }
    }

    public List<Module> getModules() { return modules; }
    public List<Module> getEnabledModules() { return modules.stream().filter(Module::isEnabled).toList(); }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) if (clazz.equals(m.getClass())) return (T) m;
        return null;
    }
}
