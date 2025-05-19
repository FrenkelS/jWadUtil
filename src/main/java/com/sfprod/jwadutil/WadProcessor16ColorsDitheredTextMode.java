package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class WadProcessor16ColorsDitheredTextMode extends WadProcessor16ColorsDithered {

	WadProcessor16ColorsDitheredTextMode(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		// Menu graphics
		List<Lump> mLumps = wadFile.getLumpsByName("M_");
		mLumps.forEach(wadFile::removeLump);

		// Status bar graphics
		List<Lump> stLumps = wadFile.getLumpsByName("ST");
		stLumps.stream().filter(l -> !("STIMA0".equals(l.nameAsString()) || l.nameAsString().startsWith("STEP")))
				.forEach(wadFile::removeLump);

		// Intermission screen graphics
		List<Lump> wiLumps = wadFile.getLumpsByName("WI");
		wiLumps.stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).forEach(wadFile::removeLump);
	}

	@Override
	void shuffleColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		// rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::shuffleColorsRaw);

		// Graphics in picture format
		List<Lump> graphics = new ArrayList<>(256);
		// Walls
		graphics.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		graphics.forEach(this::shuffleColorPicture);
	}
}
