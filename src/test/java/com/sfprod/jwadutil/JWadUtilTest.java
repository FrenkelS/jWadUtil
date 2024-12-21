package com.sfprod.jwadutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.CRC32;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * This class tests {@link JWadUtil}
 */
class JWadUtilTest {

	private static final Map<Game, String> EXPECTED_CHECKSUMS = new EnumMap<>(Map.of( //
			Game.DOOM8088, "72CFCC78", //
			Game.DOOM8088_2_COLOR_TEXT_MODE, "3FDE36CB", //
			Game.DOOM8088_16_COLOR_DITHERED, "1CFD4AC0", //
			Game.DOOM8088_16_COLOR_DITHERED_TEXT_MODE, "1558C1E8", //
			Game.DOOMTD3_BIG_ENDIAN, "D8F76736", //
			Game.DOOMTD3_LITTLE_ENDIAN, "B215BC27", //
			Game.ELKSDOOM, "93182037" //
	));

	@ParameterizedTest
	@EnumSource(value = Game.class)
	void createWad(Game game) throws Exception {
		JWadUtil.createWad(game);

		CRC32 crc32 = new CRC32();
		crc32.update(Files.readAllBytes(Path.of("target", game.getWadFile())));

		String expectedChecksum = EXPECTED_CHECKSUMS.get(game);
		String actualChecksum = Long.toHexString(crc32.getValue()).toUpperCase();
		assertEquals(expectedChecksum, actualChecksum);
	}
}
