package com.sfprod.jwadutil.sinclairql;

import java.nio.ByteOrder;
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor2Colors;

public class WadProcessor2ColorsSinclairQL extends WadProcessor2Colors {

	public WadProcessor2ColorsSinclairQL(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DS"); // Sound Blaster sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DP");
		lumps.stream().map(SinclairQLUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}
}
