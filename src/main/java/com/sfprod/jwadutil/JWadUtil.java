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

		iwadFile.saveWadFile(game.getByteOrder(), game.getWadFile());

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
