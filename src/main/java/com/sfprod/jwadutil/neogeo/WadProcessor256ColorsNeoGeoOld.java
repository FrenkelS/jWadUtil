package com.sfprod.jwadutil.neogeo;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.WadFile;
import com.sfprod.jwadutil.WadProcessor;

public class WadProcessor256ColorsNeoGeoOld extends WadProcessor {

	public WadProcessor256ColorsNeoGeoOld(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile, new MapProcessorDoom64KB(byteOrder, wadFile));
	}

	@Override
	protected void duplicateMaps() {
		int lumpNumE1M1 = wadFile.getLumpNumByName("E1M1");

		List<Lump> e1m1Lumps = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			e1m1Lumps.add(wadFile.getLumpByNum(lumpNumE1M1 + 1 + i));
		}

		List<Integer> mapNumbers = List.of(2, 3, 4, 5, 6, 7, 9);
		for (int mapNumber : mapNumbers) {
			int lumpNumE1M2 = wadFile.getLumpNumByName("E1M" + mapNumber);
			for (int i = 0; i < 9; i++) {
				wadFile.replaceLump(lumpNumE1M2 + 1 + i, e1m1Lumps.get(i));
			}	
		}		
	}

	@Override
	protected void changeColors() {
	}

	@Override
	protected void processColormap() {
	}

	@Override
	protected void shuffleColors() {
	}

	@Override
	protected void processRawGraphics() {
	}
}
