package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.utils.ByteBufferUtils;

class WadProcessor2ColorsTextMode extends WadProcessor {

	public static final byte[] COLORS_FLOORS = toByteArray(32, 250, 46, 249, 45, 95, 196, 126, 7, 61, 248, 246, 9, 43,
			247, 22, 240, 120, 42, 236, 4, 111, 254, 79, 88, 220, 223, 10, 8);
	private static final byte[] COLORS_WALLS = toByteArray(0, 176, 179, 180, 195, 181, 197, 198, 216, 177, 186, 221,
			182, 185, 199, 204, 206, 215, 222, 178, 219);

	private final List<Double> grays;
	private final List<Byte> lookupTable;

	WadProcessor2ColorsTextMode(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, new MapProcessor2ColorsTextMode(byteOrder, wadFile));

		wadFile.replaceLump(new Lump("TITLEPIC", getLump("TP80X25M").data(), ByteBufferUtils.DONT_CARE));
		wadFile.replaceLump(new Lump("WIMAP0", getLump("WM80X25M").data(), ByteBufferUtils.DONT_CARE));
		wadFile.replaceLump(new Lump("HELP2", getLump("HL80X25M").data(), ByteBufferUtils.DONT_CARE));

		List<Color> vgaColors = createVgaColors(wadFile);
		this.grays = vgaColors.stream().map(Color::gray).toList();

		List<Double> sortedGrays = grays.stream().sorted().toList();
		double[] bucketLimits = new double[COLORS_WALLS.length];
		double fracstep = 256 / COLORS_WALLS.length;
		double frac = fracstep;
		for (int i = 0; i < COLORS_WALLS.length - 1; i++) {
			bucketLimits[i] = sortedGrays.get(((int) frac));
			frac += fracstep;
		}
		bucketLimits[COLORS_WALLS.length - 1] = Double.MAX_VALUE;

		List<Byte> lut = new ArrayList<>();
		for (Double gray : grays) {
			int bucket = 0;
			while (gray >= bucketLimits[bucket]) {
				bucket++;
			}
			lut.add(COLORS_WALLS[bucket]);
		}
		this.lookupTable = lut;
	}

	private static byte[] toByteArray(int... colors) {
		byte[] result = new byte[colors.length];
		for (int i = 0; i < colors.length; i++) {
			result[i] = toByte(colors[i]);
		}
		return result;
	}

	@Override
	void changeColors() {
		// Raw graphics
		// Finale background flat
		changePaletteRaw(wadFile.getLumpByName("FLOOR4_8"));

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

	private byte convert256to2(byte b) {
		return lookupTable.get(toInt(b));
	}

	private void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = convert256to2(lump.data()[i]);
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
					lump.data()[index] = convert256to2(lump.data()[index]);
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

		for (int i = 0; i < 256; i++) {
			result.add(toByte(i));
		}

		if (colormap != 0) {
			int c = 32 - colormap;

			for (int color = 0; color < COLORS_FLOORS.length; color++) {
				int newcolor = Math.clamp(color * c / 32, 0, COLORS_FLOORS.length - 1);
				result.set(toInt(COLORS_FLOORS[color]), COLORS_FLOORS[newcolor]);
			}
			for (int color = 0; color < COLORS_WALLS.length; color++) {
				int newcolor = Math.clamp(color * c / 32, 0, COLORS_WALLS.length - 1);
				result.set(toInt(COLORS_WALLS[color]), COLORS_WALLS[newcolor]);
			}
		}

		result.set(205, toByte(205));
		result.set(207, toByte(207));
		result.set(209, toByte(209));

		return result;
	}

	private List<Byte> createColormapInvulnerability() {
		List<Byte> colormapInvulnerability = new ArrayList<>();

		for (int i = 0; i < 256; i++) {
			colormapInvulnerability.add(toByte(i));
		}

		for (int i = 0; i < COLORS_FLOORS.length; i++) {
			colormapInvulnerability.set(toInt(COLORS_FLOORS[i]), COLORS_FLOORS[COLORS_FLOORS.length - i - 1]);
		}
		for (int i = 0; i < COLORS_WALLS.length; i++) {
			colormapInvulnerability.set(toInt(COLORS_WALLS[i]), COLORS_WALLS[COLORS_WALLS.length - i - 1]);
		}

		colormapInvulnerability.set(205, toByte(209));
		colormapInvulnerability.set(207, toByte(207));
		colormapInvulnerability.set(209, toByte(205));

		return colormapInvulnerability;
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		for (int gamma = 1; gamma <= 5; gamma++) {
			wadFile.removeLump("PLAYPAL" + gamma);
		}

		// Menu graphics
		List<Lump> mLumps = wadFile.getLumpsByName("M_");
		mLumps.forEach(wadFile::removeLump);

		// Status bar graphics
		List<Lump> stLumps = wadFile.getLumpsByName("ST");
		stLumps.stream().filter(l -> !(l.nameAsString().startsWith("STEP"))).forEach(wadFile::removeLump);

		// Intermission screen graphics
		List<Lump> wiLumps = wadFile.getLumpsByName("WI");
		wiLumps.stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).forEach(wadFile::removeLump);
	}

}
