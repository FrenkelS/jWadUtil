package com.sfprod.jwadutil;

import java.nio.ByteOrder;

class WadProcessor2ColorsSinclairQL extends WadProcessor4Colors {

	WadProcessor2ColorsSinclairQL(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	@Override
	protected Lump processPcSpeakerSoundEffect(Lump vanillaLump) {
		return SinclairQLUtil.processPcSpeakerSoundEffect(vanillaLump, byteOrder);
	}
}
