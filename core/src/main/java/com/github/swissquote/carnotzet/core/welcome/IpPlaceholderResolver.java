package com.github.swissquote.carnotzet.core.welcome;

import java.util.List;

import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IpPlaceholderResolver implements WelcomePagePostProcessor {

	private final ContainerOrchestrationRuntime runtime;

	public String process(String content) {
		String res = content;
		List<Container> containers = runtime.getContainers();
		for (Container container : containers) {
			//res = res.replace("/${" + container.getServiceName() + ".ip}", "/" + container.getServiceName() + ".docker");
			res = res.replace("${" + container.getServiceName() + ".ip}",
					container.getIp() == null ? "No IP address, is container started ?" : container.getIp());
		}
		return res;
	}

}
