package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.utils.ByteBufferUtils;

class WadProcessor2ColorsTextMode extends WadProcessor {

	private static final byte C0 = toByte(0x00);
	private static final byte C1 = toByte(0xb0);
	private static final byte C2 = toByte(0xb1);
	private static final byte C3 = toByte(0xb2);
	private static final byte C4 = toByte(0xdb);
	public static final byte COLORS[] = { C0, C1, C2, C3, C4 };

	private final List<Double> grays;
	private final List<Byte> lookupTable;
	private final double bucket0;
	private final double bucket1;
	private final double bucket2;
	private final double bucket3;

	WadProcessor2ColorsTextMode(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, new MapProcessor2ColorsTextMode(byteOrder, wadFile));

		wadFile.replaceLump(new Lump("TITLEPIC", getLump("TP80X25M").data(), ByteBufferUtils.DONT_CARE));
		wadFile.replaceLump(new Lump("WIMAP0", getLump("WM80X25M").data(), ByteBufferUtils.DONT_CARE));
		wadFile.replaceLump(new Lump("HELP2", getLump("HL80X25M").data(), ByteBufferUtils.DONT_CARE));

		List<Color> vgaColors = createVgaColors(wadFile);
		this.grays = vgaColors.stream().map(Color::gray).toList();

		List<Double> sortedGrays = grays.stream().sorted().toList();
		this.bucket0 = sortedGrays.get(52);
		this.bucket1 = sortedGrays.get(103);
		this.bucket2 = sortedGrays.get(153);
		this.bucket3 = sortedGrays.get(205);

		List<Byte> lut = new ArrayList<>();
		for (Double gray : grays) {
			if (gray < bucket0) {
				lut.add(C0);
			} else if (gray < bucket1) {
				lut.add(C1);
			} else if (gray < bucket2) {
				lut.add(C2);
			} else if (gray < bucket3) {
				lut.add(C3);
			} else {
				lut.add(C4);
			}
		}
		this.lookupTable = lut;
	}

	private List<Byte> createColormapInvulnerability() {
		List<Byte> colormapInvulnerability = new ArrayList<>();
		for (double gray : grays) {
			if (gray < bucket0) {
				colormapInvulnerability.add(C4);
			} else if (gray < bucket1) {
				colormapInvulnerability.add(C3);
			} else if (gray < bucket2) {
				colormapInvulnerability.add(C2);
			} else if (gray < bucket3) {
				colormapInvulnerability.add(C1);
			} else {
				colormapInvulnerability.add(C0);
			}
		}
		return colormapInvulnerability;
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

		return result;
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
