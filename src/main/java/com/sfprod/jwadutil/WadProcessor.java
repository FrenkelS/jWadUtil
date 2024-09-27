package com.sfprod.jwadutil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.sfprod.jwadutil.WadFile.Lump;

public class WadProcessor {

	// Lump order in a map WAD: each map needs a couple of lumps
	// to provide a complete scene geometry description.
	private static final int ML_LINEDEFS = 2; // LineDefs, from editing
	private static final int ML_SIDEDEFS = 3; // SideDefs, from editing
	private static final int ML_VERTEXES = 4; // Vertices, edited and BSP splits generated
	private static final int ML_SEGS = 5; // LineSegs, from LineDefs split by BSP

	private static final byte ST_HORIZONTAL = 0;
	private static final byte ST_VERTICAL = 1;
	private static final byte ST_POSITIVE = 2;
	private static final byte ST_NEGATIVE = 3;

	private static final short NO_INDEX = (short) 0xffff;
	private static final byte ML_TWOSIDED = 4;

	private static final short ANG90_16 = 0x4000;

	private final WadFile wadFile;

	public WadProcessor(WadFile wad) {
		this.wadFile = wad;
	}

	public void processWad() {
		processPNames();
		processDoom1Levels();
		removeUnusedLumps();
		processSprites();
		processWalls();
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
		processSegs(lumpNum);
		processSides(lumpNum);
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

	private List<Vertex> getVertexes(int lumpNum) {
		int vtxLumpNum = lumpNum + ML_VERTEXES;
		Lump vxl = wadFile.getLumpByNum(vtxLumpNum);
		List<Vertex> vtx = new ArrayList<>();
		ByteBuffer vxlByteBuffer = vxl.dataAsByteBuffer();
		for (int i = 0; i < vxl.data().length / (4 + 4); i++) {
			int x = vxlByteBuffer.getInt();
			int y = vxlByteBuffer.getInt();
			vtx.add(new Vertex(x, y));
		}
		return vtx;
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

		int lineCount = lines.data().length / sizeofmaplinedef;

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
		List<Vertex> vtx = getVertexes(lumpNum);

		ByteBuffer newLineByteBuffer = ByteBuffer.allocate(lineCount * Line.SIZE_OF_LINE);
		newLineByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (short i = 0; i < lineCount; i++) {
			Maplinedef maplinedef = oldLines.get(i);
			// TODO vertexes can be int16_t
			Vertex vertex1 = vtx.get(maplinedef.v1());
			newLineByteBuffer.putInt(vertex1.x()); // v1.x
			newLineByteBuffer.putInt(vertex1.y()); // v1.y

			Vertex vertex2 = vtx.get(maplinedef.v2());
			newLineByteBuffer.putInt(vertex2.x()); // v2.x
			newLineByteBuffer.putInt(vertex2.y()); // v2.y

			newLineByteBuffer.putShort(i); // lineno

			short dx = (short) ((vertex2.x() - vertex1.x()) >> 16);
			short dy = (short) ((vertex2.y() - vertex1.y()) >> 16);
			newLineByteBuffer.putShort(dx); // dx
			newLineByteBuffer.putShort(dy); // dy

			newLineByteBuffer.putShort(maplinedef.sidenum()[0]); // sidenum[0];
			newLineByteBuffer.putShort(maplinedef.sidenum()[1]); // sidenum[1];

			newLineByteBuffer.putInt(vertex1.y() < vertex2.y() ? vertex2.y() : vertex1.y()); // bbox[BOXTOP]
			newLineByteBuffer.putInt(vertex1.y() < vertex2.y() ? vertex1.y() : vertex2.y()); // bbox[BOXBOTTOM]
			newLineByteBuffer.putInt(vertex1.x() < vertex2.x() ? vertex1.x() : vertex2.x()); // bbox[BOXLEFT]
			newLineByteBuffer.putInt(vertex1.x() < vertex2.x() ? vertex2.x() : vertex1.x()); // bbox[BOXRIGHT]

			newLineByteBuffer.put((byte) maplinedef.flags()); // flags
			newLineByteBuffer.put((byte) maplinedef.special()); // special
			newLineByteBuffer.putShort(maplinedef.tag()); // tag

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

	private static record Mapseg(short v1, short v2, short angle, short linedef, short side, short offset) {
	}

	private static record Line(Vertex v1, Vertex v2, short lineno, short dx, short dy, short[] sidenum, int[] bbox,
			byte flags, byte special, short tag, byte slopetype) {
		public static final int SIZE_OF_LINE = 2 * 4 + 2 * 4 + 2 + +2 + 2 + 2 * 2 + 4 * 4 + 1 + 1 + 2 + 1;
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

	private List<Mapsidedef> getSides(int lumpNum) {
		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		Lump sxl = wadFile.getLumpByNum(sidesLumpNum);
		List<Mapsidedef> sides = new ArrayList<>();
		ByteBuffer sidesByteBuffer = sxl.dataAsByteBuffer();
		int sizeofmapsidedef = 2 + 2 + 8 + 8 + 8 + 2;
		for (int i = 0; i < sxl.data().length / sizeofmapsidedef; i++) {
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
	 * Change vertexes, offset, angle, sidenum, linenum, frontsectornum and
	 * backsectornum
	 *
	 * @param lumpNum
	 */
	private void processSegs(int lumpNum) {
		int segsLumpNum = lumpNum + ML_SEGS;
		Lump segs = wadFile.getLumpByNum(segsLumpNum);

		int sizeofmapseg = 2 + 2 + 2 + 2 + 2 + 2;

		int segCount = segs.data().length / sizeofmapseg;

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
		List<Vertex> vtx = getVertexes(lumpNum);

		// And LineDefs. Must process lines first.
		int linesLumpNum = lumpNum + ML_LINEDEFS;
		Lump lxl = wadFile.getLumpByNum(linesLumpNum);
		List<Line> lines = new ArrayList<>();
		ByteBuffer linesByteBuffer = lxl.dataAsByteBuffer();
		for (int i = 0; i < lxl.data().length / Line.SIZE_OF_LINE; i++) {
			Vertex v1 = new Vertex(linesByteBuffer.getInt(), linesByteBuffer.getInt());
			Vertex v2 = new Vertex(linesByteBuffer.getInt(), linesByteBuffer.getInt());
			short lineno = linesByteBuffer.getShort();
			short dx = linesByteBuffer.getShort();
			short dy = linesByteBuffer.getShort();
			short[] sidenum = { linesByteBuffer.getShort(), linesByteBuffer.getShort() };
			int[] bbox = { linesByteBuffer.getInt(), linesByteBuffer.getInt(), linesByteBuffer.getInt(),
					linesByteBuffer.getInt() };
			byte flags = linesByteBuffer.get();
			byte special = linesByteBuffer.get();
			short tag = linesByteBuffer.getShort();
			byte slopetype = linesByteBuffer.get();
			lines.add(new Line(v1, v2, lineno, dx, dy, sidenum, bbox, flags, special, tag, slopetype));
		}

		// And sides too...
		List<Mapsidedef> sides = getSides(lumpNum);

		// ****************************

		int sizeofseg = 2 * 2 + 2 * 2 + 2 + 2 + 2 + 2 + 2 + 2;
		ByteBuffer newSegsByteBuffer = ByteBuffer.allocate(segCount * sizeofseg);
		newSegsByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (Mapseg oldSeg : oldSegs) {
			Vertex v1 = vtx.get(oldSeg.v1);
			Vertex v2 = vtx.get(oldSeg.v2);
			newSegsByteBuffer.putShort((short) (v1.x >> 16)); // v1.x
			newSegsByteBuffer.putShort((short) (v1.y >> 16)); // v1.y
			newSegsByteBuffer.putShort((short) (v2.x >> 16)); // v2.x
			newSegsByteBuffer.putShort((short) (v2.y >> 16)); // v2.y

			newSegsByteBuffer.putShort(oldSeg.offset()); // offset
			newSegsByteBuffer.putShort((short) (oldSeg.angle() + ANG90_16)); // angle

			short linenum = oldSeg.linedef();
			Line ldef = lines.get(linenum);
			short side = oldSeg.side();
			short sidenum = ldef.sidenum()[side];
			newSegsByteBuffer.putShort(sidenum); // sidenum

			newSegsByteBuffer.putShort(linenum); // linenum

			short frontsectornum = sidenum == NO_INDEX ? NO_INDEX : sides.get(sidenum).sector();
			newSegsByteBuffer.putShort(frontsectornum); // frontsectornum

			short backsectornum = NO_INDEX;
			if ((ldef.flags() & ML_TWOSIDED) != 0) {
				short backsectorside = ldef.sidenum()[side ^ 1];
				if (backsectorside != NO_INDEX) {
					backsectornum = sides.get(backsectorside).sector();
				}
			}
			newSegsByteBuffer.putShort(backsectornum); // backsectornum
		}

		Lump newSeg = new Lump(segs.name(), newSegsByteBuffer.array());
		wadFile.replaceLump(segsLumpNum, newSeg);
	}

	/**
	 * Change textureoffset, rowoffset, toptexture, bottomtexture, midtexture and
	 * sector
	 *
	 * @param lumpNum
	 */
	private void processSides(int lumpNum) {
		List<Mapsidedef> oldSides = getSides(lumpNum);
		int sideCount = oldSides.size();

		List<String> textureNames = getTextureNames();

		ByteBuffer newSidesByteBuffer = ByteBuffer.allocate(sideCount * (2 + 2 + 2 + 2 + 2 + 2));
		newSidesByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		for (Mapsidedef oldSide : oldSides) {
			newSidesByteBuffer.putShort(oldSide.textureoffset()); // textureoffset
			newSidesByteBuffer.putShort(oldSide.rowoffset()); // rowoffset

			newSidesByteBuffer.putShort(getTextureNumForName(textureNames, oldSide.toptextureAsString())); // toptexture
			newSidesByteBuffer.putShort(getTextureNumForName(textureNames, oldSide.bottomtextureAsString()));// bottomtexture
			newSidesByteBuffer.putShort(getTextureNumForName(textureNames, oldSide.midtextureAsString())); // midtexture

			newSidesByteBuffer.putShort(oldSide.sector());// sector
		}

		int sidesLumpNum = lumpNum + ML_SIDEDEFS;
		byte[] sidesLumpName = wadFile.getLumpByNum(sidesLumpNum).name();
		Lump newSide = new Lump(sidesLumpName, newSidesByteBuffer.array());
		wadFile.replaceLump(sidesLumpNum, newSide);
	}

	private short getTextureNumForName(List<String> names, String name) {
		int index = names.indexOf(name);
		return index == -1 ? 0 : (short) index;
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

	/**
	 * Capitalize patch names
	 *
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
	 */
	private void removeUnusedLumps() {
		wadFile.removeLump("CREDIT"); // Credits screen
		wadFile.removeLump("SW18_7"); // Duplicate wall texture

		Stream.of( //
				"AMMNUM", // Status bar numbers
				"APBX", // Arachnotron projectile in flight
				"APLS", // Arachnotron projectile impact
				"BAL2", // Cacodemon projectile
				"BOSF", // Spawn cube
				"BRDR", // Border
				"D_", // MUS music
				"DS", // Sound Blaster sound effects
				"DMXGUS", // Gravis UltraSound instrument data
				"GENMIDI", // OPL instrument data
				"HELP1", // Help screen
				"MANF", // Mancubus projectile
				"M_DETAIL", // Graphic Detail:
				"M_DIS", // Display
				"M_EPI", // Episode names
				"M_GD", // Graphic detail high and low
				"M_LGTTL", // Load game
				"M_MSENS", // Mouse sensitivity
				"M_PAUSE", // Pause
				"M_RDTHIS", // Read This!
				"M_SCRNSZ", // Screen Size
				"M_SGTTL", // Save game
				"STARMS", // Status bar arms
				"STCDROM", // Loading icon CD-ROM
				"STCFN121", // letter
				"STDISK", // Loading icon Disk
				"STFB", // Status bar face background
				"STPB", // Status bar p? background
				"STT", // Status bar numbers
				"VERTEXES", // vertexes for a map
				"WIA", // Intermission animations
				"WIBP", // P1 - P4
				"WIFRGS", // Frgs
				"WIKILRS", // Killers
				"WILV1", // Episode 2 level names
				"WILV2", // Episode 3 level names
				"WIMSTAR", // You
				"WIOSTF", // F.
				"WIOSTS", // Scrt
				"WIP1", // P1
				"WIP2", // P2
				"WIP3", // P3
				"WIP4", // P4
				"WIVCTMS" // Victims
		).forEach(prefix -> wadFile.removeLumps(prefix));
	}

	private void processSprites() {
		int start = wadFile.getLumpNumByName("S_START");
		int end = wadFile.getLumpNumByName("S_END");

		for (int lumpnum = start + 1; lumpnum < end; lumpnum++) {
			Lump vanillaLump = wadFile.getLumpByNum(lumpnum);
			Lump doom8088Lump = processSprite(vanillaLump);
			wadFile.replaceLump(doom8088Lump);
		}
	}

	/**
	 * Repeat every other column to cut the size in half
	 *
	 * @param vanillaLump
	 * @return
	 */
	private Lump processSprite(Lump vanillaLump) {
		ByteBuffer vanillaData = vanillaLump.dataAsByteBuffer();
		short width = vanillaData.getShort();
		short height = vanillaData.getShort();
		short leftoffset = vanillaData.getShort();
		short topoffset = vanillaData.getShort();
		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(vanillaData.getInt());
		}

		List<List<Byte>> columns = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			// get column
			List<Byte> column = new ArrayList<>();

			vanillaData.position(columnofs.get(columnof));
			byte topdelta = vanillaData.get();
			column.add(topdelta);
			while (topdelta != -1) {
				byte lengthByte = vanillaData.get();
				column.add(lengthByte);
				int length = lengthByte & 0xff;
				byte unused = vanillaData.get(); // unused
				column.add(unused);
				for (int y = 0; y < length; y++) {
					column.add(vanillaData.get());
				}
				unused = vanillaData.get(); // unused
				column.add(unused);

				topdelta = vanillaData.get();
				column.add(topdelta);
			}

			columns.add(column);
		}

		ByteBuffer doom8088Data = ByteBuffer.allocate(65536);
		doom8088Data.order(ByteOrder.LITTLE_ENDIAN);

		doom8088Data.putShort(width);
		doom8088Data.putShort(height);
		doom8088Data.putShort(leftoffset);
		doom8088Data.putShort(topoffset);

		// temp offset values
		for (int columnof = 0; columnof < width; columnof++) {
			doom8088Data.putInt(-1);
		}

		int l = width / 2;
		int c = 0;
		while (l-- != 0) {
			columnofs.set(c + 0, doom8088Data.position());
			columnofs.set(c + 1, doom8088Data.position());
			List<Byte> column = columns.get(c);
			for (byte b : column) {
				doom8088Data.put(b);
			}
			c += 2;
		}

		switch (width & 1) {
		case 1:
			columnofs.set(c, doom8088Data.position());
			for (byte b : columns.get(c)) {
				doom8088Data.put(b);
			}
		}

		int size = doom8088Data.position();

		doom8088Data.position(8);
		for (int columnof : columnofs) {
			doom8088Data.putInt(columnof);
		}

		byte[] doom8088ByteArray = Arrays.copyOf(doom8088Data.array(), size);
		return new Lump(vanillaLump.name(), doom8088ByteArray);
	}

	private void processWalls() {
		int start = wadFile.getLumpNumByName("P1_START");
		int end = wadFile.getLumpNumByName("P1_END");

		for (int lumpnum = start + 1; lumpnum < end; lumpnum++) {
			Lump vanillaLump = wadFile.getLumpByNum(lumpnum);
			Lump doom8088Lump = processWall(vanillaLump);
			wadFile.replaceLump(doom8088Lump);
		}
	}

	/**
	 * Repeat every fourth column four times to leave a quarter of the size
	 *
	 * @param vanillaLump
	 * @return
	 */
	private Lump processWall(Lump vanillaLump) {
		ByteBuffer vanillaData = vanillaLump.dataAsByteBuffer();
		short width = vanillaData.getShort();
		short height = vanillaData.getShort();
		short leftoffset = vanillaData.getShort();
		short topoffset = vanillaData.getShort();
		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(vanillaData.getInt());
		}

		List<List<Byte>> columns = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			// get column
			List<Byte> column = new ArrayList<>();

			vanillaData.position(columnofs.get(columnof));
			byte topdelta = vanillaData.get();
			column.add(topdelta);
			while (topdelta != -1) {
				byte lengthByte = vanillaData.get();
				column.add(lengthByte);
				int length = lengthByte & 0xff;
				byte unused = vanillaData.get(); // unused
				column.add(unused);
				for (int y = 0; y < length; y++) {
					column.add(vanillaData.get());
				}
				unused = vanillaData.get(); // unused
				column.add(unused);

				topdelta = vanillaData.get();
				column.add(topdelta);
			}

			columns.add(column);
		}

		ByteBuffer doom8088Data = ByteBuffer.allocate(65536);
		doom8088Data.order(ByteOrder.LITTLE_ENDIAN);

		doom8088Data.putShort(width);
		doom8088Data.putShort(height);
		doom8088Data.putShort(leftoffset);
		doom8088Data.putShort(topoffset);

		// temp offset values
		for (int i = 0; i < width; i++) {
			doom8088Data.putInt(-1);
		}

		int l = width / 4;
		int c = 0;
		while (l-- != 0) {
			columnofs.set(c + 0, doom8088Data.position());
			columnofs.set(c + 1, doom8088Data.position());
			columnofs.set(c + 2, doom8088Data.position());
			columnofs.set(c + 3, doom8088Data.position());
			List<Byte> column = columns.get(c);
			for (byte b : column) {
				doom8088Data.put(b);
			}
			c += 4;
		}

		switch (width & 3) {
		case 3:
			columnofs.set(c + 0, doom8088Data.position());
			columnofs.set(c + 1, doom8088Data.position());
			columnofs.set(c + 2, doom8088Data.position());
			for (byte b : columns.get(c)) {
				doom8088Data.put(b);
			}
			break;
		case 2:
			columnofs.set(c + 0, doom8088Data.position());
			columnofs.set(c + 1, doom8088Data.position());
			for (byte b : columns.get(c)) {
				doom8088Data.put(b);
			}
			break;
		case 1:
			columnofs.set(c + 0, doom8088Data.position());
			for (byte b : columns.get(c)) {
				doom8088Data.put(b);
			}
		}

		int size = doom8088Data.position();

		doom8088Data.position(8);
		for (int columnof : columnofs) {
			doom8088Data.putInt(columnof);
		}

		byte[] doom8088ByteArray = Arrays.copyOf(doom8088Data.array(), size);
		return new Lump(vanillaLump.name(), doom8088ByteArray);
	}

}
