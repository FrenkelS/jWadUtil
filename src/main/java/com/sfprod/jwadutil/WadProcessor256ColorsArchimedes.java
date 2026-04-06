package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

class WadProcessor256ColorsArchimedes extends WadProcessorLimitedColors {

	private static final List<Integer> GRAYSCALE_FROM_DARK_TO_BRIGHT = List.of(//
			0, 1, 2, 3, //
			44, 45, 46, 47, //
			208, 209, 210, 211, //
			252, 253, 254, 255);

	WadProcessor256ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, GRAYSCALE_FROM_DARK_TO_BRIGHT, 16);

		// @see
		// https://www.onirom.fr/wiki/blog/30-04-2022_Archimedes-ARM2-Graphics-Programming/#Aside_:_Default_256_colors_palette_generator
		List<Color> colors = new ArrayList<>();
		for (int i = 0; i <= 255; i++) {
			int r = 17 * ((i & 7) | ((i & 16) >> 1));
			int g = 17 * ((i & 3) | ((i & 96) >> 3));
			int b = 17 * ((i & 3) | ((i & 8) >> 1) | ((i & 128) >> 4));
			Color color = new Color(r, g, b);
			colors.add(color);
		}
		fillAvailableColorsShuffleMap(colors);
	}

	@Override
	protected byte convert256to16(byte b) {
		return convert256to16dithered(b);
	}

	@Override
	protected void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = convert256to16dithered(lump.data()[i]);
		}
	}

	@Override
	protected List<Integer> createVga256ToDitheredLUT(List<Color> vgaCols, List<Color> availableCols) {
		List<Integer> indexes = new ArrayList<>();

		for (Color vgaColor : vgaCols) {
			int minClosestColor = Integer.MAX_VALUE;
			int indexClosestColor = -1;

			for (int c = 0; c < availableCols.size(); c++) {
				Color archimedesColor = availableCols.get(c);

				int distanceToVga = archimedesColor.calculateDistance(vgaColor);
				if (distanceToVga < minClosestColor) {
					minClosestColor = distanceToVga;
					indexClosestColor = c;
				}
			}
			indexes.add(indexClosestColor);
		}

		return indexes;
	}
}
