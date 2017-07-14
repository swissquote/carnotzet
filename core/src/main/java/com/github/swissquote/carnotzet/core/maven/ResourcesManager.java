package com.github.swissquote.carnotzet.core.maven;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.find;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiPredicate;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.config.FileMerger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

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
	 * Extract all jar resources to a single directory with the following structure :<br>
	 * resourcesRoot/module1/...<br>
	 * resourcesRoot/module2/...
	 *
	 * @param modules the list of modules to extract
	 */
	public void extractResources(List<CarnotzetModule> modules) {
		try {
			log.debug("Extracting jars resources to [{}]", resourcesRoot);
			FileUtils.deleteDirectory(resourcesRoot.toFile());
			if (!resourcesRoot.toFile().mkdirs()) {
				throw new CarnotzetDefinitionException("Could not create directory [" + resourcesRoot + "]");
			}
			String topLevelModuleName = modules.get(0).getTopLevelModuleName();

			for (CarnotzetModule module : modules) {
				if (module.getName().equals(topLevelModuleName)
						&& topLevelModuleResourcesPath != null
						&& topLevelModuleResourcesPath.toFile().exists()) {
					FileUtils.copyDirectory(topLevelModuleResourcesPath.toFile(),
							resourcesRoot.resolve(topLevelModuleName).toFile());
				} else {
					copyModuleResources(module.getId(), getModuleResourcesPath(module));
				}
				// this is necessary to allow overriding of root level resource files such as carnotzet.properties
				moveRootLevelFilesToModuleDir(getModuleResourcesPath(module), module.getName());
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to copy module resources " + ex, ex);
		}
	}

	private void moveRootLevelFilesToModuleDir(Path moduleResourcesPath, String moduleName) throws IOException {
		Path targetDir = moduleResourcesPath.resolve(moduleName);
		if (!targetDir.toFile().mkdirs()) {
			throw new CarnotzetDefinitionException("Could not create directory [" + targetDir + "]");
		}

		Files.list(moduleResourcesPath)
				.filter(path -> path.toFile().isFile())
				.forEach(path -> {
					try {
						Files.move(path, targetDir.resolve(path.getFileName()), StandardCopyOption.ATOMIC_MOVE);
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

	/**
	 * Compute overrides and merges between module files
	 *
	 * @param modules the list of modules to resolve , ordered from leaves to top level module
	 */
	public void resolveResources(List<CarnotzetModule> modules) {
		try {
			log.debug("Resolving resources overrides and merges in [{}]", resourcesRoot);
			List<CarnotzetModule> processedModules = new ArrayList<>();
			for (CarnotzetModule module : modules) {
				mergeFiles(processedModules, module);
				overrideFiles(processedModules, module);
				processedModules.add(module);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Resolve module resources " + ex, ex);
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
			// to allow overriding root level resources (no dir with module name) such as carnotzet.properties

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
					fileMerger.merge(toMerge, mergeFile, toMerge);
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
	 *
	 * @param processedModules modules that have been processed so far
	 * @param module           new module to process
	 */
	private void overrideFiles(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {

		//going through all the files of the module in target/carnotzet folder
		find(getModuleResourcesPath(module), FIND_MAX_DEPTH, getPotentialOverridingFileFilter()).forEach(overridingFilePath -> {
			for (CarnotzetModule processedModule : processedModules) {
				Path relativePath = getModuleResourcesPath(module).relativize(overridingFilePath);
				if (!relativePath.subpath(0, 1).getFileName().toString().equals(processedModule.getName())) {
					continue;
				}
				Path toOverrideFile = getModuleResourcesPath(processedModule).resolve(relativePath);
				try {
					Files.move(overridingFilePath, toOverrideFile, StandardCopyOption.REPLACE_EXISTING);
					log.debug("Configuration file [" + toOverrideFile.getFileName().toString() + "] "
							+ "in carnotzet module [" + processedModule.getName() + "] "
							+ "is overridden by module [" + module.getName() + "]");
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialMergeFileFilter() {
		BiPredicate<Path, BasicFileAttributes> isMergeFile = (filePath, fileAttr) -> fileAttr.isRegularFile();
		isMergeFile = isMergeFile.and((filePath, fileAttr) -> filePath.toAbsolutePath().toString().endsWith(".merge"));
		return isMergeFile;
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialOverridingFileFilter() {
		return (filePath, fileAttr) -> fileAttr.isRegularFile();
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

	private ZipFile getJarFile(MavenCoordinate id) throws ZipException {
		File jarFile = Maven.configureResolver().workOffline()
				.resolve(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion())
				.withoutTransitivity().asSingleFile();
		return new ZipFile(jarFile);
	}

	public void copyModuleResources(MavenCoordinate moduleId, Path moduleResourcesPath) {
		try {
			ZipFile f = this.getJarFile(moduleId);
			f.extractAll(moduleResourcesPath.toAbsolutePath().toString());
		}
		catch (ZipException e) {
			throw new CarnotzetDefinitionException(e);
		}

	}
}
