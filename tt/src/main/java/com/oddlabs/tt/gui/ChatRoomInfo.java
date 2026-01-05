package com.oddlabs.tt.gui;

import com.oddlabs.matchmaking.ChatRoomUser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record ChatRoomInfo(@NonNull String name, @NonNull ChatRoomUser @Nullable [] users) {
}
