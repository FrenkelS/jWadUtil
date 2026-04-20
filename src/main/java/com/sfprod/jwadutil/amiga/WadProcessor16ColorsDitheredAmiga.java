package com.sfprod.jwadutil.amiga;

import static com.sfprod.utils.NumberUtils.toByte;
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

import com.sfprod.jwadutil.Color;
import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor16ColorsDithered;
import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor16ColorsDitheredAmiga extends WadProcessor16ColorsDithered {

	private static final List<Color> CUSTOM_AMIGA_COLORS = List.of( //
			new Color(0, 0, 0), // black
			new Color(0, 0, 182), //
			new Color(36, 73, 0), //
			new Color(73, 0, 0), //
			new Color(182, 0, 0), //
			new Color(255, 146, 36), //
			new Color(146, 109, 73), //
			new Color(146, 146, 146), // light gray
			new Color(73, 73, 73), // dark gray
			new Color(109, 109, 255), //
			new Color(109, 219, 73), //
			new Color(255, 219, 182), //
			new Color(255, 0, 0), //
			new Color(255, 0, 255), //
			new Color(255, 255, 36), //
			new Color(255, 255, 255) // white
	);

	public WadProcessor16ColorsDitheredAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, CUSTOM_AMIGA_COLORS, 7);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DP"); // PC speaker sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(AmigaUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}

	@Override
	protected List<Integer> createVga256toByteLUT(List<Color> availableCols) {
		return createLUT(availableCols);
	}

	@Override
	protected List<Integer> createVga256toSingleColorLUT(List<Integer> vga256toByteLUT) {
		return createLUT(CUSTOM_AMIGA_COLORS);
	}

	private List<Integer> createLUT(List<Color> availableCols) {
		List<Integer> indexes = new ArrayList<>();

		for (Color vgaColor : vgaColors) {
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
	protected void processColormap() {
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
	protected void changePaletteRaw(Lump lump) {
	}

	@Override
	protected void processRawGraphics() {
		Lump stbar = wadFile.getLumpByName("STBAR");
		for (int i = 0; i < stbar.length(); i++) {
			stbar.data()[i] = convertVga256toSingleColor(stbar.data()[i]);
		}
		processRawGraphic(stbar); // Status bar

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
