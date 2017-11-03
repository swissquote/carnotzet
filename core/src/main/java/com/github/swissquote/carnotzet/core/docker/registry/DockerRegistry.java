package com.github.swissquote.carnotzet.core.docker.registry;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DockerRegistry {

	public static final DockerRegistry INSTANCE = new DockerRegistry();

	private final DockerConfig config = DockerConfig.fromEnv();
	private final Map<String, RegistryEndpoints> proxyClients = new HashMap<>();

	public ImageMetaData getImageMetaData(ImageRef imageRef) {
		DistributionManifestV2 di = getDistributionManifest(imageRef);
		ContainerImageV1 im = getImageManifest(imageRef, di);
		return new ImageMetaData(di, im);
	}

	private DistributionManifestV2 getDistributionManifest(ImageRef imageRef) {
		try {
			RegistryEndpoints registry = getRegistryClient(imageRef);
			return registry.getDistributionManifest(imageRef.getImageName(), imageRef.getTag());
		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not fetch distribution manifest of [" + imageRef + "]", e);
		}
	}

	private ContainerImageV1 getImageManifest(ImageRef imageRef, DistributionManifestV2 distributionManifest) {
		if (distributionManifest.getConfig() == null || distributionManifest.getConfig().getDigest() == null) {
			throw new CarnotzetDefinitionException("Distribution manifest of images [" + imageRef + " does not contain digest of image");
		}

		try {
			RegistryEndpoints registry = getRegistryClient(imageRef);
			return registry.getImageManifest(imageRef.getImageName(), distributionManifest.getConfig().getDigest());
		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not fetch config manifest of image [" + imageRef + "]", e);
		}
	}

	private RegistryEndpoints getRegistryClient(ImageRef imageRef) {
		if (!proxyClients.containsKey(imageRef.getRegistryUrl())) {

			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());

			Client client = ClientBuilder.newClient()
					.register(new JacksonJaxbJsonProvider(mapper, new Annotations[] {Annotations.JACKSON}))
					.register(JacksonFeature.class);
			String auth = config.getAuthFor(imageRef.getRegistryName());
			if (auth != null) {
				String[] credentials = new String(Base64.getDecoder().decode(auth), StandardCharsets.UTF_8).split(":");
				client.register(HttpAuthenticationFeature.basicBuilder().credentials(credentials[0], credentials[1]));
				{
				}
				WebTarget webTarget = client.target(imageRef.getRegistryUrl());
				proxyClients.put(imageRef.getRegistryUrl(), WebResourceFactory.newResource(RegistryEndpoints.class, webTarget));
			}
		}
		return proxyClients.get(imageRef.getRegistryUrl());
	}

}
