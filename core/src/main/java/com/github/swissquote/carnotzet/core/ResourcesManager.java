package com.github.swissquote.carnotzet.core;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.find;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import com.github.swissquote.carnotzet.core.config.FileMerger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ResourcesManager {

	private static final int FIND_MAX_DEPTH = 200;

	@Getter
	private final Path resourcesRoot;
	private final Path topLevelModuleResourcesPath;
	private final ServiceLoader<FileMerger> fileMergers;

	public ResourcesManager(Path resourcesRoot, Path topLevelModuleResourcesPath) {
		this.resourcesRoot = resourcesRoot;
		this.topLevelModuleResourcesPath = topLevelModuleResourcesPath;
		this.fileMergers = ServiceLoader.load(FileMerger.class);
	}

	public Path getModuleResourcesPath(CarnotzetModule module) {
		return resourcesRoot.resolve(module.getName());
	}

	/**
	 * Extract the files of all module this one depends on, override and merge files
	 */
	public void resolveResources(List<CarnotzetModule> modules, BiConsumer<MavenCoordinate, Path> copyResources) {
		try {
			log.debug("Resolving carnotzet resources into [{}]", resourcesRoot);
			FileUtils.deleteDirectory(resourcesRoot.toFile());
			if (!resourcesRoot.toFile().mkdirs()) {
				throw new CarnotzetDefinitionException("Could not create directory [" + resourcesRoot + "]");
			}
			String topLevelModuleName = modules.get(0).getTopLevelModuleName();
			List<CarnotzetModule> processedModules = new ArrayList<>();

			for (CarnotzetModule module : modules) {
				if (module.getName().equals(topLevelModuleName)
						&& topLevelModuleResourcesPath != null
						&& topLevelModuleResourcesPath.toFile().exists()) {
					FileUtils.copyDirectory(topLevelModuleResourcesPath.toFile(),
							resourcesRoot.resolve(topLevelModuleName).toFile());
				} else {
					copyResources.accept(module.getId(), getModuleResourcesPath(module));
				}
				mergeFiles(processedModules, module);
				overrideFiles(processedModules, module);

				processedModules.add(module);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to copy module resources " + ex, ex);
		}
	}

	/**
	 * Merge the content of files in processed modules and a given module
	 */
	private void mergeFiles(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {
		find(getModuleResourcesPath(module), FIND_MAX_DEPTH, getPotentialMergeFileFilter()).forEach(mergeFile -> {
			String mergeFileModuleName = getModuleResourcesPath(module).relativize(mergeFile).getName(0).toString();

			CarnotzetModule processedModule = processedModules.stream()
					.filter(m -> m.getName().equals(mergeFileModuleName))
					.findFirst().orElse(null);

			if (processedModule == null) {
				log.warn("Module [" + mergeFileModuleName + "] not found in processed modules, "
						+ "merge file [" + mergeFile + "] will be ignored");
				try {
					delete(mergeFile);
				}
				catch (IOException e) {
					throw new CarnotzetDefinitionException(e);
				}
				return;
			}

			Path relativePath = getModuleResourcesPath(module).relativize(mergeFile);
			relativePath = Paths.get(relativePath.toString().replace(".merge", ""));
			Path toMerge = getModuleResourcesPath(processedModule).resolve(relativePath);
			if (exists(toMerge)) {
				try {
					FileMerger fileMerger = getFileMerger(toMerge);
					if (fileMerger == null) {
						log.error("Found [" + mergeFile + "] file in module [" + module.getName()
								+ "] but there is no registered FileMerger to merge it with [" + toMerge + "]. Merge file will be ignored");
						delete(mergeFile);
						return;
					}
					fileMerger.merge(toMerge, mergeFile, getModuleResourcesPath(module).resolve(relativePath));
					// File to be used is in the resources of the module passed in parameter
					delete(toMerge);
					delete(mergeFile);
					log.debug("Merged configuration file [" + toMerge.getFileName() + "] for "
							+ "service [" + processedModule.getName() + "] "
							+ "with [" + mergeFile.getFileName() + "] in [" + module.getName() + "]");
				}
				catch (IOException e) {
					throw new CarnotzetDefinitionException(e);
				}
			} else {
				log.warn("Found [" + mergeFile.getFileName() + "] in module [" + module.getName() + "]"
						+ " but there is no file to merge it with in module [" + mergeFileModuleName + "]");
			}

		});
	}

	/**
	 * Override files in processed modules by files in a given module <br>
	 * In effect, it deletes a file from the resources of
	 * the processed module if it is also present in the resources of the given module
	 */
	private void overrideFiles(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {

		//going through all the files of the module in target/carnotzet folder
		find(getModuleResourcesPath(module), FIND_MAX_DEPTH, getPotentialOverridingFileFilter()).forEach(overridingFilePath -> {
			for (CarnotzetModule processedModule : processedModules) {
				Path relativePath = getModuleResourcesPath(module).relativize(overridingFilePath);
				Path toOverrideFile = getModuleResourcesPath(processedModule).resolve(relativePath);
				if (exists(toOverrideFile)) {
					try {
						// File to be used is in the resources of the module passed in parameter
						log.debug("Configuration file [" + toOverrideFile.getFileName().toString() + "] "
								+ "in carnotzet module [" + processedModule.getName() + "] "
								+ "is overridden by the same file in the module of [" + module.getName() + "]");
						delete(toOverrideFile);
					}
					catch (IOException e) {
						throw new CarnotzetDefinitionException(e);
					}
				}
			}
		});
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialMergeFileFilter() {
		BiPredicate<Path, BasicFileAttributes> isMergeFile = (filePath, fileAttr) -> fileAttr.isRegularFile();
		isMergeFile = isMergeFile.and((filePath, fileAttr) -> filePath.toAbsolutePath().toString().endsWith(".merge"));
		isMergeFile = isMergeFile.and((filePath, fileAttr) ->
				filePath.toAbsolutePath().toString().contains("/files/") || filePath.toAbsolutePath().toString().contains("/env/"));
		return isMergeFile;
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialOverridingFileFilter() {
		BiPredicate<Path, BasicFileAttributes> isInFilesOrEnv = (filePath, fileAttr) -> fileAttr.isRegularFile();
		isInFilesOrEnv = isInFilesOrEnv.and((filePath, fileAttr) ->
				filePath.toAbsolutePath().toString().contains("/files/") || filePath.toAbsolutePath().toString().contains("/env/"));
		return isInFilesOrEnv;
	}

	/**
	 * gets the appropriate file merger for a given file type
	 **/
	private FileMerger getFileMerger(Path file) {
		for (FileMerger merger : this.fileMergers) {
			if (merger.knowsHowToMerge(file)) {
				return merger;
			}
		}
		return null;
	}

}
