package com.github.swissquote.carnotzet.maven.plugin.impl;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

public final class Utils {

	private Utils() {
		// static function holder
	}

	public static List<String> getServiceNames(Carnotzet carnotzet) {
		return carnotzet.getModules().stream().map(CarnotzetModule::getName).sorted().collect(toList());
	}

	public static void waitForUserInterrupt() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
