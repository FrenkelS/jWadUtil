package com.sfprod.jwadutil;

import java.nio.ByteOrder;

public enum Game {
	DOOM8088(ByteOrder.LITTLE_ENDIAN, "DOOM1.WAD"), //
	DOOM8088_16_COLOR(ByteOrder.LITTLE_ENDIAN, "DOOM16.WAD"), //
//	DOOMTD3_BIG_ENDIAN(ByteOrder.BIG_ENDIAN, "DOOMTD3B.WAD"), //
	DOOMTD3_LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN, "DOOMTD3L.WAD"); //
//	ELKSDOOM(ByteOrder.LITTLE_ENDIAN, "elksdoom.wad");

	private final ByteOrder byteOrder;
	private final String wadFile;

	Game(ByteOrder byteOrder, String wadFile) {
		this.byteOrder = byteOrder;
		this.wadFile = wadFile;
	}

	public String getWadFile() {
		return wadFile;
	}
}
