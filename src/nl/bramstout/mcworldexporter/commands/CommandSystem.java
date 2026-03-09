/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.commands;

import java.io.File;
import java.util.Map.Entry;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.parallel.BackgroundThread;

/**
 * A system to allow MiEx to be used without a UI
 * in a programmatic way. It can be used as a command-line tool
 * and actions can get automated.
 */
public class CommandSystem {
	
	private boolean _quit;
	private boolean listenToStdIn;
	
	public CommandSystem(boolean listenToStdIn) {
		_quit = false;
		this.listenToStdIn = listenToStdIn;
	}
	
	public void quit() {
		this._quit = true;
	}
	
	public void run() {
		System.out.println("[INITIALISED]");
		if(listenToStdIn) {
			Scanner scanner = new Scanner(System.in);
			while(!_quit) {
				String command = scanner.nextLine();
				parseAndRunCommand(command);
			}
			scanner.close();
		}
	}
	
	private void printResult(JsonObject result) {
		Gson gson = new GsonBuilder().serializeNulls().create();
		String outputString = gson.toJson(result).replace("\n", "\\n");
		System.out.println("[COMMAND] " + outputString);
	}
	
	private void printSuccess(JsonObject result) {
		JsonObject output = new JsonObject();
		output.addProperty("status", "success");
		if(result != null) {
			for(Entry<String, JsonElement> entry : result.entrySet()) {
				output.add(entry.getKey(), entry.getValue());
			}
		}
		printResult(output);
	}
	
	private void printError(Exception ex) {
		JsonObject output = new JsonObject();
		output.addProperty("status", "error");
		output.addProperty("error", ex.getMessage());
		JsonArray stackTrace = new JsonArray();
		for(StackTraceElement el : ex.getStackTrace()) {
			stackTrace.add(el.toString());
		}
		output.add("stacktrace", stackTrace);
		printResult(output);
	}
	
	/**
	 * Read in a command file and run it.
	 * @param file
	 */
	public void parseAndRunCommand(File file) {
		try {
			JsonElement el = Json.read(file);
			if(el == null)
				throw new RuntimeException("Could not parse file " + file.getPath());
			parseAndRunCommand(el);
		}catch(Exception ex) {
			printError(ex);
		}
	}
	
	private void parseArg(String args, int index, JsonObject cmdObj) {
		int sep = args.indexOf(' ', index);
		if(sep >= 0 && (sep+1) < args.length()) {
			String argName = args.substring(index, sep);
			String argValStr = null;
			
			boolean hasQuotes = args.charAt(sep+1) == '"';
			if(hasQuotes)
				sep += 1;
			
			for(int i = sep + 1; i < args.length(); ++i) {
				if(args.charAt(i) == '\\') {
					// Backslash is an escape, so skip next character
					i += 1;
					continue;
				}
				if(hasQuotes && args.charAt(i) == '"') {
					argValStr = args.substring(sep, i);
					break;
				}
				if(!hasQuotes && args.charAt(i) == ' ') {
					argValStr = args.substring(sep, i);
				}
			}
			if(argValStr == null)
				argValStr = args.substring(sep);
			
			JsonElement argVal = Json.readString(argValStr);
			if(argVal == null)
				argVal = new JsonPrimitive(argValStr);
			
			cmdObj.add(argName, argVal);
		}
	}
	
	private void parseArgs(String args, JsonObject cmdObj) {
		for(int i = 0; i < args.length(); ++i) {
			if(args.charAt(i) == ' ')
				continue;
			if(args.charAt(i) == '-') {
				// We got a flag
				parseArg(args, i + 1, cmdObj);
			}
		}
	}
	
	/**
	 * Parse a command string and run it.
	 * @param command
	 */
	public void parseAndRunCommand(String command) {
		try {
			JsonElement commandJson = Json.readString(command, false);
			if(commandJson == null)
				throw new RuntimeException("Could not parse command " + command);
			if(commandJson.isJsonPrimitive()) {
				JsonObject cmdObj = new JsonObject();
				cmdObj.addProperty("command", command);
				parseAndRunCommand(cmdObj);
			}else {
				parseAndRunCommand(commandJson);
			}
		}catch(Exception ex) {
			JsonObject cmdObj = new JsonObject();
			String commandPart = command;
			String argsPart = "";
			int sep = commandPart.indexOf(' ');
			if(sep >= 0) {
				argsPart = commandPart.substring(sep + 1);
				commandPart = commandPart.substring(0, sep);
			}
			
			cmdObj.addProperty("command", commandPart);
			
			parseArgs(argsPart, cmdObj);
			
			parseAndRunCommand(cmdObj);
		}
	}
	
	/**
	 * Parse a command JSON element and run it.
	 * @param command
	 */
	public void parseAndRunCommand(JsonElement command) {
		if(command.isJsonArray())
			parseAndRunCommand(command.getAsJsonArray());
		else if(command.isJsonObject())
			parseAndRunCommand(command.getAsJsonObject());
		else if(command.isJsonPrimitive())
			parseAndRunCommand(command.getAsString());
		else
			throw new RuntimeException("Invalid command type");
	}
	
	/**
	 * Parse a command JSON list and run each command.
	 * @param command
	 */
	public void parseAndRunCommand(JsonArray command) {
		for(JsonElement el : command.asList())
			parseAndRunCommand(el);
	}
	
	/**
	 * Parse a command JSON object and run it.
	 * @param command
	 */
	public void parseAndRunCommand(JsonObject command) {
		try {
			String commandType = "";
			if(command.has("command"))
				commandType = command.get("command").getAsString();
			
			Command commandObj = CommandRegistry.createCommand(commandType);
			if(commandObj == null)
				throw new RuntimeException("Command " + commandType + " not found");
			
			JsonObject result = commandObj.run(command);
			
			// Make sure that the command has fully finished.
			BackgroundThread.waitUntilDoneWithBackgroundTasks();
			
			printSuccess(result);
		}catch(Exception ex) {
			printError(ex);
		}
	}
	
	
}
