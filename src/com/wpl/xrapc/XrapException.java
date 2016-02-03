package com.wpl.xrapc;

public class XrapException extends Exception {
	private static final long serialVersionUID = 8428580381628251095L;
	
	public XrapException() {}
	
	public XrapException(String message) {
		super(message);
	}
	public XrapException(String message, Exception cause) {
		super(message, cause);
	}
}
