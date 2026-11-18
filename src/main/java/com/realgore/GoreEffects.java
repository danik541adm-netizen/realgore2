package com.realgore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Серверные помощники: брызги крови, пятна на земле. Всё рисуется на клиенте через частицы. */
public final class GoreEffects {
    /** Красные кубики-«пиксели» крови с гравитацией (текстура блока красного камня). */
    public static final BlockParticleOption BLOOD_CHUNK =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
    /** Тёмно-красная взвесь (кровавый туман в момент удара). */
    public static final DustParticleOptions BLOOD_MIST =
            new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.02F), 1.2F);

    /** Брызги от одного попадания. Чем больше урон, тем больше крови. */
    public static void spray(ServerLevel level, Vec3 p, float damage) {
        double k = GoreConfig.BLOOD_AMOUNT.get() / 100.0D;
        if (k <= 0.0D) {
            return;
        }
        int n = (int) Mth.clamp(damage * 1.6D * k, 2.0D, 28.0D);
        level.sendParticles(BLOOD_CHUNK, p.x, p.y, p.z, n, 0.10D, 0.10D, 0.10D, 0.14D);
        level.sendParticles(BLOOD_MIST, p.x, p.y, p.z, Math.max(1, n / 4), 0.12D, 0.12D, 0.12D, 0.02D);
        int stains = (int) Mth.clamp(damage / 3.0D * k, 0.0D, 3.0D);
        stains(level, p, 1.1D, stains);
    }

    /** Мощный фонтан крови (отрубленная конечность, смерть). */
    public static void burst(ServerLevel level, Vec3 p, int amount) {
        double k = GoreConfig.BLOOD_AMOUNT.get() / 100.0D;
        if (k <= 0.0D) {
            return;
        }
        int n = Math.max(2, (int) (amount * k));
        level.sendParticles(BLOOD_CHUNK, p.x, p.y, p.z, n, 0.10D, 0.12D, 0.10D, 0.20D);
        level.sendParticles(BLOOD_MIST, p.x, p.y, p.z, Math.max(1, n / 3), 0.15D, 0.15D, 0.15D, 0.03D);
        stains(level, p, 1.4D, 2 + (int) (n / 8));
    }

    /** Капли крови (след от летящей части тела, кровоточащая культя). */
    public static void drip(ServerLevel level, double x, double y, double z) {
        if (GoreConfig.BLOOD_AMOUNT.get() <= 0) {
            return;
        }
        level.sendParticles(BLOOD_CHUNK, x, y, z, 1, 0.03D, 0.03D, 0.03D, 0.01D);
    }

    public static void stains(ServerLevel level, Vec3 center, double radius, int count) {
        if (GoreConfig.BLOOD_AMOUNT.get() <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            splatNear(level, center, radius);
        }
    }

    /** Кровавое пятно на полу рядом с точкой: ищем пол под точкой и ставим плоскую частицу. */
    public static void splatNear(ServerLevel level, Vec3 c, double radius) {
        RandomSource r = level.random;
        double x = c.x + (r.nextDouble() - 0.5D) * 2.0D * radius;
        double z = c.z + (r.nextDouble() - 0.5D) * 2.0D * radius;

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(c.y + 0.5D), Mth.floor(z));
        if (!level.hasChunkAt(p)) {
            return;
        }
        // точка внутри твёрдого блока: пятно ставить некуда
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            p.move(Direction.DOWN);
            BlockState st = level.getBlockState(p);
            VoxelShape shape = st.getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                double top = p.getY() + shape.max(Direction.Axis.Y);
                if (!level.getFluidState(p.above()).isEmpty()) {
                    return;
                }
                level.sendParticles(ModRegistry.BLOOD_SPLAT.get(),
                        x, top + 0.012D + r.nextDouble() * 0.006D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                return;
            }
        }
    }

    private GoreEffects() {
    }
}
