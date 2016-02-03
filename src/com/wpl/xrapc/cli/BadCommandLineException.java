package com.wpl.xrapc.cli;

class BadCommandLineException extends Exception {
	private static final long serialVersionUID = -3287378467214807787L;

	public BadCommandLineException(String msg) {
		super(msg);
	}
}
