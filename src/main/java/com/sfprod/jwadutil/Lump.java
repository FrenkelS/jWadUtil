package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record Lump(byte[] name, byte[] data) {
	public Lump(byte[] name, ByteBuffer byteBuffer) {
		this(name, byteBuffer.array());
	}

	public Lump(byte[] name, int size, ByteBuffer byteBuffer) {
		this(name, ByteBufferUtils.toByteArray(byteBuffer, size));
	}

	public int length() {
		return data.length;
	}

	public String nameAsString() {
		return new String(name, StandardCharsets.US_ASCII).trim();
	}

	public ByteBuffer dataAsByteBuffer() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		return byteBuffer;
	}
}
