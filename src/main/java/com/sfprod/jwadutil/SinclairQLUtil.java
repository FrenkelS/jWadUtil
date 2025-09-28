package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface SinclairQLUtil {

	static private byte find(int frequencyAverage) {
		final int[] FREQUENCIES = { 1313, 1177, 1066, 974, 897, 831, 775, 725, 682, 643, 608, 577, 549, 524, 501, 480,
				460, 442, 426, 410, 396, 383, 370, 358, 347, 337, 327, 318, 309, 301, 293, 286, 279, 272, 266, 260, 254,
				248, 243, 238, 233, 228, 224, 220, 215, 211, 208, 203 };

		int r = 0;
		while (true) {
			if (frequencyAverage == FREQUENCIES[r]) {
				return toByte(r);
			} else if (frequencyAverage < FREQUENCIES[r]) {
				r++;
			} else {
				break;
			}
		}

		int h = FREQUENCIES[r - 1];
		int l = FREQUENCIES[r];

		if (h - frequencyAverage < frequencyAverage - l) {
			r--;
		}
		return toByte(r);
	}

	static Lump processPcSpeakerSoundEffect(Lump vanillaLump, ByteOrder byteOrder) {
		ByteBuffer vanillaData = vanillaLump.dataAsByteBuffer();
		vanillaData.getShort(); // type, 0 = PC Speaker
		short length = vanillaData.getShort();

		int frequencySum = 0;
		int frequencyLength = 0;
		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			short d = WadProcessor.DIVISORS[toInt(b)];
			short frequency = d == 0 ? 0 : (short) (1193181 / d);
			frequencySum += frequency;
			frequencyLength += frequency == 0 ? 0 : 1;
		}

		int frequencyAverage = frequencySum / frequencyLength;

		ByteBuffer doom8088Data = newByteBuffer(byteOrder);
		doom8088Data.putShort(toShort(length * 128)); // dur
		doom8088Data.put(find(frequencyAverage)); // pitch
		doom8088Data.put(toByte(0)); // pitch2
		doom8088Data.put(toByte(0)); // wrap
		doom8088Data.putShort(toShort(0));// g_x;
		doom8088Data.put(toByte(0)); // g_y
		doom8088Data.put(toByte(0)); // fuzz
		doom8088Data.put(toByte(0)); // rndm

		return new Lump(vanillaLump.name(), 10, doom8088Data);
	}
}
