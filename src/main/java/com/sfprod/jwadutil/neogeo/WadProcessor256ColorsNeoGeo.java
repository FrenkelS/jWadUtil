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

	private final List<Integer> neoGeoColorNumbers;

	public WadProcessor256ColorsNeoGeo(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, GRAYSCALE_FROM_DARK_TO_BRIGHT, 16,
				new MapProcessorDoom64KB(byteOrder, wadFile));

		this.neoGeoColorNumbers = createNeoGeoColorNumbers();

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
		Color[] neoGeoColorArray = new Color[256];
		for (int i = 0; i < 256; i++) {
			neoGeoColorArray[map.get(i)] = vgaColors.get(i);
		}
		for (int emptySlot : emptySlots) {
			neoGeoColorArray[emptySlot] = new Color(0, 0, 0);
		}
		fillAvailableColorsShuffleMap(Arrays.asList(neoGeoColorArray));
	}

	private List<Integer> createNeoGeoColorNumbers() {
		List<Integer> uniqueNeoGeoColorNumberList = new ArrayList<>(
				vgaColors.stream().map(this::toNeoGeoPalette).collect(Collectors.toCollection(LinkedHashSet::new)));

		List<Integer> neoGeoColorNumberList = new ArrayList<>();
		int index = 0;
		for (int i = 0; i < 16; i++) {
			neoGeoColorNumberList.add(uniqueNeoGeoColorNumberList.get(index));
			index++;
		}

		for (int j = 0; j < 16; j++) {
			neoGeoColorNumberList.add(0x8000);
			for (int i = 0; i < 15; i++) {
				if (index == 236) {
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					neoGeoColorNumberList.add(0x8000);
					assert neoGeoColorNumberList.size() == 256;
					return neoGeoColorNumberList;
				}
				neoGeoColorNumberList.add(uniqueNeoGeoColorNumberList.get(index));
				index++;
			}
		}

		throw new IllegalStateException();
	}

	@Override
	protected void shuffleColors() {
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
