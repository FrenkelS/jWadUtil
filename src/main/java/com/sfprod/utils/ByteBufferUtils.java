package com.sfprod.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public interface ByteBufferUtils {

	ByteOrder DONT_CARE = ByteOrder.LITTLE_ENDIAN;

	@Deprecated
	static ByteBuffer newByteBuffer() {
		return newByteBuffer(65536);
	}

	static ByteBuffer newByteBuffer(ByteOrder byteOrder) {
		ByteBuffer bb = ByteBuffer.allocate(65536);
		bb.order(byteOrder);
		return bb;
	}

	@Deprecated
	static ByteBuffer newByteBuffer(int capacity) {
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb;
	}

	static ByteBuffer newByteBuffer(ByteOrder byteOrder, int capacity) {
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(byteOrder);
		return bb;
	}

	static byte[] toArray(ByteBuffer byteBuffer, int newLength) {
		return Arrays.copyOf(byteBuffer.array(), newLength);
	}

}
