package com.sfprod.jwadutil.attunixpc;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor2ColorsAttUnixPC extends WadProcessor4Colors {

	public WadProcessor2ColorsAttUnixPC(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false, false);
	}

	@Override
	protected void changePaletteRaw(Lump lumpToReplace) {
		super.changePaletteRaw(lumpToReplace);

		Lump lump = wadFile.getLumpByName(lumpToReplace.nameAsString());
		for (int i = 0; i < lump.length() / 2; i++) {
			byte e = lump.data()[i * 2 + 0];
			byte o = lump.data()[i * 2 + 1];
			lump.data()[i * 2 + 1] = e;
			lump.data()[i * 2 + 0] = o;
		}
	}
}
