package com.sfprod.jwadutil;

import java.io.IOException;
import java.net.URISyntaxException;

public class Main {

	public static void main(String[] args) throws IOException, URISyntaxException {
		WadFile iwadFile = new WadFile("/doom1.wad");

		// Also insert the GBADoom wad file. (Extra menu options etc)
		WadFile pwadFile = new WadFile("/gbadoom.wad");

		iwadFile.mergeWadFile(pwadFile);

//		WadProcessor wadProcessor = new WadProcessor(iwadFile);
//		wadProcessor.processWad();

//		iwadFile.saveWadFile("doom8088.wad");
	}

}
