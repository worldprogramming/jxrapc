package com.wpl.xrapc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Represents a PUT request to be sent to an XRAP server.
 * @author tomq
 */
public class XrapPutRequest extends XrapRequest {
	private Date ifUnmodifiedSince;
	private String ifMatch;
	private String contentType;
	private byte[] contentBody;
	
	/**
	 * Constructs a new PUT request.
	 * @param resource The resource to PUT to. This is a string of the form /a/b/c
	 */
	public XrapPutRequest(String resource) {
		super(resource);
	}

	/** 
	 * Sets the content type of the body being sent.
	 * @param type The content type.
	 */
	public void setContentType(String type) {
		this.contentType = type;
	}
	
	/**
	 * Returns the content type specified in this request.
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Sets the content body to be posted in the request.
	 * @param body The body, as binary bytes. 
	 */
	public void setContentBody(byte[] body) {
		this.contentBody = body;
	}
	
	/**
	 * Returns the content body specified in this request.
	 * @return
	 */
	public byte[] getContentBody() {
		return contentBody;
	}

	/**
	 * Performs a conditional PUT based on modification date. 
	 * The server can return a "412 Precondition Failed" 
	 * if the resource on the server has been modified since the given date.
	 * @param ifUnmodifiedSince
	 */
	public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
		this.ifUnmodifiedSince = ifUnmodifiedSince;
	}
	
	/**
	 * Returns the value of the IfUnmodifiedSince request header parameter.
	 * This will be null if the request doesn't represent a conditional PUT
	 * based on date.
	 * @return
	 */
	public Date getIfUnmodifiedSince() {
		return ifUnmodifiedSince;
	}
	
	/**
	 * Performs a conditional PUT based on content.
	 * The server can return a "412 Precondition Failed" 
	 * if the resource on the copy of the resource on the server has
	 * a different hash.
	 * The etag is an opaque string previously returned by the server on a
	 * GET, POST or PUT request.
	 * @param etag The opaque hash previously returned from the server.
	 */
	public void setIfMatch(String etag) {
		this.ifMatch = etag;
	}
	
	/**
	 * Returns the current etag associated with the request. This will 
	 * be null if ths request doesn't represent a conditional PUT
	 * based on content.
	 * @return The current etag.
	 */
	public String getIfMatch() {
		return ifMatch;
	}
	
	
	
	@Override
	void buildRequest(DataOutputStream dos) throws IOException {
		dos.writeShort(Constants.SIGNATURE);
		dos.writeByte(Constants.PUT_COMMAND);
		dos.writeInt(getRequestId());
		writeString(dos, getResource());
		if (ifUnmodifiedSince!=null)
			dos.writeLong(ifUnmodifiedSince.getTime());
		else
			dos.writeLong(0);
		writeString(dos, ifMatch);
		writeString(dos, contentType);
		writeLongString(dos, contentBody);
	}

	@Override
	XrapReply parseResponse(ByteBuffer buffer) throws XrapException {
		checkSignature(buffer);
		int command = buffer.get();
		if (command==Constants.ERROR_COMMAND) {
			return readErrorResponse(buffer);
		}
		else if (command==Constants.PUT_OK_COMMAND) {
			XrapReply response = new XrapReply();
			response.requestId = buffer.getInt();
			response.statusCode = buffer.getShort();
			response.location = readString(buffer);
			response.etag = readString(buffer);
			response.dateModified = buffer.getLong();
			response.metadata = readHash(buffer);
			return response;
		}
		else {
			throw new UnknownResponseCodeException("PUT", command);
		}
	}

}
