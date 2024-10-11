package com.sfprod.jwadutil;

import static com.sfprod.jwadutil.JWadUtil.getLump;
import static com.sfprod.utils.NumberUtils.toInt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class WadProcessorDoomtd3 extends WadProcessor {

	WadProcessorDoomtd3(WadFile wadFile) throws IOException {
		super(wadFile, createColors(wadFile), Function.identity());
		wadFile.addLump(getLump("CACHE"));
	}

	// FIXME monochrome
	private static List<Color> createColors(WadFile wadFile) {
		Lump playpal = wadFile.getLumpByName("PLAYPAL");
		ByteBuffer bb = playpal.dataAsByteBuffer();
		List<Color> vgaColors = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			int r = toInt(bb.get());
			int g = toInt(bb.get());
			int b = toInt(bb.get());
			vgaColors.add(new Color(r, g, b));
		}
		return vgaColors;
	}

	// TODO remove more lumps
	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();
		wadFile.removeLumps("PLAYPAL");
		wadFile.removeLump("ENDOOM");

		removeMap("E1M1");
		removeMap("E1M2");
		removeMap("E1M3");
		removeMap("E1M4");
		removeMap("E1M5");
		removeMap("E1M6");
		removeMap("E1M8");
		removeMap("E1M9");

		wadFile.removeLumps("DP");
		wadFile.removeLump("HELP2");
		wadFile.removeLump("TITLEPIC");
		wadFile.removeLump("STGNUM0");
		wadFile.removeLump("STGNUM1");
		wadFile.removeLump("STGNUM8");
		wadFile.removeLump("STGNUM9");
		wadFile.removeLumps("STCFN");
		wadFile.removeLumps("M_");
		wadFile.removeLumps("WI");
		wadFile.removeLump("SKY1");
	}

	private void removeMap(String map) {
		int lumpNum = wadFile.getLumpNumByName(map);
		for (int i = 0; i < 10; i++) {
			wadFile.removeLump(lumpNum);
		}
	}
}
