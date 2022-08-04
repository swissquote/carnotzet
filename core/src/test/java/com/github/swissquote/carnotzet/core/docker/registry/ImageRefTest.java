package com.github.swissquote.carnotzet.core.docker.registry;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class ImageRefTest {

	@Test
	public void remoteImageWithUserName() {
		ImageRef ref = new ImageRef("docker.bank.swissquote.ch/djoaquim/image:3.3");
		assertEquals("djoaquim/image", ref.getImageName());
	}

	@Test
	public void remoteImage() {
		ImageRef ref = new ImageRef("docker.bank.swissquote.ch/image:3.3");
		assertEquals("image", ref.getImageName());
	}

	@Test
	public void localImageWithUserName() {
		ImageRef ref = new ImageRef("djoaquim/image:3.3");
		assertEquals("djoaquim/image", ref.getImageName());
	}

	@Test
	public void localImage() {
		ImageRef ref = new ImageRef("image:3.3");
		assertEquals("image", ref.getImageName());
	}

	@Test
	public void testCache() {
		Map<ImageRef, ImageMetaData> cache = new ConcurrentHashMap<>();
		ImageRef ref1 = new ImageRef("image:1");
		ImageRef ref2 = new ImageRef("image:1");
		ImageMetaData data = new ImageMetaData(null, null);
		cache.put(ref1, data);
		cache.put(ref2, data);

		assertEquals(1, cache.size());
	}

}