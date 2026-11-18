package com.realgore.client;

import com.realgore.GoreClientState;
import com.realgore.Limb;
import com.realgore.RealGore;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Прячет отрубленные конечности у живых зомби (на клиенте по маске, которую присылает сервер). */
@Mod.EventBusSubscriber(modid = RealGore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GoreClientEvents {
    /** На сколько опускаем зомби, если у него нет обеих ног (торс «сидит» на земле). */
    private static final float LEGLESS_DROP = 0.72F;
    private static final int BOTH_LEGS = Limb.RIGHT_LEG | Limb.LEFT_LEG;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Zombie)) {
            return;
        }
        int mask = GoreClientState.get(entity.getId());
        if (mask == 0) {
            return;
        }
        Object model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> m)) {
            return;
        }
        if ((mask & Limb.HEAD) != 0) {
            m.head.visible = false;
            m.hat.visible = false;
        }
        if ((mask & Limb.RIGHT_ARM) != 0) {
            m.rightArm.visible = false;
        }
        if ((mask & Limb.LEFT_ARM) != 0) {
            m.leftArm.visible = false;
        }
        if ((mask & Limb.RIGHT_LEG) != 0) {
            m.rightLeg.visible = false;
        }
        if ((mask & Limb.LEFT_LEG) != 0) {
            m.leftLeg.visible = false;
        }
        if ((mask & BOTH_LEGS) == BOTH_LEGS) {
            event.getPoseStack().translate(0.0D, -LEGLESS_DROP, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Zombie)) {
            return;
        }
        int mask = GoreClientState.get(entity.getId());
        if (mask == 0) {
            return;
        }
        Object model = event.getRenderer().getModel();
        if (!(model instanceof HumanoidModel<?> m)) {
            return;
        }
        // модель общая для всех зомби, поэтому возвращаем всё как было
        m.head.visible = true;
        m.hat.visible = true;
        m.rightArm.visible = true;
        m.leftArm.visible = true;
        m.rightLeg.visible = true;
        m.leftLeg.visible = true;
        if ((mask & BOTH_LEGS) == BOTH_LEGS) {
            event.getPoseStack().translate(0.0D, LEGLESS_DROP, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GoreClientState.clear();
    }


    private GoreClientEvents() {
    }
}
