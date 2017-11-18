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
		find(expandedJars.resolve(module.getName()), FIND_MAX_DEPTH, getPotentialMergeFileFilter()).forEach(mergeFile -> {
			String mergeFileModuleName = expandedJars.resolve(module.getName()).relativize(mergeFile).getName(0).toString();

			CarnotzetModule processedModule = processedModules.stream()
					.filter(m -> m.getName().equals(mergeFileModuleName))
					.findFirst().orElse(null);

			if (processedModule == null) {
				log.warn("Module [" + mergeFileModuleName + "] not found in processed modules, "
						+ "merge file [" + mergeFile + "] will be ignored");
				return;
			}

			Path relativePath = expandedJars.resolve(module.getName()).relativize(mergeFile);
			relativePath = Paths.get(relativePath.toString().replace(".merge", ""));
			Path toMerge = resolved.resolve(relativePath); // copied as own resource
			if (exists(toMerge)) {

				FileMerger fileMerger = getFileMerger(toMerge);
				if (fileMerger == null) {
					log.error("Found [" + mergeFile + "] file in module [" + module.getName()
							+ "] but there is no registered FileMerger to merge it with [" + toMerge + "]. Merge file will be ignored");
					return;
				}
				fileMerger.merge(toMerge, mergeFile, toMerge);
				log.debug("Merged [" + toMerge.getFileName() + "] for "
						+ "[" + processedModule.getName() + "] "
						+ "with .merge file from [" + module.getName() + "]");

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
		find(expandedJars.resolve(module.getName()), FIND_MAX_DEPTH, getPotentialOverridingFileFilter()).forEach(overridingFilePath -> {
			for (CarnotzetModule processedModule : processedModules) {
				Path relativePath = expandedJars.resolve(module.getName()).relativize(overridingFilePath);
				if (!relativePath.subpath(0, 1).getFileName().toString().equals(processedModule.getName())) {
					continue;
				}
				Path toOverrideFile = resolved.resolve(relativePath);
				try {
					if (!toOverrideFile.getParent().toFile().exists() && !toOverrideFile.getParent().toFile().mkdirs()) {
						throw new IOException("Unable to create directory " + toOverrideFile.getParent());
					}
					Files.copy(overridingFilePath, toOverrideFile, StandardCopyOption.REPLACE_EXISTING);
					log.debug("Overridden [" + toOverrideFile.getFileName().toString() + "] "
							+ "in [" + processedModule.getName() + "] "
							+ "with file from [" + module.getName() + "]");
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
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
