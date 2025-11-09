package com.oddlabs.tt.net;

import com.oddlabs.net.ByteBufferInputStream;
import com.oddlabs.net.DefaultARMIArgumentReader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;

final class GameArgumentReader extends DefaultARMIArgumentReader {
	private final DistributableTable distributable_table;

	GameArgumentReader(DistributableTable table) {
		this.distributable_table = table;
	}

	@Override
	public @Nullable Object readArgument(@NonNull Class<?> type, @NonNull ByteBufferInputStream in) throws IOException, ClassNotFoundException {
		if (Distributable.class.isAssignableFrom(type)) {
			int name = in.buffer().getInt();
			return distributable_table.getDistributable(name);
		} else if (Distributable[].class.isAssignableFrom(type)) {
			short length = in.buffer().getShort();
			Distributable[] distributables = (Distributable[])Array.newInstance(type.getComponentType(), length);
			for (int j = 0; j < distributables.length; j++) {
				int name = in.buffer().getInt();
				distributables[j] = distributable_table.getDistributable(name);
			}
			return distributables;
		} else {
			return super.readArgument(type, in);
		}
	}
}
