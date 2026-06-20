package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;
import org.jspecify.annotations.NonNull;

public final class Gradient {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public enum Interpolation {
        LINEAR,
        SMOOTH,
        //POLYNOMIAL
    }

    public final @NonNull Channel channel;

    public Gradient(int width, int height, float @NonNull [] @NonNull [] gradient_list,
            @NonNull Orientation orientation, @NonNull Interpolation interpolation) {
        channel = new Channel(width, height);
        float x_coord = 0;
        int index = 0;
        int index_max = gradient_list.length - 1;
        float value = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                x_coord = (float) x / width;
                if (x_coord >= gradient_list[index][0] && index < index_max) index++;
                if (x_coord < gradient_list[0][0]) {
                    value = gradient_list[0][1];
                } else {
                    if (x_coord >= gradient_list[index_max][0]) {
                        value = gradient_list[index_max][1];
                    } else {
                        value = switch (interpolation) {
                            case LINEAR ->
                                Tools.interpolateLinear(gradient_list[index - 1][1], gradient_list[index][1], (x_coord
                                        - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index
                                                - 1][0]));
                            case SMOOTH ->
                                Tools.interpolateSmooth(gradient_list[index - 1][1], gradient_list[index][1], (x_coord
                                        - gradient_list[index - 1][0]) / (gradient_list[index][0] - gradient_list[index
                                                - 1][0]));
                        };
                    }
                }
                switch (orientation) {
                    case HORIZONTAL -> channel.putPixel(x, y, value);
                    case VERTICAL -> channel.putPixel(y, x, value);
                }
            }
        }
    }

    public @NonNull Layer toLayer() {
        return new Layer(channel, channel, channel);
    }

    public @NonNull Channel toChannel() {
        return channel;
    }

}
