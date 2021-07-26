package com.github.swissquote.carnotzet.core.runtime.api;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

/**
 * Allows to perform modifications of the carnotzet using the runtime lifecycle
 */
public interface ContainerOrchestrationRuntimeExtension {

	default CarnotzetModule beforeStart(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule afterStart(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule beforeStop(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule afterStop(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule beforeClean(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule afterClean(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule beforePull(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

	default CarnotzetModule afterPull(CarnotzetModule module, ContainerOrchestrationRuntime runtime, Carnotzet carnotzet) {
		return module;
	}

}
