package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.ChatRoomUser;
import com.oddlabs.matchmaking.NickUtils;
import com.oddlabs.tt.gui.ChatPanel;
import com.oddlabs.tt.util.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

public final strictfp class ChatRoomHistory extends ChatHistory {

    private ChatRoomUser[] old_users;

    final void update(ChatRoomUser[] new_users) {
        if (old_users == null) return;
        Set new_users_set = new HashSet(Arrays.asList(new_users));
        Set old_users_set = new HashSet(Arrays.asList(old_users));
        Set joined_users = new HashSet(new_users_set);
        joined_users.removeAll(old_users_set);
        Iterator it = joined_users.iterator();
        ResourceBundle bundle = ResourceBundle.getBundle(ChatPanel.class.getName());
        while (it.hasNext()) {
            ChatRoomUser user = (ChatRoomUser) it.next();
            addMessage(Utils.getBundleString(bundle, "user_joined", new Object[] {NickUtils.toDisplayName(user.getNick())}));
        }
        Set left_users = new HashSet(old_users_set);
        left_users.removeAll(new_users_set);
        it = left_users.iterator();
        while (it.hasNext()) {
            ChatRoomUser user = (ChatRoomUser) it.next();
            addMessage(Utils.getBundleString(bundle, "user_left", new Object[] {NickUtils.toDisplayName(user.getNick())}));
        }
    }

    public final void chat(ChatMessage message) {
        if (message.type != ChatMessage.CHAT_PRIVATE && message.type != ChatMessage.CHAT_CHATROOM)
            return;
        addMessage(message.formatLong());
    }
}
