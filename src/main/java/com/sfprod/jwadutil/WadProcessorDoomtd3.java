package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toInt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class WadProcessorDoomtd3 extends WadProcessor {

	WadProcessorDoomtd3(WadFile wadFile) throws IOException {
		super(wadFile, createColors(wadFile), Function.identity());
		wadFile.addLump(JWadUtil.getLump("CACHE"));
	}

	// FIXME
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

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();
		wadFile.removeLumps("PLAYPAL");
	}
}
