package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toInt;
import static com.sfprod.utils.NumberUtils.toShort;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapProcessor2ColorsTextMode extends MapProcessor {

	private static final short C0 = toShort(0x00);
	private static final short C1 = toShort(0xb0);
	private static final short C2 = toShort(0xb1);
	private static final short C3 = toShort(0xb2);
	private static final short C4 = toShort(0xdb);

	private final double bucket0;
	private final double bucket1;
	private final double bucket2;
	private final double bucket3;

	private final Map<String, Short> flatToColor = new HashMap<>();

	private final Map<Integer, Short> buckets = new HashMap<>();

	public MapProcessor2ColorsTextMode(ByteOrder byteOrder, WadFile wadFile) {
		super(byteOrder, wadFile, Collections.emptyList());

		List<Double> grays = vgaColors.stream().map(Color::gray).toList();

		List<Double> sortedGrays = grays.stream().sorted().toList();
		this.bucket0 = sortedGrays.get(52);
		this.bucket1 = sortedGrays.get(103);
		this.bucket2 = sortedGrays.get(153);
		this.bucket3 = sortedGrays.get(205);

		buckets.put(0, toShort(0));
		buckets.put(1, toShort(0));
		buckets.put(2, toShort(0));
		buckets.put(3, toShort(0));
		buckets.put(4, toShort(0));
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
			short c;
			if (gray < bucket0) {
				short n = buckets.get(0);
				c = toShort((n << 8) | C0);
				buckets.put(0, toShort(n + 1));
			} else if (gray < bucket1) {
				short n = buckets.get(1);
				c = toShort((n << 8) | C1);
				buckets.put(1, toShort(n + 1));
			} else if (gray < bucket2) {
				short n = buckets.get(2);
				c = toShort((n << 8) | C2);
				buckets.put(2, toShort(n + 1));
			} else if (gray < bucket3) {
				short n = buckets.get(3);
				c = toShort((n << 8) | C3);
				buckets.put(3, toShort(n + 1));
			} else {
				short n = buckets.get(4);
				c = toShort((n << 8) | C4);
				buckets.put(4, toShort(n + 1));
			}

			flatToColor.put(flatname, c);
		}

		return flatToColor.get(flatname);
	}
}
