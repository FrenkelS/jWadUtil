package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

class WadProcessor2ColorsMacintosh extends WadProcessor4Colors {

	private final List<Lump> vanillaDigitalSoundEffects;

	WadProcessor2ColorsMacintosh(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, true);
		this.vanillaDigitalSoundEffects = wadFile.getLumpsByName("DS");
	}

	@Override
	protected Lump processPcSpeakerSoundEffect(Lump vanillaLump) {
		String pcName = vanillaLump.nameAsString();
		String dsName = "DS" + pcName.substring(2);

		Lump vanillaDigitalSoundlump = vanillaDigitalSoundEffects.stream().filter(l -> l.nameAsString().equals(dsName))
				.findAny().orElseThrow();

		ByteBuffer vanillaData = vanillaDigitalSoundlump.dataAsByteBuffer();
		vanillaData.getShort(); // Format number (must be 3)
		vanillaData.getShort(); // Sample rate (usually, but not necessarily, 11025)
		int length = vanillaData.getInt() - 32; // Number of samples + 32 pad bytes

		byte[] tmpBuffer = new byte[16];
		vanillaData.get(tmpBuffer);

		ByteBuffer doom8088Data = newByteBuffer(byteOrder);
		doom8088Data.putShort(toShort(length));

		byte[] buffer = new byte[length];
		vanillaData.get(buffer);
		doom8088Data.put(buffer);

		return new Lump(vanillaLump.name(), 2 + length, doom8088Data);
	}
}
