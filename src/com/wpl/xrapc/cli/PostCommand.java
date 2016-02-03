package com.wpl.xrapc.cli;

import com.wpl.xrapc.XrapPostRequest;
import com.wpl.xrapc.XrapRequest;

/**
 * Command object representing a POST request.
 */
class PostCommand extends BaseCommand {

	@Override
	public String getName() { return "POST"; }

	@Override
	public boolean needsBody() {
		return dataItems.isEmpty();
	}

	protected XrapRequest buildRequest() throws UsageException {
		XrapPostRequest request = new XrapPostRequest(resource);
		for (HeaderItem itm : headerItems) {
			if (itm.name.equalsIgnoreCase("content-type") ||
					itm.name.equalsIgnoreCase("contentType"))
			{
				request.setContentType(itm.value);
			}
			else {
				throw new UsageException(String.format("Header field '%s' not appropriate for POST method", itm.name));
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
	
	private void printRequest(XrapPostRequest request) {
		if (printRequestHeader) {
			System.out.printf("POST %s%n", request.getResource());
			if (request.getContentType()!=null) {
				System.out.printf("Content-type : %s%n", request.getContentType());
			}
		}
		
		if (printRequestBody && request.getContentBody()!=null) {
			printBody(request.getContentBody());
		}
	}
	

}
