package com.sfprod.jwadutil;

public record Color(int r, int g, int b) {

	public Color {
		assert 0x00 <= r && r <= 0xff;
		assert 0x00 <= g && g <= 0xff;
		assert 0x00 <= b && b <= 0xff;
	}

	public int getRGB() {
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	public double gray() {
		return r * 0.299 + g * 0.587 + b * 0.114;
	}

	public boolean isGrayish() {
		return r == g && g == b;
	}

	public Color invert() {
		return new Color(0xff - r, 0xff - g, 0xff - b);
	}

	public int calculateDistance(Color that) {
		int distr = this.r - that.r;
		int distg = this.g - that.g;
		int distb = this.b - that.b;

		return distr * distr + distg * distg + distb * distb;
	}

	Color blendColors(Color that) {
		return blendColors(this, that);
	}

	public static Color blendColors(Color... colors) {
		assert colors.length != 0;

		int rSum = 0;
		int gSum = 0;
		int bSum = 0;
		for (Color color : colors) {
			rSum += color.r * color.r;
			gSum += color.g * color.g;
			bSum += color.b * color.b;
		}
		int rBlended = (int) Math.sqrt(rSum / colors.length);
		int gBlended = (int) Math.sqrt(gSum / colors.length);
		int bBlended = (int) Math.sqrt(bSum / colors.length);
		return new Color(rBlended, gBlended, bBlended);
	}
}
