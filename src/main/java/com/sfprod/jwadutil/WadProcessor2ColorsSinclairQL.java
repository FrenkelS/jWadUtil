package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.List;
import java.util.stream.Stream;

class WadProcessor2ColorsSinclairQL extends WadProcessor4Colors {

	WadProcessor2ColorsSinclairQL(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		Stream.of( //
				"DPBD", // Blazing door sound effects
				"DPITMBK", // Item respawn sound effect in multiplayer mode
				"DS" // Sound Blaster sound effects
		).forEach(prefix -> wadFile.removeLumps(prefix));

		List<Lump> lumps = wadFile.getLumpsByName("DP");
		lumps.stream().map(SinclairQLUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
