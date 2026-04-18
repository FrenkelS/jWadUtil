package com.sfprod.jwadutil.pc;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor4ColorsPC extends WadProcessor4Colors {

	public WadProcessor4ColorsPC(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false, true);
	}

}
