package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor2Colors extends WadProcessorLimitedColors {

	private static final List<Color> CGA_COLORS = List.of( //
			new Color(0x00, 0x00, 0x00), // black
			new Color(0x55, 0xFF, 0xFF), // light cyan
			new Color(0xFF, 0x55, 0xFF), // light magenta
			new Color(0xFF, 0xFF, 0xFF) // white
	);

	private static final List<Integer> VGA256_TO_4_LUT = List.of( //
			0, 0, 0, // black
			1, // gray
			3, // white
			0, 0, 0, 0, // black
			0, 0, 0, -1, // dark green
			-1, 0, -1, // brown

			-1, -1, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, //
			2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, // reddish

			-1, -1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, //
			3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, // orangeish

			3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, //
			1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, // gray

			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, -1, // green
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, // brown
			3, 3, 3, 0, 0, 0, 0, 0, // brown
			1, 1, 1, 1, 0, 0, 0, 0, // brown/green
			3, 3, 1, 1, 2, 2, 0, 0, // gold

			-1, -1, -1, 2, 2, 2, 2, 2, //
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, // red

			3, 3, 3, -1, -1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, // blue
			-1, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // orange
			3, 3, -1, -1, -1, 3, -1, 3, // yellow
			-1, 0, -1, 0, // dark orange
			-1, -1, 0, 0, // brown
			0, 0, 0, 0, 0, 0, 0, -1, // dark blue
			-1, // orange
			-1, // yellow
			-1, -1, -1, -1, 2, // purple
			-1 // cream-colored
	);

	private static final List<Integer> VGA256_TO_DITHERED_LUT = List.of( //
			0x00, 0x00, 0x00, // black
			0x33, // gray
			0xff, // white
			0x33, 0x33, 0x00, 0x00, // black
			0x00, 0x00, 0x00, 0x00, // dark green
			0x00, 0x00, 0x00, // brown

			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, //
			0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // reddish

			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, //
			0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x00, 0x00, // orangeish

			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, //
			0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x03, 0x03, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, // gray

			0xdd, 0xdd, 0xdd, 0xdd, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x11, 0x11, 0x11, 0x11, // green
			0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x00, 0x00, 0x00, 0x00, // brown
			0xff, 0xff, 0xff, 0x33, 0x33, 0x33, 0x33, 0x33, // brown
			0x55, 0x55, 0x55, 0x55, 0x00, 0x00, 0x00, 0x00, // brown/green
			0xff, 0xff, 0x55, 0x55, 0xaa, 0xaa, 0x00, 0x00, // gold

			0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, //
			0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0x00, 0x00, 0x00, 0x00, // red

			0xff, 0xff, 0xdd, 0xdd, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x11, 0x11, 0x11, 0x11, // blue
			0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, 0xaa, // orange
			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, // yellow
			0x22, 0x22, 0x22, 0x22, // dark orange
			0x00, 0x00, 0x00, 0x00, // brown
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // dark blue
			0xaa, // orange
			0xff, // yellow
			0xaa, 0xaa, 0xaa, 0xaa, 0x22, // purple
			0x22 // cream-colored
	);

	private static final List<Integer> GRAYSCALE_FROM_DARK_TO_BRIGHT = List.of(0x00, 0x03, 0x30, 0x0c, 0xc0, 0xc3, 0x3c,
			0x33, 0xcc, 0x3f, 0xf3, 0xcf, 0xfc, 0xff);

	private final boolean invert;

	protected WadProcessor2Colors(String title, ByteOrder byteOrder, WadFile wadFile, boolean invert) {
		super(title, byteOrder, wadFile,
				invert ? GRAYSCALE_FROM_DARK_TO_BRIGHT.reversed() : GRAYSCALE_FROM_DARK_TO_BRIGHT, 3);
		this.invert = invert;

		List<Color> cgaColors = invert ? CGA_COLORS.stream().map(Color::invert).toList() : CGA_COLORS;
		List<Color> colors = new ArrayList<>();
		for (Color c0 : cgaColors) {
			for (Color c1 : cgaColors) {
				for (Color c2 : cgaColors) {
					for (Color c3 : cgaColors) {
						colors.add(Color.blendColors(c0, c1, c2, c3));
					}
				}
			}
		}
		fillAvailableColorsShuffleMap(colors);

		wadFile.replaceLump(createCgaLump("FLOOR4_8"));
	}

	private byte invert(byte b) {
		return toByte(~b & 0xff);
	}

	private Integer invert(Integer i) {
		return toInt(invert(toByte(i)));
	}

	@Override
	protected List<Integer> createVga256ToDitheredLUT(List<Color> vgaCols, List<Color> availableCols) {
		return invert ? VGA256_TO_DITHERED_LUT.stream().map(this::invert).toList() : VGA256_TO_DITHERED_LUT;
	}

	@Override
	protected void changePaletteRaw(Lump lump) {
		wadFile.replaceLump(createCgaLump(lump.nameAsString()));
	}

	private Lump createCgaLump(String lumpname) {
		List<Integer> rgbs = CGA_COLORS.stream().map(Color::getRGB).toList();

		try {
			BufferedImage image = ImageIO
					.read(WadProcessor2Colors.class.getResourceAsStream("/CGA/" + lumpname + ".PNG"));
			byte[] data = new byte[(image.getWidth() / 4) * image.getHeight()];
			int i = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth() / 4; x++) {
					byte b = 0;
					for (int p = 0; p < 4; p++) {
						int rgb = image.getRGB(x * 4 + p, y);
						int rgbIndex = rgbs.indexOf(rgb);
						assert 0 <= rgbIndex && rgbIndex < 4;
						b = toByte((b << 2) | rgbIndex);
					}
					data[i] = invert ? invert(b) : b;
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
		int i = VGA256_TO_4_LUT.get(toInt(b));
		if (invert) {
			i = 3 - i;
		}
		return toByte(i << 6);
	}

}
