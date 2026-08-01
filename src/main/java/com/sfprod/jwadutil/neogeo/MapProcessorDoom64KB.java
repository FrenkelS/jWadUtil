package com.sfprod.jwadutil.neogeo;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.MapProcessor;
import com.sfprod.jwadutil.WadFile;

class MapProcessorDoom64KB extends MapProcessor {

	MapProcessorDoom64KB(ByteOrder byteOrder, WadFile wadFile) {
		super(byteOrder, wadFile);
	}

	@Override
	protected void processLinedefs2(int lumpNum) {
		int lineLumpNum = lumpNum + ML_LINEDEFS;
		Lump lines = wadFile.getLumpByNum(lineLumpNum);

		int lineCount = lines.length() / Line.SIZE_OF_LINE;

		ByteBuffer oldLinesByteBuffer = lines.dataAsByteBuffer();
		ByteBuffer newLineByteBuffer = newByteBuffer(byteOrder,
				lineCount * (Line.SIZE_OF_LINE + 2 + 2 + 2 + 4 * 2 + 1 + 2));

		for (short lineno = 0; lineno < lineCount; lineno++) {
			short v1x = oldLinesByteBuffer.getShort();
			short v1y = oldLinesByteBuffer.getShort();
			short v2x = oldLinesByteBuffer.getShort();
			short v2y = oldLinesByteBuffer.getShort();
			newLineByteBuffer.putShort(v1x);
			newLineByteBuffer.putShort(v1y);
			newLineByteBuffer.putShort(v2x);
			newLineByteBuffer.putShort(v2y);

			newLineByteBuffer.putShort(lineno);

			short dx = toShort(v2x - v1x);
			short dy = toShort(v2y - v1y);
			newLineByteBuffer.putShort(dx); // dx
			newLineByteBuffer.putShort(dy); // dx

			newLineByteBuffer.putShort(oldLinesByteBuffer.getShort()); // frontsidenum
			newLineByteBuffer.putShort(oldLinesByteBuffer.getShort()); // backsidenum

			newLineByteBuffer.putShort(v1y < v2y ? v2y : v1y); // bbox BOXTOP
			newLineByteBuffer.putShort(v1y < v2y ? v1y : v2y); // bbox BOXBOTTOM
			newLineByteBuffer.putShort(v1x < v2x ? v1x : v2x); // bbox BOXLEFT
			newLineByteBuffer.putShort(v1x < v2x ? v2x : v1x); // bbox BOXRIGHT

			byte flags = oldLinesByteBuffer.get();
			byte const_special = oldLinesByteBuffer.get();
			byte tag = oldLinesByteBuffer.get();

			byte slopetype;
			if (dx == 0) {
				slopetype = 1;
			} else if (dy == 0) {
				slopetype = 0;
			} else if ((dy ^ dx) >= 0) {
				slopetype = 2;
			} else {
				slopetype = 3;
			}

			newLineByteBuffer.putShort(toShort(tag));
			newLineByteBuffer.put(flags);
			newLineByteBuffer.put(slopetype);
			newLineByteBuffer.putShort(toShort(const_special));
		}

		Lump newLine = new Lump(lines.name(), newLineByteBuffer);
		wadFile.replaceLump(lineLumpNum, newLine);
	}

	@Override
	protected void processSsectors(int lumpNum) {
		int ssectorsLumpNum = lumpNum + ML_SSECTORS;
		Lump lump = wadFile.getLumpByNum(ssectorsLumpNum);

		if (!lump.byteOrder().equals(byteOrder)) {
			for (int i = 0; i < lump.length() / 2; i++) {
				byte e = lump.data()[i * 2 + 0];
				byte o = lump.data()[i * 2 + 1];
				lump.data()[i * 2 + 1] = e;
				lump.data()[i * 2 + 0] = o;
			}
		}
	}

}
