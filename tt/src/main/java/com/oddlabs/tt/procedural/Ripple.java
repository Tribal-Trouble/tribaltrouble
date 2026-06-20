package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import org.jspecify.annotations.NonNull;

/**
 * Procedural texture generator for creating ripple effects, typically used for water or impact waves.
 */
public final class Ripple {
    public final @NonNull Channel channel;

    public Ripple(int width, int height, float point_x, float point_y, float factor) {

        // create image
        channel = new Channel(width, height);

        // fill in pixelvalues
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                float x_coord = (float) x / width;
                float y_coord = (float) y / height;

                float dx = Math.abs(x_coord - point_x);
                float dy = Math.abs(y_coord - point_y);
                float dx1 = 1 + dx;
                float dy1 = 1 + dy;
                float dx2 = dx;
                float dy2 = dy1;
                float dx3 = 1 - dx;
                float dy3 = dy1;
                float dx4 = dx1;
                float dy4 = dy;
                float dx5 = dx3;
                float dy5 = dy;
                float dx6 = dx1;
                float dy6 = 1 - dy;
                float dx7 = dx;
                float dy7 = dy6;
                float dx8 = dx3;
                float dy8 = dy6;

                float dist = (float) Math.hypot(dx, dy);
                float dist1 = (float) Math.hypot(dx1, dy1);
                float dist2 = (float) Math.hypot(dx2, dy2);
                float dist3 = (float) Math.hypot(dx3, dy3);
                float dist4 = (float) Math.hypot(dx4, dy4);
                float dist5 = (float) Math.hypot(dx5, dy5);
                float dist6 = (float) Math.hypot(dx6, dy6);
                float dist7 = (float) Math.hypot(dx7, dy7);
                float dist8 = (float) Math.hypot(dx8, dy8);

                if (dist > 1) dist = 1f;
                if (dist1 > 1) dist1 = 1f;
                if (dist2 > 1) dist2 = 1f;
                if (dist3 > 1) dist3 = 1f;
                if (dist4 > 1) dist4 = 1f;
                if (dist5 > 1) dist5 = 1f;
                if (dist6 > 1) dist6 = 1f;
                if (dist7 > 1) dist7 = 1f;
                if (dist8 > 1) dist8 = 1f;

                float value = (float) (Math.cos(factor * dist) * (-dist + 1) + Math.cos(
                        factor * dist1) * (-dist1 + 1) + Math.cos(factor * dist2) * (-dist2 + 1) + Math.cos(
                                factor * dist3) * (-dist3 + 1) + Math.cos(factor * dist4) * (-dist4 + 1) + Math.cos(
                                        factor * dist5) * (-dist5 + 1) + Math.cos(
                                                factor * dist6) * (-dist6 + 1) + Math.cos(
                                                        factor * dist7) * (-dist7 + 1) + Math.cos(
                                                                factor * dist8) * (-dist8 + 1));
                //if (value < 0) {
                //	value = -(float)Math.sqrt(-value);
                //} else {
                //	value = (float)Math.sqrt(value);
                //}
                channel.putPixel(x, y, value);
            }
        }

        // normalize image
        channel.dynamicRange();

    }

    public @NonNull Layer toLayer() {
        return new Layer(channel, channel, channel);
    }

    public @NonNull Channel toChannel() {
        return channel;
    }

}
