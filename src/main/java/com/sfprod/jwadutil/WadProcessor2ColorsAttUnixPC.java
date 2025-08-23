package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteOrder;

class WadProcessor2ColorsAttUnixPC extends WadProcessor4Colors {

	private static final int[] REVERSE_BITS = createReverseBits();

	private static int[] createReverseBits() {
		int[] r = new int[256];
		for (int i = 0; i < r.length; i++) {
			r[i] = Integer.reverse(i) >>> 24;
		}
		return r;
	}

	WadProcessor2ColorsAttUnixPC(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	@Override
	protected void changePaletteRaw(Lump lumpToReplace) {
		super.changePaletteRaw(lumpToReplace);

		Lump lump = wadFile.getLumpByName(lumpToReplace.nameAsString());
		for (int i = 0; i < lump.length() / 2; i++) {
			byte e = lump.data()[i * 2 + 0];
			byte o = lump.data()[i * 2 + 1];
			lump.data()[i * 2 + 1] = toByte(REVERSE_BITS[toInt(e)]);
			lump.data()[i * 2 + 0] = toByte(REVERSE_BITS[toInt(o)]);
		}
	}

	@Override
	protected byte convert256to16(byte b) {
		byte temp = super.convert256to16(b);
		return toByte(toInt(temp) >> 6);
	}
}
