package com.bomberman.clientfx.game.fx;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Small fixed-capacity pool for dust and crate debris; positions are logical Canvas pixels. */
public final class ParticlePool {

    private enum Kind { DUST, DEBRIS }

    private static final int MAX_PARTICLES = 200;
    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);
    private final Random random = new Random(0xB0B3_2026L);

    public void spawnDust(double x, double y, double tile) {
        int amount = 2 + random.nextInt(2);
        for (int i = 0; i < amount; i++) {
            add(new Particle(Kind.DUST, x + jitter(tile * 0.16), y + jitter(tile * 0.06),
                    jitter(tile * 0.18), -tile * (0.08 + random.nextDouble() * 0.12),
                    tile * (0.07 + random.nextDouble() * 0.05), 0.25, random.nextDouble() * 360));
        }
    }

    public void spawnCrateDebris(double x, double y, double tile) {
        for (int i = 0; i < 5; i++) {
            add(new Particle(Kind.DEBRIS, x + tile / 2, y + tile / 2,
                    jitter(tile * 1.8), -tile * (0.8 + random.nextDouble() * 1.6),
                    tile * (0.09 + random.nextDouble() * 0.06), 0.4, random.nextDouble() * 360));
        }
    }

    public void update(double elapsedSeconds) {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.age += elapsedSeconds;
            if (particle.age >= particle.life) {
                iterator.remove();
                continue;
            }
            particle.x += particle.vx * elapsedSeconds;
            particle.y += particle.vy * elapsedSeconds;
            if (particle.kind == Kind.DEBRIS) {
                particle.vy += 520 * elapsedSeconds;
                particle.rotation += 540 * elapsedSeconds;
            }
        }
    }

    public void draw(GraphicsContext graphics) {
        for (Particle particle : particles) {
            double alpha = Math.max(0, 1 - particle.age / particle.life);
            graphics.save();
            graphics.setGlobalAlpha(alpha * (particle.kind == Kind.DUST ? 0.5 : 0.9));
            graphics.translate(particle.x, particle.y);
            graphics.rotate(particle.rotation);
            if (particle.kind == Kind.DUST) {
                graphics.setFill(Color.WHITE);
                graphics.fillOval(-particle.size / 2, -particle.size / 2, particle.size, particle.size * 0.65);
            } else {
                graphics.setFill(Color.web("#9C4718"));
                graphics.fillRoundRect(-particle.size / 2, -particle.size / 3,
                        particle.size, particle.size * 0.66, 3, 3);
            }
            graphics.restore();
        }
    }

    public void clear() {
        particles.clear();
    }

    public int size() {
        return particles.size();
    }

    private void add(Particle particle) {
        if (particles.size() >= MAX_PARTICLES) {
            particles.removeFirst();
        }
        particles.add(particle);
    }

    private double jitter(double extent) {
        return (random.nextDouble() * 2 - 1) * extent;
    }

    private static final class Particle {
        private final Kind kind;
        private double x;
        private double y;
        private final double vx;
        private double vy;
        private final double size;
        private final double life;
        private double rotation;
        private double age;

        private Particle(Kind kind, double x, double y, double vx, double vy,
                         double size, double life, double rotation) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = life;
            this.rotation = rotation;
        }
    }
}
