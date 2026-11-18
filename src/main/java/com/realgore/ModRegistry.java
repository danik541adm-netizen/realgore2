package com.realgore;

import com.realgore.entity.GibEntity;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RealGore.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RealGore.MODID);

    public static final RegistryObject<EntityType<GibEntity>> GIB = ENTITIES.register("gib",
            () -> EntityType.Builder.<GibEntity>of(GibEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("gib"));

    public static final RegistryObject<SimpleParticleType> BLOOD_SPLAT =
            PARTICLES.register("blood_splat", () -> new SimpleParticleType(false));

    private ModRegistry() {
    }
}
