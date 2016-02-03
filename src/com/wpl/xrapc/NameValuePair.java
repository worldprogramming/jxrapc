package com.wpl.xrapc;

import java.nio.charset.Charset;

public class NameValuePair {
	private static Charset utf8 = Charset.forName("UTF8");
	private String name;
	private byte[] value;
	
	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value.getBytes(utf8);
	}
	
	public NameValuePair(String name, byte[] value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public byte[] getRawValue() {
		return value;
	}
	
	public String getStringValue() {
		return new String(value, utf8);
	}
}
