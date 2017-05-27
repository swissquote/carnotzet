package com.github.swissquote.carnotzet.extention;

import java.util.List;
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
					module.getLabels().put("carnotzet.hello.message", message);
					return module;
				}
		).collect(Collectors.toList());
	}
}
