package com.sfprod.jwadutil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.util.Arrays;

public class JWadUtil {

	enum Game {
		DOOM8088(ByteOrder.LITTLE_ENDIAN, "DOOM1.WAD"), //
		DOOM8088_16_COLOR(ByteOrder.LITTLE_ENDIAN, "DOOM16.WAD"), //
		DOOMTD3_BIG_ENDIAN(ByteOrder.BIG_ENDIAN, "doomtd3b.wad"), //
		DOOMTD3_LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN, "doomtd3l.wad"), //
		ELKSDOOM(ByteOrder.LITTLE_ENDIAN, "elksdoom.wad");

		private final ByteOrder byteOrder;
		private final String wadFile;

		Game(ByteOrder byteOrder, String wadFile) {
			this.byteOrder = byteOrder;
			this.wadFile = wadFile;
		}
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		Game game = Game.DOOM8088;
		if (Arrays.asList(args).contains("-NR_OF_COLORS=16")) {
			game = Game.DOOM8088_16_COLOR;
		}

		WadFile iwadFile = new WadFile("/doom1.wad");

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pwadFile = new WadFile("/gbadoom.wad");

		iwadFile.mergeWadFile(pwadFile);

		iwadFile.replaceLump(getLump("CREDITS"));
		iwadFile.replaceLump(getLump("HELP2"));
		iwadFile.replaceLump(getLump("STBAR"));
		iwadFile.replaceLump(getLump("TITLEPIC"));
		iwadFile.replaceLump(getLump("WIMAP0"));

		WadProcessor wadProcessor = WadProcessor.getWadProcessor(game, iwadFile);
		wadProcessor.processWad();

		iwadFile.saveWadFile(game.wadFile);
	}

	private static Lump getLump(String lumpname) throws IOException, URISyntaxException {
		byte[] data = WadFile.class.getResourceAsStream('/' + lumpname + ".LMP").readAllBytes();
		return new Lump(lumpname, data);
	}
}
