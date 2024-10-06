package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public interface ByteBufferUtils {

	static ByteBuffer newByteBuffer() {
		return newByteBuffer(65536);
	}

	static ByteBuffer newByteBuffer(int capacity) {
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	static byte[] toByteArray(ByteBuffer byteBuffer, int newLength) {
		return Arrays.copyOf(byteBuffer.array(), newLength);
	}

}
