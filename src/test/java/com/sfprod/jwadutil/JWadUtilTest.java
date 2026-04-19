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
			Map.entry(Game.DOOM8088, "75E8CA37"), //
			Map.entry(Game.DOOM8088_2_COLOR_TEXT_MODE, "FD538E13"), //
			Map.entry(Game.DOOM8088_4_COLOR, "359C54AD"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED, "B2CABBA2"), //
			Map.entry(Game.DOOM8088_16_COLOR_DITHERED_TEXT_MODE, "83653B57"), //
			Map.entry(Game.DOOM8088_ARCHIMEDES_2_COLOR, "20E75F2"), //
			Map.entry(Game.DOOM8088_ARCHIMEDES_256_COLOR, "609AD4F"), //
			Map.entry(Game.DOOM8088_AMIGA_2_COLOR, "E2425A49"), //
			Map.entry(Game.DOOM8088_AMIGA_16_COLOR, "909F083B"), //
			Map.entry(Game.DOOM8088_AT_T_UNIX_PC_2_COLOR, "795C58D9"), //
			Map.entry(Game.DOOM8088_ATARI_ST_2_COLOR, "6E02604B"), //
			Map.entry(Game.DOOM8088_ATARI_ST_16_COLOR, "6BD4EC4E"), //
			Map.entry(Game.DOOM8088_MACINTOSH_2_COLOR, "2E4F98B8"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_2_COLOR, "53DA9368"), //
			Map.entry(Game.DOOM8088_SINCLAIR_QL_8_COLOR, "389B908B"), //
			Map.entry(Game.DOOMTD3_BIG_ENDIAN, "62614EB0"), //
			Map.entry(Game.DOOMTD3_LITTLE_ENDIAN, "65E887FA"), //
			Map.entry(Game.ELKSDOOM, "943F2678") //
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
