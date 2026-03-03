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
			Map.entry(Game.DOOM8088, "A81E3AE3"), //
			Map.entry(Game.DOOM8088_2_COLOR_TEXT_MODE, "513E6BED"), //
			Map.entry(Game.DOOM8088_4_COLOR, "1EFBB6B2"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED, "E1E66288"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED_TEXT_MODE, "BC6C39A"), //
			Map.entry(Game.DOOM8088_AMIGA_2_COLOR, "330C006E"), //
			Map.entry(Game.DOOM8088_AMIGA_16_COLOR, "E1FFE9AB"), //
			Map.entry(Game.DOOM8088_AT_T_UNIX_PC_2_COLOR, "87107D26"), //
			Map.entry(Game.DOOM8088_ATARI_ST_2_COLOR, "A4EF339A"), //
			Map.entry(Game.DOOM8088_ATARI_ST_16_COLOR, "211D0AF0"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_2_COLOR, "402EDCCE"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_8_COLOR, "6AE87F91"), //
			Map.entry(Game.DOOMTD3_BIG_ENDIAN, "ADCB190D"), //
			Map.entry(Game.DOOMTD3_LITTLE_ENDIAN, "71B86687"), //
			Map.entry(Game.ELKSDOOM, "49C9D6AC") //
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
