package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.sfprod.utils.NumberUtils;

class WadProcessor16 extends WadProcessor {

	private static final Random RANDOM = new Random(0x1d4a11);

	private static final List<Color> CGA_COLORS = List.of( //
			new Color(0x00, 0x00, 0x00), // black
			new Color(0x00, 0x00, 0xAA), // blue
			new Color(0x00, 0xAA, 0x00), // green
			new Color(0x00, 0xAA, 0xAA), // cyan
			new Color(0xAA, 0x00, 0x00), // red
			new Color(0xAA, 0x00, 0xAA), // magenta
			new Color(0xAA, 0x55, 0x00), // brown
			new Color(0xAA, 0xAA, 0xAA), // light gray
			new Color(0x55, 0x55, 0x55), // dark gray
			new Color(0x55, 0x55, 0xFF), // light blue
			new Color(0x55, 0xFF, 0x55), // light green
			new Color(0x55, 0xFF, 0xFF), // light cyan
			new Color(0xFF, 0x55, 0x55), // light red
			new Color(0xFF, 0x55, 0xFF), // light magenta
			new Color(0xFF, 0xFF, 0x55), // yellow
			new Color(0xFF, 0xFF, 0xFF) // white
	);

	private static final List<Color> CGA136_COLORS = createCga136Colors();
	private static final Map<Integer, List<Integer>> CGA136_COLORS_SHUFFLE_MAP = createCga136ColorsShuffleMap();

	private static final List<Integer> VGA256_TO_16_LUT = List.of( //
			0x00, 0x06, 0x00, // black
			0x88, // grey
			0xff, // white
			0x08, 0x08, 0x00, 0x00, // black
			0x00, 0x00, 0x00, 0x00, // dark green
			0x68, 0x68, 0x68, // brown

			0xcf, 0xcf, 0x4f, 0x7c, 0x7c, 0x7c, 0x7c, 0x7c, 0x8c, 0x8c, 0x8c, 0x8c, 0x8c, 0x8c, 0x0c, 0x0c, //
			0x0c, 0x0c, 0x0c, 0x0c, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, // reddish

			0xff, 0xef, 0xcf, 0xcc, 0xcf, 0xcf, 0xcf, 0x5e, 0x5e, 0x5e, 0x5e, 0x7c, 0x7c, 0x7c, 0x2c, 0x2c, //
			0x2c, 0x2c, 0x2c, 0x2c, 0x68, 0x68, 0x68, 0x68, 0x68, 0x48, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, // orangeish

			0xff, 0x7f, 0x7f, 0x7f, 0x7f, 0x8f, 0x8f, 0x8f, 0x8f, 0x0f, 0x0f, 0x0f, 0x0f, 0x77, 0x77, 0x77, //
			0x77, 0x78, 0x78, 0x78, 0x78, 0x07, 0x07, 0x07, 0x07, 0x88, 0x88, 0x88, 0x88, 0x08, 0x08, 0x08, // gray

			0xaf, 0xaa, 0xaa, 0x2a, 0x2a, 0x8a, 0x8a, 0x0a, 0x0a, 0x22, 0x22, 0x28, 0x28, 0x02, 0x02, 0x00, // green
			0x1e, 0x1e, 0x3c, 0x67, 0x67, 0x47, 0x47, 0x07, 0x07, 0x07, 0x68, 0x68, 0x68, 0x68, 0x68, 0x68, // brown
			0x67, 0x67, 0x67, 0x67, 0x68, 0x68, 0x68, 0x68, // brown
			0x68, 0x68, 0x68, 0x68, 0x06, 0x06, 0x06, 0x06, // brown/green
			0xee, 0xce, 0x6e, 0x2c, 0x2c, 0x66, 0x06, 0x06, // gold

			0xff, 0x7f, 0xcf, 0x7c, 0x7c, 0xcc, 0xcc, 0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x44, 0x44, 0x44, 0x44, //
			0x44, 0x44, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, // red

			0xff, 0x9f, 0x9f, 0x79, 0x99, 0x99, 0x19, 0x19, 0x11, 0x11, 0x11, 0x11, 0x11, 0x01, 0x01, 0x01, // blue
			0xff, 0xef, 0xcf, 0xcf, 0xce, 0xce, 0xce, 0x6c, 0x6c, 0x6c, 0x6c, 0x66, 0x66, 0x66, 0x46, 0x46, // orange
			0xff, 0xef, 0xef, 0xef, 0xee, 0xee, 0xee, 0xee, // yellow
			0x46, 0x46, 0x06, 0x06, // dark orange
			0x48, 0x48, 0x06, 0x06, // brown
			0x11, 0x11, 0x11, 0x11, 0x01, 0x01, 0x01, 0x00, // dark blue
			0xce, // orange
			0xee, // yellow
			0xdd, 0x5d, 0x55, 0x55, 0x05, // purple
			0x47 // cream-colored
	);

	WadProcessor16(WadFile wadFile) {
		super(wadFile, CGA136_COLORS, WadProcessor16::shuffleColor);
	}

	private static List<Color> createCga136Colors() {
		List<Color> colors = new ArrayList<>();
		for (int h = 0; h < 16; h++) {
			for (int l = 0; l < 16; l++) {
				Color ch = CGA_COLORS.get(h);
				Color cl = CGA_COLORS.get(l);

				int r = (int) Math.sqrt((ch.r() * ch.r() + cl.r() * cl.r()) / 2);
				int g = (int) Math.sqrt((ch.g() * ch.g() + cl.g() * cl.g()) / 2);
				int b = (int) Math.sqrt((ch.b() * ch.b() + cl.b() * cl.b()) / 2);
				Color color = new Color(r, g, b);
				colors.add(color);
			}
		}
		return colors;
	}

	private static Map<Integer, List<Integer>> createCga136ColorsShuffleMap() {
		Map<Integer, List<Integer>> shuffleMap = new HashMap<>();
		for (int i = 0; i < 256; i++) {
			List<Integer> sameColorList = new ArrayList<>();
			Color cgaColor = CGA136_COLORS.get(i);
			for (int j = 0; j < 256; j++) {
				Color otherColor = CGA136_COLORS.get(j);
				if (cgaColor.equals(otherColor)) {
					sameColorList.add(j);
				}
			}

			shuffleMap.put(i, sameColorList);
		}

		return shuffleMap;
	}

	private byte convert256to16(byte b) {
		return VGA256_TO_16_LUT.get(toInt(b)).byteValue();
	}

	@Override
	void changeColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>(16);
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Flats
		rawGraphics.addAll(wadFile.getLumpsBetween("F1_START", "F1_END"));
		rawGraphics.forEach(this::changePaletteRaw);

		// Graphics in picture format
		List<Lump> graphics = new ArrayList<>(256);
		// Sprites
		graphics.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		// Status bar
		graphics.addAll(wadFile.getLumpsByName("STC"));
		graphics.addAll(wadFile.getLumpsByName("STF"));
		graphics.addAll(wadFile.getLumpsByName("STG"));
		graphics.addAll(wadFile.getLumpsByName("STK"));
		graphics.addAll(wadFile.getLumpsByName("STY"));
		// Menu
		graphics.addAll(wadFile.getLumpsByName("M_"));
		// Intermission
		graphics.addAll(wadFile.getLumpsByName("WI").stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).toList());
		// Walls
		graphics.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		graphics.forEach(this::changePalettePicture);
	}

	private void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = convert256to16(lump.data()[i]);
		}
	}

	private void changePalettePicture(Lump lump) {
		ByteBuffer dataByteBuffer = lump.dataAsByteBuffer();
		short width = dataByteBuffer.getShort();
		dataByteBuffer.getShort(); // height
		dataByteBuffer.getShort(); // leftoffset
		dataByteBuffer.getShort(); // topoffset

		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(dataByteBuffer.getInt());
		}

		for (int columnof = 0; columnof < width; columnof++) {
			int index = columnofs.get(columnof);
			byte topdelta = lump.data()[index];
			index++;
			while (topdelta != -1) {
				byte lengthByte = lump.data()[index];
				index++;
				int length = toInt(lengthByte);
				for (int i = 0; i < length + 2; i++) {
					lump.data()[index] = convert256to16(lump.data()[index]);
					index++;
				}
				topdelta = lump.data()[index];
				index++;
			}
		}
	}

	@Override
	void processColormap() {
		List<Byte> colormapInvulnerability = createColormapInvulnerability();

		for (int gamma = 0; gamma < 6; gamma++) {
			Lump colormapLump;
			if (gamma == 0) {
				colormapLump = wadFile.getLumpByName("COLORMAP");
			} else {
				colormapLump = new Lump("COLORMP" + gamma, new byte[34 * 256]);
				wadFile.addLump(colormapLump);
			}

			int index = 0;

			// colormap 0-31 from bright to dark
			int colormap = 0 - (int) (gamma * (32.0 / 5));
			for (int i = 0; i < 32; i++) {
				List<Byte> colormapBytes = createColormap(colormap);
				for (byte b : colormapBytes) {
					colormapLump.data()[index] = b;
					index++;
				}
				colormap++;
			}

			// colormap 32 invulnerability powerup
			for (int i = 0; i < 256; i++) {
				colormapLump.data()[index] = colormapInvulnerability.get(i);
				index++;
			}

			// colormap 33 all black
			for (int i = 0; i < 256; i++) {
				colormapLump.data()[index] = 0;
				index++;
			}
		}
	}

	private List<Byte> createColormap(int colormap) {
		List<Byte> result = new ArrayList<>();

		if (colormap == 0) {
			for (int i = 0; i < 256; i++) {
				result.add(toByte(i));
			}
		} else {
			int c = 32 - colormap;

			for (Color color : CGA136_COLORS) {
				int r = Math.clamp((long) Math.sqrt(color.r() * color.r() * c / 32), 0, 255);
				int g = Math.clamp((long) Math.sqrt(color.g() * color.g() * c / 32), 0, 255);
				int b = Math.clamp((long) Math.sqrt(color.b() * color.b() * c / 32), 0, 255);

				byte closestColor = calculateClosestColor(new Color(r, g, b));
				byte shuffledColor = shuffleColor(closestColor);
				result.add(shuffledColor);
			}
		}

		return result;
	}

	private byte calculateClosestColor(Color c) {
		int closestColor = -1;
		int closestDist = Integer.MAX_VALUE;

		for (int i = 0; i < 256; i++) {
			int dist = c.calculateDistance(CGA136_COLORS.get(i));
			if (dist == 0) {
				// perfect match
				closestColor = i;
				break;
			}

			if (dist < closestDist) {
				closestDist = dist;
				closestColor = i;
			}
		}

		return toByte(closestColor);
	}

	private List<Byte> createColormapInvulnerability() {
		List<Double> grays = CGA136_COLORS.stream().map(Color::gray).collect(Collectors.toSet()).stream()
				.sorted(Comparator.reverseOrder()).toList();

		List<Integer> grayscaleFromDarkToBright = List.of(0x00, 0x00, 0x08, 0x80, 0x88, 0x88, 0x07, 0x70, 0x78, 0x87,
				0x77, 0x77, 0x0f, 0xf0, 0x8f, 0xf8, 0x7f, 0xf7, 0xff, 0xff, 0xff);

		return CGA136_COLORS.stream().mapToDouble(Color::gray).mapToInt(grays::indexOf).map(i -> i / 6)
				.map(grayscaleFromDarkToBright::get).mapToObj(NumberUtils::toByte).toList();
	}

	@Override
	void shuffleColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>(16);
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::shuffleColorsRaw);

		// Graphics in picture format
		List<Lump> graphics = new ArrayList<>(256);
		// Status bar
		graphics.addAll(wadFile.getLumpsByName("STC"));
		graphics.addAll(wadFile.getLumpsByName("STF"));
		graphics.addAll(wadFile.getLumpsByName("STG"));
		graphics.addAll(wadFile.getLumpsByName("STK"));
		graphics.addAll(wadFile.getLumpsByName("STY"));
		// Menu
		graphics.addAll(
				wadFile.getLumpsByName("M_").stream().filter(l -> !l.nameAsString().startsWith("M_SKULL")).toList());
		// Intermission
		graphics.addAll(wadFile.getLumpsByName("WI").stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).toList());
		// Walls
		graphics.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		graphics.forEach(this::shuffleColorPicture);
	}

	private void shuffleColorsRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = shuffleColor(lump.data()[i]);
		}
	}

	private void shuffleColorPicture(Lump lump) {
		ByteBuffer dataByteBuffer = lump.dataAsByteBuffer();
		short width = dataByteBuffer.getShort();
		dataByteBuffer.getShort(); // height
		dataByteBuffer.getShort(); // leftoffset
		dataByteBuffer.getShort(); // topoffset

		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(dataByteBuffer.getInt());
		}

		for (int columnof = 0; columnof < width; columnof++) {
			int index = columnofs.get(columnof);
			byte topdelta = lump.data()[index];
			index++;
			while (topdelta != -1) {
				byte lengthByte = lump.data()[index];
				index++;
				int length = toInt(lengthByte);
				for (int i = 0; i < length + 2; i++) {
					lump.data()[index] = shuffleColor(lump.data()[index]);
					index++;
				}
				topdelta = lump.data()[index];
				index++;
			}
		}
	}

	private static byte shuffleColor(byte b) {
		List<Integer> list = CGA136_COLORS_SHUFFLE_MAP.get(toInt(b));
		return list.get(RANDOM.nextInt(list.size())).byteValue();
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		for (int gamma = 1; gamma <= 5; gamma++) {
			wadFile.removeLump("PLAYPAL" + gamma);
		}
	}
}
