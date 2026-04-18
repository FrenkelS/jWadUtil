package com.sfprod.jwadutil.macintosh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor2Colors;
import com.sfprod.utils.ByteBufferUtils;

public class WadProcessor2ColorsMacintosh extends WadProcessor2Colors {

	public WadProcessor2ColorsMacintosh(String title, ByteOrder byteOrder, WadFile wadFile) {
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

		byte[] buffer = new byte[length];
		vanillaData.get(buffer);

		return new Lump(vanillaDigitalSoundlump.name(), buffer, ByteBufferUtils.DONT_CARE);
	}
}
