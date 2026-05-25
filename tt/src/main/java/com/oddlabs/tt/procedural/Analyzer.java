package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import org.jspecify.annotations.NonNull;

public final class Analyzer {

    public static void analyze(@NonNull Channel height, String name) throws Exception {
        IO.println("*** Performing image analysis on \"" + name + "\" ****");
        height.flipV();
        IO.println("Height map...");
        height.toLayer().saveAsPNG(name);
        statistics(height, "height");
        IO.println("done");

        IO.println("Slope map...");
        Channel slope = height.copy().lineart().dynamicRange();
        slope.toLayer().saveAsPNG(name + "_slope");
        statistics(slope, "slope");
        score(slope, "slope");
        IO.println("done");

        IO.println("Relative height map...");
        Channel rel1 = height.copy().relativeIntensity(1);
        rel1.toLayer().saveAsPNG(name + "_relative1");
        statistics(rel1, "relative height 1");
        Channel rel2 = height.copy().relativeIntensity(2);
        rel2.toLayer().saveAsPNG(name + "_relative2");
        statistics(rel2, "relative height 2");
        Channel rel4 = height.copy().relativeIntensity(4);
        rel4.toLayer().saveAsPNG(name + "_relative4");
        statistics(rel4, "relative height 4");
        Channel rel8 = height.copy().relativeIntensity(8);
        rel8.toLayer().saveAsPNG(name + "_relative8");
        statistics(rel8, "relative height 8");
        Channel rel16 = height.copy().relativeIntensity(16);
        rel16.toLayer().saveAsPNG(name + "_relative16");
        statistics(rel16, "relative height 16");
        Channel rel32 = height.copy().relativeIntensity(32);
        rel32.toLayer().saveAsPNG(name + "_relative32");
        statistics(rel32, "relative height 32");
        IO.println("done");

        IO.println("height histogram...");
        histogram(height, height.width).flipV().toLayer().saveAsPNG(name + "_histogram");
        IO.println("done");

        IO.println("Slope histogram...");
        histogram(slope, slope.width).flipV().toLayer().saveAsPNG(name + "_slope_histogram");
        IO.println("done");

        IO.println("Relative height histogram...");
        histogram(rel1, height.width).flipV().toLayer().saveAsPNG(name + "_relative1_histogram");
        histogram(rel2, height.width).flipV().toLayer().saveAsPNG(name + "_relative2_histogram");
        histogram(rel4, height.width).flipV().toLayer().saveAsPNG(name + "_relative4_histogram");
        histogram(rel8, height.width).flipV().toLayer().saveAsPNG(name + "_relative8_histogram");
        histogram(rel16, height.width).flipV().toLayer().saveAsPNG(name + "_relative16_histogram");
        histogram(rel32, height.width).flipV().toLayer().saveAsPNG(name + "_relative32_histogram");
        IO.println("done");

        IO.println("Accessibility map...");
        Channel access = height.copy().lineart().threshold(0f, 0.025f);
        access.toLayer().saveAsPNG(name + "_access");
        IO.println("done");
        IO.println("Accessible area: " + (100f * access.count(1f) / (access.width * access.height)) + "%");
        Channel access_conn = access.copy().largestConnected(1f);
        access_conn.toLayer().saveAsPNG(name + "_access_conn");
        IO.println("Largest connected accessible area: " + (100f * access_conn.count(
                1f) / (access_conn.width * access_conn.height)) + "%");
        float access_avrconn = access.averageConnected(1f);
        IO.println(
                "Average connected accessible area: " + (100f * access_avrconn / (access.width * access.height)) + "%");

        IO.println("Flatness map...");
        Channel flat = height.copy().lineart().threshold(0f, 0.0125f);
        flat.toLayer().saveAsPNG(name + "_flatness");
        IO.println("done");
        IO.println("Flat area: " + (100f * flat.count(1f) / (flat.width * flat.height)) + "%");
        Channel flat_conn = flat.copy().largestConnected(1f);
        flat_conn.toLayer().saveAsPNG(name + "_flatness_conn");
        IO.println("Largest connected flat area: " + (100f * flat_conn.count(
                1f) / (flat_conn.width * flat_conn.height)) + "%");
        float flat_avrconn = flat.averageConnected(1f);
        IO.println("Average connected flat area: " + (100f * flat_avrconn / (flat.width * flat.height)) + "%");

        IO.println("Connectedness map...");
        Channel conn = connectedness(height, 16);
        conn.toLayer().saveAsPNG(name + "_connectedness");
        IO.println("Overall connectedness: " + conn.sum() / (height.width * height.height));
        IO.println("done");

        IO.println("Fourier transform...");
        Channel[] ffts = height.fft();
        ffts[0].copy().dynamicRange().toLayer().saveAsPNG(name + "_fft_magnitude");
        ffts[0].log().dynamicRange().toLayer().saveAsPNG(name + "_fft_magnitude_log");
        histogram(ffts[0], height.width).flipV().toLayer().saveAsPNG(name + "_fft_magnitude_log_histogram");
        ffts[1].dynamicRange().toLayer().saveAsPNG(name + "_fft_phase");
        histogram(ffts[1], height.width).flipV().toLayer().saveAsPNG(name + "_fft_phase_histogram");
        IO.println("done");

        IO.println("*** Image analysis complete ****");
    }

    public static @NonNull Channel histogram(@NonNull Channel channel, int size) {
        assert channel.findMin() >= 0 && channel.findMax() <= 1 : "image must be normalized";
        Channel hist = new Channel(size, size).fill(1f);
        int[] histogram = new int[size];
        for (int y = 0; y < channel.width; y++) {
            for (int x = 0; x < channel.height; x++) {
                histogram[(int) (channel.getPixel(x, y) * (size - 1))]++;
            }
        }
        int max = 0;
        for (int i = 0; i < size; i++) {
            if (histogram[i] > max) {
                max = histogram[i];
            }
        }
        float scale = (float) (size) / max;
        for (int x = 0; x < size; x++) {
            int lineheight = (int) (histogram[x] * scale);
            for (int y = 0; y < lineheight; y++) {
                hist.putPixel(x, y, 0f);
            }
        }
        return hist;
    }

    public static void score(@NonNull Channel channel, String name) {
        float average = average(channel);
        float variance = variance(channel);
        float deviation = standardDeviation(variance);
        IO.println(name + " erosion score: " + deviation / average);
    }

    public static float score(@NonNull Channel channel) {
        float average = average(channel);
        float variance = variance(channel);
        float deviation = standardDeviation(variance);
        return deviation / average;
    }

    public static void statistics(@NonNull Channel channel, String name) {
        float average = average(channel);
        float variance = variance(channel);
        float deviation = standardDeviation(variance);
        IO.println(name + " average: " + average);
        IO.println(name + " standard deviation: " + deviation);
    }

    public static float average(@NonNull Channel channel) {
        float sum = 0;
        for (int x = 0; x < channel.width; x++) {
            for (int y = 0; y < channel.height; y++) {
                sum += channel.getPixel(x, y);
            }
        }
        return sum / (channel.width * channel.height);
    }

    public static float variance(@NonNull Channel channel) {
        float average = average(channel);
        float sum = 0;
        for (int x = 0; x < channel.width; x++) {
            for (int y = 0; y < channel.height; y++) {
                float value = channel.getPixel(x, y) - average;
                sum += value * value;
            }
        }
        return sum / (channel.width * channel.height);
    }

    public static float deviation(@NonNull Channel channel) {
        return (float) Math.sqrt(variance(channel));
    }

    public static float standardDeviation(float variance) {
        return (float) Math.sqrt(variance);
    }

    public static @NonNull Channel connectedness(@NonNull Channel height, int steps) {
        IO.print("Analyzing connectedness");
        Channel channel = new Channel(height.width, height.height);
        Channel slope = height.copy().lineart();
        for (int i = 0; i < steps; i++) {
            //channel.channelBrightest(slope.copy().threshold(0f, 16f*i/(height.width*steps)).largestConnected(1f).multiply((float)(steps - i)/steps));
            channel.channelAdd(slope.copy().threshold(0f, 16f * i / (height.width * steps)).largestConnected(
                    1f).multiply(1f / steps));
            IO.print(".");
        }
        IO.println("done");
        return channel;
    }

    public static float squareScore(@NonNull Channel height, float threshold, int square_size) {
        return height.copy().lineart().threshold(0f, threshold).squareFit(1f, square_size).count(
                1f) / (float) (height.width * height.height);
    }

    private Analyzer() {
    }

}
