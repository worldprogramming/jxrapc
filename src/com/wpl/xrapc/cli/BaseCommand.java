package com.wpl.xrapc.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;

import com.wpl.xrapc.NameValuePair;
import com.wpl.xrapc.XrapClient;
import com.wpl.xrapc.XrapException;
import com.wpl.xrapc.XrapReply;
import com.wpl.xrapc.XrapRequest;

/**
 * Base class representing a "command" in the command line interface.
 * See Command pattern.
 * @author tomq
 */
abstract class BaseCommand {
	private static Charset utf8 = Charset.forName("UTF8");
	
	/**
	 * Represents an individual name value pair that
	 * should influence the "header" of the request.
	 */
	static class HeaderItem {
		public HeaderItem(String name, String value) {
			this.name = name;
			this.value = value;
		}
		String name;
		String value;
	}
	
	/**
	 * Represents an individual name value pair that 
	 * should form part of the "body" of the request,
	 * or the request parameters in the case of a GET
	 */
	static class Item {
		public Item(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		String name;
		Object value;
	}
	
	protected String resource;
	protected List<HeaderItem> headerItems = new ArrayList<HeaderItem>();
	protected List<Item> dataItems = new ArrayList<Item>();
	protected byte[] body;
	protected boolean printRequestHeader=false;
	protected boolean printRequestBody=false;
	protected boolean printResponseHeader=true;
	protected boolean printResponseBody=true;
	
	public abstract String getName();
	
	/**
	 * Sets whether to print the request headers before invoking the request.
	 * The default is false.
	 */
	public void setPrintRequestHeader(boolean b) {
		printRequestHeader = b;
	}
	
	/**
	 * Sets whether to print the request body before invoking the request.
	 * The default is false.
	 */
	public void setPrintRequestBody(boolean b) {
		printRequestBody = b;
	}
	
	/**
	 * Sets whether to print the reply headers after invoking the request.
	 * The default is true.
	 */
	public void setPrintResponseHeader(boolean b) {
		printResponseHeader = b;
	}
	
	/**
	 * Sets whether to print the reply body after invoking the request.
	 * The default is true.
	 */
	public void setPrintResponseBody(boolean b) {
		printResponseBody = b;
	}
	
	/**
	 * Sets the resource on which the request will operate. 
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}
	
	/**
	 * Adds a name value pair that should control the "header" of the request.
	 * Valid names depend on the type of request.
	 */
	public void addHeaderItem(String name, String value) {
		this.headerItems.add(new HeaderItem(name, value));
	}
	
	/**
	 * Adds a name value pair that should make up part of the body
	 * of the request or the query parameters.
	 */
	public void addDataItem(String name, Object value) {
		this.dataItems.add(new Item(name, value));
	}

	/**
	 * Returns true if the request requires a body.
	 * This will always be false for DELETE and GET commands.
	 * For PUT and POST it depends whether any data items have been specified.
	 * If not, then a body is required. 
	 * @return
	 */
	public boolean needsBody() {
		return false;
	}
	
	/**
	 * Sets the body to pass on the request.
	 * This is only allowed if needsBody returns true. That is
	 * it is not allowed for GET and DELETE, and only allowed for PUT and POST
	 * if there are no data items to make up the body.
	 */
	public void setBody(byte[] body) {
		if (!needsBody()) 
			throw new IllegalStateException();
		this.body = body;
	}
	
	/**
	 * Runs the command using the given XrapClient object.
	 */
	public void run(XrapClient client) throws UsageException, XrapException {
		XrapRequest request = buildRequest();
		if (printRequestHeader || printRequestBody)
			System.out.println("\n\n");
		printReply(client.send(request));
	}
	
	/**
	 * Overridden by subclasses to build the XRAP request object.
	 */
	protected abstract XrapRequest buildRequest() throws UsageException;

	/**
	 * Utility method to parse a date. 
	 */
	protected static Date parseDate(String s) throws UsageException {
		try {
			return SimpleDateFormat.getDateTimeInstance().parse(s);
		}
		catch (java.text.ParseException ex) {
			throw new UsageException(String.format("Invalid date value '%s' : %s", s, ex.getMessage()));
		}
	}
	
	/**
	 * Creates a JSON body from any data items. 
	 * The returned byte array is the UTF8 encoding of the 
	 * serialised JSON text
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected byte[] createJsonBodyFromItems() {
		JSONObject obj = new JSONObject();
		
		for (Item itm : dataItems) {
			obj.put(itm.name, itm.value);
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			OutputStreamWriter osw = new OutputStreamWriter(baos, utf8);
			obj.writeJSONString(osw);
			osw.close();
		}
		catch (IOException ex) {
			// Won't happen?
			ex.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	private void printReply(XrapReply reply) {
		if (printResponseHeader) {
			System.out.println(reply.statusCode);
			if (reply.errorText!=null) 
				System.out.println(reply.errorText);
			
			if (reply.contentType!=null) {
				System.out.printf("Content-type: %s%n", reply.contentType);
			}
			if (reply.dateModified!=0) {
				System.out.printf("Date-Modified: %tc%n", new Date(reply.dateModified));
			}
			if (reply.etag!=null) {
				System.out.printf("ETag: %s%n", reply.etag);
			}
			if (reply.location!=null) {
				System.out.printf("Location: %s%n", reply.location);
			}
			
			if (reply.metadata!=null) {
				for (NameValuePair nvp : reply.metadata) {
					System.out.printf("%s : %s%n",  nvp.getName(), nvp.getStringValue());
				}
			}
			System.out.println();
		}
		
		if (reply.body!=null && printResponseBody) {
			printBody(reply.body);
		}
	}
	
	protected void printBody(byte[] body) {
		// For now assume textual UTF8
		System.out.println(new String(body, utf8));
	}
	
	/**
	 * Returns true if the given string is a valid method name
	 */
	static boolean validMethodName(String s) {
		return "PUT".equals(s) || "GET".equals(s) ||
				"POST".equals(s) || "DELETE".equals(s);
	}

	/**
	 * Factory method for BaseCommand objects.
	 * @throws IllegalArgumentException if the given method name is not valid. 
	 */
	static BaseCommand createCommand(String methodName) {
		if (methodName.equals("GET")) return new GetCommand();
		if (methodName.equals("POST")) return new PostCommand();
		if (methodName.equals("PUT")) return new PutCommand();
		if (methodName.equals("DELETE")) return new DeleteCommand();
		throw new IllegalArgumentException(methodName);
	}
}
