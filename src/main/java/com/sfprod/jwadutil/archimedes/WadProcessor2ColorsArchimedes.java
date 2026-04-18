package com.sfprod.jwadutil.archimedes;

import static com.sfprod.utils.NumberUtils.reverse;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor2ColorsArchimedes extends WadProcessor4Colors {

	public WadProcessor2ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false, false);
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
}
