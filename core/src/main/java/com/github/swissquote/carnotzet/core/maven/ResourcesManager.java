package com.github.swissquote.carnotzet.core.maven;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.find;

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
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

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
	private final Path expandedJars;
	private final Path resolved;
	private final Path topLevelModuleResourcesPath;
	private final ServiceLoader<FileMerger> fileMergers;

	public ResourcesManager(Path resourcesRoot, Path topLevelModuleResourcesPath) {
		this.resourcesRoot = resourcesRoot;
		this.expandedJars = resourcesRoot.resolve("expanded-jars");
		this.resolved = resourcesRoot.resolve("resolved");
		this.topLevelModuleResourcesPath = topLevelModuleResourcesPath;
		this.fileMergers = ServiceLoader.load(FileMerger.class);
	}

	public Path getModuleResourcesPath(CarnotzetModule module) {
		return resolved.resolve(module.getName());
	}

	/**
	 * Extract all jar resources to a single directory with the following structure :<br>
	 * resourcesRoot/expanded-jars/module1/...<br>
	 * resourcesRoot/expanded-jars/module2/...
	 *
	 * @param modules the list of modules to extract
	 */
	public void extractResources(List<CarnotzetModule> modules) {
		try {
			log.debug("Extracting jars resources to [{}]", resourcesRoot);
			FileUtils.deleteDirectory(resourcesRoot.toFile());
			if (!expandedJars.toFile().mkdirs()) {
				throw new CarnotzetDefinitionException("Could not create directory [" + resourcesRoot + "]");
			}
			String topLevelModuleName = modules.get(0).getTopLevelModuleName();

			for (CarnotzetModule module : modules) {
				if (module.getName().equals(topLevelModuleName)
						&& topLevelModuleResourcesPath != null
						&& topLevelModuleResourcesPath.toFile().exists()) {
					FileUtils.copyDirectory(topLevelModuleResourcesPath.toFile(),
							expandedJars.resolve(topLevelModuleName).toFile());
				} else {
					copyModuleResources(module, expandedJars.resolve(module.getName()));
				}
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to copy module resources " + ex, ex);
		}
	}

	/**
	 * Compute overrides and merges between module files in expanded jars
	 *
	 * @param modules the list of modules to resolve , ordered from leaves to top level module
	 */
	public void resolveResources(List<CarnotzetModule> modules) {
		try {
			log.debug("Resolving resources overrides and merges in [{}]", resourcesRoot);
			List<CarnotzetModule> processedModules = new ArrayList<>();
			for (CarnotzetModule module : modules) {
				copyOwnResources(module);
				mergeFiles(processedModules, module);
				overrideFiles(processedModules, module);
				processedModules.add(module);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Resolve module resources " + ex, ex);
		}
	}

	private void copyOwnResources(CarnotzetModule module) throws IOException {
		Path expandedJarPath = expandedJars.resolve(module.getName());
		Path resolvedModulePath = resolved.resolve(module.getName());
		if (!resolvedModulePath.toFile().mkdirs()) {
			throw new CarnotzetDefinitionException("Could not create directory " + resolvedModulePath);
		}
		if (expandedJarPath.resolve(module.getName()).toFile().exists()) {
			FileUtils.copyDirectory(expandedJarPath.resolve(module.getName()).toFile(), resolvedModulePath.toFile());
		}
		// copy all regular files at the root of the expanded jar (such as carnotzet.properties)
		find(expandedJarPath, 1, (p, a) -> a.isRegularFile()).forEach(source -> {
			try {
				Files.copy(source, resolvedModulePath.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		// copy all directories from the expanded jar recursively
		find(expandedJarPath, 1, (p, a) -> a.isDirectory()).forEach(source -> {
			try {
				FileUtils.copyDirectory(source.toFile(), resolvedModulePath.resolve(source.getFileName()).toFile());
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Merge the content of files in processed modules and a given module
	 */
	private void mergeFiles(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {
		String moduleName = module.getName();
		// list all resource modules  in that module
		// we use an old style array to eventually swap elements
		Path[] resourceModules = find(expandedJars.resolve(moduleName), 1, (p, a) -> a.isDirectory()).toArray(Path[]::new);
		// start at 1 because 0 is the module itself
		for (int i = 1; i < resourceModules.length; i++) {
			Path tempResourceModule = resourceModules[i];
			String tempResourceModuleName = tempResourceModule.getName(tempResourceModule.getNameCount() - 1).toString();
			if ("common".equals(tempResourceModuleName) && i < resourceModules.length - 1) {
				// not the last element of the array
				// we swap common with the last one and continue
				resourceModules[i] = resourceModules[resourceModules.length - 1];
				resourceModules[resourceModules.length - 1] = tempResourceModule;
				tempResourceModule = resourceModules[i];
			}

			final Path resourceModule = tempResourceModule;
			final String resourceModuleName = resourceModule.getName(resourceModule.getNameCount() - 1).toString();
			CarnotzetModule processedModule = processedModules.stream()
					.filter(m -> m.getName().equals(resourceModuleName))
					.findFirst().orElse(null);
			if (processedModule == null && !"common".equals(resourceModuleName)) {
				continue;
			}

			find(resourceModule, FIND_MAX_DEPTH, getPotentialMergeFileFilter()).forEach(sourceFile -> {
				final Path relativePath = Paths.get(resourceModule.relativize(sourceFile).toString().replace(".merge", ""));

				Stream<Path> destinationModules;
				if ("common".equals(resourceModuleName)) {
					try {
						// .skip(1) because first one is top folder
						destinationModules = find(resolved, 1, (p, a) -> a.isDirectory()).skip(1);
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				} else {
					destinationModules = Stream.of(resolved.resolve(resourceModuleName));
				}

				destinationModules.forEach(destinationModule -> {
					Path destinationFile = destinationModule.resolve(relativePath);
					if (exists(destinationFile)) {
						FileMerger fileMerger = getFileMerger(destinationFile);
						if (fileMerger == null) {
							log.error("Found [{}] file in module [{}] but there is no registered FileMerger to merge it with [{}]. Merge file will be ignored",
									sourceFile, resourceModuleName, destinationFile);
							return;
						}
						fileMerger.merge(destinationFile, sourceFile, destinationFile);
						log.debug("Merged [{}] from [{}] into [{}]",
								sourceFile, resourceModuleName, destinationFile);

					} else {
						log.warn("Found [{}] in module [{}] but there is no file to merge it with in module [{}]",
								sourceFile, resourceModuleName, destinationModule.getFileName());
						log.debug("Merge target file would have been [{}]",
								destinationFile);
					}
				});
			});
		}
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
		String moduleName = module.getName();
		// list all resource modules  in that module
		// we use an old style array to eventually swap elements
		Path[] resourceModules = find(expandedJars.resolve(moduleName), 1, (p, a) -> a.isDirectory()).toArray(Path[]::new);
		// start at 1 because 0 is the module itself
		for (int i = 1; i < resourceModules.length; i++) {
			Path tempResourceModule = resourceModules[i];
			String tempResourceModuleName = tempResourceModule.getName(tempResourceModule.getNameCount() - 1).toString();
			if ("common".equals(tempResourceModuleName) && i < resourceModules.length - 1) {
				// not the last element of the array
				// we swap common with the last one and continue
				resourceModules[i] = resourceModules[resourceModules.length - 1];
				resourceModules[resourceModules.length - 1] = tempResourceModule;
				tempResourceModule = resourceModules[i];
			}

			final Path resourceModule = tempResourceModule;
			final String resourceModuleName = resourceModule.getName(resourceModule.getNameCount() - 1).toString();
			CarnotzetModule processedModule = processedModules.stream()
					.filter(m -> m.getName().equals(resourceModuleName))
					.findFirst().orElse(null);
			if (processedModule == null && !"common".equals(resourceModuleName)) {
				continue;
			}

			find(resourceModule, FIND_MAX_DEPTH, getPotentialOverridingFileFilter()).forEach(sourceFile -> {
				final Path relativePath = resourceModule.relativize(sourceFile);

				Stream<Path> destinationModules;
				if ("common".equals(resourceModuleName)) {
					try {
						// .skip(1) because first one is top folder
						destinationModules = find(resolved, 1, (p, a) -> a.isDirectory()).skip(1);
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				} else {
					destinationModules = Stream.of(resolved.resolve(resourceModuleName));
				}

				destinationModules.forEach(destinationModule -> {
					Path destinationFile = destinationModule.resolve(relativePath);
					try {
						if (!destinationFile.getParent().toFile().exists() && !destinationFile.getParent().toFile().mkdirs()) {
							throw new IOException("Unable to create directory " + destinationFile.getParent());
						}
						Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
						log.debug("Overridden [{}] with [{}]",
								destinationFile, sourceFile);
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			});
		}
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialMergeFileFilter() {
		BiPredicate<Path, BasicFileAttributes> isRegularFile = (filePath, fileAttr) -> fileAttr.isRegularFile();
		return isRegularFile.and((filePath, fileAttr) -> filePath.toAbsolutePath().toString().endsWith(".merge"));
	}

	private BiPredicate<Path, BasicFileAttributes> getPotentialOverridingFileFilter() {
		BiPredicate<Path, BasicFileAttributes> isRegularFile = (filePath, fileAttr) -> fileAttr.isRegularFile();
		return isRegularFile.and((filePath, fileAttr) -> !filePath.toAbsolutePath().toString().endsWith(".merge"));
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

	private void copyModuleResources(CarnotzetModule module, Path moduleResourcesPath) {
		try {
			ZipFile f = new ZipFile(module.getJarPath().toFile());
			f.extractAll(moduleResourcesPath.toAbsolutePath().toString());
		}
		catch (ZipException e) {
			throw new CarnotzetDefinitionException(e);
		}

	}
}
