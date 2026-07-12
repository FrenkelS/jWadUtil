package com.sfprod.jwadutil.neogeo;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;
import static com.sfprod.utils.StringUtils.toByteArray;
import static com.sfprod.utils.StringUtils.toStringUpperCase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sfprod.jwadutil.Color;
import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessorLimitedColors;
import com.sfprod.utils.ByteBufferUtils;
import com.sfprod.utils.NumberUtils;

public class WadProcessor256ColorsNeoGeo extends WadProcessorLimitedColors {

	// @formatter:off
	private static final List<Integer> NEO_GEO_RGBS = List.of(
			0x000000, 0x00000b, 0x0000ff, 0x17330f, 0x373737, 0x27531b, 0x574333, 0x9f3700, 0x9b3333, 0x67533f, 0x7f532f, 0xeb9797, 0xb3b3b3, 0xababff, 0xff9b9b, 0xffdbc7,
			0x000000, 0x000023, 0x00003b, 0x171f07, 0x7f0000, 0x4b371b, 0x574333, 0x972f2f, 0x4f4f4f, 0x6f5743, 0x836b4b, 0xcb5707, 0xaf977f, 0xffbbbb, 0xebdb57, 0xdfdfdf,
			0x000000, 0x00002f, 0x3b3b3b, 0x732b00, 0x5f4b37, 0xa33b3b, 0xff1f1f, 0x439337, 0xdf670f, 0x8f8fff, 0xff8f3b, 0xcbcbcb, 0xffcfb3, 0xffff23, 0xe7e7e7, 0xffffd7,
			0x000000, 0x000047, 0x000083, 0xef0000, 0xcf00cf, 0xbb5757, 0x8b735b, 0xb37347, 0xff7f1b, 0xff7b7b, 0xbfa78f, 0xe79b6b, 0xffa35b, 0xffbf9b, 0xffe3d3, 0xffebdb,
			0x000000, 0xa70000, 0x000017, 0x4f0000, 0x5b472b, 0x676b4f, 0xff00ff, 0x4b9f3f, 0x7373ff, 0x9f8363, 0xeb6f0f, 0xbf7b4b, 0x9f876f, 0xc39b2f, 0xd78b5b, 0xc7c7c7,
			0x000000, 0x430000, 0x13230b, 0x670000, 0xb30000, 0x636363, 0x3f832f, 0xbf5b5b, 0xaf7b1f, 0xc76363, 0x9b7f6b, 0x53af47, 0xd37373, 0x939393, 0xff7bff, 0xffd7bb,
			0x000000, 0x000053, 0x5b0000, 0x5b0707, 0x232323, 0x3737ff, 0x774f2b, 0x775f3f, 0xb34f4f, 0x777777, 0xa78f77, 0xe78f8f, 0xd7bb43, 0xf3a3a3, 0xbfbfbf, 0xc7c7ff,
			0x000000, 0xababab, 0x8b0000, 0x332b13, 0x6f006b, 0x3f2b1b, 0x43331b, 0x4f3b2b, 0x6b4727, 0x874307, 0x836b57, 0xab6f43, 0xffb383, 0xffdbdb, 0xefefef, 0xffff8f,
			0x000000, 0x5b5b5b, 0x000047, 0x0000b3, 0x1b1b1b, 0x2f371f, 0x473323, 0x3f472b, 0x932f00, 0x2f6323, 0x875733, 0xaf4747, 0x9b633b, 0x8f7753, 0xa76b6b, 0x7f7f7f,
			0x000000, 0x5353ff, 0x00002f, 0x00009b, 0x470000, 0x1f170b, 0x2f1b0b, 0xcb0000, 0x872300, 0xe30000, 0xa73f00, 0xcb6b6b, 0xa7a7a7, 0xffc79b, 0xe7e7ff, 0xffffb3,
			0x000000, 0x00006b, 0x232b0f, 0x6b0f0f, 0x432f1b, 0x1f4317, 0x8b2323, 0xff0000, 0xb74700, 0x775f3f, 0xd75f0b, 0x838383, 0x8b8b8b, 0xf37317, 0xdb7b7b, 0x77ff6f,
			0x000000, 0x0b0b0b, 0x0000cb, 0x9b0000, 0x3f2f17, 0xd70000, 0x9f009b, 0x533f2f, 0x474f33, 0x9b5b13, 0xc34f00, 0xff3f3f, 0xff9f43, 0xffbb93, 0xffc7a7, 0xffff00,
			0x000000, 0xff5f5f, 0x0000e3, 0x530707, 0x731313, 0x1b1bff, 0x533f1f, 0x5f4323, 0x5f4b37, 0x6b6b6b, 0xdf8787, 0xdf9363, 0x6fef67, 0xf7abab, 0xd3d3d3, 0xffff73,
			0x000000, 0x170f07, 0x730000, 0x670b0b, 0x2f2f2f, 0xbf0000, 0x373f27, 0x831f1f, 0x434343, 0x37732b, 0x7b634f, 0xa36b3f, 0x5fcf57, 0xefa373, 0xdbdbdb, 0xffe74b,
			0x000000, 0xffff47, 0x131313, 0x2b230f, 0x372313, 0x4f3b27, 0x8f2b2b, 0x53573b, 0x575757, 0x775f4b, 0x6f7357, 0xcf8353, 0x9f9f9f, 0xb79f87, 0xffb37b, 0xffb7b7,
			0x000000, 0xffffff, 0x070707, 0x0b1707, 0x7f1b1b, 0x675333, 0xaf4300, 0x7b7f63, 0x8f5f37, 0x5b6347, 0x937b63, 0x5bbf4f, 0xcb7f4f, 0x67df5f, 0xf7ab7b, 0xffff6b
	);
	// @formatter:on

	private static final List<Integer> NEO_GEO_COLOR_NUMBERS = NEO_GEO_RGBS.stream().map(Color::new)
			.map(WadProcessor256ColorsNeoGeo::toNeoGeoPalette).toList();

	public WadProcessor256ColorsNeoGeo(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, Collections.emptyList(), -1, new MapProcessorDoom64KB(byteOrder, wadFile));

		List<Integer> map = new ArrayList<>();
		for (Color vgaColor : vgaColors) {
			int neoGeoColorNumber = toNeoGeoPalette(vgaColor);
			int index = NEO_GEO_COLOR_NUMBERS.indexOf(neoGeoColorNumber);
			assert index != -1;
			map.add(index);
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

	@Override
	protected void shuffleColors() {
	}

	/**
	 * @see https://wiki.neogeodev.org/index.php/Colors
	 * @param vgaColor
	 * @return
	 */
	private static int toNeoGeoPalette(Color vgaColor) {
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
		int index = 0;

		// colormap 0-31 from bright to dark
		int colormap = 0;
		for (int i = 0; i < 32; i++) {
			List<Byte> colormapBytes = createColormap(colormap);
			for (byte b : colormapBytes) {
				colormapLump.data()[index] = b;
				index++;
			}
			colormap++;
		}

		// colormap 32 invulnerability powerup
		List<Integer> grayscaleFromDarkToBright = availableColors.stream().filter(Color::isGrayish).distinct()
				.sorted(Comparator.comparing(Color::gray)).map(c -> availableColors.indexOf(c)).toList();

		List<Double> grays = availableColors.stream().map(Color::gray).distinct().sorted(Comparator.reverseOrder())
				.toList();

		List<Byte> colormapInvulnerability = availableColors.stream().mapToDouble(Color::gray).mapToInt(grays::indexOf)
				.map(i -> i / 8).map(grayscaleFromDarkToBright::get).mapToObj(NumberUtils::toByte).toList();

		for (int i = 0; i < 256; i++) {
			colormapLump.data()[index] = colormapInvulnerability.get(i);
			index++;
		}

		// colormap 33 all black
		for (int i = 0; i < 256; i++) {
			colormapLump.data()[index] = 0;
			index++;
		}

		// Playpal
		List<Integer> map = new ArrayList<>();
		for (Color vgaColor : vgaColors) {
			int neoGeoColorNumber = toNeoGeoPalette(vgaColor);
			int index2 = NEO_GEO_COLOR_NUMBERS.indexOf(neoGeoColorNumber);
			assert index2 != -1;
			map.add(index2);
		}

		Map<Integer, Integer> duplicateSlots = new HashMap<>();
		for (int i = 0; i < 256; i++) {
			if (((i % 16) != 0) && !map.contains(i)) {
				duplicateSlots.put(i, NEO_GEO_COLOR_NUMBERS.indexOf(NEO_GEO_COLOR_NUMBERS.get(i)));
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

				bbPlaypal.position(256 * 2 * palette + 0 * 16 * 2);
				short black = bbPlaypal.getShort();
				for (int i = 1; i < 16; i++) {
					bbPlaypal.position(256 * 2 * palette + i * 16 * 2);
					bbPlaypal.putShort(black);
				}

				for (Map.Entry<Integer, Integer> entry : duplicateSlots.entrySet()) {
					int duplicateSlot = entry.getKey();
					int originalSlot = entry.getValue();

					bbPlaypal.position(256 * 2 * palette + originalSlot * 2);
					short originalValue = bbPlaypal.getShort();

					bbPlaypal.position(256 * 2 * palette + duplicateSlot * 2);
					bbPlaypal.putShort(originalValue);
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
			int index = NEO_GEO_COLOR_NUMBERS.indexOf(neoGeoColor);
			assert index != -1;
			indexes.add(index);
		}

		return indexes;
	}

	@Override
	protected void changeColors() {
		// Raw graphics
		wadFile.replaceLump(getLump("/NeoGeo", "HELP2"));
		// wadFile.replaceLump(getLump("/NeoGeo", "STBAR"));
		wadFile.replaceLump(getLump("/NeoGeo", "TITLEPIC"));
		wadFile.replaceLump(getLump("/NeoGeo", "WIMAP0"));

		wadFile.replaceLump(getLump("/NeoGeo", "FLOOR4_8"));

		// Graphics in picture format
		List<Lump> spritesAndWallsGraphics = new ArrayList<>(256);
		// Sprites
		spritesAndWallsGraphics.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		// Walls
		spritesAndWallsGraphics.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));

		spritesAndWallsGraphics.forEach(this::changePaletteSpritesAndWalls);
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

	private int numtextures;

	@Override
	protected void processTexture1() {
		Lump texture1 = wadFile.getLumpByName("TEXTURE1");
		ByteBuffer oldbb = texture1.dataAsByteBuffer();
		numtextures = oldbb.getInt();
		List<Integer> oldoffsets = new ArrayList<>();
		for (int i = 0; i < numtextures; i++) {
			oldoffsets.add(oldbb.getInt());
		}

		List<Maptexture> textures = new ArrayList<>();
		for (int offset : oldoffsets) {
			oldbb.position(offset);
			byte[] name = new byte[8];
			oldbb.get(name);
			int masked = oldbb.getInt();
			short width = oldbb.getShort();
			short height = oldbb.getShort();
			int columndirectory = oldbb.getInt();
			short patchcount = oldbb.getShort();

			List<Mappatch> patches = new ArrayList<>();
			for (int i = 0; i < patchcount; i++) {
				short originx = oldbb.getShort();
				short originy = oldbb.getShort();
				short patch = oldbb.getShort();
				short stepdir = oldbb.getShort();
				short colormap = oldbb.getShort();
				patches.add(new Mappatch(originx, originy, patch, stepdir, colormap));
			}

			textures.add(new Maptexture(name, masked, width, height, columndirectory, patchcount, patches));
		}

		ByteBuffer textureHeightbb = newByteBuffer(byteOrder);

		ByteBuffer newbb = newByteBuffer(byteOrder);
		newbb.putInt(numtextures);

		// temp offset values
		for (int i = 0; i < numtextures; i++) {
			newbb.putInt(-1);
		}

		List<Integer> newoffsets = new ArrayList<>();
		for (int i = 0; i < numtextures; i++) {
			newoffsets.add(newbb.position());

			Maptexture texture = textures.get(i);
			newbb.put(texture.name());
			newbb.putShort(texture.width());
			newbb.putShort(texture.height());
			newbb.putShort(texture.patchcount());

			textureHeightbb.putShort(texture.height());

			for (Mappatch patch : texture.patches()) {
				newbb.putShort(patch.originx());
				newbb.putShort(patch.originy());
				newbb.putShort(patch.patch());
			}
		}

		int newsize = newbb.position();

		newbb.position(4);
		for (int newoffset : newoffsets) {
			newbb.putInt(newoffset);
		}

		Lump newLump = new Lump(texture1.name(), newsize, newbb);
		wadFile.replaceLump(newLump);

		Lump textureheightLump = new Lump(toByteArray("TEXHEIGH"), textureHeightbb.position(), textureHeightbb);
		wadFile.addLump(textureheightLump);
	}

	private record TexPatch(short originx, short originy, short patch_num, short patch_width) {
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

		processTexture1Again();
	}

	private void processTexture1Again() {
		ByteBuffer processedTexture1lump = newByteBuffer(byteOrder);
		for (int i = 0; i < numtextures; i++) {
			processedTexture1lump.putShort(toShort(-1));
		}

		List<Integer> newoffsetsProcessedTexture = new ArrayList<>();
		for (int i = 0; i < numtextures; i++) {
			newoffsetsProcessedTexture.add(processedTexture1lump.position());
			processedTexture1lump.put(processTexture(i));
		}

		int processedTextureSize = processedTexture1lump.position();

		processedTexture1lump.position(0);
		for (int newoffsetProcessedTexture : newoffsetsProcessedTexture) {
			processedTexture1lump.putShort(toShort(newoffsetProcessedTexture));
		}

		Lump processedTexture = new Lump(toByteArray("TEXTUREP"), processedTextureSize, processedTexture1lump);
		int pnamesNum = wadFile.getLumpNumByName("PNAMES");
		wadFile.replaceLump(pnamesNum, processedTexture);
	}

	private byte[] processTexture(int texture_num) {
		Lump pnames = wadFile.getLumpByName("PNAMES");
		ByteBuffer pnamesbb = pnames.dataAsByteBuffer();

		Lump texture1 = wadFile.getLumpByName("TEXTURE1");
		ByteBuffer bb = texture1.dataAsByteBuffer();
		bb.position(4 + texture_num * 4);
		bb.position(bb.getInt());

		byte[] name = new byte[8];
		bb.get(name);
		short width = bb.getShort();
		short height = bb.getShort();
		short patchcount = bb.getShort();

		List<Mappatch> mappatches = new ArrayList<>();
		for (int i = 0; i < patchcount; i++) {
			short originx = bb.getShort();
			short originy = bb.getShort();
			short patch = bb.getShort();
			mappatches.add(new Mappatch(originx, originy, patch, Short.MIN_VALUE, Short.MIN_VALUE));
		}

		ByteBuffer textureData = newByteBuffer(byteOrder, 2 + 2 + 2 + 1 + 1 + patchcount * (2 + 2 + 2 + 2));
		int w = 1;
		while (w * 2 <= width) {
			w <<= 1;
		}
		int widthmask = w - 1;
		textureData.putShort(toShort(widthmask));
		textureData.putShort(width);
		textureData.putShort(height);

		boolean overlapped = false;

		List<TexPatch> texpatches = new ArrayList<>();
		for (int j = 0; j < patchcount; j++) {
			byte[] pname = new byte[8];
			Mappatch mappatch = mappatches.get(j);
			pnamesbb.position(4 + mappatch.patch() * 8);
			pnamesbb.get(pname);
			int patch_num = wadFile.getLumpNumByName(toStringUpperCase(pname));
			Lump lump = wadFile.getLumpByNum(patch_num);
			ByteBuffer lumpbb = lump.dataAsByteBuffer();
			short patch_width = lumpbb.getShort();
			texpatches.add(new TexPatch(mappatch.originx(), mappatch.originy(),
					toShort(patch_num - 0xf2 - 200 + 8 + 434), patch_width));
		}

		for (int j = 0; j < patchcount; j++) {
			TexPatch texpatch = texpatches.get(j);
			short l1 = texpatch.originx();
			short r1 = toShort(l1 + texpatch.patch_width());

			for (int k = j + 1; k < patchcount; k++) {
				TexPatch p2 = texpatches.get(k);
				short l2 = p2.originx();
				short r2 = toShort(l2 + p2.patch_width());

				if (r1 > l2 && l1 < r2) {
					overlapped = true;
					break;
				}
			}
			if (overlapped) {
				break;
			}
		}

		textureData.put(toByte(overlapped ? 1 : 0));
		textureData.put(toByte(patchcount));
		for (TexPatch texpatch : texpatches) {
			textureData.putShort(texpatch.originx());
			textureData.putShort(texpatch.originy());
			textureData.putShort(texpatch.patch_num());
			textureData.putShort(texpatch.patch_width());
		}

		return textureData.array();
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DS"); // Sound Blaster sound effects
		wadFile.removeLumps("DP"); // PC speaker sound effects
	}

}
