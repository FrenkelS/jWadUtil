package com.sfprod.jwadutil;

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
		return AmigaUtil.processPcSpeakerSoundEffect(vanillaDigitalSoundEffects, byteOrder, vanillaLump);
	}
}
