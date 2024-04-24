package nl.bramstout.mcworldexporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
	public void writeBytes(byte[] buf) {
		super.writeBytes(buf);
		consoleOut.writeBytes(buf);
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
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "An error happened somewhere. Please check the log for more information.", "Error", JOptionPane.ERROR_MESSAGE);
					hasShownPopup.set(false);
				}
				
			});
		}
	}

}
