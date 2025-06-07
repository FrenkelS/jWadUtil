package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.sfprod.utils.NumberUtils;

abstract class WadProcessor16ColorsDithered extends WadProcessorLimitedColors {

	private final List<Integer> vga256ToDitheredLUT;

	private final int divisor;

	WadProcessor16ColorsDithered(String title, ByteOrder byteOrder, WadFile wadFile, List<Color> sixteenColors,
			int divisor) {
		super(title, byteOrder, wadFile);

		List<Color> colors = new ArrayList<>();
		for (int h = 0; h < 16; h++) {
			for (int l = 0; l < 16; l++) {
				Color ch = sixteenColors.get(h);
				Color cl = sixteenColors.get(l);
				colors.add(ch.blendColors(cl));
			}
		}
		fillAvailableColorsShuffleMap(colors);

		this.divisor = divisor;
		this.vga256ToDitheredLUT = createVga256ToDitheredLUT();
	}

	protected abstract List<Integer> createVga256ToDitheredLUT();

	private byte convert256to16dithered(byte b) {
		return vga256ToDitheredLUT.get(toInt(b)).byteValue();
	}

	byte convert256to16(byte b) {
		return convert256to16dithered(b);
	}

	@Override
	void changeColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Finale background flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::changePaletteRaw);

		// Graphics in picture format

		List<Lump> spritesAndWallsGraphics = new ArrayList<>(256);
		// Sprites
		spritesAndWallsGraphics.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		// Walls
		spritesAndWallsGraphics.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));

		spritesAndWallsGraphics.forEach(this::changePaletteSpritesAndWalls);

		List<Lump> statusBarMenuAndIntermissionGraphics = new ArrayList<>(256);
		// Status bar
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("STC"));
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("STF"));
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("STG"));
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("STK"));
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("STY"));
		// Menu
		statusBarMenuAndIntermissionGraphics.addAll(wadFile.getLumpsByName("M_"));
		// Intermission
		statusBarMenuAndIntermissionGraphics
				.addAll(wadFile.getLumpsByName("WI").stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).toList());

		statusBarMenuAndIntermissionGraphics.forEach(this::changePaletteStatusBarMenuAndIntermission);
	}

	private void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = convert256to16dithered(lump.data()[i]);
		}
	}

	private void changePaletteSpritesAndWalls(Lump lump) {
		changePalettePicture(lump, this::convert256to16dithered);
	}

	private void changePaletteStatusBarMenuAndIntermission(Lump lump) {
		changePalettePicture(lump, this::convert256to16);
	}

	@Override
	protected List<Byte> createColormapInvulnerability() {
		List<Double> grays = availableColors.stream().map(Color::gray).collect(Collectors.toSet()).stream()
				.sorted(Comparator.reverseOrder()).toList();

		List<Integer> grayscaleFromDarkToBright = List.of(0x00, 0x00, 0x08, 0x80, 0x88, 0x88, 0x07, 0x70, 0x78, 0x87,
				0x77, 0x77, 0x0f, 0xf0, 0x8f, 0xf8, 0x7f, 0xf7, 0xff, 0xff, 0xff);

		return availableColors.stream().mapToDouble(Color::gray).mapToInt(grays::indexOf).map(i -> i / divisor)
				.map(grayscaleFromDarkToBright::get).mapToObj(NumberUtils::toByte).toList();
	}

	@Override
	void shuffleColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		// rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::shuffleColorsRaw);

		// Graphics in picture format
		// Walls
		wadFile.getLumpsBetween("P1_START", "P1_END").forEach(this::shuffleColorPicture);
	}

	private void shuffleColorsRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = shuffleColor(lump.data()[i]);
		}
	}
}
