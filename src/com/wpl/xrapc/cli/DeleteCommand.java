package com.wpl.xrapc.cli;

import com.wpl.xrapc.XrapDeleteRequest;
import com.wpl.xrapc.XrapRequest;

/**
 * Command object representing a DELETE request.
 */
class DeleteCommand extends BaseCommand {
	
	@Override
	public String getName() { return "DELETE"; }

	protected XrapRequest buildRequest() throws UsageException {
		XrapDeleteRequest request = new XrapDeleteRequest(resource);
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
			else {
				throw new UsageException(String.format("Header field '%s' not appropriate for DELETE method", itm.name));
			}
		}
		
		if (!dataItems.isEmpty()) {
			throw new UsageException("Data items not appropriate for DELETE method");
		}
		
		printRequest(request);
		return request;
	}
	
	private void printRequest(XrapDeleteRequest request) {
		if (printRequestHeader) {
			System.out.printf("DELETE %s%n", request.getResource());
			if (request.getIfUnmodifiedSince()!=null) {
				System.out.printf("If-UnModified-Since : %tc%n", request.getIfUnmodifiedSince());
			}
			if (request.getIfMatch()!=null) {
				System.out.printf("If-Match : %tc%n", request.getIfMatch());
			}
		}
	}
	
	
}
