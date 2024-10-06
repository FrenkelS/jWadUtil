package com.sfprod.jwadutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * This class tests {@link ByteUtils}
 */
class ByteUtilsTest {

	@Test
	void toInt() {
		assertEquals(0, ByteUtils.toInt((byte) 0));
		assertEquals(1, ByteUtils.toInt((byte) 1));
		assertEquals(127, ByteUtils.toInt((byte) 127));
		assertEquals(129, ByteUtils.toInt((byte) -127));
		assertEquals(128, ByteUtils.toInt((byte) 128));
		assertEquals(128, ByteUtils.toInt((byte) -128));
		assertEquals(255, ByteUtils.toInt((byte) 0xff));
		assertEquals(255, ByteUtils.toInt((byte) -1));
	}
}
