package com.wpl.xrapc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents a GET request to be sent to an XRAP server.
 * @author tomq
 */
public class XrapGetRequest extends XrapRequest {
	private Date ifModifiedSince;
	private String ifNoneMatch;
	private String contentType;
	private List<Parameter> parameters = new ArrayList<Parameter>();

	/**
	 * Constructs a new GET request.
	 * @param resource The resource to GET. This is a string of the form /a/b/c
	 */
	public XrapGetRequest(String resource) {
		super(resource);
	}
	
	/**
	 * Performs a conditional GET based on modification date. 
	 * The server can return a "304 Not Modified" with no content 
	 * if the resource on the server has not been modified since the given date.
	 * @param ifModifiedSince The value of the ifModifiedSince header to assign.
	 */
	public void setIfModifiedSince(Date ifModifiedSince) {
		this.ifModifiedSince = ifModifiedSince;
	}
	
	/**
	 * Returns the value of the IfModifiedSince request header parameter.
	 * This will be null if the request doesn't represent a conditional GET
	 * based on date.
	 * @return
	 */
	public Date getIfModifiedSince() {
		return ifModifiedSince;
	}
	
	/**
	 * Performs a conditional GET based on content.
	 * The server can return a "304 Not Modified" with no content 
	 * if the resource on the copy of the resource on the server has
	 * the given hash.
	 * The etag is an opaque string previously returned by the server on a
	 * GET, POST or PUT request.
	 * @param etag The opaque hash previously returned from the server.
	 */
	public void setIfNoneMatch(String etag) {
		this.ifNoneMatch = etag;
	}
	
	/**
	 * Returns the current etag associated with the request. This will 
	 * be null if ths request doesn't represent a conditional GET
	 * based on content.
	 * @return The current etag.
	 */
	public String getIfNoneMatch() {
		return ifNoneMatch;
	}
	
	/**
	 * The requested content type. This can be left null indicating that the 
	 * client doesn't care. If the a specific content type is requested
	 * and the server cann't supply the resource in that form, then it should
	 * return "501 Not Implemented" 
	 * @param contentType The content type string to request.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	/**
	 * The content type request header parameter. 
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}
	
	/**
	 * Represents an individual request parameter associated 
	 * with the GET request.
	 * The value of a request parameter can be specified either in 
	 * binary, or as a string. If specified as a string the string will 
	 * be converted to UTF8 for passing to the server.
	 * The parameter names are always strings, and are always passed in UTF8.
	 */
	public static class Parameter {
		private String name;
		private String value;
		private byte[] binaryValue;
		
		/**
		 * Constructs a new request parameter with given name
		 * and given string value.
		 * @param name The name of the request parameter
		 * @param value The value of the request parameter.
		 */
		public Parameter(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		/**
		 * Constructs a new request parameter with given name
		 * and given binary value.
		 * @param name The name of the request parameter
		 * @param value The value of the request parameter.
		 */
		public Parameter(String name, byte[] value) {
			this.name = name;
			this.binaryValue = value;
		}
		
		/**
		 * Returns the paramter name.
		 * @return
		 */
		public String getName() { return name; }
		
		/**
		 * Returns the string value, or null if the value was specified in 
		 * binary.
		 * @return
		 */
		public String getStringValue() { return value; }
		
		/**
		 * Returns the binary value, or null if the value was specified as a
		 * string
		 * @return
		 */
		public byte[] getBinaryValue() { return binaryValue; }
		
		/**
		 * Returns true if the value was specified in binary.
		 * @return
		 */
		public boolean isBinary() { return binaryValue!=null; }
	}
	
	/**
	 * Adds a parameter to the request.
	 * This always adds a new parameter, even if there is already a parameter
	 * with the same name.  
	 * @param name The parameter name 
	 * @param value The parameter value as a string.
	 */
	public void addParameter(String name, String value) {
		addParameter(new Parameter(name, value));
	}
	
	/**
	 * Adds a parameter to the request.
	 * This always adds a new parameter, even if there is already a parameter
	 * with the same name.  
	 * @param name The parameter name 
	 * @param value The parameter value as a binary value.
	 */
	public void addParameter(String name, byte[] value) {
		addParameter(new Parameter(name, value));
	}

	/**
	 * Adds a parameter to the request.
	 * This always adds a new parameter, even if there is already a parameter
	 * with the same name.  
	 * @param p The parameter object 
	 */
	public void addParameter(Parameter p) {
		parameters.add(p);
	}
	
	/**
	 * Sets a parameter on the request.
	 * This will replace any existing parameter(s) with the same name.
	 * @param name The parameter name 
	 * @param value The parameter value as a string.
	 */
	public void setParameter(String name, String value) {
		setParameter(new Parameter(name, value));
	}
	
	/**
	 * Sets a parameter on the request.
	 * This will replace any existing parameter(s) with the same name.
	 * @param name The parameter name 
	 * @param value The parameter value as a binary value.
	 */
	public void setParameter(String name, byte[] value) {
		setParameter(new Parameter(name, value));
	}

	/**
	 * Sets a parameter on the request.
	 * This will replace any existing parameter(s) with the same name (case sensitive).
	 * @param p The parameter object 
	 */
	public void setParameter(Parameter p) {
		int i=0;
		for (i=0; i<parameters.size(); i++) {
			if (parameters.get(i).getName().equalsIgnoreCase(p.getName())) {
				parameters.set(i, p);
				i++;
				break;
			}
		}
		
		for (;i<parameters.size(); i++) {
			if (parameters.get(i).getName().equalsIgnoreCase(p.getName())) {
				parameters.remove(i);
				i--;
			}
		}
	}

	
	/**
	 * Returns the current list of parameters on the request.
	 * This is returned as an unmodifiable list.
	 */
	public List<Parameter> getParameters() {
		return Collections.unmodifiableList(parameters);
	}
	
	void buildRequest(DataOutputStream dos) throws IOException {
		dos.writeShort(Constants.SIGNATURE);
		dos.writeByte(Constants.GET_COMMAND);
		dos.writeInt(getRequestId());
		writeString(dos, getResource());
		if (parameters==null) {
			dos.writeInt(0);
		}
		else {
			dos.writeInt(parameters.size());
			for (int i=0; i<parameters.size(); i++) {
				writeParameter(dos, parameters.get(i));
			}
		}
		
		if (getIfModifiedSince()!=null)
			dos.writeLong(getIfModifiedSince().getTime());
		else
			dos.writeLong(0);
		writeString(dos, getIfNoneMatch());
		writeString(dos, getContentType());
	}
	
	XrapReply parseResponse(ByteBuffer buffer) throws XrapException {
		checkSignature(buffer);
		int command = buffer.get();
		if (command==Constants.ERROR_COMMAND) {
			return readErrorResponse(buffer);
		}
		else if (command==Constants.GET_EMPTY_COMMAND) {
			XrapReply response = new XrapReply();
			response.requestId = buffer.getInt();
			response.statusCode = buffer.getShort();
			return response;
		}
		else if (command==Constants.GET_OK_COMMAND) {
			XrapReply response = new XrapReply();
			response.requestId = buffer.getInt();
			response.statusCode = buffer.getShort(); 
			response.etag = readString(buffer);
			response.dateModified = buffer.getLong();
			response.contentType = readString(buffer);
			response.body = readLongBinaryString(buffer);
			response.metadata = readHash(buffer);
			return response;
		}
		else {
			throw new UnknownResponseCodeException("GET", command);
		}
	}
	
	private void writeParameter(DataOutputStream dos, Parameter p) throws IOException {
		writeString(dos, p.getName());
		if (p.isBinary()) writeLongString(dos, p.getBinaryValue());
		else writeLongString(dos, p.getStringValue());
	}
	
	
}
