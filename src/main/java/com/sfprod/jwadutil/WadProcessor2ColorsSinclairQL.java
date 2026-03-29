package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.List;

class WadProcessor2ColorsSinclairQL extends WadProcessor4Colors {

	WadProcessor2ColorsSinclairQL(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DS"); // Sound Blaster sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DP");
		lumps.stream().map(SinclairQLUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
