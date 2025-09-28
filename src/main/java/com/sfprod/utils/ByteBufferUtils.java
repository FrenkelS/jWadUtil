package com.sfprod.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface ByteBufferUtils {

	ByteOrder DONT_CARE = ByteOrder.LITTLE_ENDIAN;

	static ByteBuffer newByteBuffer(ByteOrder byteOrder) {
		return newByteBuffer(byteOrder, 65536);
	}

	static ByteBuffer newByteBuffer(ByteOrder byteOrder, int capacity) {
		ByteBuffer bb = ByteBuffer.allocate(capacity);
		bb.order(byteOrder);
		return bb;
	}

	static byte[] toArray(ByteBuffer byteBuffer, int newLength) {
		return Arrays.copyOf(byteBuffer.array(), newLength);
	}

	static List<Byte> toByteList(ByteBuffer byteBuffer) {
		List<Byte> byteList = new ArrayList<>();
		for (byte b : byteBuffer.array()) {
			byteList.add(b);
		}
		return byteList;
	}

}
