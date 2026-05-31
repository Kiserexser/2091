package com.killaura;

import com.killaura.config.Config;
import com.killaura.gui.HudRenderer;
import com.killaura.modules.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class KillAuraClient implements ClientModInitializer {

    public static final String MOD_ID = "killaura-client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KillAuraClient instance;
    private ModuleManager moduleManager;
    private Config config;
    private HudRenderer hudRenderer;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("[KillAura Client] Initializing...");
        config = new Config();
        config.load();
        moduleManager = new ModuleManager();
        moduleManager.init();
        hudRenderer = new HudRenderer();
        hudRenderer.init();
        LOGGER.info("[KillAura Client] Loaded {} modules.", moduleManager.getModules().size());
    }

    public static KillAuraClient getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public Config getConfig() { return config; }
    public HudRenderer getHudRenderer() { return hudRenderer; }
}
