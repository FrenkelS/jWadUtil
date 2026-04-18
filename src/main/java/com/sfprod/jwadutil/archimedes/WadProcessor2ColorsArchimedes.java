package com.sfprod.jwadutil.archimedes;

import java.nio.ByteOrder;

import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor4Colors;

public class WadProcessor2ColorsArchimedes extends WadProcessor4Colors {

	public WadProcessor2ColorsArchimedes(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, false, false);
	}

}
