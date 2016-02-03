package com.wpl.xrapc.cli;

import com.wpl.xrapc.XrapGetRequest;
import com.wpl.xrapc.XrapRequest;

/**
 * Command object representing a GET request.
 */
class GetCommand extends BaseCommand {

	@Override
	public String getName() { return "GET"; }
	
	protected XrapRequest buildRequest() throws UsageException {
		XrapGetRequest request = new XrapGetRequest(resource);
		for (HeaderItem itm : headerItems) {
			if (itm.name.equalsIgnoreCase("if-modified-since") || 
					itm.name.equalsIgnoreCase("ifModifiedSince"))
			{
				request.setIfModifiedSince(parseDate(itm.value));
			}
			else if (itm.name.equalsIgnoreCase("if-none-match") ||
					itm.name.equalsIgnoreCase("ifNoneMatch"))
			{
				request.setIfNoneMatch(itm.value);
			}
			else if (itm.name.equalsIgnoreCase("content-type") ||
					itm.name.equalsIgnoreCase("contentType"))
			{
				request.setContentType(itm.value);
			}
			else {
				throw new UsageException(String.format("Header field '%s' not appropriate for GET method", itm.name));
			}
		}
		
		for (Item itm : dataItems) {
			if (itm.value instanceof String)
				request.addParameter(itm.name, (String)itm.value);
			throw new UsageException(String.format("GET only supports string data items (%s)", itm.name));
		}
		
		printRequest(request);
		return request;
	}
	
	private void printRequest(XrapGetRequest request) {
		if (printRequestHeader) {
			System.out.printf("GET %s%n", request.getResource());
			if (request.getContentType()!=null) {
				System.out.printf("Content-type : %s%n", request.getContentType());
			}
			if (request.getIfModifiedSince()!=null) {
				System.out.printf("If-Modified-Since : %tc%n", request.getIfModifiedSince());
			}
			if (request.getIfNoneMatch()!=null) {
				System.out.printf("If-None-Match : %tc%n", request.getIfNoneMatch());
			}
			for (XrapGetRequest.Parameter p : request.getParameters()) {
				System.out.printf("%s = %s%n", 
						p.getName(), p.getStringValue());
			}
		}
	}
	
	
}
