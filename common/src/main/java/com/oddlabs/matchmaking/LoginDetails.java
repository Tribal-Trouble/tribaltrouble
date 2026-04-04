package com.oddlabs.matchmaking;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class LoginDetails implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public static final int MAX_EMAIL_LENGTH = 60;

    private final String email;

    public LoginDetails(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof LoginDetails other_login))
            return false;
        return other_login.getEmail().equals(email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    public boolean isValid() {
        return email != null && email.length() <= MAX_EMAIL_LENGTH && Utils.EMAIL_PATTERN.matcher(email).matches();
    }

    public String getEmail() {
        return email;
    }
}
