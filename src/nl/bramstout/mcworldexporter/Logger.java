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

package nl.bramstout.mcworldexporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Logger extends PrintStream{

	private static FileOutputStream logFileStream = null;
	public static void init() {
		try {
			if(logFileStream == null) {
				File f = new File(FileUtil.getLogFile());
				f.getParentFile().mkdirs();
				logFileStream = new FileOutputStream(f);
				logFileStream.write(("MiEx " + ReleaseChecker.CURRENT_VERSION + "\n").getBytes());
				
				for(Entry<Object, Object> property : System.getProperties().entrySet()) {
					String key = String.valueOf(property.getKey());
					if(key.contains("java.specification") || key.contains("os.") || key.contains("java.runtime") || 
							key.contains("java.version") || key.contains("java.vm.name"))
						logFileStream.write((key + " = " + String.valueOf(property.getValue()) + "\n").getBytes());
				}
				logFileStream.write("\n".getBytes());
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	PrintStream consoleOut;
	private boolean isErrorStream;
	
	public Logger(PrintStream consoleStream, boolean isErrorStream){
		super(logFileStream);
		this.consoleOut = consoleStream;
		this.isErrorStream = isErrorStream;
	}
	
	@Override
	public void close() {
		super.close();
		consoleOut.close();
	}
	
	@Override
	public void flush() {
		super.flush();
		consoleOut.flush();
	}
	
	@Override
	public void write(byte[] buf) throws IOException {
		super.write(buf);
		consoleOut.write(buf);
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		super.write(buf, off, len);
		consoleOut.write(buf, off, len);
	}
	
	@Override
	public void write(int b) {
		super.write(b);
		consoleOut.write(b);
	}
	
	@Override
	public void println() {
		super.println();
		consoleOut.println();
	}
	
	public static AtomicBoolean hasShownPopup = new AtomicBoolean(false);
	
	@Override
	public void print(String s) {
		super.print(s);
		
		// Give the user a popup to let them know that some error occured.
		// Exceptions get printed via this method, so if we get something in here
		// then we know that there was an error.
		if(isErrorStream) {
			// Don't spam the user with a bunch of popups
			if(hasShownPopup.getAndSet(true))
				return;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if(MCWorldExporter.getApp() != null)
						JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "An error happened somewhere. Please check the log for more information.", "Error", JOptionPane.ERROR_MESSAGE);
					hasShownPopup.set(false);
				}
				
			});
		}
	}

}
