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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
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
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.Json;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

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
	
	private boolean forceBaseResourcePack;
	
	public ResourcePackSelector(boolean forceBaseResourcePack) {
		this.forceBaseResourcePack = forceBaseResourcePack;
		
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
	
	public void reset(boolean loadDefaultResourcePacks) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					availablePanel.removeAll();
					activePanel.removeAll();
					
					for(ResourcePack pack : ResourcePacks.getResourcePacks()) {
						if(pack.getName().equals("base_resource_pack") && forceBaseResourcePack)
							continue;
						AvailableResourcePack comp = new AvailableResourcePack(resourcePackSelector, pack.getName(), pack.getUUID());
						availablePanel.add(comp);
						comp.setEnabled(availablePanel.isEnabled());
					}
					
					if(forceBaseResourcePack) {
						ActiveResourcePack comp = new ActiveResourcePack(resourcePackSelector, 
								"base_resource_pack", "base_resource_pack", true); 
						activePanel.add(comp);
						comp.setEnabled(activePanel.isEnabled());
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
		sortAvailableResourcePacks();
		synchActiveResourcePacks();
		if(loadDefaultResourcePacks) {
			// Add in the resource packs that needed to be loaded by default.
			// enableResourcePack puts the specified resource pack at the top,
			// so we need to add them in reverse order.
			enableResourcePack(MCWorldExporter.defaultResourcePacks);
		}
	}
	
	public void clear() {
		disableResourcePack(activeResourcePacks);
	}
	
	public void enableResourcePack(String uuid) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					for(Component comp : availablePanel.getComponents()) {
						if(comp instanceof AvailableResourcePack) {
							if(((AvailableResourcePack)comp).getUUID().equals(uuid)) {
								availablePanel.remove(comp);
								ActiveResourcePack newComp = new ActiveResourcePack(resourcePackSelector, 
										((AvailableResourcePack)comp).getName(), uuid, false);
								activePanel.add(newComp,0);
								newComp.setEnabled(activePanel.isEnabled());
								break;
							}
						}
					}
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void enableResourcePack(List<String> uuids) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					for(int i = uuids.size() - 1; i >= 0; --i) {
						String uuid = uuids.get(i);
						for(Component comp : availablePanel.getComponents()) {
							if(comp instanceof AvailableResourcePack) {
								if(((AvailableResourcePack)comp).getUUID().equals(uuid)) {
									availablePanel.remove(comp);
									ActiveResourcePack newComp = new ActiveResourcePack(resourcePackSelector, 
											((AvailableResourcePack)comp).getName(), uuid, false);
									activePanel.add(newComp,0);
									newComp.setEnabled(activePanel.isEnabled());
									break;
								}
							}
						}
					}
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void disableResourcePack(String uuid) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					for(Component comp : activePanel.getComponents()) {
						if(comp instanceof ActiveResourcePack) {
							if(((ActiveResourcePack)comp).getUUID().equals(uuid)) {
								if(((ActiveResourcePack)comp).isNoMove())
									continue;
								activePanel.remove(comp);
								AvailableResourcePack newComp = new AvailableResourcePack(resourcePackSelector, 
										((ActiveResourcePack)comp).getName(), uuid);
								availablePanel.add(newComp);
								newComp.setEnabled(availablePanel.isEnabled());
								sortAvailableResourcePacks();
								break;
							}
						}
					}
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void disableResourcePack(List<String> uuids) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					for(Component comp : activePanel.getComponents()) {
						if(comp instanceof ActiveResourcePack) {
							if(uuids.contains(((ActiveResourcePack)comp).getUUID())) {
								if(((ActiveResourcePack)comp).isNoMove())
									continue;
								activePanel.remove(comp);
								AvailableResourcePack newComp = new AvailableResourcePack(resourcePackSelector, 
										((ActiveResourcePack)comp).getName(), ((ActiveResourcePack)comp).getUUID()); 
								availablePanel.add(newComp);
								newComp.setEnabled(availablePanel.isEnabled());
							}
						}
					}
					sortAvailableResourcePacks();
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void moveActiveResourcePackDown(String uuid) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					int i = 0;
					for(Component comp : activePanel.getComponents()) {
						if(comp instanceof ActiveResourcePack) {
							if(((ActiveResourcePack)comp).getUUID().equals(uuid)) {
								if(((ActiveResourcePack)comp).isNoMove())
									continue;
								activePanel.remove(comp);
								// getComponentCount() - 1 so that base_resource_pack will always be at the bottom
								ActiveResourcePack newComp = new ActiveResourcePack(resourcePackSelector, 
										((ActiveResourcePack)comp).getName(), uuid, false); 
								activePanel.add(newComp, 
										Math.min(i + 1, activePanel.getComponentCount()- (forceBaseResourcePack ? 1 : 0)));
								newComp.setEnabled(activePanel.isEnabled());
								break;
							}
						}
						++i;
					}
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void moveActiveResourcePackUp(String uuid) {
		try {
			final ResourcePackSelector resourcePackSelector = this;
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					int i = 0;
					for(Component comp : activePanel.getComponents()) {
						if(comp instanceof ActiveResourcePack) {
							if(((ActiveResourcePack)comp).getUUID().equals(uuid)) {
								if(((ActiveResourcePack)comp).isNoMove())
									continue;
								activePanel.remove(comp);
								ActiveResourcePack newComp = new ActiveResourcePack(resourcePackSelector, 
										((ActiveResourcePack)comp).getName(), uuid, false);
								activePanel.add(newComp, Math.max(i - 1, 0));
								newComp.setEnabled(activePanel.isEnabled());
								break;
							}
						}
						++i;
					}
					updateUI();
				}
			};
			if(SwingUtilities.isEventDispatchThread())
				runnable.run();
			else
				SwingUtilities.invokeAndWait(runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		synchActiveResourcePacks();
	}
	
	public void sortAvailableResourcePacks() {
		try {
			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					Component comps[] = availablePanel.getComponents();
					Arrays.sort(comps, new Comparator<Component>() {

						@Override
						public int compare(Component o1, Component o2) {
							if(o1 instanceof AvailableResourcePack && o2 instanceof AvailableResourcePack) {
								String s1 = ((AvailableResourcePack)o1).getName().toLowerCase();
								String s2 = ((AvailableResourcePack)o2).getName().toLowerCase();
								for(int i = 0; i < Math.min(s1.length(), s2.length()); ++i) {
									int diff = s1.codePointAt(i) - s2.codePointAt(i);
									if(diff == 0)
										continue;
									return diff;
								}
							}
							return 0;
						}
						
					});
					availablePanel.removeAll();
					for(Component comp : comps) {
						availablePanel.add(comp);
						comp.setEnabled(availablePanel.isEnabled());
					}
					updateUI();
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
	
	private void synchActiveResourcePacks() {
		List<String> activeResourcePacks = new ArrayList<String>();
		for(Component comp : activePanel.getComponents()) {
			if(comp instanceof ActiveResourcePack) {
				if(!((ActiveResourcePack)comp).isNoMove()) {
					activeResourcePacks.add(((ActiveResourcePack)comp).getUUID());
				}
			}
		}
		if(!activeResourcePacks.equals(this.activeResourcePacks)) {
			this.activeResourcePacks = activeResourcePacks;
			if(this.onResourcePackChange != null)
				this.onResourcePackChange.run();
		}
	}
	
	public void syncWithResourcePacks() {
		try {
			availablePanel.removeAll();
			activePanel.removeAll();
			List<ResourcePack> activePacks = ResourcePacks.getActiveResourcePacks();
			
			for(ResourcePack pack : ResourcePacks.getResourcePacks()) {
				if(activePacks.contains(pack))
					continue;
				AvailableResourcePack comp = new AvailableResourcePack(this, pack.getName(), pack.getUUID());
				availablePanel.add(comp);
				comp.setEnabled(availablePanel.isEnabled());
			}
			
			for(ResourcePack pack : activePacks) {
				ActiveResourcePack newComp = new ActiveResourcePack(this, 
						pack.getName(), pack.getUUID(), pack.getUUID().equals("base_resource_pack"));
				activePanel.add(newComp);
				newComp.setEnabled(activePanel.isEnabled());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sortAvailableResourcePacks();
		updateUI();
		revalidate();
		repaint();
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
		private String uuid;

		public AvailableResourcePack(ResourcePackSelector manager, String name, String uuid) {
			super();
			this.name = name;
			this.uuid = uuid;
			
			setPreferredSize(new Dimension(250, 40));
			setMinimumSize(new Dimension(250, 40));
			setMaximumSize(new Dimension(300, 40));

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(3, 3, 3, 3)));
			
			JLabel label = new JLabel(name);
			label.setMaximumSize(new Dimension(232, 32));
			label.setMinimumSize(new Dimension(232, 32));
			label.setPreferredSize(new Dimension(232, 32));
			add(label);
			
			add(new JPanel());
			
			JButton addButton = new JButton("+");
			addButton.setMinimumSize(new Dimension(32, 32));
			addButton.setMaximumSize(new Dimension(32, 32));
			addButton.setPreferredSize(new Dimension(32, 32));
			addButton.setMargin(new Insets(0, 0, 0, 0));
			add(addButton);
			
			addButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					manager.enableResourcePack(uuid);
				}
				
			});
		}
		
		public String getName() {
			return name;
		}
		
		public String getUUID() {
			return uuid;
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			
			int num = getComponentCount();
			for(int i = 0; i < num; ++i) {
				getComponent(i).setEnabled(enabled);
			}
		}
		
	}
	
	private static class ActiveResourcePack extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private String name;
		private String uuid;
		private boolean noMove;

		public ActiveResourcePack(ResourcePackSelector manager, String name, String uuid, boolean noMove) {
			super();
			this.name = name;
			this.uuid = uuid;
			this.noMove = noMove;
			
			setPreferredSize(new Dimension(250, 40));
			setMinimumSize(new Dimension(2050, 40));
			setMaximumSize(new Dimension(300, 40));

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(3, 3, 3, 3)));
			
			JPanel orderPanel = new JPanel();
			orderPanel.setBorder(new EmptyBorder(0,0,0,5));
			orderPanel.setLayout(new BoxLayout(orderPanel, BoxLayout.Y_AXIS));
			JButton upButton = new JButton("\u25B2");
			upButton.setMinimumSize(new Dimension(32, 16));
			upButton.setMaximumSize(new Dimension(32, 16));
			upButton.setPreferredSize(new Dimension(32, 16));
			upButton.setMargin(new Insets(0, 0, 0, 0));
			orderPanel.add(upButton);
			JButton downButton = new JButton("\u25BC");
			downButton.setMinimumSize(new Dimension(32, 16));
			downButton.setMaximumSize(new Dimension(32, 16));
			downButton.setPreferredSize(new Dimension(32, 16));
			downButton.setMargin(new Insets(0, 0, 0, 0));
			orderPanel.add(downButton);
			if(!noMove)
				add(orderPanel);
			
			String labelText = name;
			String packInfoPath = FileUtil.getResourcePackDir() + name + "/packInfo.json";
			if(new File(packInfoPath).exists()) {
				try {
					JsonObject data = Json.read(new File(packInfoPath)).getAsJsonObject();
					String versionName = data.get("version").getAsString();
					labelText = name + " (" + versionName + ")";
				}catch(Exception ex) {
				}
			}
			
			JLabel label = new JLabel(labelText);
			label.setMaximumSize(new Dimension(190, 32));
			label.setMinimumSize(new Dimension(190, 32));
			label.setPreferredSize(new Dimension(190, 32));
			add(label);
			
			add(new JPanel());
			
			JButton removeButton = new JButton("-");
			removeButton.setMinimumSize(new Dimension(32, 32));
			removeButton.setMaximumSize(new Dimension(32, 32));
			removeButton.setPreferredSize(new Dimension(32, 32));
			removeButton.setMargin(new Insets(0, 0, 0, 0));
			if(!noMove)
				add(removeButton);
			
			upButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					manager.moveActiveResourcePackUp(uuid);
				}
				
			});
			
			downButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					manager.moveActiveResourcePackDown(uuid);
				}
				
			});
			
			removeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					manager.disableResourcePack(uuid);
				}
				
			});
		}
		
		public String getName() {
			return name;
		}
		
		public String getUUID() {
			return uuid;
		}
		
		public boolean isNoMove() {
			return noMove;
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			
			int num = getComponentCount();
			for(int i = 0; i < num; ++i) {
				Component comp = getComponent(i);
				comp.setEnabled(enabled);
				if(comp.getClass().equals(JPanel.class)) {
					int num2 = ((JPanel) comp).getComponentCount();
					for(int j = 0; j < num2; ++j) {
						((JPanel) comp).getComponent(j).setEnabled(enabled);
					}
				}
			}
		}
		
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		availableScrollPane.setEnabled(enabled);
		availablePanel.setEnabled(enabled);
		activeScrollPane.setEnabled(enabled);
		activePanel.setEnabled(enabled);
		
		int num = availablePanel.getComponentCount();
		for(int i = 0; i < num; ++i) {
			availablePanel.getComponent(i).setEnabled(enabled);
		}
		
		num = activePanel.getComponentCount();
		for(int i = 0; i < num; ++i) {
			activePanel.getComponent(i).setEnabled(enabled);
		}
	}

}
