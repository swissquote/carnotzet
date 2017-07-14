package com.github.swissquote.carnotzet.extention;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

public final class HelloLabelExtension implements CarnotzetExtension {

	private final String message;

	public HelloLabelExtension(String message) {
		this.message = message;
	}

	@Override
	public List<CarnotzetModule> apply(Carnotzet carnotzet) {
		return carnotzet.getModules().stream().map(module -> {
					CarnotzetModule.CarnotzetModuleBuilder result = module.toBuilder();
					Map<String, String> labels = new HashMap<>();
					if (module.getLabels() != null) {
						labels.putAll(module.getLabels());
					}
					labels.put("carnotzet.hello.message", message);
					result.labels(labels);
					return result.build();
				}
		).collect(Collectors.toList());
	}
}
