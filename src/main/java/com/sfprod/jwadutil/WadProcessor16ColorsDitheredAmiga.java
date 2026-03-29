package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor16ColorsDitheredAmiga extends WadProcessor16ColorsDithered {

	private static final List<Color> CUSTOM_AMIGA_COLORS = List.of( //
			new Color(0, 0, 0), // black
			new Color(0, 0, 68), //
			new Color(51, 68, 34), //
			new Color(153, 136, 102), //
			new Color(119, 17, 17), //
			new Color(119, 68, 34), //
			new Color(136, 102, 85), //
			new Color(170, 170, 170), // light gray
			new Color(102, 102, 102), // dark gray
			new Color(0, 0, 204), //
			new Color(51, 136, 34), //
			new Color(238, 170, 119), //
			new Color(221, 85, 0), //
			new Color(204, 0, 204), //
			new Color(255, 238, 68), //
			new Color(255, 255, 255) // white
	);

	WadProcessor16ColorsDitheredAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, CUSTOM_AMIGA_COLORS, 7);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DP"); // PC speaker sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(AmigaUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}

	@Override
	protected List<Integer> createVga256ToDitheredLUT(List<Color> vgaCols, List<Color> availableCols) {
		List<Integer> indexes = new ArrayList<>();

		for (Color vgaColor : vgaCols) {
			int minClosestColor = Integer.MAX_VALUE;
			int indexClosestColor = -1;

			for (int c = 0; c < availableCols.size(); c++) {
				Color amigaColor = availableCols.get(c);

				int distanceToVga = amigaColor.calculateDistance(vgaColor);
				if (distanceToVga < minClosestColor) {
					minClosestColor = distanceToVga;
					indexClosestColor = c;
				}
			}
			indexes.add(indexClosestColor);
		}

		return indexes;
	}

	@Override
	protected byte convert256to16(byte b) {
		byte out = super.convert256to16(b);
		return toByte(toInt(out) & 0x0f);
	}

	@Override
	void processColormap() {
		super.processColormap();

		wadFile.removeLumps("COLORMP");

		ByteBuffer bb = ByteBufferUtils.newByteBuffer(byteOrder, 16 * 2);
		for (Color color : CUSTOM_AMIGA_COLORS) {
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
	void processRawGraphics() {
		processRawGraphic(wadFile.getLumpByName("STBAR")); // Status bar

		Stream.of("HELP2", "TITLEPIC", "WIMAP0", // Raw graphics
				"FLOOR4_8") // Finale background flat
				.map(this::createAmigaLump).forEach(this::processRawGraphic);
	}

	private void processRawGraphic(Lump lump) {
		int newLength = lump.length() / 2;
		ByteBuffer oldbb = lump.dataAsByteBuffer();
		ByteBuffer newbb = ByteBufferUtils.newByteBuffer(ByteBufferUtils.DONT_CARE, newLength);
		int planewidth = lump.length() % 30 == 0 ? 30 : 8;
		for (int i = 0; i < lump.length() / (8 * planewidth); i++) {
			byte[][] oldcolors = new byte[planewidth][8];
			byte[][] newcolors = new byte[planewidth][4];

			for (int l = 0; l < planewidth; l++) {
				for (int c = 0; c < 8; c++) {
					oldcolors[l][c] = oldbb.get();
				}

				for (int bitplane = 0; bitplane < 4; bitplane++) {
					for (int b = 0; b < 8; b++) {
						int bitValue = (oldcolors[l][b] >> bitplane) & 1;
						newcolors[l][bitplane] |= bitValue << (7 - b);
					}
				}
			}
			for (int bitplane = 0; bitplane < 4; bitplane++) {
				for (int l = 0; l < planewidth; l++) {
					newbb.put(newcolors[l][bitplane]);
				}
			}
		}

		wadFile.replaceLump(new Lump(lump.name(), newbb));
	}

	private Lump createAmigaLump(String lumpname) {
		List<Integer> rgbs = CUSTOM_AMIGA_COLORS.stream().map(Color::getRGB).toList();

		try {
			BufferedImage image = ImageIO
					.read(WadProcessor16ColorsDitheredAmiga.class.getResourceAsStream("/Amiga/" + lumpname + ".PNG"));
			byte[] data = new byte[image.getWidth() * image.getHeight()];
			int i = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int rgb = image.getRGB(x, y);
					int rgbIndex = rgbs.indexOf(rgb);
					assert 0 <= rgbIndex && rgbIndex < 16;
					data[i] = toByte(rgbIndex);
					i++;
				}
			}
			return new Lump(lumpname, data, ByteBufferUtils.DONT_CARE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
