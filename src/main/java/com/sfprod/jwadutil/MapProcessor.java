package com.sfprod.jwadutil;

import static com.sfprod.jwadutil.ByteBufferUtils.toByteArray;
import static com.sfprod.jwadutil.WadProcessor.FLAT_SPAN;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sfprod.jwadutil.WadFile.Lump;

public class MapProcessor {

	// Lump order in a map WAD: each map needs a couple of lumps
	// to provide a complete scene geometry description.
	private static final int ML_THINGS = 1; // Monsters, items..
	private static final int ML_LINEDEFS = 2; // LineDefs, from editing
	private static final int ML_SIDEDEFS = 3; // SideDefs, from editing
	private static final int ML_VERTEXES = 4; // Vertices, edited and BSP splits generated
	private static final int ML_SEGS = 5; // LineSegs, from LineDefs split by BSP
	private static final int ML_SSECTORS = 6; // SubSectors, list of LineSegs
	private static final int ML_SECTORS = 8; // Sectors, from editing
	private static final int ML_BLOCKMAP = 10; // LUT, motion clipping, walls/grid element

	private static final short MTF_NOTSINGLE = 16;

	private static final byte ST_HORIZONTAL = 0;
	private static final byte ST_VERTICAL = 1;
	private static final byte ST_POSITIVE = 2;
	private static final byte ST_NEGATIVE = 3;

	private static final short NO_INDEX = (short) 0xffff;
	private static final byte ML_TWOSIDED = 4;

	private static final short ANG90_16 = 0x4000;

	private final WadFile wadFile;
	private final List<Color> vgaColors;

	public MapProcessor(WadFile wadFile) {
		this.wadFile = wadFile;

		Lump playpal = wadFile.getLumpByName("PLAYPAL");
		ByteBuffer bb = playpal.dataAsByteBuffer();
		List<Color> colors = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			int r = bb.get() & 0xff;
			int g = bb.get() & 0xff;
			int b = bb.get() & 0xff;
			colors.add(new Color(r, g, b));
		}
		this.vgaColors = colors;
	}

	public void processMaps() {
		for (int map = 1; map <= 9; map++) {
			String mapName = "E1M" + map;
			int lumpNum = wadFile.getLumpNumByName(mapName);
			processMap(lumpNum);
		}
	}

	private void processMap(int lumpNum) {
		processThings(lumpNum);
		processLinedefs(lumpNum);
		processSegs(lumpNum);
		processSidedefs(lumpNum);
		processSsectors(lumpNum);
		processSectors(lumpNum);
		processBlockmap(lumpNum);
	}

	/**
	 * Remove multiplayer things
	 *
	 * @param lumpNum
	 */
	private void processThings(int lumpNum) {
		int thingsLumpNum = lumpNum + ML_THINGS;
		Lump things = wadFile.getLumpByNum(thingsLumpNum);
		ByteBuffer oldByteBuffer = things.dataAsByteBuffer();
		ByteBuffer newByteBuffer = ByteBuffer.allocate(65536);
		newByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		for (int i = 0; i < things.length() / (2 + 2 + 2 + 2 + 2); i++) {
			short x = oldByteBuffer.getShort();
			short y = oldByteBuffer.getShort();
			short angle = oldByteBuffer.getShort();
			short type = oldByteBuffer.getShort();
			short options = oldByteBuffer.getShort();

			if (type == 2 || type == 3 || type == 4 || type == 11) {
				// ignore starting spot for player 2, 3, 4 and Deathmatch
			} else if ((options & MTF_NOTSINGLE) == MTF_NOTSINGLE) {
				// ignore multiplayer things
			} else {
				newByteBuffer.putShort(x);
				newByteBuffer.putShort(y);
				newByteBuffer.putShort(type);
				newByteBuffer.put((byte) (angle / 45));
				newByteBuffer.put((byte) options);
			}
		}

		int size = newByteBuffer.position();
		Lump newLump = new Lump(things.name(), toByteArray(newByteBuffer, size));
		wadFile.replaceLump(thingsLumpNum, newLump);
	}

	/**
	 * Change vertexes, dx, dy, bbox[4] and slopetype
	 *
	 * @param lumpNum
	 */
	private void processLinedefs(int lumpNum) {
		int lineLumpNum = lumpNum + ML_LINEDEFS;
		Lump lines = wadFile.getLumpByNum(lineLumpNum);

		int sizeofmaplinedef = 2 + 2 + 2 + 2 + 2 + 2 * 2;

		int lineCount = lines.length() / sizeofmaplinedef;

		List<Maplinedef> oldLines = new ArrayList<>(lineCount);
		ByteBuffer oldLinesByteBuffer = lines.dataAsByteBuffer();
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
		List<Vertex> vertexes = getVertexes(lumpNum);

		ByteBuffer newLineByteBuffer = ByteBuffer.allocate(lineCount * Line.SIZE_OF_LINE);
		newLineByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (Maplinedef maplinedef : oldLines) {
			Vertex vertex1 = vertexes.get(maplinedef.v1());
			newLineByteBuffer.putShort(vertex1.x()); // v1.x
			newLineByteBuffer.putShort(vertex1.y()); // v1.y

			Vertex vertex2 = vertexes.get(maplinedef.v2());
			newLineByteBuffer.putShort(vertex2.x()); // v2.x
			newLineByteBuffer.putShort(vertex2.y()); // v2.y

			short dx = (short) (vertex2.x() - vertex1.x());
			short dy = (short) (vertex2.y() - vertex1.y());
			newLineByteBuffer.putShort(dx); // dx
			newLineByteBuffer.putShort(dy); // dy

			newLineByteBuffer.putShort(maplinedef.sidenum()[0]); // sidenum[0];
			newLineByteBuffer.putShort(maplinedef.sidenum()[1]); // sidenum[1];

			newLineByteBuffer.putShort(vertex1.y() < vertex2.y() ? vertex2.y() : vertex1.y()); // bbox[BOXTOP]
			newLineByteBuffer.putShort(vertex1.y() < vertex2.y() ? vertex1.y() : vertex2.y()); // bbox[BOXBOTTOM]
			newLineByteBuffer.putShort(vertex1.x() < vertex2.x() ? vertex1.x() : vertex2.x()); // bbox[BOXLEFT]
			newLineByteBuffer.putShort(vertex1.x() < vertex2.x() ? vertex2.x() : vertex1.x()); // bbox[BOXRIGHT]

			newLineByteBuffer.put((byte) maplinedef.flags()); // flags
			newLineByteBuffer.put((byte) maplinedef.special()); // special
			newLineByteBuffer.put((byte) maplinedef.tag()); // tag

			byte slopetype;
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

			newLineByteBuffer.put(slopetype); // slopetype
		}

		Lump newLine = new Lump(lines.name(), newLineByteBuffer.array());
		wadFile.replaceLump(lineLumpNum, newLine);
	}

	private List<Vertex> getVertexes(int lumpNum) {
		int vtxLumpNum = lumpNum + ML_VERTEXES;
		Lump vxl = wadFile.getLumpByNum(vtxLumpNum);
		List<Vertex> vertexes = new ArrayList<>();
		ByteBuffer vxlByteBuffer = vxl.dataAsByteBuffer();
		for (int i = 0; i < vxl.length() / (2 + 2); i++) {
			vertexes.add(new Vertex(vxlByteBuffer.getShort(), vxlByteBuffer.getShort()));
		}
		return vertexes;
	}

	private int fixedDiv(int a, int b) {
		if (Math.abs(a) >> 14 >= Math.abs(b)) {
			return ((a ^ b) >> 31) ^ Integer.MAX_VALUE;
		} else {
			return (int) ((((long) a) << 16) / b);
		}
	}

	/**
	 * Change vertexes, offset, angle, sidenum, linenum, frontsectornum and
	 * backsectornum
	 *
	 * @param lumpNum
	 */
	private void processSegs(int lumpNum) {
		int segsLumpNum = lumpNum + ML_SEGS;
		Lump segs = wadFile.getLumpByNum(segsLumpNum);

		int sizeofmapseg = 2 + 2 + 2 + 2 + 2 + 2;

		int segCount = segs.length() / sizeofmapseg;

		List<Mapseg> oldSegs = new ArrayList<>(segCount);
		ByteBuffer oldSegsByteBuffer = segs.dataAsByteBuffer();
		for (int i = 0; i < segCount; i++) {
			short v1 = oldSegsByteBuffer.getShort();
			short v2 = oldSegsByteBuffer.getShort();
			short angle = oldSegsByteBuffer.getShort();
			short linedef = oldSegsByteBuffer.getShort();
			short side = oldSegsByteBuffer.getShort();
			short offset = oldSegsByteBuffer.getShort();
			oldSegs.add(new Mapseg(v1, v2, angle, linedef, side, offset));
		}

		// We need vertexes for this...
		List<Vertex> vertexes = getVertexes(lumpNum);

		// And LineDefs. Must process lines first.
		int linesLumpNum = lumpNum + ML_LINEDEFS;
		Lump lxl = wadFile.getLumpByNum(linesLumpNum);
		List<Line> lines = new ArrayList<>();
		ByteBuffer linesByteBuffer = lxl.dataAsByteBuffer();
		for (int i = 0; i < lxl.length() / Line.SIZE_OF_LINE; i++) {
			Vertex v1 = new Vertex(linesByteBuffer.getShort(), linesByteBuffer.getShort());
			Vertex v2 = new Vertex(linesByteBuffer.getShort(), linesByteBuffer.getShort());
			short dx = linesByteBuffer.getShort();
			short dy = linesByteBuffer.getShort();
			short[] sidenum = { linesByteBuffer.getShort(), linesByteBuffer.getShort() };
			short[] bbox = { linesByteBuffer.getShort(), linesByteBuffer.getShort(), linesByteBuffer.getShort(),
					linesByteBuffer.getShort() };
			byte flags = linesByteBuffer.get();
			byte special = linesByteBuffer.get();
			byte tag = linesByteBuffer.get();
			byte slopetype = linesByteBuffer.get();
			lines.add(new Line(v1, v2, dx, dy, sidenum, bbox, flags, special, tag, slopetype));
		}

		// And sides too...
		List<Mapsidedef> sidedefs = getSidedefs(lumpNum);

		// ****************************

		int sizeofseg = 2 * 2 + 2 * 2 + 2 + 2 + 2 + 2 + 1 + 1;
		ByteBuffer newSegsByteBuffer = ByteBuffer.allocate(segCount * sizeofseg);
		newSegsByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (Mapseg oldSeg : oldSegs) {
			Vertex v1 = vertexes.get(oldSeg.v1());
			newSegsByteBuffer.putShort(v1.x()); // v1.x
			newSegsByteBuffer.putShort(v1.y()); // v1.y

			Vertex v2 = vertexes.get(oldSeg.v2());
			newSegsByteBuffer.putShort(v2.x()); // v2.x
			newSegsByteBuffer.putShort(v2.y()); // v2.y

			newSegsByteBuffer.putShort(oldSeg.offset()); // offset
			newSegsByteBuffer.putShort((short) (oldSeg.angle() + ANG90_16)); // angle

			short linenum = oldSeg.linedef();
			Line ldef = lines.get(linenum);
			short side = oldSeg.side();
			short sidenum = ldef.sidenum()[side];
			newSegsByteBuffer.putShort(sidenum); // sidenum

			newSegsByteBuffer.putShort(linenum); // linenum

			byte frontsectornum = (byte) sidedefs.get(sidenum).sector();
			newSegsByteBuffer.put(frontsectornum); // frontsectornum

			byte backsectornum = (byte) 0xff;
			if ((ldef.flags() & ML_TWOSIDED) != 0) {
				short backsectorside = ldef.sidenum()[side ^ 1];
				if (backsectorside != NO_INDEX) {
					backsectornum = (byte) sidedefs.get(backsectorside).sector();
				}
			}
			newSegsByteBuffer.put(backsectornum); // backsectornum
		}

		Lump newSeg = new Lump(segs.name(), newSegsByteBuffer.array());
		wadFile.replaceLump(segsLumpNum, newSeg);
	}

	private List<Mapsidedef> getSidedefs(int lumpNum) {
		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		Lump sxl = wadFile.getLumpByNum(sidesLumpNum);
		List<Mapsidedef> sides = new ArrayList<>();
		ByteBuffer sidesByteBuffer = sxl.dataAsByteBuffer();
		int sizeofmapsidedef = 2 + 2 + 8 + 8 + 8 + 2;
		for (int i = 0; i < sxl.length() / sizeofmapsidedef; i++) {
			short textureoffset = sidesByteBuffer.getShort();
			short rowoffset = sidesByteBuffer.getShort();
			byte[] toptexture = new byte[8];
			sidesByteBuffer.get(toptexture);
			byte[] bottomtexture = new byte[8];
			sidesByteBuffer.get(bottomtexture);
			byte[] midtexture = new byte[8];
			sidesByteBuffer.get(midtexture);
			short sector = sidesByteBuffer.getShort();
			sides.add(new Mapsidedef(textureoffset, rowoffset, toptexture, bottomtexture, midtexture, sector));
		}
		return sides;
	}

	/**
	 * Change textureoffset, rowoffset, toptexture, bottomtexture, midtexture and
	 * sector
	 *
	 * @param lumpNum
	 */
	private void processSidedefs(int lumpNum) {
		List<Mapsidedef> oldSidedefs = getSidedefs(lumpNum);
		int sideCount = oldSidedefs.size();

		List<String> textureNames = getTextureNames();

		ByteBuffer newSidedefByteBuffer = ByteBuffer.allocate(sideCount * (2 + 1 + 1 + 1 + 1 + 1));
		newSidedefByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (Mapsidedef oldSidedef : oldSidedefs) {
			newSidedefByteBuffer.putShort(oldSidedef.textureoffset()); // textureoffset
			newSidedefByteBuffer.put((byte) oldSidedef.rowoffset()); // rowoffset

			newSidedefByteBuffer.put(getTextureNumForName(textureNames, oldSidedef.toptextureAsString())); // toptexture
			newSidedefByteBuffer.put(getTextureNumForName(textureNames, oldSidedef.bottomtextureAsString()));// bottomtexture
			newSidedefByteBuffer.put(getTextureNumForName(textureNames, oldSidedef.midtextureAsString())); // midtexture

			newSidedefByteBuffer.put((byte) oldSidedef.sector()); // sector
		}

		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		byte[] sidedefsLumpName = wadFile.getLumpByNum(sidesLumpNum).name();
		Lump newSidedefs = new Lump(sidedefsLumpName, newSidedefByteBuffer.array());
		wadFile.replaceLump(sidesLumpNum, newSidedefs);
	}

	private List<String> getTextureNames() {
		Lump tex1lump = wadFile.getLumpByName("TEXTURE1");
		ByteBuffer tex1ByteBuffer = tex1lump.dataAsByteBuffer();
		int numtextures1 = tex1ByteBuffer.getInt();
		List<Integer> offsets = new ArrayList<>(numtextures1);
		for (int i = 0; i < numtextures1; i++) {
			offsets.add(tex1ByteBuffer.getInt());
		}

		List<String> textureNames = new ArrayList<>(numtextures1);
		for (int offset : offsets) {
			tex1ByteBuffer.position(offset);
			byte[] name = new byte[8];
			tex1ByteBuffer.get(name);
			textureNames.add(new String(name, StandardCharsets.US_ASCII).trim().toUpperCase());
		}
		return textureNames;
	}

	private byte getTextureNumForName(List<String> names, String name) {
		int index = names.indexOf(name);
		return index == -1 ? 0 : (byte) index;
	}

	/**
	 * Only store numsegs as a byte
	 *
	 * @param lumpNum
	 */
	private void processSsectors(int lumpNum) {
		int ssectorsLumpNum = lumpNum + ML_SSECTORS;
		Lump ssectors = wadFile.getLumpByNum(ssectorsLumpNum);
		ByteBuffer byteBuffer = ssectors.dataAsByteBuffer();
		byte[] newnumsegs = new byte[ssectors.length() / (2 + 2)];
		short derivedFirstseg = 0;
		for (int i = 0; i < ssectors.length() / (2 + 2); i++) {
			short numsegs = byteBuffer.getShort();
			newnumsegs[i] = (byte) numsegs;

			short firstseg = byteBuffer.getShort();
			if (firstseg != derivedFirstseg) {
				throw new IllegalStateException();
			}
			derivedFirstseg += numsegs;
		}
		wadFile.replaceLump(ssectorsLumpNum, new Lump(ssectors.name(), newnumsegs));
	}

	/**
	 * lightlevel and special fit in a byte, replace flat names by average color of
	 * flat
	 *
	 * @param lumpNum
	 */
	private void processSectors(int lumpNum) {
		int sectorsLumpNum = lumpNum + ML_SECTORS;
		Lump sectors = wadFile.getLumpByNum(sectorsLumpNum);
		ByteBuffer oldbb = sectors.dataAsByteBuffer();
		ByteBuffer newbb = ByteBuffer.allocate(65536);
		newbb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < sectors.length() / 26; i++) {
			short floorheight = oldbb.getShort();
			short ceilingheight = oldbb.getShort();
			byte[] floorpic = new byte[8];
			oldbb.get(floorpic);
			byte[] ceilingpic = new byte[8];
			oldbb.get(ceilingpic);
			short lightlevel = oldbb.getShort();
			short special = oldbb.getShort();
			short tag = oldbb.getShort();

			newbb.putShort(floorheight);
			newbb.putShort(ceilingheight);

			if (FLAT_SPAN) {
				// floorpic
				String floorflatname = new String(floorpic, StandardCharsets.US_ASCII).trim();
				if (floorflatname.startsWith("NUKAGE")) {
					newbb.putShort((short) -3);
				} else {
					Lump floor = wadFile.getLumpByName(floorflatname);
					newbb.putShort(calculateAverageColor(floor));
				}

				// ceilingpic
				String ceilingflatname = new String(ceilingpic, StandardCharsets.US_ASCII).trim();
				if (ceilingflatname.startsWith("NUKAGE")) {
					newbb.putShort((short) -3);
				} else if ("F_SKY1".equals(ceilingflatname)) {
					newbb.putShort((short) -2);
				} else {
					Lump ceiling = wadFile.getLumpByName(ceilingflatname);
					newbb.putShort(calculateAverageColor(ceiling));
				}
			} else {
				newbb.put(floorpic);
				newbb.put(ceilingpic);
			}
			newbb.put((byte) lightlevel);
			newbb.put((byte) special);
			newbb.putShort(tag);
		}

		int size = newbb.position();
		Lump newLump = new Lump(sectors.name(), toByteArray(newbb, size));
		wadFile.replaceLump(sectorsLumpNum, newLump);
	}

	private short calculateAverageColor(Lump flat) {
		byte[] source = flat.data();
		int sumr = 0;
		int sumg = 0;
		int sumb = 0;
		for (byte b : source) {
			Color color = vgaColors.get(b & 0xff);
			sumr += color.r() * color.r();
			sumg += color.g() * color.g();
			sumb += color.b() * color.b();
		}
		int averager = (int) Math.sqrt(sumr / (64 * 64));
		int averageg = (int) Math.sqrt(sumg / (64 * 64));
		int averageb = (int) Math.sqrt(sumb / (64 * 64));
		Color averageColor = new Color(averager, averageg, averageb);

		short closestAverageColorIndex = -1;
		int minDistance = Integer.MAX_VALUE;
		for (short i = 0; i < 256; i++) {
			Color vgaColor = vgaColors.get(i);

			int distance = averageColor.calculateDistance(vgaColor);
			if (distance == 0) {
				closestAverageColorIndex = i;
				break;
			}

			if (distance < minDistance) {
				minDistance = distance;
				closestAverageColorIndex = i;
			}
		}

		return closestAverageColorIndex;
	}

	/**
	 * Compress blockmap
	 *
	 * @param lumpNum
	 */
	private void processBlockmap(int lumpNum) {
		int blockmapLumpNum = lumpNum + ML_BLOCKMAP;
		Lump blockmap = wadFile.getLumpByNum(blockmapLumpNum);

		ByteBuffer blockmapByteBuffer = blockmap.dataAsByteBuffer();
		short bmaporgx = blockmapByteBuffer.getShort();
		short bmaporgy = blockmapByteBuffer.getShort();
		short bmapwidth = blockmapByteBuffer.getShort();
		short bmapheight = blockmapByteBuffer.getShort();

		List<Short> offsets = new ArrayList<>();
		for (int i = 0; i < bmapwidth * bmapheight; i++) {
			offsets.add(blockmapByteBuffer.getShort());
		}

		Map<Integer, List<Short>> mapOfLinenos = new HashMap<>();
		for (int i = 0; i < bmapwidth * bmapheight; i++) {
			short offset = offsets.get(i);
			List<Short> linenos = new ArrayList<>();
			blockmapByteBuffer.position(offset * 2);
			blockmapByteBuffer.getShort(); // always 0
			short lineno = blockmapByteBuffer.getShort();
			while (lineno != -1) {
				linenos.add(lineno);
				lineno = blockmapByteBuffer.getShort();
			}
			mapOfLinenos.put(i, linenos);
		}

		ByteBuffer newBlockmap = ByteBuffer.allocate(65536);
		newBlockmap.order(ByteOrder.LITTLE_ENDIAN);

		newBlockmap.putShort(bmaporgx);
		newBlockmap.putShort(bmaporgy);
		newBlockmap.putShort(bmapwidth);
		newBlockmap.putShort(bmapheight);

		// temp offset values
		for (int i = 0; i < bmapwidth * bmapheight; i++) {
			newBlockmap.putShort((short) -1);
		}

		List<Map.Entry<Integer, List<Short>>> sorted = mapOfLinenos.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.comparing(List::size))).toList().reversed();
		List<Map.Entry<List<Short>, Short>> duplicateDataList = new ArrayList<>();
		for (Map.Entry<Integer, List<Short>> entry : sorted) {
			int index = entry.getKey();
			List<Short> linenos = entry.getValue();

			short offset = -1;
			for (Map.Entry<List<Short>, Short> duplicateEntry : duplicateDataList) {
				List<Short> duplicateLinos = duplicateEntry.getKey();
				if (ListUtils.endsWith(duplicateLinos, linenos)) {
					offset = (short) (duplicateEntry.getValue() + (duplicateLinos.size() - linenos.size()));
					break;
				}
			}

			if (offset == -1) {
				offset = (short) (newBlockmap.position() / 2);

				newBlockmap.putShort((short) 0);
				for (short lineno : linenos) {
					newBlockmap.putShort(lineno);
				}
				newBlockmap.putShort((short) -1);

				duplicateDataList.add(Map.entry(linenos, offset));
			}
			offsets.set(index, offset);
		}

		int newLength = newBlockmap.position();

		newBlockmap.position(8);

		for (short offset : offsets) {
			newBlockmap.putShort(offset);
		}

		Lump newLump = new Lump(blockmap.name(), toByteArray(newBlockmap, newLength));
		wadFile.replaceLump(blockmapLumpNum, newLump);
	}

	private static record Maplinedef(short v1, short v2, short flags, short special, short tag, short[] sidenum) {
	}

	private static record Vertex(short x, short y) {
	}

	private static record Line(Vertex v1, Vertex v2, short dx, short dy, short[] sidenum, short[] bbox, byte flags,
			byte special, byte tag, byte slopetype) {
		public static final int SIZE_OF_LINE = 2 * 2 + 2 * 2 + 2 + 2 + 2 * 2 + 4 * 2 + 1 + 1 + 1 + 1;
	}

	private static record Mapseg(short v1, short v2, short angle, short linedef, short side, short offset) {
	}

	private static record Mapsidedef(short textureoffset, short rowoffset, byte[] toptexture, byte[] bottomtexture,
			byte[] midtexture, short sector) {
		private String byteArrayToString(byte[] byteArray) {
			return new String(byteArray, StandardCharsets.US_ASCII).trim().toUpperCase();
		}

		String toptextureAsString() {
			return byteArrayToString(toptexture());
		}

		String bottomtextureAsString() {
			return byteArrayToString(bottomtexture());
		}

		String midtextureAsString() {
			return byteArrayToString(midtexture());
		}
	}
}