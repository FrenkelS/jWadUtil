package com.sfprod.jwadutil;

import static com.sfprod.utils.NumberUtils.toByte;
import static com.sfprod.utils.NumberUtils.toInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.sfprod.utils.ByteBufferUtils;

abstract class WadProcessorLimitedColors extends WadProcessor {

	private Map<Integer, List<Integer>> availableColorsShuffleMap;

	WadProcessorLimitedColors(String title, ByteOrder byteOrder, WadFile wadFile) {
		super(title, byteOrder, wadFile);
	}

	protected void fillAvailableColorsShuffleMap(List<Color> availableColors) {
		Map<Integer, List<Integer>> shuffleMap = new HashMap<>();
		for (int i = 0; i < 256; i++) {
			List<Integer> sameColorList = new ArrayList<>();
			Color availableColor = availableColors.get(i);
			for (int j = 0; j < 256; j++) {
				Color otherColor = availableColors.get(j);
				if (availableColor.equals(otherColor)) {
					sameColorList.add(j);
				}
			}

			shuffleMap.put(i, sameColorList);
		}

		this.availableColorsShuffleMap = shuffleMap;
	}

	protected void changePalettePicture(Lump lump, Function<Byte, Byte> colorConvertFunction) {
		ByteBuffer dataByteBuffer = lump.dataAsByteBuffer();
		short width = dataByteBuffer.getShort();
		dataByteBuffer.getShort(); // height
		dataByteBuffer.getShort(); // leftoffset
		dataByteBuffer.getShort(); // topoffset

		List<Integer> columnofs = new ArrayList<>();
		for (int columnof = 0; columnof < width; columnof++) {
			columnofs.add(dataByteBuffer.getInt());
		}

		for (int columnof = 0; columnof < width; columnof++) {
			int index = columnofs.get(columnof);
			byte topdelta = lump.data()[index];
			index++;
			while (topdelta != -1) {
				byte lengthByte = lump.data()[index];
				index++;
				int length = toInt(lengthByte);
				for (int i = 0; i < length + 2; i++) {
					lump.data()[index] = colorConvertFunction.apply(lump.data()[index]);
					index++;
				}
				topdelta = lump.data()[index];
				index++;
			}
		}
	}

	@Override
	void processColormap() {
		List<Byte> colormapInvulnerability = createColormapInvulnerability();

		for (int gamma = 0; gamma < 6; gamma++) {
			Lump colormapLump;
			if (gamma == 0) {
				colormapLump = wadFile.getLumpByName("COLORMAP");
			} else {
				colormapLump = new Lump("COLORMP" + gamma, new byte[34 * 256], ByteBufferUtils.DONT_CARE);
				wadFile.addLump(colormapLump);
			}

			int index = 0;

			// colormap 0-31 from bright to dark
			int colormap = 0 - (int) (gamma * (32.0 / 5));
			for (int i = 0; i < 32; i++) {
				List<Byte> colormapBytes = createColormap(colormap);
				for (byte b : colormapBytes) {
					colormapLump.data()[index] = b;
					index++;
				}
				colormap++;
			}

			// colormap 32 invulnerability powerup
			for (int i = 0; i < 256; i++) {
				colormapLump.data()[index] = colormapInvulnerability.get(i);
				index++;
			}

			// colormap 33 all black
			for (int i = 0; i < 256; i++) {
				colormapLump.data()[index] = 0;
				index++;
			}
		}
	}

	protected abstract List<Byte> createColormapInvulnerability();

	private List<Byte> createColormap(int colormap) {
		List<Byte> result = new ArrayList<>();

		if (colormap == 0) {
			for (int i = 0; i < 256; i++) {
				result.add(toByte(i));
			}
		} else {
			int c = 32 - colormap;

			List<Color> availableColors = getAvailableColors();

			for (Color color : availableColors) {
				int r = Math.clamp((long) Math.sqrt(color.r() * color.r() * c / 32), 0, 255);
				int g = Math.clamp((long) Math.sqrt(color.g() * color.g() * c / 32), 0, 255);
				int b = Math.clamp((long) Math.sqrt(color.b() * color.b() * c / 32), 0, 255);

				byte closestColor = calculateClosestColor(new Color(r, g, b));
				byte shuffledColor = shuffleColor(closestColor);
				result.add(shuffledColor);
			}
		}

		return result;
	}

	private byte calculateClosestColor(Color c) {
		int closestColor = -1;
		int closestDist = Integer.MAX_VALUE;

		List<Color> availableColors = getAvailableColors();

		for (int i = 0; i < 256; i++) {
			int dist = c.calculateDistance(availableColors.get(i));
			if (dist == 0) {
				// perfect match
				closestColor = i;
				break;
			}

			if (dist < closestDist) {
				closestDist = dist;
				closestColor = i;
			}
		}

		return toByte(closestColor);
	}

	protected byte shuffleColor(byte b) {
		List<Integer> list = availableColorsShuffleMap.get(toInt(b));
		return list.get(random.nextInt(list.size())).byteValue();
	}
}
