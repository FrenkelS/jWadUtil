package com.sfprod.jwadutil;

import java.nio.ByteOrder;
import java.util.List;

class WadProcessor2ColorsAmiga extends WadProcessor4Colors {

	private final List<Lump> vanillaDigitalSoundEffects;

	WadProcessor2ColorsAmiga(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
		this.vanillaDigitalSoundEffects = wadFile.getLumpsByName("DS");
	}

	@Override
	protected Lump processPcSpeakerSoundEffect(Lump vanillaLump) {
		return AmigaUtil.processPcSpeakerSoundEffect(vanillaDigitalSoundEffects, byteOrder, vanillaLump);
	}
}
