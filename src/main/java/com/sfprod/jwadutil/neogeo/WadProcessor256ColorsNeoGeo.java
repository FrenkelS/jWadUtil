package com.sfprod.jwadutil.neogeo;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.sfprod.jwadutil.Color;
import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessorLimitedColors;
import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor256ColorsNeoGeo extends WadProcessorLimitedColors {

	private static final List<Integer> GRAYSCALE_FROM_DARK_TO_BRIGHT = List.of(//
			0x00, 0x01, 0x02, 0x03, //
			0x25, 0x28, 0x2a, 0x2f, //
			0xd0, 0xd1, 0xd2, 0xd3, //
			0xf5, 0xf8, 0xfa, 0xff);

	private List<Integer> neoGeoColorNumbers;

	public WadProcessor256ColorsNeoGeo(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, GRAYSCALE_FROM_DARK_TO_BRIGHT, 16,
				new MapProcessorDoom64KB(byteOrder, wadFile));

		List<Integer> uniqueNeoGeoColorNumberList = new ArrayList<>(
				vgaColors.stream().map(this::toNeoGeoPalette).collect(Collectors.toCollection(LinkedHashSet::new)));

		List<Integer> neoGeoColorNumberList = new ArrayList<>();
		int index = 0;
		for (int i = 0; i < 16; i++) {
			neoGeoColorNumberList.add(uniqueNeoGeoColorNumberList.get(index));
			index++;
		}
		outerloop: for (int j = 0; j < 16; j++) {
			neoGeoColorNumberList.add(0x8000);
			for (int i = 0; i < 15; i++) {
				if (index == 236) {
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					this.neoGeoColorNumbers = neoGeoColorNumberList;
					break outerloop;
				}
				neoGeoColorNumberList.add(uniqueNeoGeoColorNumberList.get(index));
				index++;
			}
		}

		List<Integer> map = new ArrayList<>();
		for (Color vgaColor : vgaColors) {
			int neoGeoColorNumber = toNeoGeoPalette(vgaColor);
			int index2 = neoGeoColorNumbers.indexOf(neoGeoColorNumber);
			map.add(index2);
		}
		List<Integer> emptySlots = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			if (!map.contains(i)) {
				emptySlots.add(i);
			}
		}
		Color[] neoGeoColorArray = new Color[256];
		for (int i = 0; i < 256; i++) {
			neoGeoColorArray[map.get(i)] = vgaColors.get(i);
		}
		for (int emptySlot : emptySlots) {
			neoGeoColorArray[emptySlot] = new Color(0, 0, 0);
		}
		fillAvailableColorsShuffleMap(Arrays.asList(neoGeoColorArray));
	}

	/**
	 * @see https://wiki.neogeodev.org/index.php/Colors
	 * @param vgaColor
	 * @return
	 */
	private int toNeoGeoPalette(Color vgaColor) {
		int red = vgaColor.r();
		int green = vgaColor.g();
		int blue = vgaColor.b();

		int luma = (int) Math.floor((54.213 * red) + (182.376 * green) + (18.411 * blue)) & 1;

		red = (int) Math.floor(red / 8);
		green = (int) Math.floor(green / 8);
		blue = (int) Math.floor(blue / 8);

		return (((luma ^ 1) << 15) | ((red & 1) << 14) | ((green & 1) << 13) | ((blue & 1) << 12) | ((red & 0x1E) << 7)
				| ((green & 0x1E) << 3) | (blue >> 1));
	}

	@Override
	protected void processColormap() {
		// Colormap
		Lump colormapLump = wadFile.getLumpByName("COLORMAP");

		for (int i = 0; i < colormapLump.length(); i++) {
			byte b = colormapLump.data()[i];

			Color vgaColor = vgaColors.get(toInt(b));
			int neoGeoColorNumber = toNeoGeoPalette(vgaColor);
			int index = neoGeoColorNumbers.indexOf(neoGeoColorNumber);

			colormapLump.data()[i] = toByte(index);
		}

		// Playpal
		List<Integer> map = new ArrayList<>();
		for (Color vgaColor : vgaColors) {
			int neoGeoColorNumber = toNeoGeoPalette(vgaColor);
			int index = neoGeoColorNumbers.indexOf(neoGeoColorNumber);
			map.add(index);
		}
		List<Integer> emptySlots = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			if (!map.contains(i)) {
				emptySlots.add(i);
			}
		}

		List<Lump> playpals = wadFile.getLumpsByName("PLAYPAL");
		for (Lump playpal : playpals) {
			ByteBuffer bbPlaypal = ByteBufferUtils.newByteBuffer(byteOrder, 256 * 2 * 14);
			for (int palette = 0; palette < 14; palette++) {
				List<Color> playpalColors = createPlaypalColors(playpal, palette);
				for (int i = 0; i < 256; i++) {
					Color playpalColor = playpalColors.get(i);
					int neoGeoColorNumber = toNeoGeoPalette(playpalColor);
					bbPlaypal.position(256 * 2 * palette + map.get(i) * 2);
					bbPlaypal.putShort(toShort(neoGeoColorNumber));
				}
				for (int emptySlot : emptySlots) {
					bbPlaypal.position(256 * 2 * palette + emptySlot * 2);
					bbPlaypal.putShort(toShort(0x8000));
				}
			}
			wadFile.replaceLump(new Lump(playpal.name(), bbPlaypal.array(), byteOrder));
		}
	}

	private List<Color> createPlaypalColors(Lump playpal, int palette) {
		ByteBuffer bb = playpal.dataAsByteBuffer();
		bb.position(3 * 256 * palette);
		List<Color> colors = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			int r = toInt(bb.get());
			int g = toInt(bb.get());
			int b = toInt(bb.get());
			colors.add(new Color(r, g, b));
		}
		return colors;
	}

	@Override
	protected void changePaletteRaw(Lump lump) {
		for (int i = 0; i < lump.length(); i++) {
			lump.data()[i] = convertVga256toByte(lump.data()[i]);
		}
	}

	@Override
	protected List<Integer> createVga256toByteLUT(List<Color> availableCols) {
		List<Integer> indexes = new ArrayList<>();
		for (Color vgaColor : vgaColors) {
			int neoGeoColor = toNeoGeoPalette(vgaColor);
			int index = neoGeoColorNumbers.indexOf(neoGeoColor);
			indexes.add(index);
		}

		return indexes;
	}

	@Override
	protected void changeColors() {
		// Raw graphics
		List<Lump> rawGraphics = new ArrayList<>();
		rawGraphics.add(wadFile.getLumpByName("HELP2"));
		// rawGraphics.add(wadFile.getLumpByName("STBAR"));
		rawGraphics.add(wadFile.getLumpByName("TITLEPIC"));
		rawGraphics.add(wadFile.getLumpByName("WIMAP0"));
		// Finale background flat
		rawGraphics.add(wadFile.getLumpByName("FLOOR4_8"));
		rawGraphics.forEach(this::changePaletteRaw);
	}

	@Override
	protected List<Integer> createVga256toSingleColorLUT(List<Integer> vga256toByteLUT) {
		return Collections.emptyList();
	}

	@Override
	protected void removeUnusedLumps() {
		List<Lump> playpals = wadFile.getLumpsByName("PLAYPAL");

		super.removeUnusedLumps();

		for (Lump playpal : playpals) {
			wadFile.addLump(playpal);
		}

		// Menu graphics
		List<Lump> mLumps = wadFile.getLumpsByName("M_");
		mLumps.forEach(wadFile::removeLump);

		// Status bar graphics
		List<Lump> stLumps = wadFile.getLumpsByName("ST");
		stLumps.stream().filter(l -> !("STIMA0".equals(l.nameAsString()) || l.nameAsString().startsWith("STEP")))
				.forEach(wadFile::removeLump);

		// Intermission screen graphics
		List<Lump> wiLumps = wadFile.getLumpsByName("WI");
		wiLumps.stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).forEach(wadFile::removeLump);
	}

	@Override
	protected void duplicateMaps() {
		int lumpNumE1M1 = wadFile.getLumpNumByName("E1M1");

		List<Lump> e1m1Lumps = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			e1m1Lumps.add(wadFile.getLumpByNum(lumpNumE1M1 + 1 + i));
		}

		List<Integer> mapNumbers = List.of(2, 3, 4, 5, 6, 7, 9);
		for (int mapNumber : mapNumbers) {
			int lumpNumE1M2 = wadFile.getLumpNumByName("E1M" + mapNumber);
			for (int i = 0; i < 9; i++) {
				wadFile.replaceLump(lumpNumE1M2 + 1 + i, e1m1Lumps.get(i));
			}
		}
	}
}
