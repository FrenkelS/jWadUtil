package com.sfprod.jwadutil;

public interface ByteUtils {

	/**
	 * Convert signed byte to an unsigned int.
	 *
	 * @param b
	 * @return
	 */
	static int toInt(byte b) {
		return b & 0xff;
	}
}
