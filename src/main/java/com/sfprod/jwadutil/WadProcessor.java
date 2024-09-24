package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sfprod.jwadutil.WadFile.Lump;

public class WadProcessor {

	// Lump order in a map WAD: each map needs a couple of lumps
	// to provide a complete scene geometry description.
	private static final int ML_LABEL = 0; // A separator, name, ExMx or MAPxx
	private static final int ML_THINGS = 1; // Monsters, items..
	private static final int ML_LINEDEFS = 2; // LineDefs, from editing
	private static final int ML_SIDEDEFS = 3; // SideDefs, from editing
	private static final int ML_VERTEXES = 4; // Vertices, edited and BSP splits generated
	private static final int ML_SEGS = 5; // LineSegs, from LineDefs split by BSP
	private static final int ML_SSECTORS = 6; // SubSectors, list of LineSegs
	private static final int ML_NODES = 7; // BSP nodes
	private static final int ML_SECTORS = 8; // Sectors, from editing
	private static final int ML_REJECT = 9; // LUT, sector-sector visibility
	private static final int ML_BLOCKMAP = 10; // LUT, motion clipping, walls/grid element

	private static final int ST_HORIZONTAL = 0;
	private static final int ST_VERTICAL = 1;
	private static final int ST_POSITIVE = 2;
	private static final int ST_NEGATIVE = 3;

	private final WadFile wadFile;

	public WadProcessor(WadFile wad) {
		this.wadFile = wad;
	}

	public void processWad() {
		removeUnusedLumps();
		processPNames();
		processDoom1Levels();
	}

	private void processDoom1Levels() {
		for (int map = 1; map <= 9; map++) {
			String mapName = "E1M" + map;
			int lumpNum = wadFile.getLumpNumByName(mapName);
			processLevel(lumpNum);
		}
	}

	private void processLevel(int lumpNum) {
		processVertexes(lumpNum);
		processLines(lumpNum);
		ProcessSegs(lumpNum);
		ProcessSides(lumpNum);
	}

	/**
	 * Convert vertex from int16_t to int32_t by shifting left 16 times.
	 *
	 * @param lumpNum
	 */
	private void processVertexes(int lumpNum) {
		int vtxLumpNum = lumpNum + ML_VERTEXES;
		Lump oldLump = wadFile.getLumpByNum(vtxLumpNum);
		byte[] oldData = oldLump.data();

		byte[] newData = new byte[oldData.length * 2];
		for (int i = 0; i < oldData.length / 2; i++) {
			newData[i * 4 + 0] = 0;
			newData[i * 4 + 1] = 0;
			newData[i * 4 + 2] = oldData[i * 2 + 0];
			newData[i * 4 + 3] = oldData[i * 2 + 1];
		}

		Lump newLump = new Lump(oldLump.name(), newData);
		wadFile.replaceLump(vtxLumpNum, newLump);
	}

	private static record Maplinedef(short v1, short v2, short flags, short special, short tag, short[] sidenum) {
	}

	private static record Vertex(int x, int y) {
	}

	private int fixedDiv(int a, int b) {
		if (Math.abs(a) >> 14 >= Math.abs(b)) {
			return ((a ^ b) >> 31) ^ Integer.MAX_VALUE;
		} else {
			return (int) ((((long) a) << 16) / b);
		}
	}

	/**
	 * Change vertexes, dx, dy, bbox[4] and slopetype
	 *
	 * @param lumpNum
	 */
	private void processLines(int lumpNum) {
		int lineLumpNum = lumpNum + ML_LINEDEFS;
		Lump lines = wadFile.getLumpByNum(lineLumpNum);

		int sizeofmaplinedef = 2 + 2 + 2 + 2 + 2 + 2 * 2;
		int sizeofline = 4 + 4 + 4 + 4 + 4 + 4 + 4 + 2 * 2 + 4 * 4 + 2 + 2 + 2 + 2;

		int lineCount = lines.data().length / sizeofmaplinedef;

		List<Maplinedef> oldLines = new ArrayList<>(lineCount);
		ByteBuffer oldLinesByteBuffer = ByteBuffer.wrap(lines.data());
		oldLinesByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < lineCount; i++) {
			short v1 = oldLinesByteBuffer.getShort();
			short v2 = oldLinesByteBuffer.getShort();
			short flags = oldLinesByteBuffer.getShort();
			short special = oldLinesByteBuffer.getShort();
			short tag = oldLinesByteBuffer.getShort();
			short[] sidenum = new short[2];
			sidenum[0] = oldLinesByteBuffer.getShort();
			sidenum[1] = oldLinesByteBuffer.getShort();
			oldLines.add(new Maplinedef(v1, v2, flags, special, tag, sidenum));
		}

		// We need vertexes for this...
		int vtxLumpNum = lumpNum + ML_VERTEXES;
		Lump vxl = wadFile.getLumpByNum(vtxLumpNum);
		List<Vertex> vtx = new ArrayList<>();
		ByteBuffer vxlByteBuffer = ByteBuffer.wrap(vxl.data());
		vxlByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < vxl.data().length / (4 + 4); i++) {
			int x = vxlByteBuffer.getInt();
			int y = vxlByteBuffer.getInt();
			vtx.add(new Vertex(x, y));
		}

		ByteBuffer newLineByteBuffer = ByteBuffer.allocate(lineCount * sizeofline);
		newLineByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < lineCount; i++) {
			Maplinedef maplinedef = oldLines.get(i);
			Vertex vertex1 = vtx.get(maplinedef.v1());
			newLineByteBuffer.putInt(vertex1.x()); // v1.x
			newLineByteBuffer.putInt(vertex1.y()); // v1.y

			Vertex vertex2 = vtx.get(maplinedef.v2());
			newLineByteBuffer.putInt(vertex2.x()); // v2.x
			newLineByteBuffer.putInt(vertex2.y()); // v2.y

			newLineByteBuffer.putInt(i); // lineno

			int dx = vertex2.x() - vertex1.x();
			int dy = vertex2.y() - vertex1.y();
			newLineByteBuffer.putInt(dx); // dx
			newLineByteBuffer.putInt(dy); // dy

			newLineByteBuffer.putShort(maplinedef.sidenum()[0]); // sidenum[0];
			newLineByteBuffer.putShort(maplinedef.sidenum()[1]); // sidenum[1];

			newLineByteBuffer.putInt(vertex1.y() < vertex2.y() ? vertex2.y() : vertex1.y()); // bbox[BOXTOP]
			newLineByteBuffer.putInt(vertex1.y() < vertex2.y() ? vertex1.y() : vertex2.y()); // bbox[BOXBOTTOM]
			newLineByteBuffer.putInt(vertex1.x() < vertex2.x() ? vertex1.x() : vertex2.x()); // bbox[BOXLEFT]
			newLineByteBuffer.putInt(vertex1.x() < vertex2.x() ? vertex2.x() : vertex1.x()); // bbox[BOXRIGHT]

			newLineByteBuffer.putShort(maplinedef.flags()); // flags
			newLineByteBuffer.putShort(maplinedef.special()); // special
			newLineByteBuffer.putShort(maplinedef.tag()); // tag

			short slopetype;
			if (dx == 0) {
				slopetype = ST_VERTICAL;
			} else if (dy == 0) {
				slopetype = ST_HORIZONTAL;
			} else {
				if (fixedDiv(dy, dx) > 0) {
					slopetype = ST_POSITIVE;
				} else {
					slopetype = ST_NEGATIVE;
				}
			}

			newLineByteBuffer.putShort(slopetype); // slopetype
		}

		Lump newLine = new Lump(lines.name(), newLineByteBuffer.array());
		wadFile.replaceLump(lineLumpNum, newLine);
	}

	private void ProcessSegs(int lumpNum) {
		int segsLumpNum = lumpNum + ML_SEGS;
		Lump segs = wadFile.getLumpByNum(segsLumpNum);

//    int segCount = segs.length / sizeof(mapseg_t);

//    seg_t* newSegs = new seg_t[segCount];

//     mapseg_t* oldSegs = segs.data.constData();

		// We need vertexes for this...
		int vtxLumpNum = lumpNum + ML_VERTEXES;
		Lump vxl = wadFile.getLumpByNum(vtxLumpNum);

//     vertex_t* vtx = vxl.data.constData();

		// And LineDefs. Must process lines first.
		int linesLumpNum = lumpNum + ML_LINEDEFS;
		Lump lxl = wadFile.getLumpByNum(linesLumpNum);

//     line_t* lines = lxl.data.constData();

		// And sides too...
		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		Lump sxl = wadFile.getLumpByNum(sidesLumpNum);

//     mapsidedef_t* sides = sxl.data.constData();

		// ****************************

//    for(unsigned int i = 0; i < segCount; i++)
//    {
//        newSegs[i].v1.x = vtx[oldSegs[i].v1].x;
//        newSegs[i].v1.y = vtx[oldSegs[i].v1].y;

//        newSegs[i].v2.x = vtx[oldSegs[i].v2].x;
//        newSegs[i].v2.y = vtx[oldSegs[i].v2].y;

//        newSegs[i].angle = oldSegs[i].angle << 16;
//        newSegs[i].offset = oldSegs[i].offset << 16;

//        newSegs[i].linenum = oldSegs[i].linedef;

//         line_t* ldef = &lines[newSegs[i].linenum];

//        int side = oldSegs[i].side;

//        newSegs[i].sidenum = ldef.sidenum[side];

//        if(newSegs[i].sidenum != NO_INDEX)
//        {
//            newSegs[i].frontsectornum = sides[newSegs[i].sidenum].sector;
//        }
//        else
//        {
//            newSegs[i].frontsectornum = NO_INDEX;
//        }

//        newSegs[i].backsectornum = NO_INDEX;

//        if(ldef.flags & ML_TWOSIDED)
//        {
//            if(ldef.sidenum[side^1] != NO_INDEX)
//            {
//                newSegs[i].backsectornum = sides[ldef.sidenum[side^1]].sector;
//            }
//        }
//    }

		Lump newSeg;
//		newSeg.name = segs.name;
//    newSeg.length = segCount * sizeof(seg_t);
//    newSeg.data = QByteArray(newSegs, newSeg.length);

//    delete[] newSegs;

//		wadFile.ReplaceLump(segsLumpNum, newSeg);
	}

	private void ProcessSides(int lumpNum) {
		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		Lump sides = wadFile.getLumpByNum(sidesLumpNum);

//    int sideCount = sides.length / sizeof(mapsidedef_t);

//    sidedef_t* newSides = new sidedef_t[sideCount];

//     mapsidedef_t* oldSides = sides.data.constData();

//    for(unsigned int i = 0; i < sideCount; i++)
//    {
//        newSides[i].textureoffset = oldSides[i].textureoffset;
//        newSides[i].rowoffset = oldSides[i].rowoffset;

//        newSides[i].toptexture = GetTextureNumForName(oldSides[i].toptexture);
//        newSides[i].bottomtexture = GetTextureNumForName(oldSides[i].bottomtexture);
//        newSides[i].midtexture = GetTextureNumForName(oldSides[i].midtexture);

//        newSides[i].sector = oldSides[i].sector;
//    }

		Lump newSide;
//		newSide.name = sides.name;
//    newSide.length = sideCount * sizeof(sidedef_t);
//    newSide.data = QByteArray(newSides, newSide.length);

//    delete[] newSides;

//		wadFile.ReplaceLump(sidesLumpNum, newSide);
	}

	private int GetTextureNumForName(char tex_name) {
//     int  *maptex1;
//    int  numtextures1, numtextures2 = 0;
//     int *directory1;

		// Convert name to uppercase for comparison.
//    char tex_name_upper[9];

//		strncpy(tex_name_upper, tex_name, 8);
//		tex_name_upper[8] = 0; // Ensure null terminated.

//		for (int i = 0; i < 8; i++) {
//			tex_name_upper[i] = toupper(tex_name_upper[i]);
//		}

		Lump tex1lump = wadFile.getLumpByName("TEXTURE1");

//    maptex1 = (int*)tex1lump.data.constData();
//    numtextures1 = *maptex1;
//		directory1 = maptex1 + 1;

//     int *directory = directory1;
//     int *maptex = maptex1;

//		int numtextures = (numtextures1 + numtextures2);

//		for (int i = 0; i < numtextures; i++, directory++) {
//			if (i == numtextures1) {

//			}

//        int offset = *directory;

//         maptexture_t* mtexture = maptex + offset;

//			if (!strncmp(tex_name_upper, mtexture.name, 8)) {
//				return i;
//			}
//		}

		return 0;
	}

	/**
	 * Capitalize patch names
	 */
	private void processPNames() {
		Lump lump = wadFile.getLumpByName("PNAMES");
		byte[] data = lump.data();
		for (int i = 4; i < data.length; i++) {
			if ('a' <= data[i] && data[i] <= 'z') {
				data[i] &= 0b11011111;
			}
		}
	}

	/**
	 * Remove unused lumps
	 *
	 * <table>
	 * <tr>
	 * <th>prefix</th>
	 * <th>description</th>
	 * </tr>
	 * <tr>
	 * <td><b>D_</b></b></td>
	 * <td>MUS music</td>
	 * </tr>
	 * <tr>
	 * <td><b>DP</b></td>
	 * <td>PC speaker sound effects</td>
	 * </tr>
	 * <tr>
	 * <td><b>DS</b></td>
	 * <td>Sound Blaster sound effects</td>
	 * </tr>
	 * <tr>
	 * <td><b>GENMIDI</b></td>
	 * <td>Lump that contains instrument data for the DMX sound library to use for
	 * OPL synthesis</td>
	 * </tr>
	 * </table>
	 */
	private void removeUnusedLumps() {
		wadFile.removeLumps("D_");
		wadFile.removeLumps("DP");
		wadFile.removeLumps("DS");
		wadFile.removeLumps("GENMIDI");
	}

}
