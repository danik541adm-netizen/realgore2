package com.realgore;

import com.realgore.entity.GibEntity;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Вся серверная логика: кровь при ударах, отрубание конечностей, разрывание на куски. */
@Mod.EventBusSubscriber(modid = RealGore.MODID)
public final class GoreEvents {
    private static final String TAG_MASK = "realgore_mask";
    private static final String TAG_GIB = "realgore_gib";
    private static final String TAG_LAST = "realgore_lastdmg";
    private static final UUID LEG_UUID = UUID.fromString("b1a2c3d4-0000-4000-8000-5ee50000ab01");
    private static final int[] LIMB_PARTS = {
            GibEntity.RIGHT_ARM, GibEntity.LEFT_ARM, GibEntity.RIGHT_LEG, GibEntity.LEFT_LEG, GibEntity.HEAD};

    private GoreEvents() {
    }

    // ------------------------------------------------------------------ helpers

    /** Какие мобы расчленяются (обычные зомби и хаски, не детёныши). */
    public static boolean isGoreZombie(LivingEntity e) {
        return (e.getType() == EntityType.ZOMBIE || e.getType() == EntityType.HUSK) && !e.isBaby();
    }

    private static boolean bleeds(LivingEntity e) {
        return !(e instanceof ArmorStand || e instanceof AbstractSkeleton || e instanceof Slime
                || e instanceof Creeper || e instanceof Blaze || e instanceof Ghast || e instanceof AbstractGolem);
    }

    private static boolean bleedFrom(DamageSource src) {
        return src.getEntity() != null || src.getDirectEntity() != null
                || src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static int getMask(Entity e) {
        return e.getPersistentData().getInt(TAG_MASK);
    }

    private static void setMask(LivingEntity e, int mask) {
        e.getPersistentData().putInt(TAG_MASK, mask);
        GoreNetwork.syncTracking(e, mask);
    }

    /** Где пуля/стрела/удар попали в моба. */
    private static Vec3 hitPos(LivingEntity t, DamageSource src) {
        AABB bb = t.getBoundingBox();
        Entity direct = src.getDirectEntity();
        if (direct instanceof Projectile p) {
            Vec3 from = p.position();
            Vec3 motion = p.getDeltaMovement();
            Optional<Vec3> hit = bb.inflate(0.2D).clip(from.subtract(motion), from.add(motion));
            if (hit.isPresent()) {
                return hit.get();
            }
            return new Vec3(
                    Mth.clamp(from.x, bb.minX, bb.maxX),
                    Mth.clamp(from.y, bb.minY + 0.05D, bb.maxY - 0.05D),
                    Mth.clamp(from.z, bb.minZ, bb.maxZ));
        }
        double y = t.getY() + t.getBbHeight() * (0.40D + 0.45D * t.getRandom().nextDouble());
        return new Vec3(t.getX(), y, t.getZ());
    }

    /** Горизонтальное направление «от источника урона». */
    private static Vec3 awayFromSource(LivingEntity t, DamageSource src) {
        Vec3 s = src == null ? null : src.getSourcePosition();
        double dx;
        double dz;
        if (s == null) {
            dx = t.getRandom().nextDouble() - 0.5D;
            dz = t.getRandom().nextDouble() - 0.5D;
        } else {
            dx = t.getX() - s.x;
            dz = t.getZ() - s.z;
        }
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return new Vec3(dx / len, 0.0D, dz / len);
    }

    /** Мировая позиция части тела зомби. */
    private static Vec3 limbPos(LivingEntity t, int part) {
        double yaw = Math.toRadians(t.yBodyRot);
        double lx = Math.cos(yaw);   // вектор «влево» от моба
        double lz = Math.sin(yaw);
        double fx = -Math.sin(yaw);  // вектор «вперёд»
        double fz = Math.cos(yaw);
        double x = t.getX();
        double y = t.getY();
        double z = t.getZ();
        return switch (part) {
            case GibEntity.HEAD -> new Vec3(x, y + 1.6D, z);
            case GibEntity.BODY -> new Vec3(x, y + 1.0D, z);
            case GibEntity.RIGHT_ARM -> new Vec3(x - lx * 0.35D + fx * 0.1D, y + 1.2D, z - lz * 0.35D + fz * 0.1D);
            case GibEntity.LEFT_ARM -> new Vec3(x + lx * 0.35D + fx * 0.1D, y + 1.2D, z + lz * 0.35D + fz * 0.1D);
            case GibEntity.RIGHT_LEG -> new Vec3(x - lx * 0.12D, y + 0.4D, z - lz * 0.12D);
            default -> new Vec3(x + lx * 0.12D, y + 0.4D, z + lz * 0.12D);
        };
    }

    private static void spawnGib(ServerLevel level, LivingEntity src, int part, Vec3 pos, Vec3 vel) {
        int skin = src.getType() == EntityType.HUSK ? 1 : 0;
        GibEntity gib = new GibEntity(level, part, skin, pos.x, pos.y - 0.1D, pos.z);
        gib.setDeltaMovement(vel);
        level.addFreshEntity(gib);
    }

    private static void applyLegPenalty(LivingEntity e, int mask) {
        AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        int legs = ((mask & Limb.RIGHT_LEG) != 0 ? 1 : 0) + ((mask & Limb.LEFT_LEG) != 0 ? 1 : 0);
        double amount = legs == 0 ? 0.0D : (legs == 1 ? -0.4D : -0.8D);
        AttributeModifier existing = speed.getModifier(LEG_UUID);
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            speed.removeModifier(LEG_UUID);
        }
        if (amount != 0.0D) {
            speed.addTransientModifier(
                    new AttributeModifier(LEG_UUID, "realgore_legs", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ------------------------------------------------------------------ dismemberment

    /** @return true, если отрублена голова (зомби должен умереть). */
    private static boolean severPart(ServerLevel level, LivingEntity t, int part, DamageSource src, boolean violent) {
        int bit = Limb.bit(part);
        int mask = getMask(t);
        if (bit == 0 || (mask & bit) != 0) {
            return false;
        }
        mask |= bit;
        setMask(t, mask);

        RandomSource r = level.random;
        Vec3 pos = limbPos(t, part);
        Vec3 dir = awayFromSource(t, src);
        double power = violent ? 0.45D : 0.22D;
        Vec3 vel = new Vec3(
                dir.x * power + (r.nextDouble() - 0.5D) * 0.25D,
                0.22D + r.nextDouble() * 0.18D,
                dir.z * power + (r.nextDouble() - 0.5D) * 0.25D);
        spawnGib(level, t, part, pos, vel);

        GoreEffects.burst(level, pos, 18);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE,
                1.0F, 0.55F + r.nextFloat() * 0.2F);
        applyLegPenalty(t, mask);
        return part == GibEntity.HEAD;
    }

    private static int pickPart(LivingEntity t, Vec3 hit, int mask, float dmg, RandomSource r) {
        double rel = hit.y - t.getY();
        int primary;
        if (rel > 1.45D) {
            if (dmg < 6.0F) {
                return -1;
            }
            primary = GibEntity.HEAD;
        } else if (rel < 0.7D) {
            primary = r.nextBoolean() ? GibEntity.RIGHT_LEG : GibEntity.LEFT_LEG;
        } else {
            primary = r.nextBoolean() ? GibEntity.RIGHT_ARM : GibEntity.LEFT_ARM;
        }
        if ((mask & Limb.bit(primary)) == 0) {
            return primary;
        }
        int alt = switch (primary) {
            case GibEntity.RIGHT_ARM -> GibEntity.LEFT_ARM;
            case GibEntity.LEFT_ARM -> GibEntity.RIGHT_ARM;
            case GibEntity.RIGHT_LEG -> GibEntity.LEFT_LEG;
            case GibEntity.LEFT_LEG -> GibEntity.RIGHT_LEG;
            default -> -1;
        };
        if (alt >= 0 && (mask & Limb.bit(alt)) == 0) {
            return alt;
        }
        return -1;
    }

    /** @return true, если удар должен стать смертельным (отрублена голова). */
    private static boolean trySever(ServerLevel level, LivingEntity t, DamageSource src, float dmg, Vec3 hit) {
        RandomSource r = level.random;
        float mult = GoreConfig.DISMEMBER_CHANCE.get().floatValue();

        if (src.is(DamageTypeTags.IS_EXPLOSION)) {
            boolean lethal = false;
            for (int part : LIMB_PARTS) {
                if (r.nextFloat() < 0.55F * mult) {
                    lethal |= severPart(level, t, part, src, true);
                }
            }
            return lethal;
        }

        float chance = Mth.clamp((dmg - 2.0F) * 0.07F, 0.04F, 0.9F);
        Entity attacker = src.getEntity();
        if (attacker instanceof LivingEntity le && src.getDirectEntity() == attacker) {
            Item held = le.getMainHandItem().getItem();
            if (held instanceof AxeItem) {
                chance += 0.25F;
            } else if (held instanceof SwordItem) {
                chance += 0.10F;
            }
        }
        if (t.getHealth() - dmg <= 0.0F) {
            chance += 0.30F;
        }
        chance *= mult;
        if (r.nextFloat() >= chance) {
            return false;
        }
        int part = pickPart(t, hit, getMask(t), dmg, r);
        return part >= 0 && severPart(level, t, part, src, false);
    }

    /** Зомби разлетается на куски целиком. */
    private static void gibAll(ServerLevel level, LivingEntity t, DamageSource src) {
        RandomSource r = level.random;
        int mask = getMask(t);
        boolean explosion = src.is(DamageTypeTags.IS_EXPLOSION);
        double power = explosion ? 0.5D : 0.28D;
        Vec3 dir = awayFromSource(t, src);

        for (int part = 0; part < 6; part++) {
            int bit = Limb.bit(part);
            if (bit != 0 && (mask & bit) != 0) {
                continue; // уже отрублено раньше
            }
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double sp = 0.12D + r.nextDouble() * power;
            Vec3 vel = new Vec3(
                    Math.cos(ang) * sp + dir.x * power * 0.6D,
                    0.2D + r.nextDouble() * 0.3D,
                    Math.sin(ang) * sp + dir.z * power * 0.6D);
            spawnGib(level, t, part, limbPos(t, part), vel);
        }
        Vec3 c = new Vec3(t.getX(), t.getY() + 1.0D, t.getZ());
        GoreEffects.burst(level, c, 45);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.3F, 0.45F);
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity t = event.getEntity();
        if (!(t.level() instanceof ServerLevel level)) {
            return;
        }
        float dmg = event.getAmount();
        DamageSource src = event.getSource();
        if (dmg < 1.0F || !bleedFrom(src) || !bleeds(t)) {
            return;
        }
        t.getPersistentData().putFloat(TAG_LAST, dmg);

        Vec3 hit = hitPos(t, src);
        GoreEffects.spray(level, hit, dmg);

        if (isGoreZombie(t) && !t.isDeadOrDying() && dmg >= 3.0F) {
            if (trySever(level, t, src, dmg, hit)) {
                event.setAmount(Math.max(event.getAmount(), t.getHealth() + t.getAbsorptionAmount() + 1.0F));
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity t = event.getEntity();
        if (!(t.level() instanceof ServerLevel level) || !bleeds(t)) {
            return;
        }
        Vec3 c = new Vec3(t.getX(), t.getY() + t.getBbHeight() * 0.5D, t.getZ());
        GoreEffects.spray(level, c, 8.0F);
        GoreEffects.stains(level, c, 1.4D, 4 + level.random.nextInt(3));

        if (isGoreZombie(t)) {
            DamageSource src = event.getSource();
            boolean explosion = GoreConfig.GIB_ON_EXPLOSION.get() && src.is(DamageTypeTags.IS_EXPLOSION);
            boolean heavy = t.getPersistentData().getFloat(TAG_LAST) >= GoreConfig.GIB_DAMAGE.get().floatValue();
            if (explosion || heavy) {
                gibAll(level, t, src);
                // убираем тело на следующем тике, после того как выпадет лут
                t.getPersistentData().putBoolean(TAG_GIB, true);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = e.getPersistentData();
        if (!data.contains(TAG_MASK) && !data.contains(TAG_GIB)) {
            return;
        }
        if (data.getBoolean(TAG_GIB)) {
            if (e.isDeadOrDying()) {
                e.discard();
            }
            return;
        }
        if (e.isDeadOrDying()) {
            return;
        }
        int mask = data.getInt(TAG_MASK);
        if (mask == 0) {
            return;
        }
        if (e.tickCount % 40 == 0) {
            applyLegPenalty(e, mask); // восстановить штраф к скорости после перезагрузки мира
        }
        if (e.tickCount % 6 == 0) {
            for (int part : LIMB_PARTS) {
                if ((mask & Limb.bit(part)) == 0) {
                    continue;
                }
                Vec3 p = limbPos(e, part);
                level.sendParticles(GoreEffects.BLOOD_CHUNK, p.x, p.y, p.z, 2, 0.04D, 0.04D, 0.04D, 0.03D);
                if (level.random.nextInt(8) == 0) {
                    GoreEffects.splatNear(level, e.position(), 0.5D);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof LivingEntity le
                && isGoreZombie(le)) {
            int mask = getMask(le);
            if (mask != 0) {
                GoreNetwork.syncTo(player, le, mask);
            }
        }
    }
}
