package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

class WadProcessor2ColorsMacintosh extends WadProcessor4Colors {

	WadProcessor2ColorsMacintosh(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, true);
	}

	@Override
	protected void processSoundEffects() {
		wadFile.removeLumps("DP"); // PC speaker sound effects

		List<Lump> lumps = wadFile.getLumpsByName("DS");
		lumps.stream().map(this::processSoundEffect).forEach(wadFile::replaceLump);
	}

	private Lump processSoundEffect(Lump vanillaDigitalSoundlump) {
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

		return new Lump(vanillaDigitalSoundlump.name(), 2 + length, doom8088Data);
	}
}
