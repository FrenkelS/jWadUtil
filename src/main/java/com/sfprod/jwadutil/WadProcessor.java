package com.sfprod.jwadutil;

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
		ProcessLines(lumpNum);
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

	private void ProcessLines(int lumpNum) {
		int lineLumpNum = lumpNum + ML_LINEDEFS;

		Lump lines;

//		if (!wadFile.GetLumpByNum(lineLumpNum, lines))
//			return;

//		if (lines.length == 0)
//			return;

//    int lineCount = lines.length / sizeof(maplinedef_t);

//    line_t* newLines = new line_t[lineCount];

//     maplinedef_t* oldLines = lines.data.constData();

		// We need vertexes for this...

		int vtxLumpNum = lumpNum + ML_VERTEXES;

		Lump vxl;

//		if (!wadFile.GetLumpByNum(vtxLumpNum, vxl))
//			return;

//		if (vxl.length == 0)
//			return;

//     vertex_t* vtx = vxl.data.constData();

//    for( int i = 0; i < lineCount; i++)
//    {
//        newLines[i].v1.x = vtx[oldLines[i].v1].x;
//        newLines[i].v1.y = vtx[oldLines[i].v1].y;

//        newLines[i].v2.x = vtx[oldLines[i].v2].x;
//        newLines[i].v2.y = vtx[oldLines[i].v2].y;

//        newLines[i].special = oldLines[i].special;
//        newLines[i].flags = oldLines[i].flags;
//        newLines[i].tag = oldLines[i].tag;

//        newLines[i].dx = newLines[i].v2.x - newLines[i].v1.x;
//        newLines[i].dy = newLines[i].v2.y - newLines[i].v1.y;

//        newLines[i].slopetype =
//                !newLines[i].dx ? ST_VERTICAL : !newLines[i].dy ? ST_HORIZONTAL :
//                FixedDiv(newLines[i].dy, newLines[i].dx) > 0 ? ST_POSITIVE : ST_NEGATIVE;

//        newLines[i].sidenum[0] = oldLines[i].sidenum[0];
//        newLines[i].sidenum[1] = oldLines[i].sidenum[1];

//        newLines[i].bbox[BOXLEFT] = (newLines[i].v1.x < newLines[i].v2.x ? newLines[i].v1.x : newLines[i].v2.x);
//        newLines[i].bbox[BOXRIGHT] = (newLines[i].v1.x < newLines[i].v2.x ? newLines[i].v2.x : newLines[i].v1.x);

//        newLines[i].bbox[BOXTOP] = (newLines[i].v1.y < newLines[i].v2.y ? newLines[i].v2.y : newLines[i].v1.y);
//        newLines[i].bbox[BOXBOTTOM] = (newLines[i].v1.y < newLines[i].v2.y ? newLines[i].v1.y : newLines[i].v2.y);

//        newLines[i].lineno = i;

//    }

		Lump newLine;
//		newLine.name = lines.name;
//    newLine.length = lineCount * sizeof(line_t);
//    newLine.data = QByteArray(newLines, newLine.length);

//    delete[] newLines;

//		wadFile.ReplaceLump(lineLumpNum, newLine);
	}

	private void ProcessSegs(int lumpNum) {
		int segsLumpNum = lumpNum + ML_SEGS;

		Lump segs;

//		if (!wadFile.GetLumpByNum(segsLumpNum, segs))
//			return;

//		if (segs.length == 0)
//			return;

//    int segCount = segs.length / sizeof(mapseg_t);

//    seg_t* newSegs = new seg_t[segCount];

//     mapseg_t* oldSegs = segs.data.constData();

		// We need vertexes for this...

		int vtxLumpNum = lumpNum + ML_VERTEXES;

		Lump vxl;

//		if (!wadFile.GetLumpByNum(vtxLumpNum, vxl))
//			return;

//		if (vxl.length == 0)
//			return;

//     vertex_t* vtx = vxl.data.constData();

		// And LineDefs. Must process lines first.

		int linesLumpNum = lumpNum + ML_LINEDEFS;

		Lump lxl;

//		if (!wadFile.GetLumpByNum(linesLumpNum, lxl))
//			return;

//		if (lxl.length == 0)
//			return;

//     line_t* lines = lxl.data.constData();

		// And sides too...

		int sidesLumpNum = lumpNum + ML_SIDEDEFS;

		Lump sxl;

//		if (!wadFile.GetLumpByNum(sidesLumpNum, sxl))
//			return;

//		if (sxl.length == 0)
//			return;

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

		Lump sides;

//		if (!wadFile.GetLumpByNum(sidesLumpNum, sides))
//			return;

//		if (sides.length == 0)
//			return;

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
//     int  *maptex1, *maptex2;
//    int  numtextures1, numtextures2 = 0;
//     int *directory1, *directory2;

		// Convert name to uppercase for comparison.
//    char tex_name_upper[9];

//		strncpy(tex_name_upper, tex_name, 8);
//		tex_name_upper[8] = 0; // Ensure null terminated.

//		for (int i = 0; i < 8; i++) {
//			tex_name_upper[i] = toupper(tex_name_upper[i]);
//		}

		Lump tex1lump;
//		wadFile.GetLumpByName("TEXTURE1", tex1lump);

//    maptex1 = (int*)tex1lump.data.constData();
//    numtextures1 = *maptex1;
//		directory1 = maptex1 + 1;

		Lump tex2lump;
//		if (wadFile.GetLumpByName("TEXTURE2", tex2lump) != -1) {
//        maptex2 = (int*)tex2lump.data.constData();
//			directory2 = maptex2 + 1;
//        numtextures2 = *maptex2;
//		} else {
//			maptex2 = NULL;
//			directory2 = NULL;
//		}

//     int *directory = directory1;
//     int *maptex = maptex1;

//		int numtextures = (numtextures1 + numtextures2);

//		for (int i = 0; i < numtextures; i++, directory++) {
//			if (i == numtextures1) {
		// Start looking in second texture file.
//				maptex = maptex2;
//				directory = directory2;
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
