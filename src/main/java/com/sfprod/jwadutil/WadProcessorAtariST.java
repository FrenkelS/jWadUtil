package com.sfprod.jwadutil;

import java.nio.ByteOrder;

class WadProcessorAtariST extends WadProcessor4Colors {

	private final short[] divisors;

	WadProcessorAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);

		short[] oldDivs = super.getDivisors();
		short[] newDivs = new short[128];
		newDivs[0] = 0;
		for (int i = 1; i < 128; i++) {
			short od = oldDivs[i];
			short frequency = (short) (1193181 / od);
			short nd = (short) ((2_000_000 / 16) / frequency);
			newDivs[i] = nd;
		}
		this.divisors = newDivs;
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();
	}

	@Override
	short[] getDivisors() {
		return divisors;
	}
}
