package com.oddlabs.tt.delegate;

import com.oddlabs.tt.gui.Form;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

@FunctionalInterface
interface FormFactory<F extends Form> extends Supplier<F> {
    @NonNull F create();

    default @NonNull F get() {
        return create();
    }
}
