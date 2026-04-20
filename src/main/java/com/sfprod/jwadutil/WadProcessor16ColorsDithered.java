package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public abstract class WadProcessor16ColorsDithered extends WadProcessorLimitedColors {

	private static final List<Integer> GRAYSCALE_FROM_DARK_TO_BRIGHT = List.of(0x00, 0x00, 0x08, 0x80, 0x88, 0x88, 0x07,
			0x70, 0x78, 0x87, 0x77, 0x77, 0x0f, 0xf0, 0x8f, 0xf8, 0x7f, 0xf7, 0xff, 0xff, 0xff);

	protected WadProcessor16ColorsDithered(String title, ByteOrder byteOrder, WadFile wadFile,
			List<Color> sixteenColors, int divisor) {
		super(title, byteOrder, wadFile, GRAYSCALE_FROM_DARK_TO_BRIGHT, divisor);

		List<Color> colors = new ArrayList<>();
		for (int h = 0; h < 16; h++) {
			for (int l = 0; l < 16; l++) {
				Color ch = sixteenColors.get(h);
				Color cl = sixteenColors.get(l);
				colors.add(ch.blendColors(cl));
			}
		}
		fillAvailableColorsShuffleMap(colors);
	}

	@Override
	protected void shuffleColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		// rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::shuffleColorsRaw);

		super.shuffleColors();
	}

	private void shuffleColorsRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = shuffleColor(lump.data()[i]);
		}
	}
}
