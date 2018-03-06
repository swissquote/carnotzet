package com.github.swissquote.cartnotzet.core.docker.registry;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.swissquote.carnotzet.core.docker.registry.ImageRef;

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

}