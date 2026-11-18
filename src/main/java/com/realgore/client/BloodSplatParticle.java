package com.realgore.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.realgore.GoreConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Плоское кровавое пятно, лежащее на земле. Живёт долго, в конце плавно исчезает. */
public class BloodSplatParticle extends TextureSheetParticle {
    private static final int FADE_TICKS = 100;

    private final float rot;

    protected BloodSplatParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        int base = GoreConfig.STAIN_LIFETIME_SEC.get() * 20;
        this.lifetime = base + this.random.nextInt(Math.max(1, base / 4));
        this.quadSize = 0.20F + this.random.nextFloat() * 0.28F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.rot = this.random.nextFloat() * Mth.TWO_PI;
        float shade = 0.75F + this.random.nextFloat() * 0.25F;
        this.setColor(shade, shade, shade);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        int left = this.lifetime - this.age;
        if (left < FADE_TICKS) {
            this.alpha = Math.max(0.0F, left / (float) FADE_TICKS);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());

        float s = this.getQuadSize(partialTicks);
        float cos = Mth.cos(this.rot) * s;
        float sin = Mth.sin(this.rot) * s;

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        // углы квадрата в плоскости XZ, повёрнутые на this.rot
        float[] cx = {-1.0F, -1.0F, 1.0F, 1.0F};
        float[] cz = {-1.0F, 1.0F, 1.0F, -1.0F};
        float[] us = {u1, u1, u0, u0};
        float[] vs = {v1, v0, v0, v1};

        // рисуем обе стороны, чтобы не зависеть от отсечения задних граней
        for (int i = 0; i < 4; i++) {
            float lx = cx[i];
            float lz = cz[i];
            float rx = lx * cos - lz * sin;
            float rz = lx * sin + lz * cos;
            buffer.vertex(px + rx, py, pz + rz).uv(us[i], vs[i])
                    .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        }
        for (int i = 3; i >= 0; i--) {
            float lx = cx[i];
            float lz = cz[i];
            float rx = lx * cos - lz * sin;
            float rz = lx * sin + lz * cos;
            buffer.vertex(px + rx, py, pz + rz).uv(us[i], vs[i])
                    .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double dx, double dy, double dz) {
            return new BloodSplatParticle(level, x, y, z, this.sprites);
        }
    }
}
