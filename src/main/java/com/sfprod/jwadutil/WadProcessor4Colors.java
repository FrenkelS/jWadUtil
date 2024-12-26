package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.sfprod.utils.ByteBufferUtils;
import com.sfprod.utils.NumberUtils;

class WadProcessor4Colors extends WadProcessor {

	private final Random random = new Random(0x1d4a11);

	private static final Map<Integer, Integer> CGA_4_COLORS = Map.of( //
			0xff000000, 0, // black
			0xff55ffff, 1, // light cyan
			0xffff55ff, 2, // light magenta
			0xffffffff, 3 // white
	);

	private static final List<Color> CGA_COLORS = List.of( //
			new Color(0x00, 0x00, 0x00), // black
			new Color(0x55, 0xFF, 0xFF), // light cyan
			new Color(0xFF, 0x55, 0xFF), // light magenta
			new Color(0xFF, 0xFF, 0xFF) // white
	);

	private static final List<Color> CGA_DITHERED_COLORS = createCgaDitheredColors();
	private static final Map<Integer, List<Integer>> CGA_DITHERED_COLORS_SHUFFLE_MAP = createCgaDitheredColorsShuffleMap();

	private static final List<Integer> VGA256_TO_4_LUT = List.of( //
			0, 0, 0, // black
			1, // grey
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

			0xff, 0x7f, 0xcf, 0x7c, 0x7c, 0xcc, 0xcc, 0x4c, //
			0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x4c, 0x44, 0x44, 0x44, 0x44, 0x04, 0x04, 0x04, // red

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

	WadProcessor4Colors(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, CGA_DITHERED_COLORS);

		wadFile.replaceLump(createCgaLump("HELP2"));
		wadFile.replaceLump(createCgaLump("STBAR"));
		wadFile.replaceLump(createCgaLump("TITLEPIC"));
		wadFile.replaceLump(createCgaLump("WIMAP0"));
		wadFile.replaceLump(createCgaLump("FLOOR4_8"));
	}

	private Lump createCgaLump(String lumpname) {
		try {
			BufferedImage image = ImageIO
					.read(WadProcessor4Colors.class.getResourceAsStream("/CGA/" + lumpname + ".PNG"));
			byte[] data = new byte[(image.getWidth() / 4) * image.getHeight()];
			int i = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth() / 4; x++) {
					byte b = 0;
					for (int p = 0; p < 4; p++) {
						int rgb = image.getRGB(x * 4 + p, y);
						b = NumberUtils.toByte((b << 2) | CGA_4_COLORS.get(rgb));
					}
					data[i] = b;
					i++;
				}
			}
			return new Lump(lumpname, data, ByteOrder.LITTLE_ENDIAN);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<Color> createCgaDitheredColors() {
		List<Color> colors = new ArrayList<>();
		for (int col0 = 0; col0 < 4; col0++) {
			for (int col1 = 0; col1 < 4; col1++) {
				for (int col2 = 0; col2 < 4; col2++) {
					for (int col3 = 0; col3 < 4; col3++) {
						Color c0 = CGA_COLORS.get(col0);
						Color c1 = CGA_COLORS.get(col1);
						Color c2 = CGA_COLORS.get(col2);
						Color c3 = CGA_COLORS.get(col3);

						int r = (int) Math
								.sqrt((c0.r() * c0.r() + c1.r() * c1.r() + c2.r() * c2.r() + c3.r() * c3.r()) / 4);
						int g = (int) Math
								.sqrt((c0.g() * c0.g() + c1.g() * c1.g() + c2.g() * c2.g() + c3.g() * c3.g()) / 4);
						int b = (int) Math
								.sqrt((c0.b() * c0.b() + c1.b() * c1.b() + c2.b() * c2.b() + c3.b() * c3.b()) / 4);
						Color color = new Color(r, g, b);
						colors.add(color);
					}
				}
			}
		}
		return colors;
	}

	private static Map<Integer, List<Integer>> createCgaDitheredColorsShuffleMap() {
		Map<Integer, List<Integer>> shuffleMap = new HashMap<>();
		for (int i = 0; i < 256; i++) {
			List<Integer> sameColorList = new ArrayList<>();
			Color cgaColor = CGA_DITHERED_COLORS.get(i);
			for (int j = 0; j < 256; j++) {
				Color otherColor = CGA_DITHERED_COLORS.get(j);
				if (cgaColor.equals(otherColor)) {
					sameColorList.add(j);
				}
			}

			shuffleMap.put(i, sameColorList);
		}

		return shuffleMap;
	}

	@Override
	void changeColors() {
		// Graphics in picture format
		List<Lump> graphics = new ArrayList<>(256);
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
		graphics.forEach(this::changePalettePictureGraphics);

		List<Lump> spritesAndWalls = new ArrayList<>(256);
		// Sprites
		spritesAndWalls.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		// Walls
		spritesAndWalls.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		spritesAndWalls.forEach(this::changePalettePictureSpritesAndWalls);
	}

	private void changePalettePictureGraphics(Lump lump) {
		changePalettePicture(lump, b -> toByte(VGA256_TO_4_LUT.get(toInt(b)).byteValue() << 6));
	}

	private void changePalettePictureSpritesAndWalls(Lump lump) {
		changePalettePicture(lump, b -> VGA256_TO_DITHERED_LUT.get(toInt(b)).byteValue());
	}

	private void changePalettePicture(Lump lump, Function<Byte, Byte> func) {
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
					lump.data()[index] = func.apply(lump.data()[index]);
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
				colormapLump = new Lump("COLORMP" + gamma, new byte[34 * 256], ByteBufferUtils.DONT_CARE);
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

			for (Color color : CGA_DITHERED_COLORS) {
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
			int dist = c.calculateDistance(CGA_DITHERED_COLORS.get(i));
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
		List<Double> grays = CGA_DITHERED_COLORS.stream().map(Color::gray).collect(Collectors.toSet()).stream()
				.sorted(Comparator.reverseOrder()).toList();

		List<Integer> grayscaleFromDarkToBright = List.of(0x00, 0x00, 0x08, 0x80, 0x88, 0x88, 0x07, 0x70, 0x78, 0x87,
				0x77, 0x77, 0x0f, 0xf0, 0x8f, 0xf8, 0x7f, 0xf7, 0xff, 0xff, 0xff);

		return CGA_DITHERED_COLORS.stream().mapToDouble(Color::gray).mapToInt(grays::indexOf).map(i -> i / 6)
				.map(grayscaleFromDarkToBright::get).mapToObj(NumberUtils::toByte).toList();
	}

	@Override
	void shuffleColors() {
		wadFile.getLumpsBetween("P1_START", "P1_END").forEach(this::shuffleColorPicture);
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

	private byte shuffleColor(byte b) {
		List<Integer> list = CGA_DITHERED_COLORS_SHUFFLE_MAP.get(toInt(b));
		return list.get(random.nextInt(list.size())).byteValue();
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		for (int gamma = 1; gamma <= 5; gamma++) {
			wadFile.removeLump("PLAYPAL" + gamma);
		}
	}
}
