package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.List;

class WadProcessor2ColorsAmiga extends WadProcessor4Colors {

	WadProcessor2ColorsAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DP"); // PC speaker sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(AmigaUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
