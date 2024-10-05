package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ByteBufferUtils {
	private ByteBufferUtils() {
	}

	public static byte[] toByteArray(ByteBuffer byteBuffer, int newLength) {
		return Arrays.copyOf(byteBuffer.array(), newLength);
	}
}
