module com.oddlabs.common {
	requires transitive org.joml;
	requires static org.jspecify;
	requires java.desktop;
	requires java.logging;

	exports com.oddlabs.event;
	exports com.oddlabs.geometry;
	exports com.oddlabs.http;
	exports com.oddlabs.matchmaking;
	exports com.oddlabs.net;
	exports com.oddlabs.procedural;
	exports com.oddlabs.registration;
	exports com.oddlabs.router;
	exports com.oddlabs.util;
}
