package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class ARMIInterfaceMethods {
    private final @NonNull Class<?> armi_interface;
    private final Method @NonNull [] methods;

    public ARMIInterfaceMethods(@NonNull Class<?> armi_interface) {
        assert armi_interface.isInterface();
        this.armi_interface = armi_interface;
        this.methods = armi_interface.getMethods();
        Arrays.sort(methods, new MethodComparator());
        for (Method method : methods) {
            assert isLegal(method);
        }
    }

    private boolean isLegal(@NonNull Method method) {
        return method.getReturnType().equals(void.class) && method.getExceptionTypes().length == 0;
    }

    boolean isInstance(Object instance) {
        return armi_interface.isInstance(instance);
    }

    @NonNull Class<?> getInterfaceClass() {
        return armi_interface;
    }

    void invoke(Object instance, @NonNull Method method, Object[] args) throws IllegalARMIEventException {
        try {
            method.invoke(instance, args);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalARMIEventException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalARMIEventException(e);
        }
    }

    Method getMethod(byte index) {
        return methods[index];
    }

    byte getMethodIndex(Method method) {
        for (byte i = 0; i < methods.length; i++) {
            if (methods[i].equals(method))
                return i;
        }
        throw new RuntimeException("Unknown method: " + method);
    }
}
