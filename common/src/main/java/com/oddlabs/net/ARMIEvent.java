package com.oddlabs.net;

import com.oddlabs.util.ByteBufferOutputStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;

public final class ARMIEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private static final ByteBufferOutputStream static_byte_stream = new ByteBufferOutputStream(false);
    private static final short HEADER_SIZE = 1;
    private static final ARMIArgumentWriter default_writer = new DefaultARMIArgumentWriter();
    private static final ARMIArgumentReader default_reader = new DefaultARMIArgumentReader();

    private final byte method_id;
    private final byte[] command_stream;

    public static @NonNull Object createProxy(ARMIEventWriter broker, @NonNull Class<?> armi_interface) {
        return createProxy(broker, default_writer, armi_interface);
    }

    public static @NonNull Object createProxy(ARMIEventWriter broker, ARMIArgumentWriter writer,
            @NonNull Class<?> armi_interface) {
        ARMIInterfaceMethods armi_interface_methods = new ARMIInterfaceMethods(armi_interface);
        ARMIInvocationHandler handler = new ARMIInvocationHandler(broker, writer, armi_interface_methods);
        return Proxy.newProxyInstance(ARMIEvent.class.getClassLoader(), new Class<?>[]{armi_interface}, handler);
    }

    public short getEventSize() {
        int command_stream_length = command_stream != null ? command_stream.length : 0;
        return (short) (HEADER_SIZE + command_stream_length);
    }

    public void write(@NonNull ByteBuffer buffer) {
        buffer.put(method_id);
        if (command_stream != null)
            buffer.put(command_stream);
    }

    public static @NonNull ARMIEvent read(@NonNull ByteBuffer buffer, short size) {
        byte method_id = buffer.get();
        int stream_length = size - HEADER_SIZE;
        byte[] command_stream;
        if (stream_length > 0) {
            command_stream = new byte[stream_length];
            buffer.get(command_stream);
        } else
            command_stream = null;
        return new ARMIEvent(method_id, command_stream);
    }

    private static byte[] createByteArrayFromCommand(@NonNull ARMIArgumentWriter writer,
            Class<?>[] method_parameter_types, Object @Nullable [] args) {
        if (args != null) {
            try {
                static_byte_stream.reset();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    Class<?> type = method_parameter_types[i];
                    writer.writeArgument(type, arg, static_byte_stream);
                }
                return static_byte_stream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else
            return null;
    }

    public ARMIEvent(@NonNull ARMIArgumentWriter writer, Class<?>[] method_parameter_types, byte method_id,
            Object[] args) {
        this(method_id, createByteArrayFromCommand(writer, method_parameter_types, args));
    }

    private ARMIEvent(byte method_id, byte[] command_stream) {
        this.method_id = method_id;
        this.command_stream = command_stream;
    }

    private Object @Nullable [] parseArgs(@NonNull ARMIArgumentReader reader,
            @NonNull Method method) throws IOException, ClassNotFoundException {
        Class<?>[] parameter_types = method.getParameterTypes();
        int num_params = parameter_types.length;
        if (num_params == 0)
            return null;
        Object[] args;
        args = new Object[num_params];
        ByteBufferInputStream byte_stream = new ByteBufferInputStream(command_stream);
        for (int i = 0; i < args.length; i++) {
            Class<?> type = parameter_types[i];
            args[i] = reader.readArgument(type, byte_stream);
        }
        return args;
    }

    public void execute(@NonNull ARMIInterfaceMethods interface_methods,
            @NonNull Object instance) throws IllegalARMIEventException {
        execute(interface_methods, default_reader, instance);
    }

    public void execute(@NonNull ARMIInterfaceMethods interface_methods, @NonNull ARMIArgumentReader reader,
            @NonNull Object instance) throws IllegalARMIEventException {
        Method method;
        try {
            method = interface_methods.getMethod(method_id);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalARMIEventException(e);
        }
        assert instance != null;
        Object[] args;
        try {
            args = parseArgs(reader, method);
        } catch (Exception e) {
            throw new IllegalARMIEventException(e);
        }
        interface_methods.invoke(instance, method, args);
    }
}
