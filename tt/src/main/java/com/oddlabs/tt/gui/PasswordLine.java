package com.oddlabs.tt.gui;

import com.oddlabs.util.CryptUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * provides password entry substituting the password characters with asterisks when displayed
 */
public class PasswordLine extends EditLine {
    private @Nullable String password_digest;

    public PasswordLine(int width, int max_chars) {
        super(width, max_chars);
    }

    /**
     * Password display text has three options. If the field is empty then the empty string is displayed. When the
     * field is inactive it is filled with stars. When the field is active a string of stars the same length as the
     * password cleartext is returned.
     *
     * @return The blank text or stars to display
     */
    @Override
    protected @NonNull CharSequence getDisplayText() {
        if (getText().isEmpty()) {
            return "";
        }
        if (isActive()) {
            return "*".repeat(getText().length());
        } else {
            int asteriskWidth = getRenderedWidth("*");
            if (asteriskWidth == 0) return "";
            int numAsterisks = max_text_width / asteriskWidth;
            return "*".repeat(numAsterisks);
        }
    }

    @Override
    protected final boolean insert(int index, char key) {
        boolean result = super.insert(index, key);
        updatePassword();
        return result;

    }

    @Override
    protected final void delete(int index) {
        super.delete(index);
        updatePassword();
    }

    private void updatePassword() {
        password_digest = CryptUtils.digest(getText().toString());
    }

    public final @Nullable String getPasswordDigest() {
        return password_digest;
    }

    public final void setPasswordDigest(@Nullable String password_digest) {
        this.password_digest = password_digest;
    }
}
