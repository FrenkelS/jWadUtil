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
			throw new IllegalArgumentException("-out is mandatory");
		}

		WadFile iwadFile = new WadFile(inFile);
		iwadFile.LoadWadFile();

		WadProcessor wadProcessor = new WadProcessor(iwadFile);

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pwadFile = new WadFile("/gbadoom.wad");
		pwadFile.LoadWadFile();

		iwadFile.MergeWadFile(pwadFile);

		wadProcessor.ProcessWad();

		iwadFile.SaveWadFile(outFile);
	}

}
