package com.oddlabs.net;

import java.io.Serial;

public class IllegalARMIEventException extends Exception {
    @Serial
    private static final long serialVersionUID = 6874182030169648695L;

    public IllegalARMIEventException(String message) {
        super(message);
    }

    public IllegalARMIEventException(Throwable e) {
        super(e);
    }
}
