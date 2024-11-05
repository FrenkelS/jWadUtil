package com.sfprod.jwadutil;

import static com.sfprod.jwadutil.WadProcessor2ColorsTextMode.COLORS;
import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapProcessor2ColorsTextMode extends MapProcessor {

	private final double bucketLimit0;
	private final double bucketLimit1;
	private final double bucketLimit2;
	private final double bucketLimit3;

	private final Map<String, Short> flatToColor = new HashMap<>();

	private final short[] buckets = new short[5];

	public MapProcessor2ColorsTextMode(ByteOrder byteOrder, WadFile wadFile) {
		super(byteOrder, wadFile, Collections.emptyList());

		List<Double> grays = vgaColors.stream().map(Color::gray).toList();

		List<Double> sortedGrays = grays.stream().sorted().toList();
		this.bucketLimit0 = sortedGrays.get(52);
		this.bucketLimit1 = sortedGrays.get(103);
		this.bucketLimit2 = sortedGrays.get(153);
		this.bucketLimit3 = sortedGrays.get(205);
	}

	@Override
	protected short calculateAverageColor(String flatname) {
		if (!flatToColor.containsKey(flatname)) {
			Lump flat = wadFile.getLumpByName(flatname);

			byte[] source = flat.data();
			int sumr = 0;
			int sumg = 0;
			int sumb = 0;
			for (byte b : source) {
				Color color = vgaColors.get(toInt(b));
				sumr += color.r() * color.r();
				sumg += color.g() * color.g();
				sumb += color.b() * color.b();
			}
			int averager = (int) Math.sqrt(sumr / (64 * 64));
			int averageg = (int) Math.sqrt(sumg / (64 * 64));
			int averageb = (int) Math.sqrt(sumb / (64 * 64));
			Color averageColor = new Color(averager, averageg, averageb);

			double gray = averageColor.gray();

			int bucket;
			if (gray < bucketLimit0) {
				bucket = 0;
			} else if (gray < bucketLimit1) {
				bucket = 1;
			} else if (gray < bucketLimit2) {
				bucket = 2;
			} else if (gray < bucketLimit3) {
				bucket = 3;
			} else {
				bucket = 4;
			}
			short n = buckets[bucket];
			short c = toShort((n << 8) | toShort(COLORS[bucket]));
			buckets[bucket]++;

			flatToColor.put(flatname, c);
		}

		return flatToColor.get(flatname);
	}
}
