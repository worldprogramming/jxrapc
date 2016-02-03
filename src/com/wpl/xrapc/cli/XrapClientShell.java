package com.wpl.xrapc.cli;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.wpl.xrapc.XrapClient;
import com.wpl.xrapc.XrapException;

import jline.console.ConsoleReader;

public class XrapClientShell {
	public static void main(String[] args) {
		try {
			new XrapClientShell(args).run();
		}
		catch (UsageException ex) {
			System.err.println(ex.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("xrapc", buildCommandLineOptions(), true);
		}
		catch (IOException ex) {
			System.err.println(ex.getMessage());
		}
	}
	
	private static Options buildCommandLineOptions() {
		Options options = new Options();
		
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
	
	private String host;
	private int port;
	private int timeoutSeconds;
	private XrapClient client;
	
	public XrapClientShell(String[] args) throws UsageException {
		parseArgs(args);
		client = new XrapClient(
					String.format("tcp://%s:%d", host, port));
		client.setTimeout(timeoutSeconds);
	}
	
	private void parseArgs(String[] args) throws UsageException {
		try {
			CommandLineParser parser = new DefaultParser();
			Options options = buildCommandLineOptions();
			CommandLine cmd = parser.parse(options, args);
			
			String[] remainingArgs = cmd.getArgs();
			if (remainingArgs.length!=1) {
				throw new UsageException("Must supply host:port");
			}
			
			String hostAndPort = remainingArgs[0];
			int colonPos = hostAndPort.indexOf(':');
			if (colonPos==-1) {
				throw new UsageException("Port number required");
			}
			this.host = hostAndPort.substring(0, colonPos);
			this.port = Utils.parsePort(hostAndPort.substring(colonPos+1));
			
			Number timeoutArg = (Number)cmd.getParsedOptionValue("timeout");
			if (timeoutArg!=null) {
				timeoutSeconds = timeoutArg.intValue();
			}
		}
		catch (ParseException ex) {
			throw new UsageException(ex.getMessage());
		}
	}
	
	private void run() throws IOException {
		
		ConsoleReader reader = new ConsoleReader();
		try {
			PrintWriter out = new PrintWriter(reader.getOutput());
			reader.setPrompt("> ");
			
			StringBuilder commandString = new StringBuilder();
			while (true) {
				String line = reader.readLine();
				if (line==null) break;
				if (line.endsWith("\\")) {
					commandString.append(line.substring(0, line.length()-1));
					commandString.append(" ");
					continue;
				}
				commandString.append(line);
				
				try {
					if (parseLine(commandString.toString())) break;
				}
				catch (UsageException ex) {
					out.println(ex.getMessage());
				}
				catch (XrapException ex) {
					out.println(ex.getMessage());
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				
				commandString = new StringBuilder();
			}
		}
		finally {
			reader.shutdown();
		}
	}
	
	private boolean parseLine(String commandString) throws UsageException, XrapException {
		CommandTokenizer tok = new CommandTokenizer(commandString);
		
		String commandName = tok.nextToken();
		if (commandName==null) return false;
		
		if (BaseCommand.validMethodName(commandName)) {
			BaseCommand command = BaseCommand.createCommand(commandName);
			parseXrapCommand(command, tok);
		}
		else if (commandName.equalsIgnoreCase("quit") || commandName.equalsIgnoreCase("exit")) {
			return true;
		}
		else {
			throw new UsageException(String.format("Unrecognised command '%'", commandName));
		}
		return false;
	}
	
	private void parseXrapCommand(BaseCommand command, CommandTokenizer tok) throws UsageException, XrapException {
		String resource = tok.nextToken();
		if (resource==null) 
			throw new UsageException(String.format("Must supply resource with '%s'", command.getName()));
		command.setResource(resource);
		
		String item;
		while ((item = tok.nextToken())!=null) {
			Utils.parseItem(command, item);
		}
		
		command.run(client);
	}
	
	static class CommandTokenizer {
		char[] line;
		int pos;
		
		CommandTokenizer(String line) {
			this.line = line.toCharArray();
		}
		
		String nextToken() {
			skipWhite();
			if (pos==line.length) return null;
			
			if (line[pos]=='\'' || line[pos]=='"')
				return matchString();
			return matchWord();
		}
		
		private void skipWhite() {
			while (pos<line.length && Character.isWhitespace(line[pos])) pos++;
		}
		
		private String matchWord() {
			int startpos=pos;
			while (pos<line.length && !Character.isWhitespace(line[pos])) pos++;
			return new String(line, startpos, pos-startpos);
		}
		
		private String matchString() {
			char quoteChar = line[pos];
			pos++;
			int startpos=pos;
			while (pos<line.length) {
				if (line[pos]==quoteChar) {
					String result = new String(line, startpos, pos-startpos);
					pos++;
					return result;
				}
				pos++;
			}
			// Unterminated string.
			return new String(line, startpos, pos-startpos);
		}
	}
}
