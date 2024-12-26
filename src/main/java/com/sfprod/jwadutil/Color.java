package com.sfprod.jwadutil;

public record Color(int r, int g, int b) {

	int getRGB() {
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	double gray() {
		return r * 0.299 + g * 0.587 + b * 0.114;
	}

	int calculateDistance(Color that) {
		int distr = this.r - that.r;
		int distg = this.g - that.g;
		int distb = this.b - that.b;

		return distr * distr + distg * distg + distb * distb;
	}
}
