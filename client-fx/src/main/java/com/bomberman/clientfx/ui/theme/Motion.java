package com.bomberman.clientfx.ui.theme;

import javafx.animation.Interpolator;

/** Easing curves from docs/ui-redesign/04-design-system.md, section 6. */
public final class Motion {

    /**
     * Overshoots slightly past the target and settles back ("back-out"), used for popups and
     * the result title. {@link Interpolator#SPLINE} cannot overshoot, so the curve is computed directly.
     */
    public static final Interpolator BACK_OUT = new Interpolator() {
        private static final double OVERSHOOT = 1.70158;

        @Override
        protected double curve(double t) {
            double shifted = t - 1;
            return 1 + (OVERSHOOT + 1) * shifted * shifted * shifted + OVERSHOOT * shifted * shifted;
        }

        @Override
        public String toString() {
            return "Motion.BACK_OUT";
        }
    };

    private Motion() {
    }
}
