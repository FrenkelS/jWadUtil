package com.sfprod.jwadutil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.sfprod.jwadutil.WadFile.Lump;

public class JWadUtil {

	private enum Game {
		DOOM8088(ByteOrder.LITTLE_ENDIAN, "doom8088.wad"), //
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

		WadFile iwadFile = new WadFile("/doom1.wad");

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pwadFile = new WadFile("/gbadoom.wad");

		iwadFile.mergeWadFile(pwadFile);

		iwadFile.replaceLump(getLump("HELP2"));
		iwadFile.replaceLump(getLump("STBAR"));
		iwadFile.replaceLump(getLump("TITLEPIC"));
		iwadFile.replaceLump(getLump("WIMAP0"));

		WadProcessor wadProcessor = new WadProcessor(iwadFile);
		wadProcessor.processWad();

		iwadFile.saveWadFile(game.wadFile);
	}

	private static Lump getLump(String lumpname) throws IOException, URISyntaxException {
		byte[] name = Arrays.copyOf(lumpname.getBytes(StandardCharsets.US_ASCII), 8);
		byte[] data = WadFile.class.getResourceAsStream('/' + lumpname + ".LMP").readAllBytes();
		return new Lump(name, data);
	}
}