package com.sfprod.jwadutil;

import static com.sfprod.jwadutil.JWadUtil.getLump;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class WadProcessorDoomtd3 extends WadProcessor {

	WadProcessorDoomtd3(WadFile wadFile) {
		super(wadFile, createColors(wadFile), Function.identity());
		wadFile.addLump(getLump("CACHE"));
	}

	// FIXME monochrome
	private static List<Color> createColors(WadFile wadFile) {
		Lump playpal = wadFile.getLumpByName("PLAYPAL");
		ByteBuffer bb = playpal.dataAsByteBuffer();
		List<Color> vgaColors = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			int r = toInt(bb.get());
			int g = toInt(bb.get());
			int b = toInt(bb.get());
			vgaColors.add(new Color(r, g, b));
		}
		return vgaColors;
	}

	private static final List<String> sprnames = List.of( //
			"TROO", "SHTG", "PISG", "PISF", "SHTF", //
			"BLUD", "PUFF", "BAL1", //
			"PLAY", "POSS", //
			"SPOS", "SARG", //
			"ARM1", "ARM2", "BAR1", "BEXP", //
			"BON1", "BON2", "BKEY", "RKEY", "YKEY", "STIM", "MEDI", //
			"SOUL", "PINS", "SUIT", "PMAP", "CLIP", "AMMO", //
			"ROCK", "BROK", "SHEL", "SBOX", "BPAK", "MGUN", "CSAW", //
			"LAUN", "SHOT", "COLU", "POL5", //
			"CBRA", "ELEC" //
	);

	@Override
	protected void removeUnusedLumps() {
		super.removeUnusedLumps();

		wadFile.removeLumps("PLAYPAL");
		wadFile.removeLump("ENDOOM");

		for (int i = 1; i <= 9; i++) {
			if (i != 7) {
				removeMap("E1M" + i);
			}
		}

		wadFile.removeLumps("DP");
		wadFile.removeLump("HELP2");
		wadFile.removeLump("TITLEPIC");
		wadFile.removeLump("STGNUM0");
		wadFile.removeLump("STGNUM1");
		wadFile.removeLump("STGNUM8");
		wadFile.removeLump("STGNUM9");
		wadFile.removeLumps("STCFN");
		wadFile.removeLumps("M_");
		wadFile.removeLumps("WI");
		wadFile.removeLump("CREDITS");
		wadFile.removeLump("P_START");
//		wadFile.removeLump("P1_START");
//		wadFile.removeLump("P1_END");
		wadFile.removeLump("P_END");

		List<Lump> sprites = new ArrayList<>(wadFile.getLumpsBetween("S_START", "S_END"));
		for (Lump sprite : sprites) {
			if (!sprnames.contains(sprite.nameAsString().substring(0, 4))) {
				wadFile.removeLump(sprite);
			}
		}

		wadFile.removeLumps("PLAY");

		wadFile.removeLump("WALL03_1");
		wadFile.removeLump("WALL03_4");
		wadFile.removeLumps("WALL04");
		wadFile.removeLumps("WALL05");
		wadFile.removeLumps("W15");
		wadFile.removeLumps("W28");
		wadFile.removeLumps("W31");
		wadFile.removeLumps("W32");
		wadFile.removeLump("W33_5");
		wadFile.removeLump("W33_7");
		wadFile.removeLump("WALL57_2");
		wadFile.removeLump("WALL57_3");
		wadFile.removeLump("WALL57_4");
		wadFile.removeLump("WALL62_2");
		wadFile.removeLump("W94_1");
		wadFile.removeLumps("W11");
		wadFile.removeLumps("SW11");
		wadFile.removeLump("SW12_4");
		wadFile.removeLump("SW12_5");
		wadFile.removeLumps("SW17");
		wadFile.removeLumps("SW18");
		wadFile.removeLumps("AG128");
		wadFile.removeLump("AGB128_1");
		wadFile.removeLumps("TOMW2");
		wadFile.removeLump("STEP03");
		wadFile.removeLump("STEP04");
		wadFile.removeLump("STEP08");
		wadFile.removeLump("STEP09");
		wadFile.removeLumps("TP2");
		wadFile.removeLumps("COMP01");
		wadFile.removeLumps("COMP1");
		wadFile.removeLump("COMP02_4");
		wadFile.removeLump("COMP02_5");
		wadFile.removeLump("COMP02_6");
		wadFile.removeLump("COMP02_8");
		wadFile.removeLump("COMP03_1");
		wadFile.removeLump("COMP03_6");
		wadFile.removeLump("COMP03_7");
		wadFile.removeLump("COMP03_9");
		wadFile.removeLump("DOOR2_1");
		wadFile.removeLumps("BLIT");
		wadFile.removeLump("NUKEDGE");
		wadFile.removeLump("FLAMP");
		wadFile.removeLumps("TSCRN");
		wadFile.removeLump("PS20A0");
		wadFile.removeLumps("SW3S");
		wadFile.removeLump("SKY1");
	}

	private void removeMap(String map) {
		int lumpNum = wadFile.getLumpNumByName(map);
		for (int i = 0; i < 10; i++) {
			wadFile.removeLump(lumpNum);
		}
	}
}
