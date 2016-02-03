package com.wpl.xrapc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for the classes that represent the 
 * different types of XRAP request. 
 * @author tomq
 */
public abstract class XrapRequest {
	private static Charset utf8 = Charset.forName("UTF8");
	
	private String resource;
	private int requestId;
	private static AtomicInteger nextRequestId = new AtomicInteger(1);
	
	protected XrapRequest(String resource) {
		this.resource = resource;
		this.requestId = nextRequestId.getAndIncrement();
	}
	
	/** 
	 * Returns the request ID for this request.
	 * @return The request ID.
	 */
	public int getRequestId() {
		return requestId;
	}
	
	/** 
	 * Sets the resource that is the subject of the request.
	 * This is a string of the form /a/b/c.
	 * The resource string is always passed to the server as a UTF8 string.
	 * @param resource The new resource.
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}
	
	/** 
	 * Returns the resource that is the subject of the request.
	 * @return The resource
	 */
	public String getResource() {
		return resource;
	}
	
	abstract void buildRequest(DataOutputStream dos) throws IOException;
	abstract XrapReply parseResponse(ByteBuffer response) throws XrapException;
	
	XrapReply parseResponse(byte[] responseBytes) throws XrapException {
		ByteBuffer response = ByteBuffer.wrap(responseBytes);
		response.order(ByteOrder.BIG_ENDIAN);
		return parseResponse(response);
	}
	
	protected void writeString(DataOutputStream dos, String s) throws IOException {
		writeString(dos, s==null ? null : s.getBytes(utf8));
	}

	protected void writeString(DataOutputStream dos, byte[] bytes) throws IOException {
		if (bytes==null) {
			dos.writeByte(0);
			return;
		}
		if (bytes.length>255) throw new IllegalArgumentException();
		dos.writeByte(bytes.length);
		dos.write(bytes);
	}
	
	protected void writeLongString(DataOutputStream dos, String s) throws IOException {
		writeLongString(dos, s==null ? null : s.getBytes(utf8));
	}
	
	protected void writeLongString(DataOutputStream dos, byte[] bytes) throws IOException {
		if (bytes==null) {
			dos.writeInt(0);
			return;
		}
		dos.writeInt(bytes.length);
		dos.write(bytes);
	}
	
	protected void checkSignature(ByteBuffer dis) throws XrapException {
		short signature = dis.getShort();
		if (signature!=Constants.SIGNATURE) {
			throw new InvalidSignatureException(signature);
		}
	}
	
	protected XrapReply readErrorResponse(ByteBuffer dis) {
		XrapReply response = new XrapReply();
		response.requestId = dis.getInt();
		response.statusCode = dis.getShort();
		response.errorText = readString(dis);
		return response;
	}
	
	protected String readString(ByteBuffer dis) {
		int length = dis.get()&0xff;
		byte[] stringBytes = new byte[length];
		dis.get(stringBytes);
		return new String(stringBytes, utf8);
	}
	
	protected byte[] readLongBinaryString(ByteBuffer dis) {
		int length = dis.getInt();
		if (length > dis.remaining()) {
			// TODO:
		}
		byte[] stringBytes = new byte[length];
		dis.get(stringBytes);
		return stringBytes;
	}
	
	protected NameValuePair[] readHash(ByteBuffer buffer) {
		int count = buffer.getInt();
		// for each entry there is a short string and a long string
		// So the minimum number of bytes we require is count*5
		if (buffer.remaining() < count*5) {
			// TODO
		}
		
		NameValuePair[] result = new NameValuePair[count];
		for (int i=0; i<count; i++) {
			String name = readString(buffer);
			result[i] = new NameValuePair(name, readLongBinaryString(buffer));
		}
		return result;
	}
}
