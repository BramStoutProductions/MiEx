package nl.bramstout.mcworldexporter.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class ResourcePackSelector extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JScrollPane availableScrollPane;
	private JPanel availablePanel;
	private JScrollPane activeScrollPane;
	private JPanel activePanel;
	
	private List<String> activeResourcePacks;
	private Runnable onResourcePackChange;
	
	public ResourcePackSelector() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(0, 0, 0, 0));
		
		activeResourcePacks = new ArrayList<String>();
		onResourcePackChange = null;
		
		JLabel availableLabel = new JLabel("Available:");
		availableLabel.setBorder(null);
		availableLabel.setHorizontalAlignment(SwingConstants.CENTER);
		availableLabel.setPreferredSize(new Dimension(260, 20));
		availableLabel.setMinimumSize(availableLabel.getPreferredSize());
		availableLabel.setMaximumSize(availableLabel.getPreferredSize());
		add(availableLabel);
		availablePanel = new JPanel();
		availablePanel.setLayout(new BoxLayout(availablePanel, BoxLayout.Y_AXIS));
		availablePanel.setBackground(getBackground().brighter());
		availableScrollPane = new JScrollPane(availablePanel);
		availableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		availableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		availableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(availableScrollPane);
		
		JPanel separator = new JPanel();
		separator.setMinimumSize(new Dimension(0, 10));
		separator.setMaximumSize(new Dimension(0, 10));
		separator.setPreferredSize(new Dimension(0, 10));
		add(separator);
		
		
		JLabel activeLabel = new JLabel("Active:");
		activeLabel.setBorder(null);
		activeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		activeLabel.setPreferredSize(new Dimension(260, 20));
		activeLabel.setMinimumSize(activeLabel.getPreferredSize());
		activeLabel.setMaximumSize(activeLabel.getPreferredSize());
		add(activeLabel);
		activePanel = new JPanel();
		activePanel.setLayout(new BoxLayout(activePanel, BoxLayout.Y_AXIS));
		activePanel.setBackground(getBackground().brighter());
		activeScrollPane = new JScrollPane(activePanel);
		activeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		activeScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		activeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(activeScrollPane);
		
		addComponentListener(new ComponentListener() {

			@Override
			public void componentResized(ComponentEvent e) {
				int width = 300;
				int height = (getHeight() - 16) / 2;
				availableScrollPane.setPreferredSize(new Dimension(width, height));
				activeScrollPane.setPreferredSize(new Dimension(width, height));
			}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentShown(ComponentEvent e) {}

			@Override
			public void componentHidden(ComponentEvent e) {}
			
		});
		
		int width = 300;
		int height = (getHeight() - 16) / 2;
		availableScrollPane.setPreferredSize(new Dimension(width, height));
		activeScrollPane.setPreferredSize(new Dimension(width, height));
	}
	
	public void reset() {
		availablePanel.removeAll();
		activePanel.removeAll();
		
		File dir = new File(FileUtil.getResourcePackDir().substring(0, FileUtil.getResourcePackDir().length()-1));
		if(dir.exists()) {
			File[] files = dir.listFiles();
			if(files != null)
				for (File f : files)
					if (f.isDirectory() && !f.getName().equals("base_resource_pack"))
						availablePanel.add(new AvailableResourcePack(this, f.getName()));
		}
		
		activePanel.add(new ActiveResourcePack(this, "base_resource_pack", true));
		sortAvailableResourcePacks();
		synchActiveResourcePacks();
		// Add in the resource packs that needed to be loaded by default.
		// enableResourcePack puts the specified resource pack at the top,
		// so we need to add them in reverse order.
		for(int i = MCWorldExporter.defaultResourcePacks.size() - 1; i >= 0; --i)
			enableResourcePack(MCWorldExporter.defaultResourcePacks.get(i));
	}
	
	public void clear() {
		for(String pack : activeResourcePacks)
			disableResourcePack(pack);
	}
	
	public void enableResourcePack(String name) {
		for(Component comp : availablePanel.getComponents()) {
			if(comp instanceof AvailableResourcePack) {
				if(((AvailableResourcePack)comp).getName().equals(name)) {
					availablePanel.remove(comp);
					activePanel.add(new ActiveResourcePack(this, name, false),0);
					break;
				}
			}
		}
		updateUI();
		synchActiveResourcePacks();
	}
	
	public void disableResourcePack(String name) {
		for(Component comp : activePanel.getComponents()) {
			if(comp instanceof ActiveResourcePack) {
				if(((ActiveResourcePack)comp).getName().equals(name)) {
					if(((ActiveResourcePack)comp).isNoMove())
						continue;
					activePanel.remove(comp);
					availablePanel.add(new AvailableResourcePack(this, name));
					sortAvailableResourcePacks();
					break;
				}
			}
		}
		updateUI();
		synchActiveResourcePacks();
	}
	
	public void moveActiveResourcePackDown(String name) {
		int i = 0;
		for(Component comp : activePanel.getComponents()) {
			if(comp instanceof ActiveResourcePack) {
				if(((ActiveResourcePack)comp).getName().equals(name)) {
					if(((ActiveResourcePack)comp).isNoMove())
						continue;
					activePanel.remove(comp);
					// getComponentCount() - 1 so that base_resource_pack will always be at the bottom
					activePanel.add(new ActiveResourcePack(this, name, false), Math.min(i + 1, activePanel.getComponentCount()-1));
					break;
				}
			}
			++i;
		}
		updateUI();
		synchActiveResourcePacks();
	}
	
	public void moveActiveResourcePackUp(String name) {
		int i = 0;
		for(Component comp : activePanel.getComponents()) {
			if(comp instanceof ActiveResourcePack) {
				if(((ActiveResourcePack)comp).getName().equals(name)) {
					if(((ActiveResourcePack)comp).isNoMove())
						continue;
					activePanel.remove(comp);
					activePanel.add(new ActiveResourcePack(this, name, false), Math.max(i - 1, 0));
					break;
				}
			}
			++i;
		}
		updateUI();
		synchActiveResourcePacks();
	}
	
	public void sortAvailableResourcePacks() {
		Component comps[] = availablePanel.getComponents();
		Arrays.sort(comps, new Comparator<Component>() {

			@Override
			public int compare(Component o1, Component o2) {
				if(o1 instanceof AvailableResourcePack && o2 instanceof AvailableResourcePack) {
					String s1 = ((AvailableResourcePack)o1).getName();
					String s2 = ((AvailableResourcePack)o2).getName();
					for(int i = 0; i < Math.min(s1.length(), s2.length()); ++i) {
						int diff = s2.codePointAt(i) - s1.codePointAt(i);
						if(diff == 0)
							continue;
						return diff;
					}
				}
				return 0;
			}
			
		});
		availablePanel.removeAll();
		for(Component comp : comps)
			availablePanel.add(comp);
		updateUI();
	}
	
	private void synchActiveResourcePacks() {
		List<String> activeResourcePacks = new ArrayList<String>();
		for(Component comp : activePanel.getComponents()) {
			if(comp instanceof ActiveResourcePack) {
				if(!((ActiveResourcePack)comp).isNoMove()) {
					activeResourcePacks.add(((ActiveResourcePack)comp).getName());
				}
			}
		}
		if(!activeResourcePacks.equals(this.activeResourcePacks)) {
			this.activeResourcePacks = activeResourcePacks;
			if(this.onResourcePackChange != null)
				this.onResourcePackChange.run();
		}
	}
	
	public List<String> getActiveResourcePacks(){
		return activeResourcePacks;
	}
	
	public void addChangeListener(Runnable listener) {
		this.onResourcePackChange = listener;
	}
	
	private static class AvailableResourcePack extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String name;

		public AvailableResourcePack(ResourcePackSelector manager, String name) {
			super();
			this.name = name;
			
			setPreferredSize(new Dimension(250, 64));
			setMinimumSize(new Dimension(250, 64));
			setMaximumSize(new Dimension(300, 64));

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(5, 5, 5, 5)));
			
			JLabel label = new JLabel(name);
			add(label);
			
			add(new JPanel());
			
			JButton addButton = new JButton("+");
			addButton.setMinimumSize(new Dimension(52, 52));
			addButton.setMaximumSize(new Dimension(52, 52));
			addButton.setPreferredSize(new Dimension(52, 52));
			add(addButton);
			
			addButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					manager.enableResourcePack(name);
				}
				
			});
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	private static class ActiveResourcePack extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String name;
		private boolean noMove;

		public ActiveResourcePack(ResourcePackSelector manager, String name, boolean noMove) {
			super();
			this.name = name;
			this.noMove = noMove;
			
			setPreferredSize(new Dimension(250, 64));
			setMinimumSize(new Dimension(2050, 64));
			setMaximumSize(new Dimension(300, 64));

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(5, 5, 5, 5)));
			
			JPanel orderPanel = new JPanel();
			orderPanel.setBorder(new EmptyBorder(0,0,0,5));
			orderPanel.setLayout(new BoxLayout(orderPanel, BoxLayout.Y_AXIS));
			JButton upButton = new JButton("\u25B2");
			upButton.setMinimumSize(new Dimension(52, 26));
			upButton.setMaximumSize(new Dimension(52, 26));
			upButton.setPreferredSize(new Dimension(52, 26));
			orderPanel.add(upButton);
			JButton downButton = new JButton("\u25BC");
			downButton.setMinimumSize(new Dimension(52, 26));
			downButton.setMaximumSize(new Dimension(52, 26));
			downButton.setPreferredSize(new Dimension(52, 26));
			orderPanel.add(downButton);
			if(!noMove)
				add(orderPanel);
			
			String labelText = name;
			String packInfoPath = FileUtil.getResourcePackDir() + name + "/packInfo.json";
			if(new File(packInfoPath).exists()) {
				try {
					JsonObject data = JsonParser.parseReader(new JsonReader(new BufferedReader(new FileReader(new File(packInfoPath))))).getAsJsonObject();
					String versionName = data.get("version").getAsString();
					labelText = name + " (" + versionName + ")";
				}catch(Exception ex) {
				}
			}
			
			JLabel label = new JLabel(labelText);
			add(label);
			
			add(new JPanel());
			
			JButton removeButton = new JButton("-");
			removeButton.setMinimumSize(new Dimension(52, 52));
			removeButton.setMaximumSize(new Dimension(52, 52));
			removeButton.setPreferredSize(new Dimension(52, 52));
			if(!noMove)
				add(removeButton);
			
			upButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					manager.moveActiveResourcePackUp(name);
				}
				
			});
			
			downButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					manager.moveActiveResourcePackDown(name);
				}
				
			});
			
			removeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					manager.disableResourcePack(name);
				}
				
			});
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isNoMove() {
			return noMove;
		}
		
	}

}
