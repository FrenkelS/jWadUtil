package com.sfprod.jwadutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.util.Arrays;

public class JWadUtil {

	public static void main(String[] args) {
		// createWad(Game.DOOMTD3_BIG_ENDIAN);
		Arrays.stream(Game.values()).forEach(JWadUtil::createWad);
	}

	static void createWad(Game game) {
		System.out.println("Creating WAD file for " + game);

		WadFile wadFile = new WadFile("/doom1.wad");

		wadFile.addLump(getLump("CREDITS"));
		wadFile.addLump(getLump("M_ARUN"));
		wadFile.addLump(getLump("M_GAMMA"));
		wadFile.addLump(getLump("STGANUM0"));
		wadFile.addLump(getLump("STGANUM1"));
		wadFile.addLump(getLump("STGANUM2"));
		wadFile.addLump(getLump("STGANUM3"));
		wadFile.addLump(getLump("STGANUM4"));
		wadFile.addLump(getLump("STGANUM5"));
		wadFile.addLump(getLump("STGANUM6"));
		wadFile.addLump(getLump("STGANUM7"));
		wadFile.addLump(getLump("STGANUM8"));
		wadFile.addLump(getLump("STGANUM9"));
		wadFile.addLump(getLump("PLAYPAL1"));
		wadFile.addLump(getLump("PLAYPAL2"));
		wadFile.addLump(getLump("PLAYPAL3"));
		wadFile.addLump(getLump("PLAYPAL4"));
		wadFile.addLump(getLump("PLAYPAL5"));

		wadFile.replaceLump(getLump("HELP2"));
		wadFile.replaceLump(getLump("STBAR"));
		wadFile.replaceLump(getLump("TITLEPIC"));
		wadFile.replaceLump(getLump("WIMAP0"));

		WadProcessor wadProcessor = WadProcessor.getWadProcessor(game, wadFile);
		wadProcessor.processWad();

		wadFile.saveWadFile(game.getByteOrder(), game.getWadFile());

		System.out.println();
	}

	static Lump getLump(String lumpname) {
		try {
			byte[] data = JWadUtil.class.getResourceAsStream('/' + lumpname + ".LMP").readAllBytes();
			return new Lump(lumpname, data, ByteOrder.LITTLE_ENDIAN);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
