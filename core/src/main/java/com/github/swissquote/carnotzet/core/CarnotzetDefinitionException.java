package com.github.swissquote.carnotzet.core;

public class CarnotzetDefinitionException extends RuntimeException {

	public CarnotzetDefinitionException(String message) {
		super(message);
	}


	public CarnotzetDefinitionException(Throwable cause) {
		super(cause);
	}

	public CarnotzetDefinitionException(String message, Throwable cause) {
		super(message, cause);
	}
}
