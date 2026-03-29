package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Stream;

class WadProcessor2ColorsAmiga extends WadProcessor4Colors {

	WadProcessor2ColorsAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		Stream.of( //
				"DSBD", // Blazing door sound effects
				"DSITMBK", // Item respawn sound effect in multiplayer mode
				"DP" // PC speaker sound effects
		).forEach(prefix -> wadFile.removeLumps(prefix));

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(AmigaUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
