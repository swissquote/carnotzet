package com.github.swissquote.carnotzet.runtime.extension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ContainerIpsDumpExtension implements ContainerOrchestrationRuntimeExtension {

	private final String message;
	private final Path dumpDirectory;

	public ContainerIpsDumpExtension(String message, Path dumpDirectory) {
		this.message = message;
		this.dumpDirectory = dumpDirectory;
	}

	@Override
	public CarnotzetModule beforeStart(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		log.info("Before start");
		CarnotzetModule.CarnotzetModuleBuilder result = module.toBuilder();
		Map<String, String> labels = new HashMap<>();
		if (module.getLabels() != null) {
			labels.putAll(module.getLabels());
		}
		labels.put("carnotzet.runtime.hello.message", message);
		result.labels(labels);
		return result.build();
	}

	@Override
	@SneakyThrows
	public CarnotzetModule afterStart(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		log.info("After start");
		dumpDirectory.toFile().mkdirs();
		Container container = runtime.getContainer(module.getServiceId());
		byte[] ipStrBytes = container.getIp().getBytes();
		Files.write(dumpDirectory.resolve(module.getServiceId()), ipStrBytes);
		return module;
	}

	@Override
	public CarnotzetModule beforeStop(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		log.info("Before stop");
		deleteDirectoryStream(dumpDirectory);
		return module;
	}

	@SneakyThrows
	void deleteDirectoryStream(Path path) {
		Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

}
