package com.wpl.xrapc;

public class InvalidSignatureException extends XrapException {
	private static final long serialVersionUID = 1565953526375140802L;
	public InvalidSignatureException(short signature) {
		super(String.format("Unexpected message signature %04X in response", signature));
	}
}
