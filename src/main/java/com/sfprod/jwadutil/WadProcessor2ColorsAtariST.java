package com.sfprod.jwadutil;

import java.nio.ByteOrder;

class WadProcessor2ColorsAtariST extends WadProcessor4Colors {

	WadProcessor2ColorsAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		AtariSTUtil.processSoundEffects(wadFile);
	}
}
