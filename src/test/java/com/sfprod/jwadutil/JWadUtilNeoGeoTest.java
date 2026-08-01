package com.sfprod.jwadutil;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.sfprod.utils.NumberUtils;

/**
 * This class tests {@link JWadUtil}
 */
class JWadUtilNeoGeoTest {

	@Test
	void createWad() {
//		Game game = Game.DOOM64KB_TEXT_MODE_BIG_ENDIAN;
//		Game game = Game.DOOM64KB_LITTLE_ENDIAN;
		Game game = Game.DOOM64KB_NEO_GEO_256_COLOR;

		WadFile wadFile = new WadFile("/doom1.wad");

		WadProcessor wadProcessor = WadProcessorFactory.getWadProcessor(game, wadFile);
		wadProcessor.processWad();

		String cByteArray = wadFile.toCByteArrayString(game.getByteOrder(), "doom_iwad");
		System.out.println(cByteArray);
	}

	private String toHex(byte b) {
		int i = NumberUtils.toInt(b);
		if (i < 16) {
			return "0x0" + Integer.toHexString(i);
		} else {
			return "0x" + Integer.toHexString(i);
		}
	}

	@Test
	void createWadMap() throws Exception {
		byte[] bytes = Files.readAllBytes(Path.of("target", "DOOMMAPB.WAD"));

		System.out.println("static const unsigned char doom_iwad_maps[" + bytes.length + "] = {");
		int i = 1;
		for (byte b : bytes) {
			System.out.print(toHex(b) + ',');
			if (i % 40 == 0) {
				System.out.println();
			}
			i++;
		}
		System.out.println();
		System.out.println("};");
	}
}
