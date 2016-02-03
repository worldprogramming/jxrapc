package com.wpl.xrapc.cli;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;

class Utils {
	private static int MAX_PORT = 65535;
	
	static int parsePort(String portString) throws UsageException {
		if (portString.isEmpty()) {
			throw new UsageException("URL must specify a port number");
		}
		try {
			int port = Integer.parseInt(portString);
			if (port>0 && port<=MAX_PORT) return port;
			throw new UsageException(String.format("Port number must be in [1, %d]", MAX_PORT));
		}
		catch (NumberFormatException ex) {
			throw new UsageException(String.format("Invalid port number '%s' in URL", portString));
		}
	}
	
	private static Pattern HEADER_ITEM_PATTERN = Pattern.compile("([A-Za-z0-9-]+):(.*)"); 
	private static Pattern DATA_ITEM_PATTERN = Pattern.compile("([A-Za-z0-9-]+)=(.*)"); 
	private static Pattern DATA_FILE_ITEM_PATTERN = Pattern.compile("([A-Za-z0-9-]+)=@(.*)"); 
	private static Pattern JSON_FILE_ITEM_PATTERN = Pattern.compile("([A-Za-z0-9-]+):=@(.*)"); 
	private static Pattern JSON_ITEM_PATTERN = Pattern.compile("([A-Za-z0-9-]+):=(.*)"); 
	
	static void parseItem(BaseCommand command, String item) throws UsageException {
		Matcher m;
		
		m = JSON_FILE_ITEM_PATTERN.matcher(item);
		if (m.matches()) {
			command.addDataItem(m.group(1), readJsonFile(m.group(2)));
			return;
		}
		
		m = JSON_ITEM_PATTERN.matcher(item);
		if (m.matches()) {
			command.addDataItem(m.group(1), readJsonText(m.group(2)));
			return;
		}
		
		m = HEADER_ITEM_PATTERN.matcher(item);
		if (m.matches()) {
			command.addHeaderItem(m.group(1), m.group(2));
			return;
		}

		m = DATA_FILE_ITEM_PATTERN.matcher(item);
		if (m.matches()) {
			command.addDataItem(m.group(1), readFully(m.group(2)));
			return;
		}

		m = DATA_ITEM_PATTERN.matcher(item);
		if (m.matches()) {
			command.addDataItem(m.group(1), m.group(2));
			return;
		}

		throw new UsageException(String.format("Invalid item syntax '%s'", item));
	}
	
	private static Object readJsonFile(String filename) throws UsageException {
		try {
			try (FileReader in = new FileReader(filename)) {
				return JSONValue.parseWithException(in);
			}
		}
		catch (org.json.simple.parser.ParseException ex) {
			throw new UsageException(String.format("Error parsing JSON file '%s' : %s", filename, ex.getMessage()));
		}
		catch (IOException ex) {
			throw new UsageException(String.format("Error reading file '%s' : %s", filename, ex.getMessage()));
		}
	}
	
	private static Object readJsonText(String text) throws UsageException {
		try {
			return JSONValue.parseWithException(new StringReader(text));
		}
		catch (org.json.simple.parser.ParseException ex) {
			throw new UsageException(String.format("Error parsing JSON item text '%s' : %s", text, ex.toString()));
		}
		catch (IOException ex) {
			throw new UsageException(String.format("Error parsing JSON item text '%s' : %s", text, ex.toString()));
		}
	}
	
	private static String readFully(String filename) throws UsageException {
		try {
			StringBuilder result = new StringBuilder();
			char[] buff = new char[64*1024];
			try (Reader r = new FileReader(filename)) {
				while (true) {
					int charsRead = r.read(buff, 0, buff.length);
					if (charsRead==-1) break;
					result.append(buff, 0, charsRead);
				}
			}
			return result.toString();
		}
		catch (IOException ex) {
			throw new UsageException(String.format("Error reading file '%s' : %s", filename, ex.getMessage()));
		}
	}
	

}
