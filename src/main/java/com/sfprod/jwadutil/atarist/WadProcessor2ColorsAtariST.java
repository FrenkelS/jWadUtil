package com.sfprod.jwadutil.atarist;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor2Colors;

public class WadProcessor2ColorsAtariST extends WadProcessor2Colors {

	public WadProcessor2ColorsAtariST(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false);
	}

	@Override
	protected void processSoundEffects() {
		AtariSTUtil.processSoundEffects(wadFile);
	}
}
