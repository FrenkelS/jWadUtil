package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;

import com.sfprod.utils.ByteBufferUtils;

interface AmigaUtil {

	static Lump processSoundEffect(Lump vanillaDigitalSoundlump) {
		ByteBuffer vanillaData = vanillaDigitalSoundlump.dataAsByteBuffer();
		vanillaData.getShort(); // Format number (must be 3)
		vanillaData.getShort(); // Sample rate (usually, but not necessarily, 11025)
		int length = vanillaData.getInt() - 32; // Number of samples + 32 pad bytes

		byte[] tmpBuffer = new byte[16];
		vanillaData.get(tmpBuffer);

		// from unsigned 8-bit to signed 8-bit
		byte[] signedBytes = new byte[length];
		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			int x = toInt(b) - 128;
			if (x < 0) {
				x = 0;
			}
			signedBytes[i] = toByte(x);
		}

		return new Lump(vanillaDigitalSoundlump.name(), signedBytes, ByteBufferUtils.DONT_CARE);
	}
}
