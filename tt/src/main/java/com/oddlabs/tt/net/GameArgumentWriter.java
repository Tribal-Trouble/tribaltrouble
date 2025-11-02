package com.oddlabs.tt.net;

import com.oddlabs.net.DefaultARMIArgumentWriter;
import com.oddlabs.util.ByteBufferOutputStream;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

final class GameArgumentWriter extends DefaultARMIArgumentWriter {
	private final DistributableTable distributable_table;

	GameArgumentWriter(DistributableTable table) {
		this.distributable_table = table;
	}

        @Override
	public void writeArgument(@NonNull Class<?> type, @NonNull Object arg, @NonNull ByteBufferOutputStream out) throws IOException {
		if (Distributable.class.isAssignableFrom(type)) {
			int name = distributable_table.getName((Distributable)arg);
			out.buffer().putInt(name);
		} else if (Distributable[].class.isAssignableFrom(type)) {
			Distributable[] distributables = (Distributable[])arg;
			out.buffer().putShort((short)distributables.length);
                    for (Distributable distributable : distributables) {
                        int name = distributable_table.getName(distributable);
                        out.buffer().putInt(name);
                    }
		} else {
			super.writeArgument(type, arg, out);
		}
	}
}
