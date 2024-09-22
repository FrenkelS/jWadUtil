package com.sfprod.jwadutil;

public class Main {

	public static void main(String[] args) {
		String inFile = "doom1.wad";
		String outFile = null;

		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("-out")) {
				outFile = args[++i];
			}
		}

		if (outFile == null) {
			throw new IllegalArgumentException("out is mandatory");
		}

		WadFile wf = new WadFile(inFile);
		wf.LoadWadFile();

		WadProcessor wp = new WadProcessor(wf);

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pf = new WadFile("/gbadoom.wad");
		pf.LoadWadFile();

		wf.MergeWadFile(pf);

		wp.ProcessWad();

		wf.SaveWadFile(outFile);
	}

}
