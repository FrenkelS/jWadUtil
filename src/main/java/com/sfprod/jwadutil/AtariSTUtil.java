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
		List<Lump> oldDpLumps = wadFile.getLumpsByName("DP");
		List<Lump> newDpLumps = oldDpLumps.stream().map(AtariSTUtil::processPcSpeakerSoundEffect).toList();

		List<Lump> oldDsLumps = wadFile.getLumpsByName("DS");
		List<Lump> newDsLumps = oldDsLumps.stream().map(AtariSTUtil::processDigitalSoundEffect).toList();

		int lumpnum = wadFile.getLumpNumByName(newDpLumps.getFirst().nameAsString());
		for (Lump newDpLump : newDpLumps) {
			wadFile.replaceLump(lumpnum, newDpLump);
			lumpnum++;
		}
		for (Lump newDsLump : newDsLumps) {
			wadFile.replaceLump(lumpnum, newDsLump);
			lumpnum++;
		}
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

	private static Lump processDigitalSoundEffect(Lump vanillaDigitalSoundlump) {
		ByteBuffer vanillaData = vanillaDigitalSoundlump.dataAsByteBuffer();
		vanillaData.getShort(); // Format number (must be 3)
		vanillaData.getShort(); // Sample rate (usually, but not necessarily, 11025)
		int length = vanillaData.getInt() - 32; // Number of samples + 32 pad bytes

		byte[] tmpBuffer = new byte[16];
		vanillaData.get(tmpBuffer);

		// from unsigned 8-bit to signed 8-bit
		byte[] signedBytes = new byte[length];
		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			int x = toInt(b) - 128;
			if (x < 0) {
				x = 0;
			}
			signedBytes[i] = toByte(x);
		}

		// from 11025 Hz to 12157 Hz
		byte[] resampledBytes = resample(signedBytes);

		ByteBuffer doom8088Data = newByteBuffer(ByteOrder.BIG_ENDIAN);
		doom8088Data.putShort(toShort(resampledBytes.length));
		doom8088Data.put(resampledBytes);
		return new Lump(vanillaDigitalSoundlump.name(), 2 + resampledBytes.length, doom8088Data);
	}

	private static byte[] resample(byte[] input) {
		int newLength = (int) (input.length * 12157.0 / 11025.0);
		byte[] output = new byte[newLength];

		for (int i = 0; i < newLength; i++) {
			double srcIndex = i * 11025.0 / 12157.0;
			int index = (int) srcIndex;
			double frac = srcIndex - index;

			int sample1 = input[Math.min(index, input.length - 1)];
			int sample2 = input[Math.min(index + 1, input.length - 1)];

			double sample = (1 - frac) * sample1 + frac * sample2;
			output[i] = toByte(sample);
		}

		return output;
	}
}
