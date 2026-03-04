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

	private static final Map<Game, String> EXPECTED_CHECKSUMS = new EnumMap<>(Map.ofEntries( //
			Map.entry(Game.DOOM8088, "CDB627A8"), //
			Map.entry(Game.DOOM8088_2_COLOR_TEXT_MODE, "80AADBE9"), //
			Map.entry(Game.DOOM8088_4_COLOR, "53DE565"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED, "2BDE79CF"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED_TEXT_MODE, "D51B2960"), //
			Map.entry(Game.DOOM8088_AMIGA_2_COLOR, "1EAB3EED"), //
			Map.entry(Game.DOOM8088_AMIGA_16_COLOR, "26A73F36"), //
			Map.entry(Game.DOOM8088_AT_T_UNIX_PC_2_COLOR, "76C2CB8D"), //
			Map.entry(Game.DOOM8088_ATARI_ST_2_COLOR, "B7DA40A3"), //
			Map.entry(Game.DOOM8088_ATARI_ST_16_COLOR, "C57B375C"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_2_COLOR, "95996BF0"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_8_COLOR, "36438169"), //
			Map.entry(Game.DOOMTD3_BIG_ENDIAN, "3F272103"), //
			Map.entry(Game.DOOMTD3_LITTLE_ENDIAN, "37DD23A3"), //
			Map.entry(Game.ELKSDOOM, "2C61CBE7") //
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
