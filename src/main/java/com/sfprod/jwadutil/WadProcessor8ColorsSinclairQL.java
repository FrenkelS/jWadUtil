package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.ByteBufferUtils.toByteList;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import com.sfprod.utils.ByteBufferUtils;

class WadProcessor8ColorsSinclairQL extends WadProcessorLimitedColors {

	private static final List<Color> SINCLAIR_QL_COLORS = List.of( //
			new Color(0x00, 0x00, 0x00), // black
			new Color(0x00, 0x00, 0xff), // blue
			new Color(0xff, 0x00, 0x00), // red
			new Color(0xff, 0x00, 0xff), // magenta
			new Color(0x00, 0xff, 0x00), // green
			new Color(0x00, 0xff, 0xff), // cyan
			new Color(0xff, 0xff, 0x00), // yellow
			new Color(0xff, 0xff, 0xff) // white
	);

	private static final List<Integer> SINCLAIR_QL_BITS = List.of( //
			0b0__00_00_00_00__00_00_00_00, // black
			0b0__00_00_00_00__00_00_00_01, // blue
			0b0__00_00_00_00__00_00_00_10, // red
			0b0__00_00_00_00__00_00_00_11, // magenta
			0b0__00_00_00_10__00_00_00_00, // green
			0b0__00_00_00_10__00_00_00_01, // cyan
			0b0__00_00_00_10__00_00_00_10, // yellow
			0b0__00_00_00_10__00_00_00_11 // white
	);

	private static final List<Integer> VGA256_TO_8_LUT = List.of( //
			0, 0, 0, // black
			5, // grey
			7, // white
			0, 0, 0, 0, // black
			4, 4, 4, -1, // dark green
			-1, 0, -1, // brown

			-1, -1, 7, 7, 7, 7, 7, 7, 3, 3, 3, 3, 3, 3, 3, 3, //
			2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, // reddish

			-1, -1, 7, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, //
			6, 6, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, // orangeish

			7, 7, 7, 7, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5, 5, 5, //
			5, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, // gray

			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, 0, -1, // green
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, // brown
			6, 6, 6, 0, 0, 0, 0, 0, // brown
			4, 4, 4, 4, 0, 0, 0, 0, // brown/green
			6, 6, 6, 6, 2, 2, 0, 0, // gold

			-1, -1, -1, 3, 3, 3, 3, 3, //
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, // red

			7, 5, 5, -1, -1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, // blue
			-1, 7, 7, 6, 6, 6, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // orange
			7, 6, -1, -1, -1, 6, -1, 6, // yellow
			-1, 2, -1, 2, // dark orange
			-1, -1, 0, 0, // brown
			1, 0, 0, 0, 0, 0, 0, -1, // dark blue
			-1, // orange
			-1, // yellow
			-1, -1, -1, -1, 3, // purple
			-1 // cream-colored
	);

	private static final List<Integer> VGA256_TO_DITHERED_LUT = List.of( //
			0, 0, 0, // black
			34, // grey
			124, // white
			0, 0, 0, 0, // black
			14, 14, 14, 14, // dark green
			5, 5, 5, // brown

			115, 115, 115, 92, 92, 92, 60, 60, 60, 52, 52, 52, 43, 43, 43, 21, //
			21, 21, 15, 15, 15, 15, 10, 10, 10, 10, 7, 7, 7, 5, 5, 5, // reddish

			115, 115, 111, 111, 107, 107, 92, 92, 87, 87, 83, 83, 79, 79, 76, 76, //
			67, 67, 60, 60, 52, 52, 49, 49, 43, 43, 41, 35, 35, 31, 21, 5, // orangeish

			124, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 108, 74, 74, 74, //
			74, 74, 74, 74, 74, 74, 74, 74, 74, 34, 34, 34, 34, 34, 34, 0, // gray

			120, 110, 91, 81, 75, 62, 62, 48, 48, 36, 36, 30, 30, 14, 14, 0, // green
			111, 107, 99, 87, 79, 76, 69, 67, 56, 49, 43, 41, 35, 34, 31, 5, // brown
			43, 43, 34, 34, 31, 31, 5, 5, // brown
			54, 45, 43, 41, 35, 34, 31, 14, // brown/green
			119, 99, 79, 56, 41, 35, 31, 5, // gold

			124, 117, 117, 115, 115, 96, 96, 92, //
			92, 66, 66, 60, 60, 21, 21, 17, 17, 15, 15, 10, 10, 7, 7, 5, // red

			121, 116, 109, 105, 86, 82, 63, 42, 40, 39, 38, 37, 28, 26, 4, 3, // blue
			124, 115, 115, 111, 111, 107, 107, 95, 95, 92, 87, 76, 67, 49, 41, 35, // orange
			124, 123, 122, 122, 119, 119, 112, 112, // yellow
			49, 41, 35, 15, // dark orange
			35, 31, 5, 0, // brown
			4, 4, 3, 3, 2, 2, 1, 0, // dark blue
			87, // orange
			107, // yellow
			73, 29, 22, 13, 6, // purple
			43 // cream-colored
	);

	private static final SortedMap<Color, List<Integer>> SINCLAIR_QL_PALETTE = createSinclairQLPalette();

	private static SortedMap<Color, List<Integer>> createSinclairQLPalette() {
		SortedMap<Color, List<Integer>> sinclairQLPalette = new TreeMap<>(Comparator.comparing(Color::gray));

		for (int i = 0; i < 8 * 8 * 8 * 8; i++) {
			Color color0 = SINCLAIR_QL_COLORS.get((i >> 0) & 7);
			Color color1 = SINCLAIR_QL_COLORS.get((i >> 3) & 7);
			Color color2 = SINCLAIR_QL_COLORS.get((i >> 6) & 7);
			Color color3 = SINCLAIR_QL_COLORS.get((i >> 9) & 7);
			Color blendedColor = Color.blendColors(color0, color1, color2, color3);

			sinclairQLPalette.computeIfAbsent(blendedColor, c -> new ArrayList<>()).add(i);
		}

		return sinclairQLPalette;
	}

	WadProcessor8ColorsSinclairQL(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, Collections.emptyList(), 0);

		List<Color> availColors = new ArrayList<>();
		for (int i : SINCLAIR_QL_PALETTE.values().stream().map(List::getFirst).toList()) {
			Color color0, color1, color2, color3;

			// green
			color0 = SINCLAIR_QL_COLORS.get(((i >> 0) & 4) >> 2);
			color1 = SINCLAIR_QL_COLORS.get(((i >> 3) & 4) >> 2);
			color2 = SINCLAIR_QL_COLORS.get(((i >> 6) & 4) >> 2);
			color3 = SINCLAIR_QL_COLORS.get(((i >> 9) & 4) >> 2);
			availColors.add(Color.blendColors(color0, color1, color2, color3));

			// red blue
			color0 = SINCLAIR_QL_COLORS.get((i >> 0) & 3);
			color1 = SINCLAIR_QL_COLORS.get((i >> 3) & 3);
			color2 = SINCLAIR_QL_COLORS.get((i >> 6) & 3);
			color3 = SINCLAIR_QL_COLORS.get((i >> 9) & 3);
			availColors.add(Color.blendColors(color0, color1, color2, color3));
		}

		availColors.add(new Color(0, 0, 0));
		availColors.add(new Color(0, 0, 0));
		availColors.add(new Color(0, 0, 0));
		availColors.add(new Color(0, 0, 0));
		availColors.add(new Color(0, 0, 0));
		availColors.add(new Color(0, 0, 0));
		assert availColors.size() == 256;
		fillAvailableColorsShuffleMap(availColors);

		wadFile.replaceLump(createSinclairQLLump("FLOOR4_8"));
	}

	@Override
	protected Lump processPcSpeakerSoundEffect(Lump vanillaLump) {
		return SinclairQLUtil.processPcSpeakerSoundEffect(vanillaLump, byteOrder);
	}

	@Override
	protected void changePaletteRaw(Lump lump) {
		wadFile.replaceLump(createSinclairQLLump(lump.nameAsString()));
	}

	private Lump createSinclairQLLump(String lumpname) {
		List<Integer> rgbs = SINCLAIR_QL_COLORS.stream().map(Color::getRGB).toList();

		try {
			BufferedImage image = ImageIO
					.read(WadProcessor8ColorsSinclairQL.class.getResourceAsStream("/QL/" + lumpname + ".PNG"));
			byte[] data = new byte[(image.getWidth() / 4) * image.getHeight() * 2];
			int i = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth() / 4; x++) {
					byte bh = 0;
					byte bl = 0;
					for (int p = 0; p < 4; p++) {
						int rgb = image.getRGB(x * 4 + p, y);
						int rgbIndex = rgbs.indexOf(rgb);
						assert 0 <= rgbIndex && rgbIndex < 8;
						if (rgbIndex < 4) {
							bh = toByte(bh << 2);
							bl = toByte((bl << 2) | rgbIndex);
						} else {
							bh = toByte((bh << 2) | 2);
							bl = toByte((bl << 2) | (rgbIndex - 4));
						}
					}
					data[i] = bh;
					i++;
					data[i] = bl;
					i++;
				}
			}
			return new Lump(lumpname, data, ByteBufferUtils.DONT_CARE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected byte convert256to16(byte b) {
		return toByte(VGA256_TO_8_LUT.get(toInt(b)));
	}

	@Override
	protected List<Integer> createVga256ToDitheredLUT(List<Color> vgaCols, List<Color> availableCols) {
		return VGA256_TO_DITHERED_LUT;
	}

	@Override
	List<Byte> createColormapInvulnerability() {
		ByteBuffer bb = newByteBuffer(byteOrder, 256);

		// [0, 25)
		for (int i = 0; i < 25; i++) {
			bb.putShort(toColormapShort("1111"));
		}

		// [25, 50)
		for (int i = 0; i < 25 / 4; i++) {
			bb.putShort(toColormapShort("0111"));
			bb.putShort(toColormapShort("1011"));
			bb.putShort(toColormapShort("1101"));
			bb.putShort(toColormapShort("1110"));
		}
		bb.putShort(toColormapShort("0111"));

		// [50, 75)
		for (int i = 0; i < 25 / 5; i++) {
			bb.putShort(toColormapShort("0011"));
			bb.putShort(toColormapShort("0101"));
			bb.putShort(toColormapShort("0110"));
			bb.putShort(toColormapShort("1010"));
			bb.putShort(toColormapShort("1100"));
		}

		// [75, 100)
		for (int i = 0; i < 25 / 4; i++) {
			bb.putShort(toColormapShort("0001"));
			bb.putShort(toColormapShort("0010"));
			bb.putShort(toColormapShort("0100"));
			bb.putShort(toColormapShort("1000"));
		}
		bb.putShort(toColormapShort("0001"));

		// [100, 125)
		for (int i = 0; i < 25; i++) {
			bb.putShort(toColormapShort("0000"));
		}

		return toByteList(bb);
	}

	private short toColormapShort(String bitmask) {
		char[] bitarray = bitmask.toCharArray();
		short r = 0x0000;

		for (int i = 0; i < bitarray.length; i++) {
			if (bitarray[i] == '1') {
				r |= 0b0__00_00_00_10__00_00_00_11 << (i * 2);
			}
		}

		return r;
	}

	private short toColormapShort(int i) {
		int c0 = SINCLAIR_QL_BITS.get((i >> 0) & 7) << 0;
		int c1 = SINCLAIR_QL_BITS.get((i >> 3) & 7) << 2;
		int c2 = SINCLAIR_QL_BITS.get((i >> 6) & 7) << 4;
		int c3 = SINCLAIR_QL_BITS.get((i >> 9) & 7) << 6;
		return toShort(c3 | c2 | c1 | c0);
	}

	@Override
	List<Byte> createColormap(int colormap) {
		ByteBuffer bb = newByteBuffer(byteOrder, 256);

		if (colormap == 0) {
			for (int i : SINCLAIR_QL_PALETTE.values().stream().map(List::getFirst).toList()) {
				short s = toColormapShort(i);
				bb.putShort(s);
			}
		} else {
			int c = 32 - colormap;

			for (Color color : SINCLAIR_QL_PALETTE.keySet()) {
				int r = Math.clamp((long) Math.sqrt(color.r() * color.r() * c / 32), 0, 255);
				int g = Math.clamp((long) Math.sqrt(color.g() * color.g() * c / 32), 0, 255);
				int b = Math.clamp((long) Math.sqrt(color.b() * color.b() * c / 32), 0, 255);
				Color closestColor = calculateClosestColor(new Color(r, g, b));
				List<Integer> list = SINCLAIR_QL_PALETTE.get(closestColor);

				int i = list.get(random.nextInt(list.size()));

				short s = toColormapShort(i);
				bb.putShort(s);
			}
		}

		return toByteList(bb);
	}

	private Color calculateClosestColor(Color c) {
		Color closestColor = null;
		int closestDist = Integer.MAX_VALUE;

		for (Color possibleColor : SINCLAIR_QL_PALETTE.keySet()) {
			int dist = c.calculateDistance(possibleColor);
			if (dist == 0) {
				// perfect match
				closestColor = possibleColor;
				break;
			}

			if (dist < closestDist) {
				closestDist = dist;
				closestColor = possibleColor;
			}
		}

		return closestColor;
	}

	@Override
	void shuffleColors() {

	}

}
