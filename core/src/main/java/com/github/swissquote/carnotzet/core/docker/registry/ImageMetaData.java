package com.github.swissquote.carnotzet.core.docker.registry;

import lombok.Value;

@Value
public class ImageMetaData {
	private final DistributionManifestV2 distributionManifest;
	private final ContainerImageV1 containerImage;
}
