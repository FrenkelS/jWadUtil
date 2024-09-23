package com.sfprod.jwadutil;

public class Main {

	public static void main(String[] args) {
		String inFile = "doom1.wad";
		String outFile = null;

		for (int i = 0; i < args.length - 1; i++) {
			if ("-out".equals(args[i])) {
				i++;
				outFile = args[i];
			}
		}

		if (outFile == null) {
			throw new IllegalArgumentException("-out is mandatory");
		}

		WadFile iwadFile = new WadFile(inFile);

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pwadFile = new WadFile("/gbadoom.wad");

		iwadFile.MergeWadFile(pwadFile);

		WadProcessor wadProcessor = new WadProcessor(iwadFile);
		wadProcessor.ProcessWad();

		iwadFile.SaveWadFile(outFile);
	}

}
