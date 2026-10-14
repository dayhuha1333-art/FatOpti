package com.example.fatoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderEntityItemEvent;
import net.minecraftforge.client.event.RenderItemFrameEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "fatoptimizer", name = "FatOptimizer", version = "1.0",
        acceptedMinecraftVersions = "[1.12,1.13)", clientSideOnly = true)
public class FatOptimizer {

    private static final double ITEM_CULL_SQ = 32.0D * 32.0D;
    private static final double LIVING_CULL_SQ = 48.0D * 48.0D;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;

        Entity ent = event.getEntity();
        if (ent == player) return;
        if (ent instanceof EntityPlayer) return;
        if (ent.getDistanceSq(player) > LIVING_CULL_SQ) return;

        if (isHidden(mc, player, ent)) {
            event.setCanceled(true);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderItem(RenderEntityItemEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;

        EntityItem item = event.getEntityItem();
        if (item == null) return;
        if (item.getDistanceSq(player) > ITEM_CULL_SQ) return;

        if (isHidden(mc, player, item)) {
            event.setCanceled(true);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderItemFrame(RenderItemFrameEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;

        EntityItemFrame frame = event.getEntityItemFrame();
        if (frame == null) return;

        if (isHidden(mc, player, frame)) {
            event.setCanceled(true);
        }
    }

    private boolean isHidden(Minecraft mc, EntityPlayerSP player, Entity ent) {
        Vec3d eye = player.getPositionEyes(1.0F);
        Vec3d target = new Vec3d(ent.posX, ent.posY + ent.height * 0.5D, ent.posZ);
        RayTraceResult hit = mc.world.rayTraceBlocks(eye, target, false, true, false);
        return hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK;
    }
}
