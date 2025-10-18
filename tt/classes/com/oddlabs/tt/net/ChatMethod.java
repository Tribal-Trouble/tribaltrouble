package com.oddlabs.tt.net;

import com.oddlabs.tt.gui.InfoPrinter;

@FunctionalInterface
public interface ChatMethod {
	void execute(InfoPrinter info_printer, String text);
}
