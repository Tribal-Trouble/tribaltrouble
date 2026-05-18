package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;
import org.jspecify.annotations.NonNull;

public final class Ring {
    public final @NonNull Channel channel;

    public enum Interpolation {
        LINEAR,
        SMOOTH
    }

    public Ring(int width, int height, float @NonNull [] @NonNull [] gradient_list,
            @NonNull Interpolation interpolation) {
        var quarter = new Channel(width >> 1, height >> 1);
        float x_coord;
        float y_coord;
        float radius;
        int index;
        int index_max = gradient_list.length - 1;
        float value = 0;

        for (int x = 0; x < width >> 1; x++) {
            for (int y = 0; y < height >> 1; y++) {
                x_coord = 0.5f - (x + 0.5f) / width;
                y_coord = 0.5f - (y + 0.5f) / height;
                radius = (float) Math.sqrt(x_coord * x_coord + y_coord * y_coord);
                index = 0;

                while (radius >= gradient_list[index][0] && index < index_max) {
                    index++;
                }

                if (radius < gradient_list[0][0]) {
                    value = gradient_list[0][1];
                } else {
                    if (radius >= gradient_list[index_max][0]) {
                        value = gradient_list[index_max][1];
                    } else {
                        value = switch (interpolation) {
                            case LINEAR ->
                                Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1],
                                        (radius - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index - 1][0]));
                            case SMOOTH ->
                                Tools.interpolateSmooth(gradient_list[index - 1][1], gradient_list[index][1],
                                        (radius - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index - 1][0]));
                        };
                    }
                }
                quarter.putPixel(x, y, value);
            }
        }

        channel = new Channel(width, height);
        channel.quadJoin(quarter, quarter.copy().rotate(270), quarter.copy().rotate(90), quarter.copy().rotate(180));
    }

    public @NonNull Layer toLayer() {
        return new Layer(channel, channel.copy(), channel.copy());
    }

    public @NonNull Channel toChannel() {
        return channel;
    }

}
