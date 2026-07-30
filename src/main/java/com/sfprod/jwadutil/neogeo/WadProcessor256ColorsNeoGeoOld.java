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
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor;

public class WadProcessor256ColorsNeoGeoOld extends WadProcessor {

	private static final List<String> SPRNAMES = List.of( //
			"TROO", "SHTG", "PUNG", "PISG", "PISF", "SHTF", "CHGG", "CHGF", "MISG", //
			"MISF", "SAWG", "BLUD", "PUFF", "BAL1", //
			"MISL", "TFOG", "PLAY", "POSS", //
			"SPOS", "SARG", //
			"BAL7", "BOSS", //
			"ARM1", "ARM2", "BAR1", "BEXP", //
			"BON1", "BON2", "BKEY", "RKEY", "YKEY", "STIM", "MEDI", //
			"SOUL", "PINS", "SUIT", "PMAP", "PVIS", "CLIP", "AMMO", //
			"ROCK", "BROK", "SHEL", "SBOX", "BPAK", "MGUN", "CSAW", //
			"LAUN", "SHOT", "COLU", "POL5", //
			"CAND", "CBRA", "ELEC", //
			"TRED");

	private int numtextures;

	public WadProcessor256ColorsNeoGeoOld(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, new MapProcessorDoom64KB(byteOrder, wadFile));
	}

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
	protected void storeMapsInSeparateWad() {
		WadFile maps = new WadFile();
		for (int mapNumber = 1; mapNumber <= 9; mapNumber++) {
			int lumpNumE1Mx = wadFile.getLumpNumByName("E1M" + mapNumber);
			for (int i = 0; i < 10; i++) {
				maps.addLump(wadFile.getLumpByNum(lumpNumE1Mx + i));
			}
			for (int i = 10 - 1; i >= 0; i--) {
				wadFile.removeLump(lumpNumE1Mx + i);
			}
		}
		String wadPath = byteOrder == ByteOrder.LITTLE_ENDIAN ? "DOOMMAPL.WAD" : "DOOMMAPB.WAD";
		maps.saveWadFile(byteOrder, wadPath);

		processTexture1Again();
		processSprites();
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

	private void processSprites() {
		List<Lump> spriteLumps = wadFile.getLumpsBetween("S_START", "S_END");

		int spriteframesize = 8 * 2 + 1 + 1;
		ByteBuffer bb = newByteBuffer(byteOrder, SPRNAMES.size() * 23 * spriteframesize);
		for (int i = 0; i < SPRNAMES.size(); i++) {
			String sprname = SPRNAMES.get(i);
			List<Lump> lumps = spriteLumps.stream().filter(l -> l.nameAsString().startsWith(sprname)).toList();

			for (Lump lump : lumps) {
				String suffix = lump.nameAsString().substring(4);
				int frame = suffix.charAt(0) - 'A';
				int rotation = suffix.charAt(1) - '0';

				short lumpNum = toShort(wadFile.getLumpNumByName(lump.nameAsString()));
				if (rotation == 0) {
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + rotation * 2);
					for (int j = 0; j < 8; j++) {
						bb.putShort(lumpNum);
					}
				} else {
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + (rotation - 1) * 2);
					bb.putShort(lumpNum);
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + 8 * 2 + 1);
					bb.put(toByte(1));
				}

				if (suffix.length() == 4) {
					assert suffix.charAt(0) == suffix.charAt(2);
					rotation = suffix.charAt(3) - '0';
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + (rotation - 1) * 2);
					bb.putShort(lumpNum);
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + 8 * 2);

					byte flipmask = bb.get();
					bb.position(i * 23 * spriteframesize + frame * spriteframesize + 8 * 2);
					bb.put(toByte(toInt(flipmask) | (1 << (rotation - 1))));
				}
			}
		}

		wadFile.addLump(new Lump("SPRITES", bb.array(), byteOrder));
	}

	@Override
	protected void changeColors() {
	}

	@Override
	protected void processColormap() {
	}

	@Override
	protected void shuffleColors() {
	}

	@Override
	protected void processRawGraphics() {
	}
}
