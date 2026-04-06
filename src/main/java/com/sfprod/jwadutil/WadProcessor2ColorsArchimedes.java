package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.reverse;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteOrder;

class WadProcessor2ColorsArchimedes extends WadProcessor4Colors {

	WadProcessor2ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void changePaletteRaw(Lump lumpToReplace) {
		super.changePaletteRaw(lumpToReplace);

		Lump lump = wadFile.getLumpByName(lumpToReplace.nameAsString());
		for (int i = 0; i < lump.length(); i++) {
			byte b = lump.data()[i];
			lump.data()[i] = reverse(b);
		}
	}

	@Override
	protected byte convert256to16(byte b) {
		byte temp = super.convert256to16(b);
		return toByte(toInt(temp) >> 6);
	}
}
