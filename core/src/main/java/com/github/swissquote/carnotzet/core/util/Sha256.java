package com.github.swissquote.carnotzet.core.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import lombok.SneakyThrows;

public final class Sha256 {
	public static final int RADIX = 16;
	public static final int PAD = 32;
	public static final String ALGORITHM = "SHA-256";

	private Sha256() {
	}


	@SneakyThrows
	public static String getSHA(String input) {
		// Static getInstance method is called with hashing SHA
		MessageDigest md = MessageDigest.getInstance(ALGORITHM);
		// digest() method called
		// to calculate message digest of an input
		// and return array of byte
		return toHexString(md.digest(input.getBytes(StandardCharsets.UTF_8)));
	}

	private static String toHexString(byte[] hash) {
		// Convert byte array into signum representation
		BigInteger number = new BigInteger(1, hash);
		// Convert message digest into hex value
		StringBuilder hexString = new StringBuilder(number.toString(RADIX));
		// Pad with leading zeros
		while (hexString.length() < PAD) {
			hexString.insert(0, '0');
		}
		return hexString.toString();
	}
}
