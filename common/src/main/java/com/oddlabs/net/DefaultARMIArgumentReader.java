package com.oddlabs.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;

public class DefaultARMIArgumentReader implements ARMIArgumentReader {
    @Override
    public @Nullable Object readArgument(@NonNull Class<?> type, @NonNull ByteBufferInputStream in) throws IOException, ClassNotFoundException {
        if (type.equals(char.class)) {
            return in.buffer().getChar();
        } else if (type.equals(byte.class)) {
            return in.buffer().get();
        } else if (type.equals(short.class)) {
            return in.buffer().getShort();
        } else if (type.equals(int.class)) {
            return in.buffer().getInt();
        } else if (type.equals(long.class)) {
            return in.buffer().getLong();
        } else if (type.equals(float.class)) {
            return in.buffer().getFloat();
        } else if (type.equals(double.class)) {
            return in.buffer().getDouble();
        } else if (type.equals(boolean.class)) {
            return in.buffer().get() != 0;
        } else if (type.equals(HostSequenceID.class)) {
            int host_id = in.buffer().getInt();
            int seq_id = in.buffer().getInt();
            return new HostSequenceID(host_id, seq_id);
        } else if (type.equals(ARMIEvent.class)) {
            short event_size = in.buffer().getShort();
            return ARMIEvent.read(in.buffer(), event_size);
        } else {
            try (ObjectInputStream input_stream = new ObjectInputStream(in)) {
                return input_stream.readObject();
            }
        }
    }
}
