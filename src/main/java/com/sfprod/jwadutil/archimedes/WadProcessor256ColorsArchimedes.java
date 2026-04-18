package com.sfprod.jwadutil.archimedes;

import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sfprod.jwadutil.Color;
import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessorLimitedColors;
import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor256ColorsArchimedes extends WadProcessorLimitedColors {

	private static final List<Integer> GRAYSCALE_FROM_DARK_TO_BRIGHT = List.of(//
			0x00, 0x01, 0x02, 0x03, //
			0x25, 0x28, 0x2a, 0x2f, //
			0xd0, 0xd1, 0xd2, 0xd3, //
			0xf5, 0xf8, 0xfa, 0xff);

	public WadProcessor256ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, GRAYSCALE_FROM_DARK_TO_BRIGHT, 16);

		Color[] colors = new Color[256];
		// base colors;
		colors[0] = createArchimedesColor(0, 0, 0);
		colors[1] = createArchimedesColor(1, 1, 1);
		colors[2] = createArchimedesColor(2, 2, 2);
		colors[3] = createArchimedesColor(3, 3, 3);
		colors[4] = createArchimedesColor(4, 0, 0);
		colors[5] = createArchimedesColor(4, 0, 4);
		colors[6] = createArchimedesColor(4, 3, 2);
		colors[7] = createArchimedesColor(5, 0, 0);
		colors[8] = createArchimedesColor(5, 1, 5);
		colors[9] = createArchimedesColor(6, 0, 0);
		colors[10] = createArchimedesColor(6, 2, 6);
		colors[11] = createArchimedesColor(7, 1, 4);
		colors[12] = createArchimedesColor(7, 2, 5);
		colors[13] = createArchimedesColor(7, 3, 0);
		colors[14] = createArchimedesColor(7, 3, 3);
		colors[15] = createArchimedesColor(7, 3, 7);

		for (int x = 0; x < 16; x++) {
			Color baseColor = colors[x];
			for (int y = 1; y < 16; y++) {
				int r = baseColor.r() + 0x88 * (y & 1);
				int g = baseColor.g() + 0x44 * ((y & 7) >> 1);
				int b = baseColor.b() + 0x88 * ((y & 8) >> 3);
				Color color = new Color(r, g, b);
				colors[y * 16 + x] = color;
			}
		}

		fillAvailableColorsShuffleMap(Arrays.asList(colors));
	}

	private static Color createArchimedesColor(int r, int g, int b) {
		assert 0 <= r && r < 8;
		assert 0 <= g && g < 4;
		assert 0 <= b && b < 8;
		return new Color(r * 0x11, g * 0x11, b * 0x11);
	}

	@Override
	protected byte convert256to16(byte b) {
		return convert256to16dithered(b);
	}

	@Override
	protected void processColormap() {
		super.processColormap();

		wadFile.removeLumps("COLORMP");

		ByteBuffer bb = ByteBufferUtils.newByteBuffer(byteOrder, 16 * 2);
		for (int i = 0; i < 16; i++) {
			Color color = availableColors.get(i);
			int r = color.r() / 16;
			int g = color.g() / 16;
			int b = color.b() / 16;
			short p = toShort((r << 8) | (g << 4) | (b << 0));
			bb.putShort(p);
		}
		Lump playpal = new Lump("PLAYPAL", bb.array(), byteOrder);
		wadFile.addLump(playpal);
	}

	@Override
	protected void processRawGraphics() {
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

				if (vgaColor.isGrayish() == archimedesColor.isGrayish()) {
					int distanceToVga = archimedesColor.calculateDistance(vgaColor);
					if (distanceToVga < minClosestColor) {
						minClosestColor = distanceToVga;
						indexClosestColor = c;
					}
				}
			}
			indexes.add(indexClosestColor);
		}

		return indexes;
	}
}
