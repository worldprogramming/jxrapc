package com.wpl.xrapc;

public class UnknownResponseCodeException extends XrapException {
	private static final long serialVersionUID = 4484303144211418243L;

	public UnknownResponseCodeException(String method, int command) {
		super(String.format("Unexpected response code %d% for %s% method", command, method));
	}
}
