package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

interface AtariSTUtil {

	static void processSoundEffects(WadFile wadFile) {
		List<Lump> dpLumps = wadFile.getLumpsByName("DP");
		dpLumps.stream().map(AtariSTUtil::processPcSpeakerSoundEffect).forEach(wadFile::replaceLump);

		wadFile.removeLumps("DS");
		// List<Lump> dsLumps = wadFile.getLumpsByName("DS");
		// dsLumps.stream().map(AtariSTUtil::processSoundEffect).forEach(wadFile::replaceLump);
	}

	private static Lump processPcSpeakerSoundEffect(Lump vanillaLump) {
		ByteBuffer vanillaData = vanillaLump.dataAsByteBuffer();
		vanillaData.getShort(); // type, 0 = PC Speaker
		short length = vanillaData.getShort();

		ByteBuffer doom8088Data = newByteBuffer(ByteOrder.BIG_ENDIAN);
		doom8088Data.putShort(length);

		short[] divisors = getDivisors();
		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			short d = divisors[toInt(b)];
			doom8088Data.putShort(d);
		}

		return new Lump(vanillaLump.name(), 2 + length * 2, doom8088Data);
	}

	private static short[] getDivisors() {
		short[] newDivs = new short[128];
		newDivs[0] = 0;
		for (int i = 1; i < 128; i++) {
			short od = WadProcessor.DIVISORS[i];
			short frequency = (short) (1193181 / od);
			short nd = (short) ((2_000_000 / 16) / frequency);
			newDivs[i] = nd;
		}
		return newDivs;
	}

	private static Lump processSoundEffect(Lump vanillaDigitalSoundlump) {
		ByteBuffer vanillaData = vanillaDigitalSoundlump.dataAsByteBuffer();
		vanillaData.getShort(); // Format number (must be 3)
		vanillaData.getShort(); // Sample rate (usually, but not necessarily, 11025)
		int length = vanillaData.getInt() - 32; // Number of samples + 32 pad bytes

		byte[] tmpBuffer = new byte[16];
		vanillaData.get(tmpBuffer);

		ByteBuffer doom8088Data = newByteBuffer(ByteOrder.BIG_ENDIAN);
		doom8088Data.putShort(toShort(length));

		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			int x = toInt(b) - 128;
			if (x < 0) {
				x = 0;
			}
			doom8088Data.put(toByte(x));
		}

		return new Lump(vanillaDigitalSoundlump.name(), 2 + length, doom8088Data);
	}
}
