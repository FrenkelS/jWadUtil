package com.sfprod.jwadutil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WadFile {

	private static final List<String> FILE_SIGNATURES = List.of("IWAD", "PWAD");

	private final int numlumps;
	private final int infotableofs;

	private final List<Lump> lumps = new ArrayList<>();

	private static record Filelump(int filepos, int size, byte[] name) {
	}

	public WadFile(String wadPath) throws IOException, URISyntaxException {
		ByteBuffer byteBuffer = ByteBuffer
				.wrap(Files.readAllBytes(Path.of(WadFile.class.getResource(wadPath).toURI())));
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		byte[] identification = new byte[4];
		byteBuffer.get(identification);

		String identificationAsString = new String(identification);
		if (!FILE_SIGNATURES.contains(identificationAsString)) {
			throw new IllegalArgumentException(wadPath + " is not a WAD file");
		}

		this.numlumps = byteBuffer.getInt();
		this.infotableofs = byteBuffer.getInt();

		byteBuffer.position(infotableofs);

		List<Filelump> filelumps = new ArrayList<>();

		for (int i = 0; i < numlumps; i++) {
			int filepos = byteBuffer.getInt();
			int size = byteBuffer.getInt();
			byte[] name = new byte[8];
			byteBuffer.get(name);
			filelumps.add(new Filelump(filepos, size, name));
		}

		for (Filelump filelump : filelumps) {
			byte[] data = new byte[filelump.size];
			byteBuffer.position(filelump.filepos);
			byteBuffer.get(data);
			lumps.add(new Lump(new String(filelump.name).trim(), data));
		}
	}

	public void saveWadFile(String filePath) {
		File f = new File(filePath);

//		if (!f.open(QIODevice::Truncate | QIODevice::ReadWrite))
//			return;

//		SaveWadFile(f);
//		f.close();
	}

	private int ROUND_UP4(int x) {
		return (x + 3) & -4;
	}

	public void SaveWadFile(File device) {
//		if (!device.isOpen() || !device.isWritable())
//			return;

//		WadInfo header;

//		header.numlumps = lumps.size();

//		header.identification[0] = 'I';
//		header.identification[1] = 'W';
//		header.identification[2] = 'A';
//		header.identification[3] = 'D';

//		header.infotableofs = sizeof(WadInfo);

//		device.write(header, sizeof(header));

//		int fileOffset = sizeof(WadInfo) + (sizeof(filelump_t) * lumps.count());

//		fileOffset = ROUND_UP4(fileOffset);

		// Write the file info blocks.
//		for (int i = 0; i < lumps.count(); i++) {
//			Lump l = lumps.at(i);

//			filelump_t fl;

//			memset(fl.name, 0, 8);
//			strncpy(fl.name, l.name.toLatin1().toUpper().constData(), 8);

//			fl.size = l.length;

//			if (l.length > 0)
//				fl.filepos = fileOffset;
//			else
//				fl.filepos = 0;

//			device.write(fl, sizeof(fl));

//			fileOffset += l.length;
//			fileOffset = ROUND_UP4(fileOffset);
//		}

		// Write the lump data out.
//		for (int i = 0; i < lumps.size(); i++) {
//			Lump l = lumps.get(i);

//			if (l.length == 0)
//				continue;

//			int pos = device.pos();

//			pos = ROUND_UP4(pos);

//			device.seek(pos);

//			device.write(l.data, l.length);
//		}
	}

	public int GetLumpByName(String name, Lump lump) {
		for (int i = lumps.size() - 1; i >= 0; i--) {
			if (lumps.get(i).name().equalsIgnoreCase(name)) {
				lump = lumps.get(i);
				return i;
			}
		}

		return -1;
	}

	public boolean GetLumpByNum(int lumpnum, Lump lump) {
		if (lumpnum >= lumps.size()) {
			return false;
		}

		lump = lumps.get(lumpnum);

		return true;
	}

	public void ReplaceLump(int lumpnum, Lump newLump) {
		if (lumpnum >= lumps.size()) {
		}

//		lumps.replace(lumpnum, newLump);
	}

	private void InsertLump(int lumpnum, Lump newLump) {
		lumps.add(lumpnum, newLump);
	}

	public void RemoveLump(int lumpnum) {
		if (lumpnum >= lumps.size()) {
			return;
		}

		lumps.remove(lumpnum);
	}

	public int LumpCount() {
		return lumps.size();
	}

	public void mergeWadFile(WadFile wadFile) {
//		for (int i = 0; i < wadFile.LumpCount(); i++) {
//			Lump l;
//
//			wadFile.GetLumpByNum(i, l);
//
//			InsertLump(0xffff, l);
//		}
	}

}
