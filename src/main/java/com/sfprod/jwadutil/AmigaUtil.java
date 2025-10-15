package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

interface AmigaUtil {

	static Lump processPcSpeakerSoundEffect(List<Lump> vanillaDigitalSoundEffects, ByteOrder byteOrder,
			Lump vanillaLump) {
		String pcName = vanillaLump.nameAsString();
		String dsName = "DS" + pcName.substring(2);

		Lump vanillaDigitalSoundlump = vanillaDigitalSoundEffects.stream().filter(l -> l.nameAsString().equals(dsName))
				.findAny().orElseThrow();

		ByteBuffer vanillaData = vanillaDigitalSoundlump.dataAsByteBuffer();
		vanillaData.getShort(); // Format number (must be 3)
		vanillaData.getShort(); // Sample rate (usually, but not necessarily, 11025)
		int length = vanillaData.getInt() - 32; // Number of samples + 32 pad bytes

		for (int i = 0; i < 16; i++) {
			vanillaData.get();
		}

		ByteBuffer doom8088Data = newByteBuffer(byteOrder);
		doom8088Data.putShort(toShort(length));

		for (int i = 0; i < length; i++) {
			byte b = vanillaData.get();
			int x = toInt(b) - 128;
			if (x < 0) {
				x = 0;
			}
			doom8088Data.put(toByte(x));
		}

		return new Lump(vanillaLump.name(), 2 + length, doom8088Data);
	}
}
