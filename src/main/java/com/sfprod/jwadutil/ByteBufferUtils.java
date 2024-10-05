package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public interface ByteBufferUtils {

	static byte[] toByteArray(ByteBuffer byteBuffer, int newLength) {
		return Arrays.copyOf(byteBuffer.array(), newLength);
	}

}
