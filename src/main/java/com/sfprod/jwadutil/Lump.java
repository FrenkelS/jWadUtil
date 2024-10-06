package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.toArray;
import static com.sfprod.utils.StringUtils.toByteArray;
import static com.sfprod.utils.StringUtils.toStringUpperCase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record Lump(byte[] name, byte[] data) {
	public Lump(String name, byte[] data) {
		this(toByteArray(name, 8), data);
	}

	public Lump(byte[] name, ByteBuffer byteBuffer) {
		this(name, byteBuffer.array());
	}

	public Lump(byte[] name, int size, ByteBuffer byteBuffer) {
		this(name, toArray(byteBuffer, size));
	}

	public int length() {
		return data.length;
	}

	public String nameAsString() {
		return toStringUpperCase(name);
	}

	public ByteBuffer dataAsByteBuffer() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		return byteBuffer;
	}
}
