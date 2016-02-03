package com.wpl.xrapc;

/**
 * Represents a reply from an XRAP request.
 * Currently a single class is used to represent the 
 * replies from all 4 request types.
 * @author tomq
 */
public class XrapReply {
	public int requestId;
	public short statusCode;
	public String errorText;
	public String etag;
	public String location;
	public long dateModified;
	public String contentType;
	public byte[] body;
	public NameValuePair[] metadata;
}
