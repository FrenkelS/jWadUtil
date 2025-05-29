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
			Game.DOOM8088, "E580A8BC", //
			Game.DOOM8088_2_COLOR_TEXT_MODE, "248B4FC0", //
			Game.DOOM8088_4_COLOR, "6A64EE74", //
			Game.DOOM8088_16_COLOR_DITHERED, "BD42FF1E", //
			Game.DOOM8088_16_COLOR_DITHERED_TEXT_MODE, "29FAF676", //
			Game.DOOM8088_ATARI_ST_2_COLOR, "F1DF5901", //
			Game.DOOM8088_ATARI_ST_16_COLOR, "E446F4BB", //
			Game.DOOMTD3_BIG_ENDIAN, "D8F76736", //
			Game.DOOMTD3_LITTLE_ENDIAN, "B215BC27", //
			Game.ELKSDOOM, "45744F3" //
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
