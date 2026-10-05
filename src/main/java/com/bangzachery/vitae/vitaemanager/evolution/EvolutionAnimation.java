package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.*;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

public final class EvolutionAnimation {
    private EvolutionAnimation() {
    }

    public record Vec(double x, double y, double z) {
        public Vec mix(Vec end, double p) {
            return new Vec(
                    x + (end.x - x) * p,
                    y + (end.y - y) * p,
                    z + (end.z - z) * p
            );
        }
    }

    public record Frame(
            Vec weapon,
            List<Vec> ingredients,
            double rotation,
            double tilt,
            Stage stage
    ) {
    }

    public static Frame frame(
            Preset preset, double progress, int elapsed, List<Vec> origins
    ) {
        range(progress, 0, 1, "Progress");
        require(elapsed >= 0 && !origins.isEmpty() && origins.size() <= 8,
                "Frame tidak valid.");

        double lift = clamp((progress - .15) / .20);
        double gather = lift;
        double shrink = 1 - clamp((progress - .70) / .30);

        double rotation = switch (preset) {
            case INFERNO -> elapsed * .055;
            case STORMFORGE -> Math.floor(elapsed / 6.0) * Math.PI / 3;
            case VOID -> -elapsed * .035;
        };

        Vec weapon = new Vec(
                0,
                lift * 2.4 + Math.sin(elapsed * .08) * lift * .10 * shrink,
                0
        );

        List<Vec> positions = new ArrayList<>();

        for (int i = 0; i < origins.size(); i++) {
            double angle = rotation * 2 + Math.PI * 2 * i / origins.size();
            double height = switch (preset) {
                case INFERNO -> Math.sin(angle * 2) * .25;
                case STORMFORGE -> i % 2 == 0 ? .45 : -.45;
                case VOID -> Math.sin(elapsed * .06 + i) * .8;
            };

            Vec orbit = new Vec(
                    Math.cos(angle) * 1.8 * shrink,
                    weapon.y + height * shrink,
                    Math.sin(angle) * 1.8 * shrink
            );
            positions.add(origins.get(i).mix(orbit, gather));
        }

        return new Frame(
                weapon,
                List.copyOf(positions),
                rotation,
                preset == Preset.STORMFORGE ? .35 : 0,
                stage(progress)
        );
    }

    public static Stage stage(double progress) {
        return progress < .15 ? Stage.AWAKEN
                : progress < .35 ? Stage.LIFT
                  : progress < .70 ? Stage.ORBIT
                    : progress < 1 ? Stage.ABSORB : Stage.FINISH;
    }

    public static int remaining(Stage stage, int ticks, int elapsed) {
        double end = switch (stage) {
            case AWAKEN -> .15;
            case LIFT -> .35;
            case ORBIT -> .70;
            case ABSORB -> 1;
            case FINISH -> 0;
        };
        return stage == Stage.FINISH
                ? 100 : Math.max(10, (int) Math.ceil(ticks * end) - elapsed);
    }

    private static double clamp(double progress) {
        return Math.max(0, Math.min(1, progress));
    }
}