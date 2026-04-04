package com.oddlabs.procedural;

public final class Tools {
    private Tools() {
        // no instances
    }

    public static float modulo(float x, float n) {
        return x - n * (float) Math.floor(x / n);
    }

    public static int modulo(int x, int n) {
        return (int) (x - n * (float) Math.floor((float) x / n));
    }

    public static float interpolateLinear(float v1, float v2, float fraction) {
        return v1 * (1f - fraction) + v2 * fraction;
    }

    public static float interpolateSmooth(float v1, float v2, float fraction) {
        fraction = fraction < 0.5f
                ? 2f * fraction * fraction
                : 1f - 2f * (fraction - 1f) * (fraction - 1f);
        return v1 * (1f - fraction) + v2 * fraction;
    }

    public static float interpolateSmooth2(float v1, float v2, float fraction) {
        float fraction2 = fraction * fraction;
        fraction = 3 * fraction2 - 2f * fraction * fraction2;
        return v1 * (1f - fraction) + v2 * fraction;
    }

    public static float interpolateCubic(float v0, float v1, float v2, float v3, float fraction) {
        float p = (v3 - v2) - (v0 - v1);
        float q = (v0 - v1) - p;
        float r = v2 - v0;
        float fraction2 = fraction * fraction;
        return p * fraction * fraction2 + q * fraction2 + r * fraction + v1;
    }

    public static float rampLinear(float start, float end, float segment_length, float x) {
        x = modulo(x, segment_length);
        if (x < start) return 0f;
        if (x > end) return 1f;
        return interpolateLinear(0f, 1f, (x - start) / (end - start));
    }

    public static float step(float start, float end, float segment_length, float x) {
        x = modulo(x, segment_length);
        return x < start || x > end ? 0f : 1f;
    }

    public static float sawtooth(float x) {
        x++; // hack!
        float x_frac = x - (int) x;
        return x_frac < 0.5
                ? 2 * x_frac
                : -2 * x_frac + 2;
    }

    public static float gaussify(float x) {
        return x >= 0 && x < 0.5
                ? 0.5f * (float) Math.sqrt(2 * x)
                : x >= 0.5f && x <= 1f
                  ? 1f - 0.5f * (float) Math.sqrt(-2 * x + 2)
                  : 0;
    }

    public static float gaussify(float x, float exponent) {
        return x >= 0 && x < 0.5
                ? 0.5f * (float) Math.pow(2 * x, exponent)
                : x >= 0.5f && x <= 1f
                  ? 1f - 0.5f * (float) Math.pow(-2 * x + 2, exponent)
                  : 0;
    }

    public static float gain(float gain, float x) {
        return x < 0.5f
                ? (float) (Math.pow(2 * x, Math.log(1 - gain) / Math.log(0.5d)) / 2f)
                : 1f - (float) (Math.pow(2 - 2 * x, Math.log(1 - gain) / Math.log(0.5d)) / 2f);
    }
}
