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

package nl.bramstout.mcworldexporter.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.BuiltInFiles.BuiltInFile;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Reference;

public class BuiltInFilesDialog extends JDialog implements ComponentListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public List<BuiltInFile> filesToChange;
	private FileList fileList;
	private DeltaViewer deltaViewer;
	private AtomicBoolean doneFlag;
	
	public BuiltInFilesDialog() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		setModalityType(ModalityType.APPLICATION_MODAL);
		doneFlag = new AtomicBoolean(false);
		
		JPanel root = new JPanel();
		root.setLayout(new DialogLayout());
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(root);
		
		JLabel descriptionLabel = new JLabel("New versions of the following files are available, but you have changed these. " + 
											"Would you like to update them anyways?");
		root.add(descriptionLabel);
		
		fileList = new FileList(this);
		JScrollPane fileListScrollPane = new JScrollPane(fileList);
		fileListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		fileListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		root.add(fileListScrollPane);
		
		deltaViewer = new DeltaViewer();
		JScrollPane deltaViewerScrollPane = new JScrollPane(deltaViewer);
		deltaViewerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		deltaViewerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		root.add(deltaViewerScrollPane);
		
		fileList.setListener(new FileListListener() {

			@Override
			public void selectionChanged(BuiltInFile file) {
				deltaViewer.load(file);
				revalidate();
				repaint();
			}
			
		});
		
		JButton overwriteAll = new JButton("Overwrite All");
		root.add(overwriteAll);
		overwriteAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for(Component comp : fileList.getComponents()) {
					if(comp instanceof FileListItem) {
						filesToChange.add(((FileListItem) comp).builtInFile);
					}
				}
				complete(false);
			}
			
		});
		
		JButton overwriteSelected = new JButton("Overwrite Selected");
		root.add(overwriteSelected);
		overwriteSelected.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for(Component comp : fileList.getComponents()) {
					if(comp instanceof FileListItem) {
						if(((FileListItem) comp).overwriteCheckBox.isSelected())
							filesToChange.add(((FileListItem) comp).builtInFile);
					}
				}
				complete(false);
			}
			
		});
		
		JButton close = new JButton("Close");
		root.add(close);
		close.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				complete(true);
			}
			
		});
		
		setSize(1200, 600);
		setTitle("Built In Files");
		
		addComponentListener(this);
	}
	
	public void show(List<BuiltInFile> files) {
		filesToChange = new ArrayList<BuiltInFile>();
		
		doneFlag.set(false);
		
		deltaViewer.load(null);
		fileList.load(files);
		
		setLocationRelativeTo(null);
		setVisible(true);
		
		if(SwingUtilities.isEventDispatchThread())
			return;
		
		try {
			while(!doneFlag.get()) {
				Thread.sleep(10);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void complete(boolean cancelled) {
		doneFlag.set(true);
		if(isVisible())
			setVisible(false);
		if(MCWorldExporter.getApp().getUI() != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					MCWorldExporter.getApp().getUI().requestFocus();
				}
			});
		}
	}
	
	@Override
	public void componentHidden(ComponentEvent e) {
		complete(true);
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	
	private static class DialogLayout implements LayoutManager{

		@Override
		public void addLayoutComponent(String name, Component comp) {
			
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			int width = 1200;
			int height = 600;
			return new Dimension(width, height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		@Override
		public void layoutContainer(Container parent) {
			int padding = 8;
			int buttonHeight = 24;
			int textHeight = 24;
			
			int i = 0;
			int numButtons = parent.getComponents().length - 3;
			int buttonWidth = (parent.getWidth() - (padding * (numButtons + 1))) / numButtons;
			int buttonX = padding;
			for(Component comp : parent.getComponents()) {
				if(i == 0) {
					comp.setBounds(padding, padding, parent.getWidth() - padding - padding, textHeight);
				}else if(i == 1) {
					comp.setBounds(padding, padding + textHeight + padding, 
							parent.getWidth() / 2 - padding, 
							parent.getHeight() - buttonHeight - padding - padding - padding - padding - textHeight);
				}else if(i == 2) {
					comp.setBounds(parent.getWidth() / 2 + (padding / 2), padding + textHeight + padding, 
							parent.getWidth() / 2 - padding, 
							parent.getHeight() - buttonHeight - padding - padding - padding - textHeight - padding);
				}else{
					comp.setBounds(buttonX, parent.getHeight() - buttonHeight - padding, buttonWidth, buttonHeight);
					buttonX += buttonWidth + padding;
				}
				i++;
			}
		}

		
	}
	
	private static interface FileListListener{
		
		public void selectionChanged(BuiltInFile file);
		
	}
	
	private static class FileList extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private FileListListener listener;
		private BuiltInFilesDialog parent;
		
		public FileList(BuiltInFilesDialog parent) {
			super();
			this.parent = parent;
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		}
		
		public void setListener(FileListListener listener) {
			this.listener = listener;
		}
		
		public void load(List<BuiltInFile> files) {
			removeAll();
			
			for(BuiltInFile file : files) {
				add(new FileListItem(this, file.name, file));
			}
		}
		
	}
	
	private static class FileListItem extends JPanel implements MouseListener{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private FileList parent;
		private BuiltInFile builtInFile;
		private JCheckBox overwriteCheckBox;
		
		public FileListItem(FileList parent, String label, BuiltInFile builtInFile) {
			super();
			this.parent = parent;
			this.builtInFile = builtInFile;
			
			setMinimumSize(new Dimension(50, 24));
			setMaximumSize(new Dimension(10000, 24));
			
			setLayout(new BorderLayout());
			
			overwriteCheckBox = new JCheckBox("", true);
			overwriteCheckBox.setPreferredSize(new Dimension(24, 24));
			overwriteCheckBox.setMinimumSize(new Dimension(24, 24));
			overwriteCheckBox.setMaximumSize(new Dimension(24, 24));
			overwriteCheckBox.setOpaque(false);
			overwriteCheckBox.setBorder(new EmptyBorder(0, 6, 0, 0));
			overwriteCheckBox.setToolTipText("Overwrite file");
			add(overwriteCheckBox, BorderLayout.WEST);
			
			JLabel nameLabel = new JLabel(label);
			nameLabel.setBorder(new EmptyBorder(0, 2, 0, 0));
			nameLabel.setOpaque(false);
			nameLabel.setVerticalAlignment(JLabel.CENTER);
			nameLabel.setToolTipText(label);
			nameLabel.addMouseListener(this);
			add(nameLabel, BorderLayout.CENTER);
			
			addMouseListener(this);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {}
		
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {
			parent.listener.selectionChanged(builtInFile);
			if(parent.parent.deltaViewer.getCurrentFile() == builtInFile) {
				setBackground(new Color(0.75f, 0.85f, 1.0f));
			}else {
				setBackground(parent.getBackground());
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {}
		
	}
	
	private static class CodeLayout implements LayoutManager{

		@Override
		public void addLayoutComponent(String name, Component comp) {
			
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			int width = 0;
			int height = 0;
			for(Component comp : parent.getComponents()) {
				width = Math.max(width, comp.getPreferredSize().width);
				height += comp.getPreferredSize().height;
			}
			return new Dimension(width, height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		@Override
		public void layoutContainer(Container parent) {
			int width = parent.getWidth();
			int y = 0;
			for(Component comp : parent.getComponents()) {
				comp.setBounds(0, y, width, comp.getPreferredSize().height);
				y += comp.getPreferredSize().height;
			}
		}

		
	}
	
	private static class DeltaViewer extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Reference<byte[]> bufferOriginal;
		private Reference<byte[]> bufferNew;
		private int bufferOriginalSize;
		private int bufferNewSize;
		private int lineHeight;
		private BuiltInFile currentFile;
		
		public DeltaViewer() {
			super();
			bufferOriginal = new Reference<byte[]>(new byte[4096]);
			bufferNew = new Reference<byte[]>(new byte[4096]);
			bufferOriginalSize = 0;
			bufferNewSize = 0;
			lineHeight = 16;
			
			setLayout(new CodeLayout());
		}
		
		public BuiltInFile getCurrentFile() {
			return currentFile;
		}
		
		public void load(BuiltInFile file) {
			currentFile = file;
			this.removeAll();
			revalidate();
			repaint();
			
			if(file == null) {
				invalidate();
				return;
			}
			
			try {
				// Read the data
				bufferOriginalSize = 0;
				bufferNewSize = 0;
				File actualFile = new File(FileUtil.getResourcePackDir(), file.name);
				if(actualFile.exists())
					bufferOriginalSize = readFromURL(actualFile.toURI().toURL(), bufferOriginal);
				
				bufferNewSize = readFromURL(file.url, bufferNew);
				
				if(bufferNewSize > (64 * 1024)) {
					invalidate();
					return;
				}
				
				// Now parse the data.
				List<Line> originalLines = parseLines(bufferOriginal, bufferOriginalSize);
				List<Line> newLines = parseLines(bufferNew, bufferNewSize);
				
				// First we go through the lines and try to match lines together
				int j = 0;
				for(int i = 0; i < originalLines.size(); ++i) {
					if(j >= newLines.size()) {
						// We've reached the end of newLines, so all of the 
						// following original lines aren't in newLines anymore
						// and thus got deleted.
						break;
					}
					Line originalLine = originalLines.get(i);
					Line newLine = newLines.get(j);
					if(originalLine.equals(newLine)) {
						// Lines match
						originalLine.matchedLineIndex = j;
						newLine.matchedLineIndex = i;
						j++;
					}else {
						// They don't match.
						// Either we have one or more lines removed from the original
						// or we have one or more lines added to the new
						
						// We check if lines got added by having a small window of a couple
						// of lines from original that we try to find in new.
						// If we find a match, then all lines until that match were added.
						// If we can't find a match, then we check if the original line was
						// removed.
						int searchWindow = Math.min(5, Math.min(originalLines.size() - i, newLines.size() - j));
						boolean match = false;
						for(int k = j + 1; k < newLines.size() - searchWindow; ++k) {
							// Check if we have our search window at line K
							match = true;
							for(int ki = 0; ki < searchWindow; ++ki) {
								Line originalLine2 = originalLines.get(i + ki);
								Line newLine2 = newLines.get(k + ki);
								if(!originalLine2.equals(newLine2)) {
									match = false;
									break;
								}
							}
							if(match) {
								j = k;
								break;
							}
						}
						if(match) {
							// We found our search window, so that means that
							// lines got added.
							newLine = newLines.get(j);
							originalLine.matchedLineIndex = j;
							newLine.matchedLineIndex = i;
							j++;
							continue;
						}
						
						// We didn't match, so it could be that the line got removed.
						// We basically do the same thing again, but the other way around.
						// We try to find the next few newLines in the original lines.
						searchWindow = Math.min(5, Math.min(originalLines.size() - i, newLines.size() - j));
						match = false;
						for(int k = i + 1; k < originalLines.size() - searchWindow; ++k) {
							match = true;
							for(int ki = 0; ki < searchWindow; ++ki) {
								Line originalLine2 = originalLines.get(k + ki);
								Line newLine2 = newLines.get(j + ki);
								if(!originalLine2.equals(newLine2)) {
									match = false;
									break;
								}
							}
							if(match) {
								i = k;
								break;
							}
						}
						if(match) {
							// We found our search window, so that means that
							// lines got removed
							originalLine = originalLines.get(i);
							originalLine.matchedLineIndex = j;
							newLine.matchedLineIndex = i;
							j++;
							continue;
						}
						
						// Both lines don't match and we can't find it further ahead,
						// so we assume that originalLine got removed and newLine got added.
						// In other words, the line got modified.
						j++;
					}
				}
				
				// Now that we've matched lines, let's add them in
				int originalI = 0;
				int newI = 0;
				while(true) {
					Line originalLine = null;
					Line newLine = null;
					if(originalI < originalLines.size())
						originalLine = originalLines.get(originalI);
					if(newI < newLines.size())
						newLine = newLines.get(newI);
					if(originalLine == null && newLine == null)
						break;
					
					if(originalLine == null) {
						// We've reached the end of the original lines, but we still
						// have new lines, so add those.
						add(new DeltaLine(newLine.getString(), newI, LineChange.ADDED, lineHeight));
						newI++;
						continue;
					}
					if(newLine == null) {
						// We've reached the end of the new lines, but we still
						// have original lines, so add those.
						add(new DeltaLine(originalLine.getString(), originalI, LineChange.REMOVED, lineHeight));
						originalI++;
						continue;
					}
					
					if(newLine.matchedLineIndex == originalI && originalLine.matchedLineIndex == newI) {
						// Line match
						add(new DeltaLine(newLine.getString(), newI, LineChange.NOTHING, lineHeight));
						originalI++;
						newI++;
					}else {
						if(originalLine.matchedLineIndex < 0) {
							// Original lines got deleted.
							add(new DeltaLine(originalLine.getString(), originalI, LineChange.REMOVED, lineHeight));
							originalI++;
							continue;
						}
						if(newLine.matchedLineIndex < 0) {
							// New line got added.
							add(new DeltaLine(newLine.getString(), newI, LineChange.ADDED, lineHeight));
							newI++;
							continue;
						}
						// The two lines don't point at each other, so somehow our parsing was wrong,
						// but let's try to show it anyways
						for(int i = originalI; i < newLine.matchedLineIndex && i < originalLines.size(); ++i) {
							add(new DeltaLine(originalLines.get(i).getString(), originalI, LineChange.REMOVED, lineHeight));
						}
						originalI = newLine.matchedLineIndex;
						if(originalI < originalLines.size()) {
							if(originalLines.get(originalI).matchedLineIndex != newI) {
								// newLine points to originalLine but originalLine doesn't point back
								// to newLine. Should never happen.
								// Just add both lines and increase.
								add(new DeltaLine(originalLines.get(originalI).getString(), originalI, LineChange.REMOVED, lineHeight));
								add(new DeltaLine(newLine.getString(), newI, LineChange.ADDED, lineHeight));
								originalI++;
								newI++;
							}
						}
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			invalidate();
		}
		
		private byte[] streamBuffer = new byte[4096];
		
		private int readFromURL(URL url, Reference<byte[]> buffer) {
			int read = 0;
			int totalRead = 0;
			InputStream is = null;
			try {
				is = url.openStream();
				
				while((read = is.read(streamBuffer)) > 0) {
					int newSize = totalRead + read;
					if(newSize > buffer.value.length)
						buffer.value = Arrays.copyOf(buffer.value, newSize);
					
					System.arraycopy(streamBuffer, 0, buffer.value, totalRead, read);
					totalRead += read;
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if(is != null) {
				try {
					is.close();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			return totalRead;
		}
		
		private List<Line> parseLines(Reference<byte[]> buffer, int length){
			List<Line> lines = new ArrayList<Line>();
			
			int startIndex = 0;
			for(int i = 0; i < length; ++i) {
				if(buffer.value[i] == '\r' || buffer.value[i] == '\n') {
					// We've reached the end of a line
					lines.add(new Line(buffer, startIndex, i));
					startIndex = i + 1;
					if((i + 1) < length) {
						if((buffer.value[i] == '\r' && buffer.value[i+1] == '\n') ||
								(buffer.value[i] == '\n' && buffer.value[i+1] == '\r')) {
							// On Windows \r\n is a new line, so we need to skip both.
							i++;
							startIndex = i + 1;
						}
					}
				}
			}
			if(startIndex < length) {
				// Add the final line
				lines.add(new Line(buffer, startIndex, length));
			}
			
			return lines;
		}
		
	}
	
	private static class Line{
		
		private Reference<byte[]> buffer;
		private int startIndex;
		private int endIndex;
		public int matchedLineIndex;
		
		public Line(Reference<byte[]> buffer, int startIndex, int endIndex) {
			this.buffer = buffer;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.matchedLineIndex = -1;
		}
		
		public int length() {
			return endIndex - startIndex;
		}
		
		public String getString() {
			return new String(buffer.value, startIndex, endIndex - startIndex);
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null)
				return false;
			if(!(obj instanceof Line))
				return false;
			Line other = (Line) obj;
			if(other.length() != length())
				return false;
			for(int i = 0; i < length(); ++i) {
				if(buffer.value[i + startIndex] != other.buffer.value[i + other.startIndex])
					return false;
			}
			return true;
		}
		
	}
	
	private static enum LineChange{
		NOTHING(""), ADDED("+"), REMOVED("-");
		
		public String indicatorString;
		LineChange(String indicatorString){
			this.indicatorString = indicatorString;
		}
	}
	
	private static class DeltaLine extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public DeltaLine(String line, int lineNumber, LineChange change, int height) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			
			if(change == LineChange.ADDED) {
				setBackground(new Color(0.5f, 1.0f, 0.5f));
			}else if(change == LineChange.REMOVED) {
				setBackground(new Color(1.0f, 0.5f, 0.5f));
			}
			
			setFont(new Font(Font.MONOSPACED, Font.PLAIN, getFont().getSize()));
			
			JLabel lineNumberLabel = new JLabel(Integer.toString(lineNumber));
			lineNumberLabel.setPreferredSize(new Dimension(height * 3, height));
			lineNumberLabel.setMinimumSize(lineNumberLabel.getPreferredSize());
			lineNumberLabel.setMaximumSize(lineNumberLabel.getPreferredSize());
			lineNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			lineNumberLabel.setOpaque(false);
			lineNumberLabel.setFont(getFont());
			add(lineNumberLabel);
			
			JLabel changeIndicator = new JLabel(change.indicatorString);
			changeIndicator.setPreferredSize(new Dimension((height * 3) / 2, height));
			changeIndicator.setMinimumSize(changeIndicator.getPreferredSize());
			changeIndicator.setMaximumSize(changeIndicator.getPreferredSize());
			changeIndicator.setOpaque(false);
			changeIndicator.setFont(getFont());
			add(changeIndicator);
			
			JLabel lineLabel = new JLabel(line);
			lineLabel.setFont(getFont());
			int lineWidth = lineLabel.getFontMetrics(lineLabel.getFont()).stringWidth(line) + 24;
			lineLabel.setPreferredSize(new Dimension(lineWidth, height));
			lineLabel.setMinimumSize(lineLabel.getPreferredSize());
			lineLabel.setMaximumSize(lineLabel.getPreferredSize());
			lineLabel.setOpaque(false);
			add(lineLabel);
			
			setPreferredSize(new Dimension(lineNumberLabel.getPreferredSize().width + 
							changeIndicator.getPreferredSize().width + lineLabel.getPreferredSize().width, height));
			setMinimumSize(getPreferredSize());
			setMaximumSize(getPreferredSize());
			
			setAlignmentX(0f);
		}
		
	}

}
