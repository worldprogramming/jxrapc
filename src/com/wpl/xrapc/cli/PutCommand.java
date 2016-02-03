package com.wpl.xrapc.cli;

import com.wpl.xrapc.XrapPutRequest;
import com.wpl.xrapc.XrapRequest;

/**
 * Command object representing a PUT request.
 */
class PutCommand extends BaseCommand {

	@Override
	public String getName() { return "PUT"; }

	@Override
	public boolean needsBody() {
		return dataItems.isEmpty();
	}

	protected XrapRequest buildRequest() throws UsageException {
		XrapPutRequest request = new XrapPutRequest(resource);
		for (HeaderItem itm : headerItems) {
			if (itm.name.equalsIgnoreCase("if-unmodified-since") || 
					itm.name.equalsIgnoreCase("ifUnmodifiedSince"))
			{
				request.setIfUnmodifiedSince(parseDate(itm.value));
			}
			else if (itm.name.equalsIgnoreCase("if-match") ||
					itm.name.equalsIgnoreCase("ifmatch"))
			{
				request.setIfMatch(itm.value);
			}
			else if (itm.name.equalsIgnoreCase("content-type") ||
					itm.name.equalsIgnoreCase("contentType"))
			{
				request.setContentType(itm.value);
			}
			else {
				throw new UsageException(String.format("Header field '%s' not appropriate for PUT method", itm.name));
			}
		}
		
		if (body!=null) {
			request.setContentBody(body);
		}
		else {
			request.setContentBody(createJsonBodyFromItems());
		}
		
		printRequest(request);
		return request;
	}
	
	private void printRequest(XrapPutRequest request) {
		if (printRequestHeader) {
			System.out.printf("PUT %s%n", request.getResource());
			if (request.getIfUnmodifiedSince()!=null) {
				System.out.printf("If-UnModified-Since : %tc%n", request.getIfUnmodifiedSince());
			}
			if (request.getIfMatch()!=null) {
				System.out.printf("If-Match : %tc%n", request.getIfMatch());
			}
			if (request.getContentType()!=null) {
				System.out.printf("Content-type : %s%n", request.getContentType());
			}
		}
		
		if (printRequestBody && request.getContentBody()!=null) {
			printBody(request.getContentBody());
		}
	}
	

}
