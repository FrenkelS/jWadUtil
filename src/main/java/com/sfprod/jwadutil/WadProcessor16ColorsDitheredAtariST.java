package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor16ColorsDitheredAtariST extends WadProcessor16ColorsDithered {

	private final short[] divisors;

	WadProcessor16ColorsDitheredAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
		this.divisors = AtariSTUtil.getDivisors();
	}

	@Override
	short[] getDivisors() {
		return divisors;
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
