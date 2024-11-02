package com.sfprod.jwadutil;

import java.nio.ByteOrder;

public enum Game {
	DOOM8088("Doom8088", ByteOrder.LITTLE_ENDIAN, "DOOM1.WAD"), //
	DOOM8088_136_COLOR("Doom8088", ByteOrder.LITTLE_ENDIAN, "DOOM136.WAD"), //
	DOOMTD3_BIG_ENDIAN("doomtd3", ByteOrder.BIG_ENDIAN, "DOOMTD3B.WAD"), //
	DOOMTD3_LITTLE_ENDIAN("doomtd3", ByteOrder.LITTLE_ENDIAN, "DOOMTD3L.WAD"), //
	ELKSDOOM("ELKSDOOM", ByteOrder.LITTLE_ENDIAN, "elksdoom.wad");

	private final String title;
	private final ByteOrder byteOrder;
	private final String wadFile;

	Game(String title, ByteOrder byteOrder, String wadFile) {
		this.title = title;
		this.byteOrder = byteOrder;
		this.wadFile = wadFile;
	}

	public String getTitle() {
		return title;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public String getWadFile() {
		return wadFile;
	}
}
