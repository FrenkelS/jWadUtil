package com.sfprod.jwadutil.amiga;

import java.nio.ByteOrder;
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor2ColorsAmiga extends WadProcessor4Colors {

	public WadProcessor2ColorsAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false, true);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DP"); // PC speaker sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(AmigaUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
