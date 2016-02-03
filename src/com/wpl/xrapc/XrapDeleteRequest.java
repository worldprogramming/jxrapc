package com.wpl.xrapc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Represents a DELETE request sent to an XRAP server.
 * @author tomq
 */
public class XrapDeleteRequest extends XrapRequest {
	private Date ifUnmodifiedSince;
	private String ifMatch;
	
	/**
	 * Constructs a new DELETE request.
	 * @param resource The resource to DELETE
	 */
	public XrapDeleteRequest(String resource) {
		super(resource);
	}

	/**
	 * Performs a conditional DELETE based on modification date. 
	 * The server can return a "412 Precondition Failed" 
	 * if the resource on the server has been modified since the given date.
	 * @param ifUnmodifiedSince A timestamp to compare to
	 */
	public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
		this.ifUnmodifiedSince = ifUnmodifiedSince;
	}
	
	/**
	 * Returns the value of the IfUnmodifiedSince request header parameter.
	 * This will be null if the request doesn't represent a conditional DELETE
	 * based on date.
	 * @return The value of the IfUnmodifiedSince header, or null.
	 */
	public Date getIfUnmodifiedSince() {
		return ifUnmodifiedSince;
	}
	
	/**
	 * Performs a conditional DELETE based on content.
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
	 * be null if ths request doesn't represent a conditional DELETE
	 * based on content.
	 * @return The current etag.
	 */
	public String getIfMatch() {
		return ifMatch;
	}
	
	
	@Override
	void buildRequest(DataOutputStream dos) throws IOException {
		dos.writeShort(Constants.SIGNATURE);
		dos.writeByte(Constants.DELETE_COMMAND);
		dos.writeInt(getRequestId());
		writeString(dos, getResource());
		if (ifUnmodifiedSince!=null)
			dos.writeLong(ifUnmodifiedSince.getTime());
		else 
			dos.writeLong(0);
		writeString(dos, ifMatch);
	}

	@Override
	XrapReply parseResponse(ByteBuffer buffer) throws XrapException {
		checkSignature(buffer);
		int command = buffer.get();
		if (command==Constants.ERROR_COMMAND) {
			return readErrorResponse(buffer);
		}
		else if (command==Constants.DELETE_OK_COMMAND) {
			XrapReply response = new XrapReply();
			response.requestId = buffer.getInt();
			response.statusCode = buffer.getShort(); 
			response.metadata = readHash(buffer);
			return response;
		}
		else {
			throw new UnknownResponseCodeException("DELETE", command);
		}
	}

}
