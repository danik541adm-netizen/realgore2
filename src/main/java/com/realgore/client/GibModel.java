package com.realgore.client;

import com.realgore.RealGore;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Модели отрубленных частей тела. Каждая часть - куб с UV как у зомби (текстура зомби накладывается напрямую),
 * плюс «срез» (красный кусочек мяса) с другой текстурой. Начало координат - центр части.
 */
public final class GibModel {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RealGore.MODID, "gib"), "main");

    /** Порядок совпадает с GibEntity: 0 голова, 1 торс, 2 правая рука, 3 левая рука, 4 правая нога, 5 левая нога */
    public static final String[] NAMES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // голова: срез снизу (шея)
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -4F, -4F, 8F, 8F, 8F), PartPose.ZERO);
        root.addOrReplaceChild("head_cap",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.9F, 4F, -3.9F, 7.8F, 0.25F, 7.8F), PartPose.ZERO);

        // торс: срезы сверху, снизу и по бокам (на месте рук)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4F, -6F, -2F, 8F, 12F, 4F), PartPose.ZERO);
        root.addOrReplaceChild("body_cap",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.9F, -6.25F, -1.9F, 7.8F, 0.25F, 3.8F)
                        .addBox(-3.9F, 6F, -1.9F, 7.8F, 0.25F, 3.8F)
                        .addBox(4F, -5.9F, -1.9F, 0.25F, 3.8F, 3.8F)
                        .addBox(-4.25F, -5.9F, -1.9F, 0.25F, 3.8F, 3.8F), PartPose.ZERO);

        // руки и ноги: срез сверху (плечо / бедро)
        addLimb(root, "right_arm", 40, 16, false);
        addLimb(root, "left_arm", 40, 16, true);
        addLimb(root, "right_leg", 0, 16, false);
        addLimb(root, "left_leg", 0, 16, true);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLimb(PartDefinition root, String name, int u, int v, boolean mirror) {
        CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
        if (mirror) {
            cube = cube.mirror();
        }
        root.addOrReplaceChild(name, cube.addBox(-2F, -6F, -2F, 4F, 12F, 4F), PartPose.ZERO);
        root.addOrReplaceChild(name + "_cap",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.9F, -6.25F, -1.9F, 3.8F, 0.25F, 3.8F), PartPose.ZERO);
    }

    private GibModel() {
    }
}
