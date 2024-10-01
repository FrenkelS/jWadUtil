package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.sfprod.jwadutil.WadFile.Lump;

class WadProcessor16 extends WadProcessor {

	private static record Color(int r, int g, int b) {
		double gray() {
			return r * 0.299 + g * 0.587 + b * 0.114;
		}

		int calculateDistance(Color that) {
			int distr = Math.abs(this.r - that.r);
			int distg = Math.abs(this.g - that.g);
			int distb = Math.abs(this.b - that.b);

			return distr + distg + distb;
		}
	}

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

	private static final int[] VGA256_TO_16_LUT = { //
			0x00, 0x06, 0x00, 0x07, 0xff, 0x08, 0x80, 0x00, 0x00, 0x88, 0x08, 0x80, 0x00, 0x68, 0x86, 0x68, //
			0xff, 0xff, 0x7f, 0xcf, 0xfc, 0xcf, 0xfc, 0xcf, 0xfc, 0x7c, 0xc7, 0x7c, 0xc7, 0x7c, 0xcc, 0xcc, //
			0xcc, 0xcc, 0x6c, 0x4c, 0xc4, 0x4c, 0x0c, 0xc0, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x04, 0x40, //
			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xef, 0xfe, 0xef, 0xfe, 0xef, 0x7e, 0xe7, 0x7e, 0xce, 0xec, //
			0xce, 0xec, 0xce, 0xec, 0x4e, 0xe4, 0x2c, 0xc2, 0x2c, 0xc2, 0x2c, 0x68, 0x86, 0x68, 0x06, 0x60, //
			0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f, 0xf7, 0x7f, 0xf7, 0x7f, 0xf7, 0x7f, //
			0x8f, 0xf8, 0x0f, 0x77, 0x77, 0x77, 0x78, 0x87, 0x78, 0x07, 0x70, 0x07, 0x88, 0x88, 0x88, 0x08, //
			0xaf, 0x3e, 0x7a, 0xa7, 0x7a, 0xa7, 0xaa, 0x8a, 0xa8, 0x8a, 0x0a, 0x28, 0x82, 0x02, 0x02, 0x00, //
			0x7f, 0xf7, 0xcf, 0x6f, 0xf6, 0x6f, 0x5e, 0xe5, 0x5e, 0x3c, 0xc3, 0x67, 0x76, 0x47, 0x68, 0x86, //
			0x5e, 0xe5, 0x5e, 0x67, 0x2c, 0xc2, 0x68, 0x86, 0x1e, 0xe1, 0x1e, 0x27, 0x36, 0x63, 0x88, 0x88, //
			0xef, 0xee, 0xee, 0xce, 0x6e, 0x4e, 0x66, 0x66, 0xff, 0xff, 0xff, 0x7f, 0xcf, 0x7c, 0xcc, 0x4c, //
			0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x04, //
			0xff, 0xff, 0xff, 0x9f, 0xf9, 0x79, 0x99, 0x19, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, //
			0xff, 0xff, 0xff, 0xef, 0xef, 0x7e, 0xce, 0x6e, 0x4e, 0xe4, 0x4e, 0xe4, 0x4e, 0x6c, 0xc6, 0x6c, //
			0xff, 0xff, 0xff, 0xef, 0xfe, 0xee, 0xee, 0xee, 0x6c, 0xc6, 0x66, 0x46, 0x68, 0x86, 0x06, 0x60, //
			0x11, 0x01, 0x10, 0x01, 0x10, 0x00, 0x00, 0x00, 0xee, 0xee, 0xdf, 0x5d, 0xd5, 0x5d, 0x55, 0x4f };

	WadProcessor16(WadFile wadFile) {
		super(wadFile);
	}

	@Override
	void processColorSpecific() {
		changePalette();
		processColormap();
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

	private final Random random = new Random(0x1d4a11);

	private byte convert256to16Random(byte b) {
		int r = VGA256_TO_16_LUT[b & 0xff];
		if (random.nextBoolean()) {
			int h = r / 16;
			int l = r % 16;
			r = (l << 4) | h;
		}

		return (byte) r;
	}

	private byte convert256to16(byte b) {
		return (byte) VGA256_TO_16_LUT[b & 0xff];
	}

	private void changePalette() {
		// Raw graphics
		changePaletteRaw(wadFile.getLumpByName("HELP2"));
		changePaletteRaw(wadFile.getLumpByName("STBAR"));
		changePaletteRaw(wadFile.getLumpByName("TITLEPIC"));
		changePaletteRaw(wadFile.getLumpByName("WIMAP0"));

		List<Lump> flats = wadFile.getLumpsBetween("F1_START", "F1_END");
		flats.forEach(this::changePaletteRaw);

		// Graphics random new color
		List<Lump> graphicsRandom = new ArrayList<>(256);
		// Status bar
		graphicsRandom.addAll(wadFile.getLumpsByName("STC"));
		graphicsRandom.addAll(wadFile.getLumpsByName("STF"));
		graphicsRandom.addAll(wadFile.getLumpsByName("STG"));
		graphicsRandom.addAll(wadFile.getLumpsByName("STK"));
		graphicsRandom.addAll(wadFile.getLumpsByName("STY"));
		// Menu
		graphicsRandom.addAll(
				wadFile.getLumpsByName("M_").stream().filter(l -> !l.nameAsString().startsWith("M_SKULL")).toList());
		// Intermission
		graphicsRandom
				.addAll(wadFile.getLumpsByName("WI").stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).toList());
		// Walls
		graphicsRandom.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		graphicsRandom.forEach(g -> changePalettePicture(g, true));

		// Randomly flipped colors look weird on graphics that don't move but have
		// multiple frames like the menu skull graphics and barrels
		List<Lump> graphics = new ArrayList<>(256);
		// Menu skull
		graphics.addAll(wadFile.getLumpsByName("M_SKULL"));
		// Sprites
		graphics.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		graphics.forEach(g -> changePalettePicture(g, false));
	}

	private void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.data().length; i++) {
			lump.data()[i] = convert256to16Random(lump.data()[i]);
		}
	}

	private void changePalettePicture(Lump lump, final boolean random) {
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
				int length = lengthByte & 0xff;
				for (int i = 0; i < length + 2; i++) {
					lump.data()[index] = random ? convert256to16Random(lump.data()[index])
							: convert256to16(lump.data()[index]);
					index++;
				}
				topdelta = lump.data()[index];
				index++;
			}
		}
	}

	private void processColormap() {
		Lump colormapLump = wadFile.getLumpByName("COLORMAP");

		int index = 0;

		// colormap 0-31 from bright to dark
		for (int colormap = 0; colormap < 32; colormap++) {
			List<Byte> colormapBytes = createColormap(colormap);
			for (byte b : colormapBytes) {
				colormapLump.data()[index] = b;
				index++;
			}
		}

		// colormap 32 invulnerability powerup
		List<Byte> colormapInvulnerability = createColormapInvulnerability();
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

	private List<Byte> createColormap(int colormap) {
		List<Color> cga136colorsForColormap = new ArrayList<>();

		int c = 32 - colormap;

		for (Color color : CGA136_COLORS) {
			int r = (int) Math.sqrt(color.r() * color.r() * c / 32);
			int g = (int) Math.sqrt(color.g() * color.g() * c / 32);
			int b = (int) Math.sqrt(color.b() * color.b() * c / 32);
			cga136colorsForColormap.add(new Color(r, g, b));
		}

		List<Byte> result = new ArrayList<>();
		boolean ascending = true;
		for (Color color : cga136colorsForColormap) {
			byte closestColor = calculateClosestColor(color, ascending);
			result.add(closestColor);
			ascending = !ascending;
		}

		return result;
	}

	private byte calculateClosestColor(Color c, boolean ascending) {
		int closestColor = -1;
		int closestDist = Integer.MAX_VALUE;

		if (ascending) {
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
		} else {
			for (int i = 255; i >= 0; i--) {
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
		}

		return (byte) closestColor;
	}

	private List<Byte> createColormapInvulnerability() {
		List<Double> grays = CGA136_COLORS.stream().map(Color::gray).collect(Collectors.toSet()).stream()
				.sorted(Comparator.reverseOrder()).toList();

		List<Integer> grayscaleFromDarkToBright = List.of(0x00, 0x00, 0x08, 0x80, 0x88, 0x88, 0x07, 0x70, 0x78, 0x87,
				0x77, 0x77, 0x0f, 0xf0, 0x8f, 0xf8, 0x7f, 0xf7, 0xff, 0xff, 0xff);

		return CGA136_COLORS.stream().mapToDouble(Color::gray).mapToInt(grays::indexOf).map(i -> i / 6)
				.map(grayscaleFromDarkToBright::get).mapToObj(i -> (byte) i).toList();
	}

}
