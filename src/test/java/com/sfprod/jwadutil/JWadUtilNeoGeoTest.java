package com.sfprod.jwadutil;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.sfprod.utils.NumberUtils;

/**
 * This class tests {@link JWadUtil}
 */
public class JWadUtilNeoGeoTest {

	@Test
	void createWad() throws Exception {
//		Game game = Game.DOOM64KB_TEXT_MODE_BIG_ENDIAN;
//		Game game = Game.DOOM64KB_LITTLE_ENDIAN;
		Game game = Game.DOOM64KB_NEO_GEO_256_COLOR;
		JWadUtil.createWad(game);
		byte[] bytes = Files.readAllBytes(Path.of("target", game.getWadFile()));

		System.out.println("static const unsigned char doom_iwad[" + bytes.length + "] = {");
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

	private String toHex(byte b) {
		int i = NumberUtils.toInt(b);
		if (i < 16) {
			return "0x0" + Integer.toHexString(i);
		} else {
			return "0x" + Integer.toHexString(i);
		}
	}
}
