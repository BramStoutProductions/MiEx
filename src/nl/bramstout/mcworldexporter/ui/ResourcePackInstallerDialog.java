package nl.bramstout.mcworldexporter.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.ui.WorldBrowser.SearchField;

public class ResourcePackInstallerDialog extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel availablePanel;
	private JScrollPane availableScrollPane;
	private JPanel sourcesPanel;
	private JScrollPane sourcesScrollPane;
	private JTextField saveToInput;
	private String availableSearchString;
	private String sourcesSearchString;
	
	public ResourcePackInstallerDialog() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		setTitle("Resource Pack Installer");
		
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
		root.setBorder(new EmptyBorder(0, 0, 0, 0));
		add(root);
		
		JPanel availableRootPanel = new JPanel();
		availableRootPanel.setLayout(new BoxLayout(availableRootPanel, BoxLayout.Y_AXIS));
		availableRootPanel.setBorder(new EmptyBorder(16, 16, 16, 8));
		availableRootPanel.setPreferredSize(new Dimension(400, 400));
		availableRootPanel.setMinimumSize(new Dimension(300, 100));
		availableRootPanel.setMaximumSize(new Dimension(100000, 100000));
		root.add(availableRootPanel);
		
		JPanel sourcesRootPanel = new JPanel();
		sourcesRootPanel.setLayout(new BoxLayout(sourcesRootPanel, BoxLayout.Y_AXIS));
		sourcesRootPanel.setBorder(new EmptyBorder(16, 8, 16, 16));
		sourcesRootPanel.setPreferredSize(new Dimension(400, 400));
		sourcesRootPanel.setMinimumSize(new Dimension(300, 100));
		sourcesRootPanel.setMaximumSize(new Dimension(100000, 100000));
		root.add(sourcesRootPanel);
		
		
		
		JLabel availableLabel = new JLabel("Available Sources");
		availableLabel.setAlignmentX(0f);
		availableRootPanel.add(availableLabel);
		
		SearchField availableSearchField = new SearchField();
		availableSearchField.setPlaceholder("Search");
		availableSearchField.setPreferredSize(new Dimension(260, 14));
		availableSearchField.setFont(availableSearchField.getFont().deriveFont(10F));
		availableSearchField.setMargin(new Insets(1, 4, 1, 4));
		availableSearchField.setBorder(new EmptyBorder(1, 4, 1, 4));
		availableSearchField.setAlignmentX(0f);
		availableRootPanel.add(availableSearchField);
		availableSearchField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				setAvailableSearchString(availableSearchField.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				setAvailableSearchString(availableSearchField.getText());
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				setAvailableSearchString(availableSearchField.getText());
			}
		});
		
		availablePanel = new JPanel();
		availablePanel.setLayout(new BoxLayout(availablePanel, BoxLayout.Y_AXIS));
		availablePanel.setBackground(getBackground().brighter());
		availableScrollPane = new JScrollPane(availablePanel);
		availableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		availableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		availableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		availableScrollPane.setPreferredSize(new Dimension(400, 400));
		availableScrollPane.setAlignmentX(0f);
		availableRootPanel.add(availableScrollPane);
		
		JPanel additionalSourcesPanel = new JPanel();
		additionalSourcesPanel.setLayout(new BoxLayout(additionalSourcesPanel, BoxLayout.X_AXIS));
		additionalSourcesPanel.setAlignmentX(0f);
		additionalSourcesPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		additionalSourcesPanel.setMinimumSize(new Dimension(0, 24));
		additionalSourcesPanel.setPreferredSize(new Dimension(400, 24));
		additionalSourcesPanel.setMaximumSize(new Dimension(10000, 24));
		availableRootPanel.add(additionalSourcesPanel);
		JButton browseSource = new JButton("Browse Sources");
		browseSource.setMinimumSize(new Dimension(0, 24));
		browseSource.setPreferredSize(new Dimension(200, 24));
		browseSource.setMaximumSize(new Dimension(10000, 24));
		additionalSourcesPanel.add(browseSource);
		JButton browseModsFolder = new JButton("Browse Mods Folder");
		browseModsFolder.setMinimumSize(new Dimension(0, 24));
		browseModsFolder.setPreferredSize(new Dimension(200, 24));
		browseModsFolder.setMaximumSize(new Dimension(10000, 24));
		additionalSourcesPanel.add(browseModsFolder);
		JPanel paddingPanel = new JPanel();
		paddingPanel.setMinimumSize(new Dimension(0, 24));
		paddingPanel.setPreferredSize(new Dimension(400, 24));
		paddingPanel.setMaximumSize(new Dimension(10000, 24));
		paddingPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		paddingPanel.setAlignmentX(0f);
		paddingPanel.add(new JLabel(" "));
		availableRootPanel.add(paddingPanel);
		
		
		
		JLabel sourcesLabel = new JLabel("Sources To Install");
		sourcesLabel.setAlignmentX(0f);
		sourcesRootPanel.add(sourcesLabel);
		
		SearchField sourcesSearchField = new SearchField();
		sourcesSearchField.setPlaceholder("Search");
		sourcesSearchField.setPreferredSize(new Dimension(260, 14));
		sourcesSearchField.setFont(sourcesSearchField.getFont().deriveFont(10F));
		sourcesSearchField.setMargin(new Insets(1, 4, 1, 4));
		sourcesSearchField.setBorder(new EmptyBorder(1, 4, 1, 4));
		sourcesSearchField.setAlignmentX(0f);
		sourcesRootPanel.add(sourcesSearchField);
		sourcesSearchField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				setSourcesSearchString(sourcesSearchField.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				setSourcesSearchString(sourcesSearchField.getText());
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				setSourcesSearchString(sourcesSearchField.getText());
			}
		});
		
		sourcesPanel = new JPanel();
		sourcesPanel.setLayout(new BoxLayout(sourcesPanel, BoxLayout.Y_AXIS));
		sourcesPanel.setBackground(getBackground().brighter());
		sourcesScrollPane = new JScrollPane(sourcesPanel);
		sourcesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sourcesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		sourcesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		sourcesScrollPane.setPreferredSize(new Dimension(400, 400));
		sourcesScrollPane.setAlignmentX(0f);
		sourcesRootPanel.add(sourcesScrollPane);
		
		saveToInput = new JTextField();
		saveToInput.setToolTipText("The resource pack name to install the sources.");
		saveToInput.setAlignmentX(0f);
		saveToInput.setMinimumSize(new Dimension(0, 24));
		saveToInput.setPreferredSize(new Dimension(400, 24));
		saveToInput.setMaximumSize(new Dimension(100000, 24));
		sourcesRootPanel.add(saveToInput);
		
		JButton installButton = new JButton("Install Sources");
		installButton.setAlignmentX(0f);
		installButton.setMinimumSize(new Dimension(0, 24));
		installButton.setPreferredSize(new Dimension(400, 24));
		installButton.setMaximumSize(new Dimension(10000, 24));
		sourcesRootPanel.add(installButton);
		
		pack();
		setSize(700, 500);
		
		
		browseSource.addActionListener(new ActionListener() {
			
			private void findSources(File folder) {
				for(File f : folder.listFiles()) {
					if(ResourcePacks.isValidResourcePackFile(f)) {
						ResourcePackSource source = new ResourcePackSource(f.getName(), null);
						source.addSource(ResourcePackSource.getHash(f), f);
						enableSource(source);
					}else if(f.isDirectory()) {
						findSources(f);
					}
				}
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Add");
				chooser.setDialogTitle("Select Sources");
				chooser.setMultiSelectionEnabled(true);
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				if (new File(FileUtil.getMinecraftRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMinecraftRootDir()));
				if (!FileUtil.getMultiMCRootDir().equals("") && new File(FileUtil.getMultiMCRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMultiMCRootDir()));
				if (!FileUtil.getTechnicRootDir().equals("") && new File(FileUtil.getTechnicRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getTechnicRootDir()));
				chooser.setFileFilter(null);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					for(File f : chooser.getSelectedFiles()) {
						if(ResourcePacks.isValidResourcePackFile(f)) {
							ResourcePackSource source = new ResourcePackSource(f.getName(), null);
							source.addSource(ResourcePackSource.getHash(f), f);
							enableSource(source);
						}else if(f.isDirectory()) {
							findSources(f);
						}
					}
				}
			}
		});
		
		browseModsFolder.addActionListener(new ActionListener() {
			
			private void findSources(File folder) {
				for(File f : folder.listFiles()) {
					if(ResourcePacks.isValidResourcePackFile(f)) {
						ResourcePackSource source = new ResourcePackSource(f.getName(), null);
						source.addSource(ResourcePackSource.getHash(f), f);
						enableSource(source);
					}else if(f.isDirectory()) {
						findSources(f);
					}
				}
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setApproveButtonText("Add");
				chooser.setDialogTitle("Select Sources");
				chooser.setMultiSelectionEnabled(true);
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				if (new File(FileUtil.getMinecraftRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMinecraftRootDir()));
				if (!FileUtil.getMultiMCRootDir().equals("") && new File(FileUtil.getMultiMCRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getMultiMCRootDir()));
				if (!FileUtil.getTechnicRootDir().equals("") && new File(FileUtil.getTechnicRootDir()).exists())
					chooser.setCurrentDirectory(new File(FileUtil.getTechnicRootDir()));
				chooser.setFileFilter(null);
				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
				if (result == JFileChooser.APPROVE_OPTION) {
					for(File f : chooser.getSelectedFiles()) {
						if(ResourcePacks.isValidResourcePackFile(f)) {
							ResourcePackSource source = new ResourcePackSource(f.getName(), null);
							source.addSource(ResourcePackSource.getHash(f), f);
							enableSource(source);
						}else if(f.isDirectory()) {
							findSources(f);
						}
					}
				}
			}
		});
		
		installButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(saveToInput.getText().isEmpty() || saveToInput.getText().equalsIgnoreCase("Install To Resource Pack")) {
					Popups.showMessageDialog(MCWorldExporter.getApp().getUI(), "Invalid resource pack name!", "", Popups.ERROR_MESSAGE);
					return;
				}
				
				List<ResourcePackSource> sources = new ArrayList<ResourcePackSource>();
				
				for(Component comp : sourcesPanel.getComponents()) {
					if(comp instanceof ActiveSource) {
						sources.add(((ActiveSource) comp).getSource());
					}
				}
				
				if(sources.isEmpty()) {
					Popups.showMessageDialog(MCWorldExporter.getApp().getUI(), "No sources!", "", Popups.ERROR_MESSAGE);
					return;
				}
				
				File resourcePackFolder = new File(FileUtil.getResourcePackDir(), saveToInput.getText());
				
				System.out.println("Installing resource pack from sources into " + saveToInput.getText());
				MCWorldExporter.getApp().getUI().getProgressBar().setText("Extracting resources");
				
				try {
					ResourcePackDefaults.extractSourcesIntoResourcePack(sources, resourcePackFolder);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
				MCWorldExporter.getApp().getUI().getProgressBar().setText("");
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(true);
				
				// Reload everything
				ResourcePacks.load();
				List<ResourcePack> currentlyLoaded = ResourcePacks.getActiveResourcePacks();
				List<String> currentlyLoadedUUIDS = new ArrayList<String>();
				for(ResourcePack pack : currentlyLoaded)
					currentlyLoadedUUIDS.add(pack.getUUID());
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
				MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(currentlyLoadedUUIDS);
				
				Config.load();
			}
		});
	}
	
	public void enableSource(ResourcePackSource source) {
		for(Component comp : availablePanel.getComponents()) {
			if(comp instanceof AvailableSource) {
				if(((AvailableSource)comp).getSource().equals(source)) {
					availablePanel.remove(comp);
					if(source.getLauncher() == null) {
						source = ((AvailableSource)comp).getSource();
					}
				}
			}
		}
		for(Component comp : sourcesPanel.getComponents()) {
			if(comp instanceof ActiveSource) {
				if(((ActiveSource)comp).getSource().equals(source)) {
					// Already enabled.
					return;
				}
			}
		}
		sourcesPanel.add(new ActiveSource(source, this));
		sortPanel(sourcesPanel);
		setSourcesSearchString(sourcesSearchString);
		invalidate();
		revalidate();
		repaint();
	}
	
	public void disableSource(ResourcePackSource source) {
		for(Component comp : sourcesPanel.getComponents()) {
			if(comp instanceof ActiveSource) {
				if(((ActiveSource)comp).getSource().equals(source)) {
					sourcesPanel.remove(comp);
					if(source.getLauncher() == null) {
						source = ((ActiveSource)comp).getSource();
					}
				}
			}
		}
		for(Component comp : availablePanel.getComponents()) {
			if(comp instanceof AvailableSource) {
				if(((AvailableSource)comp).getSource().equals(source)) {
					// Already enabled.
					return;
				}
			}
		}
		availablePanel.add(new AvailableSource(source, this));
		sortPanel(availablePanel);
		setAvailableSearchString(availableSearchString);
		invalidate();
		revalidate();
		repaint();
	}
	
	public void sortPanel(JPanel panel) {
		try {
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					Component comps[] = panel.getComponents();
					Arrays.sort(comps, new Comparator<Component>() {
						
						private int compareString(String s1, String s2) {
							for(int i = 0; i < Math.min(s1.length(), s2.length()); ++i) {
								int diff = s1.codePointAt(i) - s2.codePointAt(i);
								if(diff == 0)
									continue;
								return diff;
							}
							return 0;
						}

						@Override
						public int compare(Component o1, Component o2) {
							String l1 = "";
							String l2 = "";
							if(o1 instanceof AvailableSource && ((AvailableSource)o1).getSource().getLauncher() != null)
								l1 = ((AvailableSource)o1).getSource().getLauncher().getName().toLowerCase();
							if(o2 instanceof AvailableSource && ((AvailableSource)o2).getSource().getLauncher() != null)
								l2 = ((AvailableSource)o2).getSource().getLauncher().getName().toLowerCase();
							if(o1 instanceof ActiveSource && ((ActiveSource)o1).getSource().getLauncher() != null)
								l1 = ((ActiveSource)o1).getSource().getLauncher().getName().toLowerCase();
							if(o2 instanceof ActiveSource && ((ActiveSource)o2).getSource().getLauncher() != null)
								l2 = ((ActiveSource)o2).getSource().getLauncher().getName().toLowerCase();
							
							int c = compareString(l1, l2);
							if(c != 0)
								return c;
							
							String s1 = "";
							String s2 = "";
							if(o1 instanceof AvailableSource)
								s1 = ((AvailableSource)o1).getSource().getName().toLowerCase();
							if(o2 instanceof AvailableSource)
								s2 = ((AvailableSource)o2).getSource().getName().toLowerCase();
							if(o1 instanceof ActiveSource)
								s1 = ((ActiveSource)o1).getSource().getName().toLowerCase();
							if(o2 instanceof ActiveSource)
								s2 = ((ActiveSource)o2).getSource().getName().toLowerCase();
							return compareString(s1, s2);
						}
						
					});
					panel.removeAll();
					for(Component comp : comps) {
						panel.add(comp);
					}
				}
				
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void setSourcesSearchString(String searchString) {
		this.sourcesSearchString = searchString;
		setPanelSearchString(searchString, sourcesPanel, sourcesScrollPane);
	}
	
	private void setAvailableSearchString(String searchString) {
		this.availableSearchString = searchString;
		setPanelSearchString(searchString, availablePanel, availableScrollPane);
	}
	
	private void setPanelSearchString(String searchString, JPanel panel, JScrollPane scrollpane) {
		String[] searchStrings = null;
		if(searchString != null) {
			searchStrings = searchString.toLowerCase().split(" ");
		}
		for(Component comp : panel.getComponents()) {
			if(comp instanceof AvailableSource) {
				if(((AvailableSource)comp).matchesSearchString(searchStrings)) {
					comp.setVisible(true);
				}else {
					comp.setVisible(false);
				}
			}
			if(comp instanceof ActiveSource) {
				if(((ActiveSource)comp).matchesSearchString(searchStrings)) {
					comp.setVisible(true);
				}else {
					comp.setVisible(false);
				}
			}
		}
		
		panel.invalidate();
		scrollpane.invalidate();
		validate();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				panel.repaint();
				scrollpane.repaint();
				repaint();
			}
			
		});
	}
	
	@Override
	public void setVisible(boolean b) {
		if(b) {
			saveToInput.setText("Install To Resource Pack");
			
			availablePanel.removeAll();
			for(Launcher launcher : LauncherRegistry.getLaunchers()) {
				for(ResourcePackSource source : launcher.getAllResourcePackSources()) {
					availablePanel.add(new AvailableSource(source, this));
				}
			}
			
			sortPanel(availablePanel);
			invalidate();
			revalidate();
			repaint();
		}
		super.setVisible(b);
	}
	
	public static class AvailableSource extends JPanel{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private ResourcePackSource source;
		
		public AvailableSource(ResourcePackSource source, ResourcePackInstallerDialog dialog) {
			this.source = source;
			
			setPreferredSize(new Dimension(200, 48));
			setMinimumSize(new Dimension(100, 48));
			setMaximumSize(new Dimension(10000, 48));
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(4, 4, 4, 4)));
			
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
			infoPanel.setBorder(new EmptyBorder(0, 0, 0, 4));
			infoPanel.setToolTipText(source.getName());
			add(infoPanel);
			
			JLabel nameLabel = new JLabel(source.getName());
			nameLabel.setMinimumSize(new Dimension(0, 20));
			nameLabel.setMaximumSize(new Dimension(10000, 20));
			nameLabel.setPreferredSize(new Dimension(100, 20));
			nameLabel.setFont(nameLabel.getFont().deriveFont(16f));
			infoPanel.add(nameLabel);
			
			JLabel launcherLabel = new JLabel("");
			if(source.getLauncher() != null) {
				launcherLabel.setText(source.getLauncher().getName());
			}
			launcherLabel.setMinimumSize(new Dimension(0, 16));
			launcherLabel.setMaximumSize(new Dimension(10000, 16));
			launcherLabel.setPreferredSize(new Dimension(100, 16));
			nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
			infoPanel.add(launcherLabel);
			
			JButton addButton = new JButton("+");
			addButton.setMinimumSize(new Dimension(32, 32));
			addButton.setMaximumSize(new Dimension(32, 32));
			addButton.setPreferredSize(new Dimension(32, 32));
			addButton.setMargin(new Insets(4, 0, 4, 0));
			add(addButton);
			
			addButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.enableSource(source);
				}
			});
		}
		
		public ResourcePackSource getSource() {
			return source;
		}
		
		public boolean matchesSearchString(String[] searchStrings) {
			if(searchStrings == null)
				return true;
			
			String name = source.getName().toLowerCase();
			String launcher = "";
			if(source.getLauncher() != null)
				launcher = source.getLauncher().getName().toLowerCase();
			
			for(String searchString : searchStrings) {
				if(!name.contains(searchString) && !launcher.contains(searchString))
					return false;
			}
			return true;
		}
		
	}
	
	public static class ActiveSource extends JPanel{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private ResourcePackSource source;
		
		public ActiveSource(ResourcePackSource source, ResourcePackInstallerDialog dialog) {
			this.source = source;
			
			setPreferredSize(new Dimension(200, 48));
			setMinimumSize(new Dimension(100, 48));
			setMaximumSize(new Dimension(10000, 48));
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(4, 4, 4, 4)));
			
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
			infoPanel.setBorder(new EmptyBorder(0, 0, 0, 4));
			infoPanel.setToolTipText(source.getName());
			add(infoPanel);
			
			JLabel nameLabel = new JLabel(source.getName());
			nameLabel.setMinimumSize(new Dimension(0, 20));
			nameLabel.setMaximumSize(new Dimension(10000, 20));
			nameLabel.setPreferredSize(new Dimension(100, 20));
			nameLabel.setFont(nameLabel.getFont().deriveFont(16f));
			infoPanel.add(nameLabel);
			
			JLabel launcherLabel = new JLabel("");
			if(source.getLauncher() != null) {
				launcherLabel.setText(source.getLauncher().getName());
			}
			launcherLabel.setMinimumSize(new Dimension(0, 16));
			launcherLabel.setMaximumSize(new Dimension(10000, 16));
			launcherLabel.setPreferredSize(new Dimension(100, 16));
			nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
			infoPanel.add(launcherLabel);
			
			JButton removeButton = new JButton("-");
			removeButton.setMinimumSize(new Dimension(32, 32));
			removeButton.setMaximumSize(new Dimension(32, 32));
			removeButton.setPreferredSize(new Dimension(32, 32));
			removeButton.setMargin(new Insets(4, 0, 4, 0));
			add(removeButton);
			
			removeButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.disableSource(source);
				}
			});
		}
		
		public ResourcePackSource getSource() {
			return source;
		}
		
		public boolean matchesSearchString(String[] searchStrings) {
			if(searchStrings == null)
				return true;
			
			String name = source.getName().toLowerCase();
			String launcher = "";
			if(source.getLauncher() != null)
				launcher = source.getLauncher().getName().toLowerCase();
			
			for(String searchString : searchStrings) {
				if(!name.contains(searchString) && !launcher.contains(searchString))
					return false;
			}
			return true;
		}
		
	}

}
