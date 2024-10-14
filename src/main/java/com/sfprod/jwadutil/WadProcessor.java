package com.sfprod.jwadutil;

import static com.sfprod.utils.ByteBufferUtils.newByteBuffer;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class WadProcessor {

	public static final boolean FLAT_SPAN = true;

	final WadFile wadFile;
	private final MapProcessor mapProcessor;

	private WadProcessor(WadFile wadFile) {
		this(wadFile, createVgaColors(wadFile), Function.identity());
	}

	WadProcessor(WadFile wadFile, List<Color> colors, Function<Byte, Byte> shuffleColorFunction) {
		this.wadFile = wadFile;
		this.mapProcessor = new MapProcessor(wadFile, colors, shuffleColorFunction);
	}

	private static List<Color> createVgaColors(WadFile wadFile) {
		Lump playpal = wadFile.getLumpByName("PLAYPAL");
		ByteBuffer bb = playpal.dataAsByteBuffer();
		List<Color> vgaColors = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			int r = toInt(bb.get());
			int g = toInt(bb.get());
			int b = toInt(bb.get());
			vgaColors.add(new Color(r, g, b));
		}
		return vgaColors;
	}

	public static WadProcessor getWadProcessor(Game game, WadFile wadFile) {
		if (game == Game.DOOM8088_16_COLOR) {
			return new WadProcessor16(wadFile);
		} else {
			return new WadProcessor(wadFile);
		}
	}

	public void processWad() {
		changeColors();
		processColormap();
		processPNames();
		mapProcessor.processMaps();
		processPlayerSprites();
		removeUnusedLumps();
		processSprites();
		processWalls();
		compressPictures();

		shuffleColors();
	}

	void changeColors() {
	}

	void processColormap() {
	}

	void shuffleColors() {
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
	 * Replace player sprites by zombieman sprites
	 *
	 */
	private void processPlayerSprites() {
		List<Lump> playerSprites = wadFile.getLumpsByName("PLAY");
		List<Lump> zombiemanSprites = wadFile.getLumpsByName("POSS");

		List<String> skip = List.of( //
				"PLAYPAL", "PLAYPAL1", "PLAYPAL2", "PLAYPAL3", "PLAYPAL4", "PLAYPAL5", //
				"PLAYN0", "PLAYW0", "PLAYV0");
		for (Lump playerSprite : playerSprites) {
			if (!skip.contains(playerSprite.nameAsString())) {
				String suffix = playerSprite.nameAsString().substring(4);
				Lump zombiemanSprite = zombiemanSprites.stream().filter(z -> z.nameAsString().endsWith(suffix))
						.findAny().orElseThrow();
				Lump newLump = new Lump(playerSprite.name(), zombiemanSprite.data());
				wadFile.replaceLump(newLump);
			}
		}

		Lump playerBloodyMess = playerSprites.stream().filter(p -> "PLAYV0".equals(p.nameAsString())).findAny()
				.orElseThrow();
		Lump zombieBloodyMess = zombiemanSprites.stream().filter(z -> "POSSU0".equals(z.nameAsString())).findAny()
				.orElseThrow();
		Lump newBloodyMessLump = new Lump(playerBloodyMess.name(), zombieBloodyMess.data());
		wadFile.replaceLump(newBloodyMessLump);
	}

	/**
	 * Remove unused lumps
	 *
	 */
	protected void removeUnusedLumps() {
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
				"DEMO1", // Demo 1
				"DEMO2", // Demo 2
				"DPBD", // Blazing door sound effects
				"DPITMBK", // Item respawn sound effect in multiplayer mode
				"DS", // Sound Blaster sound effects
				"DMXGUS", // Gravis UltraSound instrument data
				"GENMIDI", // OPL instrument data
				"HELP1", // Help screen
				"IFOG", // Item respawn blue fog in multiplayer mode
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
				"STKEYS3", // Status bar blue skull key
				"STKEYS4", // Status bar yellow skull key
				"STKEYS5", // Status bar red skull key
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

		if (FLAT_SPAN) {
			List<Lump> flats = new ArrayList<>();
			flats.add(wadFile.getLumpByName("F_START"));
			flats.add(wadFile.getLumpByName("F_END"));
			flats.addAll(wadFile.getLumpsBetween("F_START", "F_END"));
			flats.stream().filter(f -> !"FLOOR4_8".equals(f.nameAsString())).forEach(wadFile::removeLump);
		}
	}

	private void processSprites() {
		List<Lump> lumps = wadFile.getLumpsBetween("S_START", "S_END");
		lumps.stream().map(this::processSprite).forEach(wadFile::replaceLump);
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
				int length = toInt(lengthByte);
				for (int y = 0; y < length + 2; y++) {
					column.add(vanillaData.get());
				}

				topdelta = vanillaData.get();
				column.add(topdelta);
			}

			columns.add(column);
		}

		ByteBuffer doom8088Data = newByteBuffer();
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

		return new Lump(vanillaLump.name(), size, doom8088Data);
	}

	private void processWalls() {
		List<Lump> lumps = wadFile.getLumpsBetween("P1_START", "P1_END");
		lumps.stream().map(this::processWall).forEach(wadFile::replaceLump);
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
				int length = toInt(lengthByte);
				for (int y = 0; y < length + 2; y++) {
					column.add(vanillaData.get());
				}

				topdelta = vanillaData.get();
				column.add(topdelta);
			}

			columns.add(column);
		}

		ByteBuffer doom8088Data = newByteBuffer();
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

		return new Lump(vanillaLump.name(), size, doom8088Data);
	}

	/**
	 * Remove duplicate columns in graphics in picture format
	 *
	 */
	private void compressPictures() {
		List<Lump> pictures = new ArrayList<>(256);
		// Status bar
		pictures.addAll(wadFile.getLumpsByName("STC"));
		pictures.addAll(wadFile.getLumpsByName("STF"));
		pictures.addAll(wadFile.getLumpsByName("STG"));
		pictures.addAll(wadFile.getLumpsByName("STK"));
		pictures.addAll(wadFile.getLumpsByName("STY"));
		// Menu
		pictures.addAll(wadFile.getLumpsByName("M_"));
		// Intermission
		pictures.addAll(wadFile.getLumpsByName("WI").stream().filter(l -> !"WIMAP0".equals(l.nameAsString())).toList());
		// Sprites
		pictures.addAll(wadFile.getLumpsBetween("S_START", "S_END"));
		// Walls
		pictures.addAll(wadFile.getLumpsBetween("P1_START", "P1_END"));
		pictures.stream().map(this::compressPicture).forEach(wadFile::replaceLump);
	}

	/**
	 * Graphic squashing
	 *
	 * @param picture
	 * @return
	 */
	private Lump compressPicture(Lump picture) {
		ByteBuffer pictureData = picture.dataAsByteBuffer();
		short width = pictureData.getShort();
		short height = pictureData.getShort();
		short leftoffset = pictureData.getShort();
		short topoffset = pictureData.getShort();
		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(pictureData.getInt());
		}

		List<List<Byte>> columns = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			// get column
			List<Byte> column = new ArrayList<>();

			pictureData.position(columnofs.get(columnof));
			byte topdelta = pictureData.get();
			column.add(topdelta);
			while (topdelta != -1) {
				byte lengthByte = pictureData.get();
				column.add(lengthByte);
				int length = toInt(lengthByte);
				for (int y = 0; y < length + 2; y++) {
					column.add(pictureData.get());
				}

				topdelta = pictureData.get();
				column.add(topdelta);
			}

			columns.add(column);
		}

		ByteBuffer compressedData = newByteBuffer();
		compressedData.putShort(width);
		compressedData.putShort(height);
		compressedData.putShort(leftoffset);
		compressedData.putShort(topoffset);

		// temp offset values
		for (int columnof = 0; columnof < width; columnof++) {
			compressedData.putInt(-1);
		}

		Map<List<Byte>, Integer> persistedColumns = new HashMap<>();
		for (int i = 0; i < width; i++) {
			List<Byte> column = columns.get(i);
			if (persistedColumns.containsKey(column)) {
				columnofs.set(i, persistedColumns.get(column));
			} else {
				columnofs.set(i, compressedData.position());
				persistedColumns.put(column, compressedData.position());

				for (byte b : column) {
					compressedData.put(b);
				}
			}
		}

		int size = compressedData.position();

		compressedData.position(8);
		for (int columnof : columnofs) {
			compressedData.putInt(columnof);
		}

		return new Lump(picture.name(), size, compressedData);
	}

}
