package com.sfprod.jwadutil.neogeo;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.pc.WadProcessor16ColorsDitheredPC;

public class WadProcessor16ColorsDitheredTextModeNeoGeo extends WadProcessor16ColorsDitheredPC {

	public WadProcessor16ColorsDitheredTextModeNeoGeo(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, new MapProcessorDoom64KB(byteOrder, wadFile));
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
	protected void duplicateMaps() {
		int lumpNumE1M1 = wadFile.getLumpNumByName("E1M1");

		List<Lump> e1m1Lumps = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			e1m1Lumps.add(wadFile.getLumpByNum(lumpNumE1M1 + 1 + i));
		}

		List<Integer> mapNumbers = List.of(2, 3, 4, 5, 6, 7, 9);
		for (int mapNumber : mapNumbers) {
			int lumpNumE1M2 = wadFile.getLumpNumByName("E1M" + mapNumber);
			for (int i = 0; i < 9; i++) {
				wadFile.replaceLump(lumpNumE1M2 + 1 + i, e1m1Lumps.get(i));
			}
		}
	}
}
