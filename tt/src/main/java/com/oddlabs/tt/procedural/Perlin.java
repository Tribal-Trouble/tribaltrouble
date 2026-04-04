package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.procedural.Tools;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Random;

public final class Perlin {

    public enum Interpolation {
        LINEAR,
        SMOOTH,
        CUBIC,
        AUTO
    }

    public enum Summation {
        NORMAL,
        ABS,
        SINE,
        XSINE,
        NORMAL_MOD,
        ABS_MOD,
        XSINE_MOD,
        WOOD1,
        WOOD2
    }

    public Channel channel;
    public Channel[] noise_channels;

    public Perlin(int width, int height, int x_factor, int y_factor, float pers, int oct, long seed, @NonNull Interpolation interpolation, @NonNull Summation summation) {
        assert Utils.isPowerOf2(width) : "width must be power of 2";
        assert Utils.isPowerOf2(height) : "height must be power of 2";
        assert Utils.isPowerOf2(x_factor) : "x_factor must be power of 2";
        assert Utils.isPowerOf2(y_factor) : "y_factor must be power of 2";
        generateNoiseChannels(x_factor, y_factor, oct, seed);
        mergeNoiseChannels(width, height, pers, oct, interpolation);
        transformImage(width, height, summation);
        channel.dynamicRange();
    }

    // generate noise octaves
    private void generateNoiseChannels(int x_factor, int y_factor, int oct, long seed) {
        x_factor = Math.max(2, x_factor);
        y_factor = Math.max(2, y_factor);
        noise_channels = new Channel[oct];
        Random random = new Random(seed);
        for (int i = 0; i < oct; i++) {
            int width_noise = x_factor * (1 << i);
            int height_noise = y_factor * (1 << i);
            noise_channels[i] = new Channel(width_noise, height_noise);
            for (int y = 0; y < height_noise; y++) {
                for (int x = 0; x < width_noise; x++) {
                    noise_channels[i].putPixel(x, y, random.nextFloat());
                }
            }
        }
    }

    // interpolate and sum octave channels
    private void mergeNoiseChannels(int width, int height, float pers, int oct, @NonNull Interpolation interpolation) {
        channel = new Channel(width, height);
        int method_threshold = 0;
        if (interpolation == Interpolation.SMOOTH) {
            method_threshold = noise_channels.length;
        } else if (interpolation == Interpolation.AUTO) {
            while (width >> 3 > (int) Math.pow(2, method_threshold))
                method_threshold++;
        }

        for (int i = 0; i < oct; i++) {
            Channel octave = noise_channels[i];
            float amplitude = 2 * (float) Math.pow(pers, i);
            float height_ratio = (float) octave.height / height;
            float width_ratio = (float) octave.width / width;
            int block_height = height / octave.height;
            int block_width = height / octave.width;
            int y_blocks = octave.height;
            int x_blocks = octave.width;
            int y_block_lo, y_block_hi, x_block_lo, x_block_hi, x_pixel, y_pixel;
            float val, val1, val2;

            if (width == octave.width && height == octave.height) { // no interpolation needed when octave matches image size
                for (y_pixel = 0; y_pixel < height; y_pixel++) {
                    for (x_pixel = 0; x_pixel < width; x_pixel++) {
                        channel.putPixel(x_pixel, y_pixel, channel.getPixel(x_pixel, y_pixel) + octave.getPixel(x_pixel, y_pixel) * amplitude);
                    }
                }
            } else if (interpolation != Interpolation.CUBIC && i >= method_threshold) { // interpolate linear
                float y_incr1, y_incr2, x_incr;
                for (y_block_lo = 0; y_block_lo < y_blocks; y_block_lo++) {
                    y_block_hi = (y_block_lo + 1) % octave.height;
                    for (x_block_lo = 0; x_block_lo < x_blocks; x_block_lo++) {
                        x_block_hi = (x_block_lo + 1) % octave.width;
                        y_incr1 = (octave.getPixel(x_block_lo, y_block_hi) - octave.getPixel(x_block_lo, y_block_lo)) / block_height;
                        y_incr2 = (octave.getPixel(x_block_hi, y_block_hi) - octave.getPixel(x_block_hi, y_block_lo)) / block_height;
                        val1 = octave.getPixel(x_block_lo, y_block_lo) - 0.5f * y_incr1;
                        val2 = octave.getPixel(x_block_hi, y_block_lo) - 0.5f * y_incr2;
                        for (int y = 0; y < block_height; y++) {
                            y_pixel = y + y_block_lo * block_height;
                            val1 += y_incr1;
                            val2 += y_incr2;
                            x_incr = (val2 - val1) / block_width;
                            val = val1 - 0.5f * x_incr;
                            for (int x = 0; x < block_width; x++) {
                                x_pixel = x + x_block_lo * block_width;
                                val += x_incr;
                                channel.putPixel(x_pixel, y_pixel, channel.getPixel(x_pixel, y_pixel) + val * amplitude);
                            }
                        }
                    }
                }
            } else if (interpolation != Interpolation.CUBIC) { // interpolate smooth
                float y_coord, x_coord, y_diff, x_diff;
                for (y_block_lo = 0; y_block_lo < y_blocks; y_block_lo++) {
                    y_block_hi = (y_block_lo + 1) % octave.height;
                    for (int y = 0; y < block_height; y++) {
                        y_pixel = y + y_block_lo * block_height;
                        y_coord = y_pixel * height_ratio;
                        y_diff = y_coord - y_block_lo;
                        for (x_block_lo = 0; x_block_lo < x_blocks; x_block_lo++) {
                            x_block_hi = (x_block_lo + 1) % octave.width;
                            val1 = Tools.interpolateSmooth(octave.getPixel(x_block_lo, y_block_lo), octave.getPixel(x_block_lo, y_block_hi), y_diff);
                            val2 = Tools.interpolateSmooth(octave.getPixel(x_block_hi, y_block_lo), octave.getPixel(x_block_hi, y_block_hi), y_diff);
                            for (int x = 0; x < block_width; x++) {
                                x_pixel = x + x_block_lo * block_width;
                                x_coord = x_pixel * width_ratio;
                                x_diff = x_coord - x_block_lo;
                                val = Tools.interpolateSmooth(val1, val2, x_diff);
                                channel.putPixel(x_pixel, y_pixel, channel.getPixel(x_pixel, y_pixel) + val * amplitude);
                            }
                        }
                    }
                }
            } else { // interpolate cubic
                float y_coord, x_coord, y_diff, x_diff, val0, val3;
                int y_block_lolo, y_block_hihi, x_block_lolo, x_block_hihi;
                for (y_block_lo = 0; y_block_lo < y_blocks; y_block_lo++) {
                    y_block_hi = y_block_lo + 1;
                    y_block_lolo = y_block_lo - 1;
                    y_block_hihi = y_block_hi + 1;
                    for (int y = 0; y < block_height; y++) {
                        y_pixel = y + y_block_lo * block_height;
                        y_coord = y_pixel * height_ratio;
                        y_diff = y_coord - y_block_lo;
                        for (x_block_lo = 0; x_block_lo < x_blocks; x_block_lo++) {
                            x_block_hi = x_block_lo + 1;
                            x_block_lolo = x_block_lo - 1;
                            x_block_hihi = x_block_hi + 1;
                            val0 = Tools.interpolateCubic(
                                    octave.getPixelWrap(x_block_lolo, y_block_lolo),
                                    octave.getPixelWrap(x_block_lolo, y_block_lo),
                                    octave.getPixelWrap(x_block_lolo, y_block_hi),
                                    octave.getPixelWrap(x_block_lolo, y_block_hihi),
                                    y_diff);
                            val1 = Tools.interpolateCubic(
                                    octave.getPixelWrap(x_block_lo, y_block_lolo),
                                    octave.getPixelWrap(x_block_lo, y_block_lo),
                                    octave.getPixelWrap(x_block_lo, y_block_hi),
                                    octave.getPixelWrap(x_block_lo, y_block_hihi),
                                    y_diff);
                            val2 = Tools.interpolateCubic(
                                    octave.getPixelWrap(x_block_hi, y_block_lolo),
                                    octave.getPixelWrap(x_block_hi, y_block_lo),
                                    octave.getPixelWrap(x_block_hi, y_block_hi),
                                    octave.getPixelWrap(x_block_hi, y_block_hihi),
                                    y_diff);
                            val3 = Tools.interpolateCubic(
                                    octave.getPixelWrap(x_block_hihi, y_block_lolo),
                                    octave.getPixelWrap(x_block_hihi, y_block_lo),
                                    octave.getPixelWrap(x_block_hihi, y_block_hi),
                                    octave.getPixelWrap(x_block_hihi, y_block_hihi),
                                    y_diff);
                            for (int x = 0; x < block_width; x++) {
                                x_pixel = x + x_block_lo * block_width;
                                x_coord = x_pixel * width_ratio;
                                x_diff = x_coord - x_block_lo;
                                val = Tools.interpolateCubic(
                                        val0, val1, val2, val3, x_diff);
                                channel.putPixelWrap(x_pixel, y_pixel, channel.getPixelWrap(x_pixel, y_pixel) + val * amplitude);
                            }
                        }
                    }
                }
            }
        }
    }

    // transform image
    private void transformImage(int width, int height, @NonNull Summation summation) {
        float value = 0;
        switch (summation) {
            case NORMAL:
                break;
            case ABS:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        channel.putPixel(x, y, Math.abs(channel.getPixel(x, y)));
                    }
                }
                break;
            case SINE:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        channel.putPixel(x, y, (float) Math.sin(Math.PI * 8 * channel.getPixel(x, y)));
                    }
                }
                break;
            case XSINE:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        channel.putPixel(x, y, (float) Math.sin(x / ((width / 256f) * Math.PI) + channel.getPixel(x, y)));
                    }
                }
                break;
            case NORMAL_MOD:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        value = channel.getPixel(x, y);
                        value = value - (int) (value);
                        channel.putPixel(x, y, value);
                    }
                }
                break;
            case ABS_MOD:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        value = Math.abs(channel.getPixel(x, y));
                        value = value - (int) (value);
                        channel.putPixel(x, y, value);
                    }
                }
                break;
            case XSINE_MOD:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        value = (float) Math.sin(x / ((width / 256f) * Math.PI) + channel.getPixel(x, y));
                        value = value - (int) (value);
                        channel.putPixel(x, y, value);
                    }
                }
                break;
            case WOOD1:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        value = 4 * channel.getPixel(x, y);
                        value = value - (int) (value);
                        value = 1 - value * value;
                        channel.putPixel(x, y, value);
                    }
                }
                break;
            case WOOD2:
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        value = 0.5f * (1 + (float) Math.sin(x / ((width / 256f) * Math.PI) + channel.getPixel(x, y)));
                        value = 1 - value * value * value * value;
                        channel.putPixel(x, y, value);
                    }
                }
                break;
            default:
                assert false : "incorrect sum_method";
        }
    }

    public @NonNull Layer toLayer() {
        return new Layer(channel, channel.copy(), channel.copy());
    }

    public Channel toChannel() {
        return channel;
    }

}
