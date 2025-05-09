package com.sfprod.jwadutil;

import java.nio.ByteOrder;

class WadProcessorAtariST extends WadProcessor4Colors {

	WadProcessorAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		wadFile.removeLumps("DP");
	}
}
