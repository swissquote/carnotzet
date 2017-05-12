package com.github.swissquote.carnotzet.extention;

import java.util.List;
import java.util.stream.Collectors;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

public class HelloLabelExtension implements CarnotzetExtension {
	@Override
	public List<CarnotzetModule> apply(Carnotzet carnotzet) {
		return carnotzet.getModules().stream().map(module -> {
					module.getLabels().put("carnotzet.hello.message", "Hello Carnotzet");
					return module;
				}
		).collect(Collectors.toList());

	}
}
