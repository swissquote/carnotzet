package com.github.swissquote.carnotzet.core.runtime.log;

import static java.util.stream.Collectors.toList;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

import lombok.Setter;

/**
 * Prints logs to the system default output, using different colors for different services,
 * very similar to what docker-compose does, but supports pre-processing log entries and other container orchestrators
 */
public class StdOutLogPrinter extends LogListenerBase {

	private final static List<Ansi.Color> unicornRainbowMagic = new ArrayList<>();

	private Map<String, Ansi.Color> serviceColors = new HashMap<>();

	private Integer longestServiceName = 0;

	@Setter
	private boolean padServiceName = true;

	static {
		AnsiConsole.systemInstall();
		unicornRainbowMagic.add(Ansi.Color.CYAN);
		unicornRainbowMagic.add(Ansi.Color.GREEN);
		unicornRainbowMagic.add(Ansi.Color.YELLOW);
		unicornRainbowMagic.add(Ansi.Color.MAGENTA);
		unicornRainbowMagic.add(Ansi.Color.RED);
		unicornRainbowMagic.add(Ansi.Color.BLUE);
	}

	/**
	 * colors will be based on the order of received log entries and may differ from one execution to the other.
	 * You can make the order predictable if you know all the services in advance, using the appropriate constructor
	 */
	public StdOutLogPrinter() {
		super();
	}

	/**
	 * colors will be based on the order of received log entries and may differ from one execution to the other.
	 * You can make the order predictable if you know all the services in advance, using the appropriate constructor
	 */
	public StdOutLogPrinter(Integer tail, boolean follow) {
		super(tail, follow);
	}

	/**
	 * Provides a reproducible color order for services in a carnotzet environment.
	 */
	public StdOutLogPrinter(Carnotzet carnotzet, Integer tail, boolean follow) {
		this(carnotzet.getModules().stream().map(CarnotzetModule::getName).sorted().collect(toList()), tail, follow);
	}

	/**
	 * Provides a reproducible color order for services
	 */
	public StdOutLogPrinter(List<String> services, Integer tail, boolean follow) {
		super(tail, follow);
		this.longestServiceName = services.stream().mapToInt(String::length).max().orElse(0);
		for (int i = 0; i < services.size(); i++) {
			int colorIndex = i % unicornRainbowMagic.size();
			serviceColors.put(services.get(i), unicornRainbowMagic.get(colorIndex));
		}
	}

	@Override
	public void acceptInternal(LogEvent event) {
		Ansi.Color color = serviceColors.computeIfAbsent(event.getService(), (service) -> {
			if (service.length() > this.longestServiceName) {
				longestServiceName = service.length();
			}
			int colorIndex = serviceColors.size() % unicornRainbowMagic.size();
			return unicornRainbowMagic.get(colorIndex);
		});
		System.out.println(ansi().
				fg(color)
				.a(padServiceName(event.getService()))
				.a(" | ")
				.reset()
				.a(event.getLogEntry()));
	}

	private String padServiceName(String serviceName) {
		if (!padServiceName) {
			return serviceName;
		}
		return String.format("%1$-" + longestServiceName + "s", serviceName);
	}

}
