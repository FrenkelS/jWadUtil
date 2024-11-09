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

	private final Map<String, Short> flatToColor = new HashMap<>();

	private final short[] buckets = new short[COLORS.length];
	private final double[] bucketLimits = new double[COLORS.length];

	public MapProcessor2ColorsTextMode(ByteOrder byteOrder, WadFile wadFile) {
		super(byteOrder, wadFile, Collections.emptyList());

		List<Double> grays = vgaColors.stream().map(Color::gray).toList();

		List<Double> sortedGrays = grays.stream().sorted().toList();
		double fracstep = 256 / COLORS.length;
		double frac = fracstep;
		for (int i = 0; i < COLORS.length - 1; i++) {
			this.bucketLimits[i] = sortedGrays.get(((int) frac));
			frac += fracstep;
		}
		this.bucketLimits[COLORS.length - 1] = Double.MAX_VALUE;
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

			int bucket = 0;
			while (gray >= bucketLimits[bucket]) {
				bucket++;
			}

			short n = buckets[bucket];
			short c = toShort((n << 8) | toShort(COLORS[bucket]));
			buckets[bucket]++;

			flatToColor.put(flatname, c);
		}

		return flatToColor.get(flatname);
	}
}
