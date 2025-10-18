package com.oddlabs.tt.net;

import com.oddlabs.router.RouterClientInterface;

public interface RouterHandler extends RouterClientInterface {
	void routerFailed(Exception e);
}
