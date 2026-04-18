package com.sfprod.jwadutil.archimedes;

import static com.sfprod.utils.NumberUtils.reverse;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor2Colors;

public class WadProcessor2ColorsArchimedes extends WadProcessor2Colors {

	public WadProcessor2ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
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
