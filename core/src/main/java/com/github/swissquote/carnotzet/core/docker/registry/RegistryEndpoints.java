package com.github.swissquote.carnotzet.core.docker.registry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Docker registry V2 endpoints
 */
@Path("v2")
public interface RegistryEndpoints {

	@GET
	@Path("{name}/manifests/{reference}")
	@Produces("application/vnd.docker.distribution.manifest.v2+json")
	DistributionManifestV2 getDistributionManifest(@PathParam("name") String name, @PathParam("reference") String reference);

	@GET
	@Path("{name}/manifests/{reference}")
	@Produces("application/vnd.docker.container.image.v1+json")
	ContainerImageV1 getImageManifest(@PathParam("name") String name, @PathParam("reference") String reference);

	@GET
	@Path("{name}/blobs/{digest}")
		// Could not be an imageconfig if a tgz layer is requested.
	DistributionManifestConfig getLayer(@PathParam("name") String name, @PathParam("digest") String digest);

}
