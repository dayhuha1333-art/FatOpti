package com.example.fatoptimizer.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    private static final double CULL_DIST_SQ = 64.0D * 64.0D;

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void onRenderEntity(Entity entity, double x, double y, double z,
                                 float partialTicks, boolean debug,
                                 CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) return;
        if (entity == player) return;
        if (entity instanceof EntityPlayer) return;
        if (entity.getDistanceSq(player) > CULL_DIST_SQ) return;

        Vec3d eye = player.getPositionEyes(partialTicks);
        Vec3d target = new Vec3d(entity.posX, entity.posY + entity.height * 0.5D, entity.posZ);
        RayTraceResult hit = mc.world.rayTraceBlocks(eye, target, false, true, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
