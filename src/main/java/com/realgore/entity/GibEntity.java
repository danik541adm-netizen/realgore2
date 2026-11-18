package com.realgore.entity;

import com.realgore.GoreConfig;
import com.realgore.GoreEffects;
import com.realgore.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Отрубленная часть тела зомби: летит, кувыркается, истекает кровью и падает на землю. */
public class GibEntity extends Entity {
    public static final int HEAD = 0;
    public static final int BODY = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 3;
    public static final int RIGHT_LEG = 4;
    public static final int LEFT_LEG = 5;

    private static final EntityDataAccessor<Integer> DATA_PART =
            SynchedEntityData.defineId(GibEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(GibEntity.class, EntityDataSerializers.INT);

    // углы поворота (для рендера, с интерполяцией)
    public float rotX;
    public float rotY;
    public float rotZ;
    public float rotXO;
    public float rotYO;
    public float rotZO;

    private float spinX;
    private float spinY;
    private float spinZ;
    private boolean resting;

    public GibEntity(EntityType<? extends GibEntity> type, Level level) {
        super(type, level);
        this.spinX = (this.random.nextFloat() - 0.5F) * 50.0F;
        this.spinY = (this.random.nextFloat() - 0.5F) * 30.0F;
        this.spinZ = (this.random.nextFloat() - 0.5F) * 50.0F;
        this.rotY = this.random.nextFloat() * 360.0F;
        this.rotYO = this.rotY;
    }

    public GibEntity(Level level, int part, int skin, double x, double y, double z) {
        this(ModRegistry.GIB.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(DATA_PART, part);
        this.entityData.set(DATA_SKIN, skin);
    }

    public int getPart() {
        return this.entityData.get(DATA_PART);
    }

    public int getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_PART, 0);
        this.entityData.define(DATA_SKIN, 0);
    }

    @Override
    public void tick() {
        super.tick();

        this.rotXO = this.rotX;
        this.rotYO = this.rotY;
        this.rotZO = this.rotZ;

        Vec3 m = this.getDeltaMovement();
        if (this.isInWater()) {
            m = m.scale(0.85D).add(0.0D, -0.01D, 0.0D);
        } else {
            m = m.add(0.0D, -0.045D, 0.0D);
        }
        this.setDeltaMovement(m);
        double vyBefore = m.y;
        this.move(MoverType.SELF, m);

        Vec3 a = this.getDeltaMovement();
        boolean floor = vyBefore < 0.0D && a.y == 0.0D;
        double impact = -vyBefore;

        if (floor) {
            if (impact > 0.22D) {
                // отскок
                this.setDeltaMovement(a.x * 0.6D, impact * 0.32D, a.z * 0.6D);
                this.spinX *= 0.5F;
                this.spinY *= 0.5F;
                this.spinZ *= 0.5F;
                this.resting = false;
                if (this.level() instanceof ServerLevel sl) {
                    GoreEffects.splatNear(sl, this.position(), 0.6D);
                    GoreEffects.drip(sl, this.getX(), this.getY() + 0.1D, this.getZ());
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 0.8F, 0.6F + this.random.nextFloat() * 0.2F);
                }
            } else {
                this.setDeltaMovement(a.x * 0.55D, 0.0D, a.z * 0.55D);
                this.resting = true;
            }
        } else {
            this.setDeltaMovement(a.x * 0.98D, a.y * 0.98D, a.z * 0.98D);
            this.resting = false;
        }

        if (!this.resting) {
            this.rotX += this.spinX;
            this.rotY += this.spinY;
            this.rotZ += this.spinZ;
        } else {
            // ложимся плашмя: длинная ось горизонтально
            float tx = Math.round((this.rotX - 90.0F) / 180.0F) * 180.0F + 90.0F;
            float tz = Math.round(this.rotZ / 180.0F) * 180.0F;
            this.rotX += (tx - this.rotX) * 0.35F;
            this.rotZ += (tz - this.rotZ) * 0.35F;
        }

        if (this.level() instanceof ServerLevel sl) {
            if (this.tickCount > GoreConfig.GIB_LIFETIME_SEC.get() * 20) {
                this.discard();
                return;
            }
            // кровавый след в полёте
            if (!this.resting && this.tickCount % 2 == 0 && this.tickCount < 80) {
                GoreEffects.drip(sl, this.getX(), this.getY() + 0.15D, this.getZ());
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_PART, tag.getInt("Part"));
        this.entityData.set(DATA_SKIN, tag.getInt("Skin"));
        this.tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Part", this.getPart());
        tag.putInt("Skin", this.getSkin());
        tag.putInt("Age", this.tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
