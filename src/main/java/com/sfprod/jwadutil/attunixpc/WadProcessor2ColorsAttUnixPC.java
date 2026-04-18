package com.sfprod.jwadutil.attunixpc;

import static com.sfprod.utils.NumberUtils.reverse;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor2ColorsAttUnixPC extends WadProcessor4Colors {

	public WadProcessor2ColorsAttUnixPC(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void changePaletteRaw(Lump lumpToReplace) {
		super.changePaletteRaw(lumpToReplace);

		Lump lump = wadFile.getLumpByName(lumpToReplace.nameAsString());
		for (int i = 0; i < lump.length() / 2; i++) {
			byte e = lump.data()[i * 2 + 0];
			byte o = lump.data()[i * 2 + 1];
			lump.data()[i * 2 + 1] = reverse(e);
			lump.data()[i * 2 + 0] = reverse(o);
		}
	}

	@Override
	protected byte convert256to16(byte b) {
		byte temp = super.convert256to16(b);
		return toByte(toInt(temp) >> 6);
	}
}
