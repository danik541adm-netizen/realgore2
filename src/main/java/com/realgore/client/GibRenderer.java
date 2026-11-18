package com.realgore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.realgore.RealGore;
import com.realgore.entity.GibEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GibRenderer extends EntityRenderer<GibEntity> {
    // текстуры берутся у ванильных зомби, поэтому ресурспаки со скинами зомби подхватываются автоматически
    private static final ResourceLocation ZOMBIE = new ResourceLocation("textures/entity/zombie/zombie.png");
    private static final ResourceLocation HUSK = new ResourceLocation("textures/entity/zombie/husk.png");
    private static final ResourceLocation CAP = new ResourceLocation(RealGore.MODID, "textures/entity/blood_cap.png");

    /** Высота центра части над землёй, когда она лежит (половина толщины). */
    private static final float[] REST_HEIGHT = {0.25F, 0.125F, 0.125F, 0.125F, 0.125F, 0.125F};

    private final ModelPart root;

    public GibRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.root = ctx.bakeLayer(GibModel.LAYER);
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(GibEntity gib, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int packedLight) {
        int part = Mth.clamp(gib.getPart(), 0, GibModel.NAMES.length - 1);

        ps.pushPose();
        ps.translate(0.0D, REST_HEIGHT[part], 0.0D);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, gib.rotYO, gib.rotY)));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, gib.rotXO, gib.rotX)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, gib.rotZO, gib.rotZ)));
        ps.scale(-1.0F, -1.0F, 1.0F);

        ModelPart body = this.root.getChild(GibModel.NAMES[part]);
        ModelPart cap = this.root.getChild(GibModel.NAMES[part] + "_cap");

        body.render(ps, buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(gib))),
                packedLight, OverlayTexture.NO_OVERLAY);
        cap.render(ps, buffer.getBuffer(RenderType.entityCutoutNoCull(CAP)),
                packedLight, OverlayTexture.NO_OVERLAY);

        ps.popPose();
        super.render(gib, entityYaw, partialTick, ps, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GibEntity gib) {
        return gib.getSkin() == 1 ? HUSK : ZOMBIE;
    }
}
