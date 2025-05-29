package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor16ColorsDitheredAtariST extends WadProcessor16ColorsDithered {

	private static final List<Color> CUSTOM_ATARI_ST_COLORS = List.of( //
			new Color(0, 0, 0), //
			new Color(0, 0, 68), //
			new Color(51, 68, 34), //
			new Color(153, 136, 102), //
			new Color(119, 17, 17), //
			new Color(119, 68, 34), //
			new Color(136, 102, 85), //
			new Color(170, 170, 170), //
			new Color(102, 102, 102), //
			new Color(0, 0, 204), //
			new Color(51, 136, 34), //
			new Color(238, 170, 119), //
			new Color(221, 85, 0), //
			new Color(204, 0, 204), //
			new Color(255, 238, 68), //
			new Color(255, 255, 255) //
	);

	private final short[] divisors;

	WadProcessor16ColorsDitheredAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, CUSTOM_ATARI_ST_COLORS, 7);
		this.divisors = AtariSTUtil.getDivisors();
	}

	@Override
	protected short[] getDivisors() {
		return divisors;
	}

	@Override
	protected List<Integer> createVga256ToDitheredLUT() {
		List<Integer> indexes = new ArrayList<>();

		for (Color vgaColor : vgaColors) {
			int minClosestColor = Integer.MAX_VALUE;
			int indexClosestColor = -1;

			for (int c = 0; c < availableColors.size(); c++) {
				Color atariStColor = availableColors.get(c);

				int distanceToVga = atariStColor.calculateDistance(vgaColor);
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
	void changePaletteStatusBarMenuAndIntermission(Lump lump) {
		changePalettePicture(lump, this::convert256to16);
	}

	private byte convert256to16(byte b) {
		byte out = convert256to16dithered(b);
		return toByte(toInt(out) & 0x0f);
	}

	@Override
	void processRawGraphics() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Finale background flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::processRawGraphic);
	}

	private void processRawGraphic(Lump lump) {
		int newLength = lump.length() / 2;
		ByteBuffer oldbb = lump.dataAsByteBuffer();
		ByteBuffer newbb = ByteBufferUtils.newByteBuffer(ByteBufferUtils.DONT_CARE, newLength);
		byte[] oldcolors = new byte[8];
		for (int i = 0; i < lump.length() / 16; i++) {
			for (int c = 0; c < 8; c++) {
				oldcolors[c] = oldbb.get();
			}

			byte[] newcolorshi = new byte[4];
			for (int bitplane = 0; bitplane < 4; bitplane++) {
				for (int b = 0; b < 8; b++) {
					int bitValue = (oldcolors[b] >> bitplane) & 1;
					newcolorshi[bitplane] |= bitValue << (7 - b);
				}
			}

			for (int c = 0; c < 8; c++) {
				oldcolors[c] = oldbb.get();
			}

			byte[] newcolorslo = new byte[4];
			for (int bitplane = 0; bitplane < 4; bitplane++) {
				for (int b = 0; b < 8; b++) {
					int bitValue = (oldcolors[b] >> bitplane) & 1;
					newcolorslo[bitplane] |= bitValue << (7 - b);
				}
			}

			for (int bitplane = 0; bitplane < 4; bitplane++) {
				newbb.put(newcolorshi[bitplane]);
				newbb.put(newcolorslo[bitplane]);
			}
		}
		wadFile.replaceLump(new Lump(lump.name(), newbb));
	}
}
