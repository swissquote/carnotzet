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
				// First copy all of the resources in the .jar of the module
				copyModuleResources(module, expandedJars.resolve(module.getName()));

				// If the module is the top level one, then we attempt to overwrite the files from the jar
				// with fresher files coming directly from the source resource folder
				if (module.getName().equals(topLevelModuleName)
						&& topLevelModuleResourcesPath != null
						&& topLevelModuleResourcesPath.toFile().exists()) {
					FileUtils.copyDirectory(topLevelModuleResourcesPath.toFile(),
							expandedJars.resolve(topLevelModuleName).toFile());
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
				processedModules.add(module);
				copyOwnResources(processedModules, module);
				mergeFiles(processedModules, module);
				overrideFiles(processedModules, module);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Resolve module resources " + ex, ex);
		}
	}

	/**
	 * Copies the resources from the specified module that affects itself. Resources that affect dependencies of the
	 * specified module are not copied.
	 */
	private void copyOwnResources(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {
		Path expandedJarPath = expandedJars.resolve(module.getName());
		Path resolvedModulePath = resolved.resolve(module.getName());
		if (!resolvedModulePath.toFile().mkdirs()) {
			throw new CarnotzetDefinitionException("Could not create directory " + resolvedModulePath);
		}

		// copy all regular files at the root of the expanded jar (such as carnotzet.properties)
		// copy all directories that do not reconfigure another module from the expanded jar recursively
		Files.find(expandedJarPath, 1, isRegularFile().or(nameMatchesModule(processedModules).negate()))
				.forEach(source -> {
					try {
						if (Files.isRegularFile(source)) {
							Files.copy(source, resolvedModulePath.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
						} else if (Files.isDirectory(source)) {
							FileUtils.copyDirectory(source.toFile(), resolvedModulePath.resolve(source.getFileName()).toFile());
						}
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

	/**
	 * Merge the content of files in processed modules and a given module. Note that when this method is called, all the dependencies
	 * of the specified module have been fully processed, and their currently resolved files are available in ${resolved}/${dep}.
	 */
	private void mergeFiles(List<CarnotzetModule> processedModules, CarnotzetModule module) throws IOException {
		Path moduleExpandedJarPath = expandedJars.resolve(module.getName());

		//going through all the files of the module in target/carnotzet folder
		find(moduleExpandedJarPath, FIND_MAX_DEPTH, isPotentialMerge()).forEach(mergingFilePath -> {

			Path relativePath = Paths.get(moduleExpandedJarPath.relativize(mergingFilePath).toString().replace(".merge", ""));
			Path mergedModulePath = relativePath.subpath(0, 1).getFileName();
			if (mergedModulePath == null) {
				// should not happen
				return;
			}
			String mergedModuleName = mergedModulePath.toString(); // ${mergedModule}
			processedModules.stream()
					.filter(m -> m.getName().equals(mergedModuleName))
					.findFirst()
					.ifPresent(mergedModule -> {
						Path toMergeFile = resolved.resolve(relativePath); // ${resolved}/${mergedModule}/path/to/file
						FileMerger fileMerger = getFileMerger(toMergeFile);
						if (fileMerger == null) {
							log.error("Found [{}] file in module [{}] but there is no registered FileMerger to merge"
											+ " it with [{}]. Merge file will be ignored",
									mergingFilePath, module.getName(), toMergeFile);
							return;
						}
						if (!exists(toMergeFile)) {
							Path toMergeParent = toMergeFile.getParent();
							if (toMergeParent == null) {
								return;
							}
							try {
								if (!toMergeParent.toFile().exists() && !toMergeParent.toFile().mkdirs()) {
									throw new IOException("Unable to create directory " + toMergeFile.getParent());
								}
								Files.createFile(toMergeFile);
							}
							catch (IOException e) {
								throw new UncheckedIOException(e);
							}
							log.debug("Created empty file [{}]", toMergeFile);
						}
						fileMerger.merge(toMergeFile, mergingFilePath, toMergeFile);
						log.debug("Merged [{}] from [{}] into [{}]", mergingFilePath, mergedModule.getName(), toMergeFile);
					});
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
		Path moduleExpandedJarPath = expandedJars.resolve(module.getName());

		//going through all the files of the module in target/carnotzet folder
		find(moduleExpandedJarPath, FIND_MAX_DEPTH, isPotentialOverride()).forEach(overridingFilePath -> {

			Path relativePath = moduleExpandedJarPath.relativize(overridingFilePath); // ${overridenModule}/path/to/file
			Path overriddenModulePath = relativePath.subpath(0, 1).getFileName();
			if (overriddenModulePath == null) {
				// should not happen
				return;
			}
			String overriddenModuleName = overriddenModulePath.toString(); // ${overridenModule}
			processedModules.stream()
					.filter(m -> m.getName().equals(overriddenModuleName))
					.findFirst()
					.ifPresent(overriddenModule -> {
						Path toOverrideFile = resolved.resolve(relativePath); // ${resolved}/${overrideModule}/path/to/file
						Path toOverrideParent = toOverrideFile.getParent();
						if (toOverrideParent == null) {
							return;
						}
						try {
							if (!toOverrideParent.toFile().exists() && !toOverrideParent.toFile().mkdirs()) {
								throw new IOException("Unable to create directory " + toOverrideFile.getParent());
							}
							Files.copy(overridingFilePath, toOverrideFile, StandardCopyOption.REPLACE_EXISTING);
							log.debug("Overridden [" + toOverrideFile.getFileName() + "] "
									+ "in [" + overriddenModule.getName() + "] "
									+ "with file from [" + module.getName() + "]");
						}
						catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		});
	}

	private BiPredicate<Path, BasicFileAttributes> isPotentialMerge() {
		return isRegularFile().and(nameEndsWithMerge());
	}

	private BiPredicate<Path, BasicFileAttributes> isPotentialOverride() {
		return isRegularFile().and(nameEndsWithMerge().negate());
	}

	private BiPredicate<Path, BasicFileAttributes> isRegularFile() {
		return (filePath, fileAttr) -> fileAttr.isRegularFile();
	}

	private BiPredicate<Path, BasicFileAttributes> nameEndsWithMerge() {
		return (filePath, fileAttr) -> filePath.toAbsolutePath().toString().endsWith(".merge");
	}

	private BiPredicate<Path, BasicFileAttributes> nameMatchesModule(List<CarnotzetModule> modules) {
		return (filePath, fileAttr) -> {
			Path fileName = filePath.getFileName();
			if (fileName == null) {
				// Should not happen
				return false;
			}
			return modules.stream().anyMatch(m -> m.getName().equals(fileName.toString()));
		};
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
