package com.github.swissquote.carnotzet.core.runtime.api;

import java.time.Instant;

import javax.annotation.Nullable;

import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.docker.registry.ImageMetaData;

import lombok.NonNull;

public interface PullPolicy {

	/**
	 * Always pulls registry images, disregarding the state of the local image. This policy will override any local image for that module.
	 */
	PullPolicy ALWAYS = new PullPolicy() {
		@Override
		public boolean shouldPullImage(CarnotzetModule module,
				@Nullable Instant localImageCreated,
				@Nullable ImageMetaData registryImageMetadata) {
			return true;
		}

		@Override
		public boolean requiresLocalMetadata() {
			return false;
		}

		@Override
		public boolean requiresRegistryMetadata() {
			return false;
		}
	};

	/**
	 * Only pulls if there is no local image for the carnotzet module. This policy will fill in missing images, but will never override any
	 * local image/tags.
	 */
	PullPolicy IF_LOCAL_IMAGE_ABSENT = new PullPolicy() {
		@Override
		public boolean shouldPullImage(CarnotzetModule module,
				@Nullable Instant localImageCreated,
				@Nullable ImageMetaData registryImageMetadata) {
			return localImageCreated == null;
		}

		@Override
		public boolean requiresLocalMetadata() {
			return true;
		}

		@Override
		public boolean requiresRegistryMetadata() {
			return false;
		}
	};

	/**
	 * Only pulls if the image in the remote registry has been created more recently than the local image.
	 * - If there is no local image, the registry image will be pulled.
	 * - If there is no matching image on the registry but there is a local image, nothing will be pulled and no errors will occur
	 * - If there is no matching image locally or on the registry, then an error will occur.
	 */
	PullPolicy IF_REGISTRY_IMAGE_NEWER = new PullPolicy() {
		@Override
		public boolean shouldPullImage(CarnotzetModule module, @Nullable Instant localImageCreated,
				@Nullable ImageMetaData registryImageMetadata) {
			if (localImageCreated == null) {
				// No local image
				//  => We must always pull. It might cause failures if the image does not exist on the registry but that is on purpose
				// because the user is doing something wrong if that happens.
				return true;
			}
			if (registryImageMetadata == null || registryImageMetadata.getContainerImage().getCreated() == null) {
				// Image doesn't exist on remote registry, or we cannot determine the timestamp
				//  => There is nothing to pull
				return false;
			}

			// Pull if registry image is newer.
			return localImageCreated.isBefore(registryImageMetadata.getContainerImage().getCreated().toInstant());
		}

		@Override
		public boolean requiresLocalMetadata() {
			return true;
		}

		@Override
		public boolean requiresRegistryMetadata() {
			return true;
		}
	};

	/**
	 * Checks if the registry image required by the specified Carnotzet module must be pulled.
	 *
	 * @param module                the module whose image might need to be pulled
	 * @param localImageCreated     the timestamp at which the matching local image was created. null if there is no matching local image or
	 *                              if requiresLocalMetadata() returned false.
	 * @param registryImageMetadata the full metadata of the matching image on the remote registry. null if there is no such image on the
	 *                              registry, or if requiresRegistryMetadata() returned false.
	 */
	boolean shouldPullImage(@NonNull CarnotzetModule module,
			@Nullable Instant localImageCreated,
			@Nullable ImageMetaData registryImageMetadata);

	/**
	 * Indicates if this policy requires the metadata of the local image to take its decision. If this method returns true, the metadata will
	 * be fetched and passed to shouldPullImage(). If false, null will be passed to shouldPullImage().
	 *
	 * @return true to fetch metadata and pass it to shouldPullImage(), false to skip and pass null
	 */
	boolean requiresLocalMetadata();

	/**
	 * Indicates if this policy requires the metadata of the registry image to take its decision. If this method returns true, the metadata will
	 * be fetched and passed to shouldPullImage(). If false, null will be passed to shouldPullImage().
	 *
	 * @return true to fetch metadata and pass it to shouldPullImage(), false to skip and pass null
	 */
	boolean requiresRegistryMetadata();
}
