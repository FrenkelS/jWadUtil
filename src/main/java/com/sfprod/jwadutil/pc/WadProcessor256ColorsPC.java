package com.sfprod.jwadutil.pc;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor;

public class WadProcessor256ColorsPC extends WadProcessor {

	public WadProcessor256ColorsPC(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	@Override
	protected void changeColors() {
	}

	@Override
	protected void processColormap() {
	}

	@Override
	protected void shuffleColors() {
	}

	@Override
	protected void processRawGraphics() {
	}
}
