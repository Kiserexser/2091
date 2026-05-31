package com.killaura.modules.combat;

import com.killaura.modules.Module;
import com.killaura.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class KillAura extends Module {

    public final FloatSetting   range      = flt("Range",      "Attack range",        3.0f, 1.0f, 6.0f, 0.01f);
    public final ModeSetting    targetMode = mode("Target",     "Target priority",     "Closest",
                                                  "Closest","LowestHP","HighestHP","LeastArmor");
    public final BooleanSetting players    = bool("Players",    "Attack players",      true);
    public final BooleanSetting mobs       = bool("Mobs",       "Attack mobs",         true);
    public final BooleanSetting animals    = bool("Animals",    "Attack animals",      false);
    public final BooleanSetting invisible  = bool("Invisible",  "Attack invisible",    false);
    public final FloatSetting   minDelay   = flt("MinDelay",    "Min ms between hits", 550f, 50f, 2000f, 10f);
    public final FloatSetting   maxDelay   = flt("MaxDelay",    "Max ms between hits", 650f, 50f, 2000f, 10f);
    public final BooleanSetting onlyFull   = bool("OnlyCooldown","Only on full CD",    true);
    public final ModeSetting    rotMode    = mode("Rotations",  "Rotation mode",       "Silent",
                                                  "Off","Smooth","Silent","Strict");
    public final FloatSetting   fovCheck   = flt("FOVCheck",    "Attack FOV",          180f, 0f, 180f, 5f);
    public final ModeSetting    bypass     = mode("Bypass",     "Anti-cheat profile",  "Grim",
                                                  "Grim","Vulcan","Polar","Matrix","Spartan","AAC","Verus","Custom");
    public final BooleanSetting losCheck   = bool("LOS",        "Line-of-sight check", false);
    public final BooleanSetting filterNpcs = bool("FilterNPCs", "Skip bots/NPCs",      false);

    private record Profile(
        float reachMin, float reachMax, long delayMin, long delayMax,
        float yawJitter, float pitchJitter, int rotTicks, boolean requireGround,
        boolean sendGroundPre, boolean sendMovePost, boolean reduceSpeed, float speedFactor,
        boolean zerohSpeed, boolean randomHitboxY, float hitboxXZJitter, boolean losRequired,
        int switchHitsMin, int switchHitsMax, boolean npcFilter,
        boolean sinusoidalCps, boolean newTargetDelay, long newTargetDelayMs) {}

    private long lastAttackMs = 0, currentDelay = 600;
    private LivingEntity currentTarget = null, previousTarget = null;
    private int hitsOnTarget = 0, maxHitsSwitch = 9, newTargetHits = 0;
    private float rotTargetYaw = 0, rotTargetPitch = 0;
    private int rotTicksLeft = 0;
    private boolean hasRotTarget = false;
    private float serverYaw = 0, serverPitch = 0;
    private double sinPhase = 0;
    private final Random rng = new Random();

    public KillAura() { super("KillAura", "Auto-attacks with per-AC bypass logic", Category.COMBAT, GLFW.GLFW_KEY_R); }

    @Override public void onEnable()  { hitsOnTarget = 0; newTargetHits = 0; }
    @Override public void onDisable() { currentTarget = null; hasRotTarget = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        Profile p = profile();
        if (hasRotTarget) stepRotation(mc, p);
        long now = System.currentTimeMillis();
        if (now - lastAttackMs < currentDelay) return;
        LivingEntity newTarget = findTarget(mc, p);
        if (newTarget != currentTarget) { previousTarget = currentTarget; currentTarget = newTarget; hitsOnTarget = 0; newTargetHits = 0; }
        if (currentTarget == null) return;
        if (p.newTargetDelay() && newTargetHits < 2 && previousTarget != null) {
            if (now - lastAttackMs < p.newTargetDelayMs() + rng.nextLong(50)) return;
        }
        if (hitsOnTarget >= maxHitsSwitch) { currentTarget = null; hitsOnTarget = 0; currentDelay = nextDelay(p); lastAttackMs = now; return; }
        if (p.requireGround() && !mc.player.isOnGround()) return;
        if ((p.losRequired() || losCheck.getValue()) && !hasLineOfSight(mc, currentTarget)) return;
        setRotationTarget(mc, currentTarget, p);
        if (onlyFull.getValue() && mc.player.getAttackCooldownProgress(0f) < 1.0f) return;
        if (fovCheck.getValue() < 180 && !isInFov(mc, currentTarget)) return;
        preAttack(mc, p);
        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.player.swingHand(Hand.MAIN_HAND);
        postAttack(mc, p);
        hitsOnTarget++; newTargetHits++; lastAttackMs = now; currentDelay = nextDelay(p); sinPhase += 0.3;
    }

    private void preAttack(MinecraftClient mc, Profile p) {
        if (p.sendGroundPre()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
            double ox = mc.player.getX() + (rng.nextDouble() - 0.5) * 0.05;
            double oz = mc.player.getZ() + (rng.nextDouble() - 0.5) * 0.05;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ox, mc.player.getY(), oz, true));
        }
        if (p.reduceSpeed()) { Vec3d v = mc.player.getVelocity(); mc.player.setVelocity(v.x * p.speedFactor(), v.y, v.z * p.speedFactor()); }
        if (p.zerohSpeed()) { Vec3d v = mc.player.getVelocity(); mc.player.setVelocity(0, v.y, 0); }
    }

    private void postAttack(MinecraftClient mc, Profile p) {
        if (p.sendMovePost()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        }
    }

    private void setRotationTarget(MinecraftClient mc, LivingEntity target, Profile p) {
        float[] ang = calcAngles(mc, target, p);
        rotTargetYaw = ang[0]; rotTargetPitch = ang[1]; rotTicksLeft = p.rotTicks(); hasRotTarget = true;
    }

    private void stepRotation(MinecraftClient mc, Profile p) {
        if (rotTicksLeft <= 0) { hasRotTarget = false; return; }
        float factor = 1.0f / rotTicksLeft;
        float newYaw   = lerpAngle(mc.player.getYaw(), rotTargetYaw, factor);
        float newPitch = MathHelper.clamp(lerpAngle(mc.player.getPitch(), rotTargetPitch, factor), -90, 90);
        switch (rotMode.getValue()) {
            case "Silent" -> {
                serverYaw = newYaw; serverPitch = newPitch;
                if (mc.getNetworkHandler() != null)
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(serverYaw, serverPitch, mc.player.isOnGround()));
            }
            case "Smooth", "Strict" -> { mc.player.setYaw(newYaw); mc.player.setPitch(newPitch); }
        }
        rotTicksLeft--;
    }

    private float[] calcAngles(MinecraftClient mc, LivingEntity target, Profile p) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d tp  = targetPoint(target, p);
        Vec3d dir = tp.subtract(eye);
        float yaw   = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
        float yj = p.yawJitter(), pj = p.pitchJitter();
        if (yj > 0) yaw   += (rng.nextFloat() - 0.5f) * 2 * yj;
        if (pj > 0) pitch += (rng.nextFloat() - 0.5f) * 2 * pj;
        return new float[]{ yaw, pitch };
    }

    private Vec3d targetPoint(LivingEntity entity, Profile p) {
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        double targetY = p.randomHitboxY() ? y + entity.getHeight() * (0.3 + rng.nextFloat() * 0.5) : y + entity.getHeight() * 0.55;
        double xOff = 0, zOff = 0;
        if (p.hitboxXZJitter() > 0) {
            float hw = entity.getWidth() / 2f;
            xOff = (rng.nextDouble() - 0.5) * 2 * Math.min(hw, p.hitboxXZJitter());
            zOff = (rng.nextDouble() - 0.5) * 2 * Math.min(hw, p.hitboxXZJitter());
        }
        return new Vec3d(x + xOff, targetY, z + zOff);
    }

    private LivingEntity findTarget(MinecraftClient mc, Profile p) {
        float r = effectiveRange(p);
        Box box = mc.player.getBoundingBox().expand(r);
        Vec3d eye = mc.player.getEyePos();
        List<LivingEntity> list = mc.world.getEntitiesByClass(LivingEntity.class, box, e -> isValid(mc, e, eye, r, p));
        if (list.isEmpty()) return null;
        return switch (targetMode.getValue()) {
            case "LowestHP"   -> list.stream().min(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            case "HighestHP"  -> list.stream().max(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            case "LeastArmor" -> list.stream().min(Comparator.comparingDouble(LivingEntity::getArmor)).orElse(null);
            default           -> list.stream().min(Comparator.comparingDouble(e -> eye.distanceTo(e.getEyePos()))).orElse(null);
        };
    }

    private boolean isValid(MinecraftClient mc, LivingEntity e, Vec3d eye, float r, Profile p) {
        if (e == mc.player || !e.isAlive() || e.isInvulnerable()) return false;
        if (!invisible.getValue() && e.isInvisible()) return false;
        if (p.npcFilter() || filterNpcs.getValue()) {
            String name = e.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("bot_") || name.contains("_npc") || name.startsWith("npc") || name.contains("anticheat")) return false;
        }
        if (e instanceof PlayerEntity pl) { if (!players.getValue() || pl.isCreative() || pl.isSpectator()) return false; }
        else if (e instanceof HostileEntity) { if (!mobs.getValue()) return false; }
        else if (e instanceof AnimalEntity)  { if (!animals.getValue()) return false; }
        return eye.distanceTo(e.getEyePos()) <= r;
    }

    private float effectiveRange(Profile p) {
        float spread = p.reachMax() - p.reachMin();
        if (spread > 0) return p.reachMin() + rng.nextFloat() * spread;
        return MathHelper.clamp(range.getValue(), p.reachMin(), p.reachMax());
    }

    private long nextDelay(Profile p) {
        maxHitsSwitch = p.switchHitsMin() + rng.nextInt(Math.max(1, p.switchHitsMax() - p.switchHitsMin() + 1));
        long min = p.delayMin(), max = p.delayMax();
        if (p.sinusoidalCps()) {
            double amplitude = (max - min) / 2.0, base = (max + min) / 2.0;
            return Math.max(50, (long)(base + amplitude * Math.sin(sinPhase)));
        }
        long extra = bypass.is("AAC") ? 30 + rng.nextLong(40) : 0;
        return min + (long)(rng.nextFloat() * (max - min)) + extra;
    }

    private boolean isInFov(MinecraftClient mc, LivingEntity target) {
        float fov = fovCheck.getValue();
        if (fov >= 180) return true;
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d dir  = targetPoint(target, profile()).subtract(mc.player.getEyePos()).normalize();
        return Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(dir), -1, 1))) <= fov;
    }

    private boolean hasLineOfSight(MinecraftClient mc, LivingEntity target) {
        Vec3d from = mc.player.getEyePos();
        Vec3d to   = target.getEyePos();
        HitResult hit = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private float lerpAngle(float from, float to, float t) {
        float diff = ((to - from) % 360 + 540) % 360 - 180;
        return from + diff * t;
    }

    private Profile profile() {
        return switch (bypass.getValue()) {
            case "Grim"    -> new Profile(3.040f,3.045f,500,650,1.5f,1.0f,1,true,true,false,false,1f,false,false,0f,false,8,10,false,false,false,0);
            case "Vulcan"  -> new Profile(3.0f,3.15f,450,650,1.5f,1.0f,3,false,false,true,true,0.6f,false,false,0f,true,6,8,false,false,false,0);
            case "Polar"   -> new Profile(3.0f,3.40f,500,700,1.5f,1.0f,2,true,false,false,false,1f,false,true,0f,false,7,10,false,false,false,0);
            case "Matrix"  -> new Profile(3.0f,3.44f,400,550,2.0f,1.5f,4,false,false,false,true,0.8f,false,false,0f,false,6,8,false,false,false,0);
            case "Spartan" -> new Profile(3.0f,3.44f,500,650,1.5f,1.0f,1,true,false,false,false,1f,false,true,0.15f,false,7,9,true,false,false,0);
            case "AAC"     -> new Profile(3.0f,3.44f,500,700,2.0f,2.0f,1,true,false,false,false,1f,false,false,0f,false,8,10,false,false,false,0);
            case "Verus"   -> new Profile(3.0f,3.44f,500,800,1.0f,1.0f,2,false,false,false,false,1f,true,false,0f,false,7,9,true,true,true,300);
            default        -> new Profile(3.0f,3.44f,500,700,1.5f,1.0f,2,false,false,false,false,1f,false,false,0f,false,8,10,false,false,false,0);
        };
    }
}
