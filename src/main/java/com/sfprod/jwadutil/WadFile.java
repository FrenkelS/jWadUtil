package com.sfprod.jwadutil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//#include "doomtypes.h"

public class WadFile {

	private String wadPath;

	private List<Lump> lumps = new ArrayList<>();

	public WadFile(String filePath) {
		this.wadPath = filePath;
	}

	private int ROUND_UP4(int x) {
		return (x + 3) & -4;
	}

	public void LoadWadFile() {
		File f = new File(wadPath);

//		if(!f.open(QIODevice::ReadOnly))
//			return ;

//		byte[] fd = f.readAll();

//		char* wadData = fd.constData();

//		f.close();

//		WadInfo header = wadData;

//		String id = new String(QLatin1String(header.identification, 4));

//		if(String.compare(id, "IWAD") && String.compare(id, "PWAD"))
//			return ;

//		filelump_t* fileinfo = wadData[header.infotableofs];

//		for(int i = 0; i < header.numlumps; i++)
//		{
//			Lump l;
//			l.name = new String(QLatin1String(fileinfo[i].name, 8));
//			l.name = new String(QLatin1String(l.name.toLatin1().constData()));

//			l.length = fileinfo[i].size;
//			l.data = QByteArray(wadData[fileinfo[i].filepos], fileinfo[i].size);

//			this.lumps.append(l);
//		}
	}

	public void SaveWadFile(String filePath) {
		File f = new File(filePath);

//		if (!f.open(QIODevice::Truncate | QIODevice::ReadWrite))
//			return;

//		SaveWadFile(f);
//		f.close();
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
			if (lumps.get(i).name.compareToIgnoreCase(name) == 0) {
				lump = lumps.get(i);
				return i;
			}
		}

		return -1;
	}

	public boolean GetLumpByNum(int lumpnum, Lump lump) {
		if (lumpnum >= lumps.size())
			return false;

		lump = lumps.get(lumpnum);

		return true;
	}

	public void ReplaceLump(int lumpnum, Lump newLump) {
		if (lumpnum >= lumps.size())
			return;

//		lumps.replace(lumpnum, newLump);
	}

	private void InsertLump(int lumpnum, Lump newLump) {
		lumps.add(lumpnum, newLump);
	}

	public void RemoveLump(int lumpnum) {
		if (lumpnum >= lumps.size())
			return;

		lumps.remove(lumpnum);
	}

	public int LumpCount() {
		return lumps.size();
	}

	public void MergeWadFile(WadFile wadFile) {
//		for (int i = 0; i < wadFile.LumpCount(); i++) {
//			Lump l;
//
//			wadFile.GetLumpByNum(i, l);
//
//			InsertLump(0xffff, l);
//		}
	}

}
