package com.example.fatoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@Mod(modid = "fatoptimizer", name = "FatOptimizer", version = "1.0",
        acceptedMinecraftVersions = "[1.12,1.13)", clientSideOnly = true)
public class FatOptimizer {

    private static final int FPS_LOW = 40;
    private static final int FPS_HIGH = 75;
    private static final int CHECK_INTERVAL = 40;
    private static final double CULL_DISTANCE_SQ = 64.0D * 64.0D;

    private int tickCounter = 0;
    private int fpsAccum = 0;
    private int fpsSamples = 0;
    private int savedRenderDistance = -1;
    private int savedParticleSetting = -1;
    private int savedClouds = -1;
    private boolean savedEntityShadows = true;
    private boolean degraded = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        fpsAccum += mc.getDebugFPS();
        fpsSamples++;

        if (++tickCounter < CHECK_INTERVAL) return;

        int avgFps = fpsSamples == 0 ? 0 : fpsAccum / fpsSamples;
        fpsAccum = 0;
        fpsSamples = 0;
        tickCounter = 0;

        GameSettings gs = mc.gameSettings;

        if (!degraded && avgFps > 0 && avgFps < FPS_LOW) {
            savedRenderDistance = gs.renderDistanceChunks;
            savedParticleSetting = gs.particleSetting;
            savedClouds = gs.clouds;
            savedEntityShadows = gs.entityShadows;

            gs.renderDistanceChunks = Math.max(2, savedRenderDistance - 2);
            gs.particleSetting = Math.min(2, savedParticleSetting + 1);
            gs.clouds = 0;
            gs.entityShadows = false;
            degraded = true;
        } else if (degraded && avgFps > FPS_HIGH) {
            if (savedRenderDistance > 0) gs.renderDistanceChunks = savedRenderDistance;
            if (savedParticleSetting >= 0) gs.particleSetting = savedParticleSetting;
            if (savedClouds >= 0) gs.clouds = savedClouds;
            gs.entityShadows = savedEntityShadows;
            degraded = false;
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;

        Vec3d eye = player.getPositionEyes(event.getPartialTicks());
        List<Entity> entities = mc.world.getLoadedEntityList();

        for (Entity ent : entities) {
            if (ent == player) continue;
            if (ent instanceof EntityPlayer) continue;
            if (ent.getDistanceSq(player) > CULL_DISTANCE_SQ) continue;

            Vec3d target = new Vec3d(ent.posX, ent.posY + ent.height * 0.5D, ent.posZ);
            RayTraceResult hit = mc.world.rayTraceBlocks(eye, target, false, true, false);
            boolean visible = (hit == null || hit.typeOfHit == RayTraceResult.Type.MISS);

            if (!visible) {
                ent.setInvisible(ent.isInvisible());
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;

        Entity ent = event.getEntity();
        if (ent == player) return;
        if (ent.getDistanceSq(player) > CULL_DISTANCE_SQ) return;

        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d target = new Vec3d(ent.posX, ent.posY + ent.height * 0.5D, ent.posZ);
        RayTraceResult hit = mc.world.rayTraceBlocks(eye, target, false, true, false);
        boolean visible = (hit == null || hit.typeOfHit == RayTraceResult.Type.MISS);

        if (!visible) {
            event.setCanceled(true);
        }
    }
}
