package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.TabListener;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class ChatLine extends EditLine {
	private final List<TabListener> tab_listeners = new ArrayList<>();
	private final boolean catch_tab;

	private String[] tab_complete_list;

	public ChatLine(int width, int max_chars, boolean catch_tab) {
		super(width, max_chars);
		this.catch_tab = catch_tab;
		this.tab_complete_list = new String[0];
	}

	public void setTabCompleteList(String[] list) {
		tab_complete_list = list;
	}

        @Override
	protected void keyRepeat(@NonNull KeyboardEvent e) {
		if (catch_tab && e.getKeyChar() == '\t')
			tabComplete(getText());
		else
			super.keyRepeat(e);
	}

	private void tabComplete(@NonNull StringBuffer line) {
		int index = getIndex();

		int word_start = line.lastIndexOf(" ", index - 1) + 1;
		if (word_start == -1)
			word_start = 0;

		int word_end = line.indexOf(" ", index);
		if (word_end == -1)
			word_end = line.length();

		String partial_word = line.substring(word_start, word_end);

		List<String> new_words = new ArrayList<>();
		int num_hits = 0;
            for (String tab_word : tab_complete_list) {
                if (tab_word.startsWith(partial_word)) {
                    num_hits++;
                    new_words.add(tab_word);
                }
            }
		if (num_hits == 1) {
			String new_word = new_words.getFirst();
			line.replace(word_start, word_end, new_word);
			// move index to end of new_word
			int new_index = word_start + new_word.length();
			setIndex(new_index);
		} else if (num_hits > 1) {
			String[] tab_words = new String[new_words.size()];
			for (int i = 0; i < new_words.size(); i++) {
                tab_words[i] = new_words.get(i);
            }
			tabPressedAll(tab_words);
		}
	}

/*	private final void saveHistory() {
		if (current != start)
		   history[start] = history[current].copy();
	}
	protected final void newLine() {
		if (running_cmd) {
			super.newLine();
			return;
		}
		saveHistory();
		String command = extractCommand(start);
		runCommand(command);
	}
*/
	private void tabPressedAll(String[] words) {
		tabPressed(words);
        for (TabListener listener : tab_listeners) {
            if (listener != null)
                listener.tabPressed(words);
        }
	}

	void tabPressed(String[] words) {
	}

	public void addTabListener(TabListener listener) {
		tab_listeners.add(listener);
	}
}

