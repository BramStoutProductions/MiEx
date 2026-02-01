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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.image.ImageReader;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;
import nl.bramstout.mcworldexporter.launcher.MinecraftSave;

public class WorldBrowser extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<Tab> tabs;
	private Tab activeTab;
	private JScrollPane savesScrollPane;
	private JPanel savesPanel;
	private String searchString;
	
	public WorldBrowser() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		JPanel root = new JPanel();
		root.setLayout(new BorderLayout());
		add(root);
		
		tabs = new ArrayList<Tab>();
		activeTab = null;
		JPanel tabPanel = new JPanel();
		tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.X_AXIS));
		for(Launcher launcher : LauncherRegistry.getLaunchers()) {
			Tab tab = new Tab(launcher, this);
			tabs.add(tab);
			tabPanel.add(tab);
		}
		tabPanel.add(new BrowseTab(this));
		root.add(tabPanel, BorderLayout.NORTH);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout(0, 0));
		root.add(centerPanel, BorderLayout.CENTER);
		
		SearchField searchField = new SearchField();
		searchField.setPlaceholder("Search");
		searchField.setPreferredSize(new Dimension(150, 28));
		searchField.setFont(searchField.getFont().deriveFont(15F));
		searchField.setMargin(new Insets(2, 8, 2, 8));
		searchField.setBorder(new EmptyBorder(2, 8, 6, 8));
		centerPanel.add(searchField, BorderLayout.NORTH);
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				setSearchString(searchField.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				setSearchString(searchField.getText());
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				setSearchString(searchField.getText());
			}
		});
		
		savesPanel = new JPanel();
		savesPanel.setLayout(new BoxLayout(savesPanel, BoxLayout.Y_AXIS));
		savesScrollPane = new JScrollPane(savesPanel);
		savesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		savesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		savesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		centerPanel.add(savesScrollPane, BorderLayout.CENTER);
		
		setSize(600, 600);
		setTitle("Open World");
		
		if(tabs.size() > 0)
			setActiveTab(tabs.get(0));
	}
	
	private void setActiveTab(Tab tab) {
		if(activeTab == tab)
			return;
		
		for(Tab tab2 : tabs) {
			tab2.setBackground(getBackground());
			tab2.label.setForeground(new Color(64, 64, 64));
		}
		tab.setBackground(getBackground().brighter());
		tab.label.setForeground(new Color(0, 0, 0));
		
		savesPanel.removeAll();
		for(MinecraftSave save : tab.launcher.getSaves()) {
			savesPanel.add(new Save(this, save));
		}
		
		setSearchString(searchString);
	}
	
	private void setSearchString(String searchString) {
		this.searchString = searchString;
		
		for(Component comp : savesPanel.getComponents()) {
			if(comp instanceof Save) {
				if(((Save)comp).matchesSearchString(searchString)) {
					comp.setVisible(true);
				}else {
					comp.setVisible(false);
				}
			}
		}
		
		savesPanel.invalidate();
		savesScrollPane.invalidate();
		validate();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				savesPanel.repaint();
				savesScrollPane.repaint();
				repaint();
			}
			
		});
	}
	
	private void openWorld(File worldFolder, String name, Launcher launcher) {
		setVisible(false);
		MCWorldExporter.getApp().setLastExportFileOpened(null);
		MCWorldExporter.getApp().setWorld(worldFolder, name, launcher);
	}
	
	private static class Tab extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		JLabel label;
		Launcher launcher;

		public Tab(Launcher launcher, WorldBrowser browser) {
			super();
			this.launcher = launcher;
			
			setLayout(new BorderLayout());
			
			label = new JLabel(launcher.getName());
			label.setFont(new Font(getFont().getName(), Font.BOLD, 14));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			
			add(label, BorderLayout.CENTER);
			
			setPreferredSize(new Dimension(150, 48));
			
			final Tab thisTab = this;
			
			addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {}

				@Override
				public void mousePressed(MouseEvent e) {
					browser.setActiveTab(thisTab);
				}

				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {}

				@Override
				public void mouseExited(MouseEvent e) {}
				
			});
		}
		
	}
	
	private static class BrowseTab extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static File currentDir = null;

		public BrowseTab(WorldBrowser browser) {
			super();
			
			setLayout(new BorderLayout());
			
			JLabel label = new JLabel("Browse");
			label.setFont(new Font(getFont().getName(), Font.BOLD, 14));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setForeground(new Color(64, 64, 64));
			
			add(label, BorderLayout.CENTER);
			
			setPreferredSize(new Dimension(150, 48));
			
			addMouseListener(new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {}

				@Override
				public void mousePressed(MouseEvent e) {
					if(currentDir == null) {
						currentDir = new File(FileUtil.getHomeDir());
					}
					
					JFileChooser chooser = new JFileChooser();
					chooser.setApproveButtonText("Load");
					chooser.setDialogTitle("Load World");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setFileFilter(null);
					chooser.setCurrentDirectory(currentDir);
					chooser.setAcceptAllFileFilterUsed(false);
					int result = chooser.showOpenDialog(browser);
					currentDir = chooser.getCurrentDirectory();
					if (result == JFileChooser.APPROVE_OPTION) {
						browser.openWorld(chooser.getSelectedFile(), chooser.getSelectedFile().getName(), null);
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {}

				@Override
				public void mouseExited(MouseEvent e) {}
				
			});
		}
		
	}
	
	private static class Save extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private static BufferedImage noIcon = new BufferedImage(52, 52, BufferedImage.TYPE_INT_ARGB);
		private MinecraftSave save;
		
		public Save(WorldBrowser browser, MinecraftSave save) {
			super();
			this.save = save;
			
			setPreferredSize(new Dimension(250, 64));
			setMinimumSize(new Dimension(0, 64));
			setMaximumSize(new Dimension(100000, 64));

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(5, 5, 5, 5)));
			
			BufferedImage iconImage = noIcon;
			if(save.getIcon().exists()) {
				try {
					BufferedImage img = ImageReader.readImage(save.getIcon());
					
					iconImage = new BufferedImage(52, 52, BufferedImage.TYPE_INT_ARGB);
					if(img != null) {
						Graphics2D g = (Graphics2D) iconImage.getGraphics();
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						
						float aspectRatio = ((float) img.getWidth()) / ((float) img.getHeight());
						g.drawImage(img, (int) (((52f * aspectRatio) - 52f) / -2f), 0, (int) (52f * aspectRatio), 52, null);
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
			
			Icon icon = new ImageIcon(iconImage);
			
			JLabel iconLabel = new JLabel(icon);
			iconLabel.setBorder(new EmptyBorder(0, 0, 0, 8));
			add(iconLabel);
			
			JLabel label = new JLabel(save.getLabel());
			label.setFont(new Font(getFont().getName(), Font.PLAIN, 14));
			add(label);
			
			add(new JPanel());
			
			JButton openButton = new JButton("Open");
			openButton.setMinimumSize(new Dimension(100, 52));
			openButton.setMaximumSize(new Dimension(100, 52));
			openButton.setPreferredSize(new Dimension(100, 52));
			add(openButton);
			
			openButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					browser.openWorld(save.getWorldFolder(), save.getLabel(), save.getLauncher());
				}
				
			});
		}
		
		public boolean matchesSearchString(String searchString) {
			if(searchString == null || searchString.isEmpty())
				return true;
			
			return save.getLabel().contains(searchString);
		}
		
	}
	
	private static class SearchField extends JTextField{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String placeholder;
		
		public SearchField() {
			super();
			addFocusListener(new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {}
				
				@Override
				public void focusGained(FocusEvent e) {
					selectAll();
				}
			});
		}
		
		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			if(this.getText().isEmpty()) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setFont(getFont());
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(getDisabledTextColor());
				g2d.drawString(placeholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
			}
		}
		
	}
	
}
