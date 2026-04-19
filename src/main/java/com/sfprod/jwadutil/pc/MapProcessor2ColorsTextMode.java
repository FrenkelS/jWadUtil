package com.sfprod.jwadutil.pc;

import static com.sfprod.jwadutil.pc.WadProcessor2ColorsTextMode.COLORS_FLOORS;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.sfprod.jwadutil.Color;
import com.sfprod.jwadutil.Lump;
import com.sfprod.jwadutil.MapProcessor;
import com.sfprod.jwadutil.WadFile;

public class MapProcessor2ColorsTextMode extends MapProcessor {

	private final List<Double> sortedGrays;

	private final List<Short> availableColors;
	private final List<Short> flatAvailableColors = new ArrayList<>();

	public MapProcessor2ColorsTextMode(ByteOrder byteOrder, WadFile wadFile) {
		super(byteOrder, wadFile);

		List<Double> grays = vgaColors.stream().map(Color::gray).toList();
		this.sortedGrays = grays.stream().sorted().toList();

		List<Short> colors = new ArrayList<>(COLORS_FLOORS.length);
		for (byte b : COLORS_FLOORS) {
			colors.add(toShort(b));
		}
		this.availableColors = Collections.unmodifiableList(colors);
	}

	@Override
	protected short calculateAverageColor(Map<String, Short> flatToColor, String flatname) {
		if (flatToColor.isEmpty()) {
			flatAvailableColors.clear();
			flatAvailableColors.addAll(availableColors);
		}

		if (!flatToColor.containsKey(flatname)) {
			Lump flat = wadFile.getLumpByName(flatname);

			byte[] source = flat.data();
			Color[] colors = new Color[source.length];
			for (int i = 0; i < source.length; i++) {
				byte b = source[i];
				colors[i] = vgaColors.get(toInt(b));
			}
			Color averageColor = Color.blendColors(colors);

			double averageGray = averageColor.gray();
			int possibleIndex = Math.clamp(Math.abs(Collections.binarySearch(sortedGrays, averageGray)), 0,
					sortedGrays.size() - 1);

			int indexAvailableColors = possibleIndex * flatAvailableColors.size() / sortedGrays.size();
			short c = flatAvailableColors.get(indexAvailableColors);
			flatAvailableColors.remove(indexAvailableColors);

			flatToColor.put(flatname, c);
			assert flatToColor.size() == new HashSet<>(flatToColor.values()).size();
		}

		return flatToColor.get(flatname);
	}
}
