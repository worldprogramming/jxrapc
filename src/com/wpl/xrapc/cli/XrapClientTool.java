package com.wpl.xrapc.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.wpl.xrapc.XrapClient;
import com.wpl.xrapc.XrapException;

/**
 * General purpose command line XRAP tool, a bit like cURL.
 * Command line syntax is modelled after HTTPie(https://github.com/jkbrzt/httpie), 
 * and looks like:
 *   xrap [flags] [METHOD] URL [ITEM [ITEM]]
 *   
 * @author tomq
 */
public class XrapClientTool {
	public static void main(String[] args) {
		try {
			new XrapClientTool(args).run();
		}
		catch (XrapException ex) {
			System.err.println("Communication error");
			ex.printStackTrace();
		}
		catch (UsageException ex) {
			System.err.println(ex.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("xrapc", buildCommandLineOptions(), true);
		}
		catch (IOException ex) {
			System.err.println(ex.getMessage());
		}
		catch (InterruptedException ex) {
			System.err.println(ex.getMessage());
		}
	}
	
	private BaseCommand command;
	private String host;
	private int port;
	private int timeoutSeconds=30;
	
	
	public XrapClientTool(String[] args) throws UsageException {
		command = parseArgs(args);
	}
	
	
	private BaseCommand parseArgs(String[] args) throws UsageException {
		try {
			CommandLineParser parser = new DefaultParser();
			Options options = buildCommandLineOptions();
			CommandLine cmd = parser.parse(options, args);
			
			String[] remainingArgs = cmd.getArgs();
			if (remainingArgs.length<2) {
				throw new UsageException("Must supply METHOD and URL");
			}
			
			if (!BaseCommand.validMethodName(remainingArgs[0])) {
				throw new UsageException(String.format("Unknown method '%s'", remainingArgs[0]));
			}
			
			BaseCommand command=BaseCommand.createCommand(remainingArgs[0]);
			command.setResource(parseUrl(remainingArgs[1]));
			
			for (int i=2; i<remainingArgs.length; i++) {
				Utils.parseItem(command, remainingArgs[i]);
			}
			
			if (cmd.hasOption("verbose")) { 
				command.setPrintRequestHeader(true);
				command.setPrintRequestBody(true);
				command.setPrintResponseHeader(true);
				command.setPrintResponseBody(true);
			}
			if (cmd.hasOption("headers")) { 
				command.setPrintResponseHeader(true);
				command.setPrintResponseBody(false);
			}
			if (cmd.hasOption("body")) { 
				command.setPrintResponseHeader(false);
				command.setPrintResponseBody(true);
			}
			
			Number timeoutArg = (Number)cmd.getParsedOptionValue("timeout");
			if (timeoutArg!=null) {
				timeoutSeconds = timeoutArg.intValue();
			}
			
			
			return command;
		}
		catch (ParseException ex) {
			throw new UsageException(ex.getMessage());
		}
	}
	
	private String parseUrl(String url) throws UsageException {
		if (url.indexOf('?')!=-1) {
			throw new UsageException("Query parameters should be specified using items on the command line");
		}
		if (url.indexOf("://")!=-1) {
			throw new UsageException("Url should not contain a scheme");
		}
		int slashpos = url.indexOf('/');
		String resource;
		String hostAndPort;
		if (slashpos==-1) {
			resource = "/";
			hostAndPort = url;
		}
		else {
			resource = url.substring(slashpos);
			hostAndPort = url.substring(0, slashpos);
		}
		
		int colonPos = hostAndPort.indexOf(':');
		if (colonPos==-1) {
			throw new UsageException("URL must contain a port number");
		}
		
		this.host = hostAndPort.substring(0, colonPos);
		this.port = Utils.parsePort(hostAndPort.substring(colonPos+1));
		
		return resource;
	}
	
	public void run() throws XrapException, UsageException, IOException, InterruptedException {
		XrapClient client = new XrapClient(
				String.format("tcp://%s:%d", host, port));
		client.setTimeout(timeoutSeconds);
		
		if (command.needsBody()) {
			command.setBody(readBodyFromStdin());
		}
		command.run(client);
	}
	
	private byte[] readBodyFromStdin() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[4096];
		while (true) {
			int bytesRead = System.in.read(buff);
			if (bytesRead==-1) break;
			baos.write(buff, 0, bytesRead);
		}
		return baos.toByteArray();
	}
	
	private static Options buildCommandLineOptions() {
		Options options = new Options();
		
		options.addOption(
				Option.builder("h")
					.longOpt("help")
					.desc("Prints the help")
					.argName("help")
					.build());
		
		options.addOption(
				Option.builder("v")
					.longOpt("verbose")
					.desc("Prints both request and response")
					.argName("verbose")
					.build());
		
		options.addOption(
				Option.builder("h")
					.longOpt("headers")
					.desc("Only response headers are printed")
					.build());
		
		options.addOption(
				Option.builder("b")
					.longOpt("body")
					.desc("Only response body is printed")
					.build());
		
		options.addOption(
				Option.builder("t")
					.longOpt("timeout")
					.desc("Request timeout in seconds")
					.argName("<SECONDS>t")
					.hasArg(true)
					.type(Number.class)
					.build());
		return options;
	}
	

	
	

}
