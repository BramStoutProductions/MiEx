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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class ExampleResourcePackDownloader extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel availablePanel;

	public ExampleResourcePackDownloader() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(root);
		
		JLabel availableLabel = new JLabel("Available:");
		availableLabel.setBorder(null);
		availableLabel.setHorizontalAlignment(SwingConstants.CENTER);
		availableLabel.setPreferredSize(new Dimension(484, 20));
		availableLabel.setMinimumSize(availableLabel.getPreferredSize());
		availableLabel.setMaximumSize(availableLabel.getPreferredSize());
		availableLabel.setAlignmentX(0);
		root.add(availableLabel);
		
		availablePanel = new JPanel();
		availablePanel.setLayout(new BoxLayout(availablePanel, BoxLayout.Y_AXIS));
		JScrollPane availableScrollPane = new JScrollPane(availablePanel);
		availableScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		availableScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		availableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		availableScrollPane.setAlignmentX(0);
		root.add(availableScrollPane);
		
		JPanel separator = new JPanel();
		separator.setMinimumSize(new Dimension(0, 10));
		separator.setMaximumSize(new Dimension(0, 10));
		separator.setPreferredSize(new Dimension(0, 10));
		separator.setAlignmentX(0);
		root.add(separator);
		
		JButton downloadButton = new JButton("Download Resource Packs");
		downloadButton.setAlignmentX(0.0f);
		downloadButton.setPreferredSize(new Dimension(464, 32));
		downloadButton.setMinimumSize(downloadButton.getPreferredSize());
		downloadButton.setMaximumSize(downloadButton.getPreferredSize());
		downloadButton.setAlignmentX(0);
		root.add(downloadButton);
		
		setSize(500, 600);
		setResizable(false);
		setTitle("Example Resource Pack Downloader");
		
		downloadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Map<String, List<String>> packs = new HashMap<String, List<String>>();
				for(Component comp : availablePanel.getComponents()) {
					if(comp instanceof ExampleResourcePackItem) {
						if(((ExampleResourcePackItem) comp).isSelected()) {
							String repository = ((ExampleResourcePackItem) comp).getRepository();
							String resourcePack = ((ExampleResourcePackItem) comp).getText();
							
							List<String> packs2 = packs.getOrDefault(repository, null);
							if(packs2 == null) {
								packs2 = new ArrayList<String>();
								packs.put(repository, packs2);
							}
							packs2.add(resourcePack);
						}
					}
				}
				if(packs.isEmpty())
					return;
				downloadExampleResourcePacks(packs);
				
				setVisible(false);
			}
			
		});
	}
	
	public static List<String> getExampleResourcePacks() {
		List<String> items = new ArrayList<String>();
		for(ExampleResourcePackItem item : loadItems()) {
			items.add(item.getText());
		}
		return items;
	}
	
	public static void downloadExampleResourcePacks(List<String> packs) {
		Map<String, List<String>> packsMap = new HashMap<String, List<String>>();
		for(ExampleResourcePackItem comp : loadItems()) {
			if(packs.contains(comp.getText())) {
				String repository = ((ExampleResourcePackItem) comp).getRepository();
				String resourcePack = ((ExampleResourcePackItem) comp).getText();
				
				List<String> packs2 = packsMap.getOrDefault(repository, null);
				if(packs2 == null) {
					packs2 = new ArrayList<String>();
					packsMap.put(repository, packs2);
				}
				packs2.add(resourcePack);
			}
		}
		if(packsMap.isEmpty())
			return;
		
		downloadExampleResourcePacks(packsMap);
	}
	
	public static void downloadExampleResourcePacks(Map<String, List<String>> packs) {
		MCWorldExporter.getApp().getUI().getProgressBar().setText("Downloading resource packs");
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.25f);
		System.out.println("Downloading example resource packs:");
		for(Entry<String, List<String>> entry : packs.entrySet()) {
			for(String pack : entry.getValue()) {
				System.out.println("  " + entry.getKey() + ":" + pack);
				// Make sure to delete the old pack, in case the new version
				// has fewer or different files.
				File packDir = new File(FileUtil.getResourcePackDir(), pack);
				if(packDir.exists() && packDir.isDirectory())
					packDir.delete();
			}
			downloadExampleResourcePacks(entry.getKey(), entry.getValue());
		}
		
		
		
		MCWorldExporter.getApp().getUI().getProgressBar().setText("");
		MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
		System.out.println("Example resource packs downloaded.");
		
		Popups.showMessageDialog(MCWorldExporter.getApp().getUI(), "Resource Packs successfully downloaded!", "Done", Popups.PLAIN_MESSAGE);
		
		// Reload resource packs.
		List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
		List<String> activeResourcePackUUIDS = new ArrayList<String>();
		for(ResourcePack pack : activeResourcePacks)
			activeResourcePackUUIDS.add(pack.getUUID());
		
		ResourcePacks.load();
		ResourcePacks.setActiveResourcePacks(activeResourcePacks);
		
		MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
		MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(activeResourcePackUUIDS);
		
		Config.load();
	}
	
	public static void downloadExampleResourcePacks(String repository, List<String> packs) {
		HttpURLConnection connection = null;
		InputStream stream = null;
		byte[] buffer = new byte[4096];
		try {
			URL url = new URI("https://api.github.com/repos/" + repository + "/zipball").toURL();
			connection = (HttpURLConnection) url.openConnection();
			stream = connection.getInputStream();
			
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(stream));
			
			MCWorldExporter.getApp().getUI().getProgressBar().setText("Installing resource packs");
			MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.5f);
			
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				if(entry.isDirectory())
					continue;
				String path = entry.getName();
				// Get rid of the first directory
				int slashIndex = path.indexOf((int) '/');
				path = path.substring(slashIndex + 1);
				
				if(!path.startsWith("extras/example_resource_packs"))
					continue;
				
				path = path.substring(30); // Get rid of "extras/example_resource_packs"
				
				slashIndex = path.indexOf((int) '/');
				String packName = path.substring(0, slashIndex);
				if(!packs.contains(packName))
					continue;
				
				File outFile = new File(FileUtil.getResourcePackDir(), path);
				File parentDir = outFile.getParentFile();
				parentDir.mkdirs();
				
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(outFile);
					int read = 0;
					while((read = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, read);
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				if(fos != null) {
					try {
						fos.close();
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		try {
			if(stream != null)
				stream.close();
		}catch(Exception ex) {}
		try {
			if(connection != null)
				connection.disconnect();
		}catch(Exception ex) {}
	}
	
	
	private static void loadFromRepository(String repository, Set<String> foundRPs, List<ExampleResourcePackItem> items) {
		HttpURLConnection connection = null;
		InputStream stream = null;
		try {
			URL url = new URI("https://api.github.com/repos/" + repository + "/contents/extras/example_resource_packs").toURL();
			connection = (HttpURLConnection) url.openConnection();
			stream = connection.getInputStream();
			
			JsonArray packs = JsonParser.parseReader(new JsonReader(new BufferedReader(
								new InputStreamReader(stream)))).getAsJsonArray();
			for(JsonElement el : packs.asList()) {
				JsonObject pack = el.getAsJsonObject();
				if(!pack.has("name"))
					continue;
				String name = pack.get("name").getAsString();
				if(foundRPs.contains(name))
					continue;
				foundRPs.add(name);
				items.add(new ExampleResourcePackItem(name, repository));
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		try {
			if(stream != null)
				stream.close();
		}catch(Exception ex) {}
		try {
			if(connection != null)
				connection.disconnect();
		}catch(Exception ex) {}
	}
	private static List<ExampleResourcePackItem> loadItems(){
		List<ExampleResourcePackItem> items = new ArrayList<ExampleResourcePackItem>();
		Set<String> foundRPs = new HashSet<String>();
		for(String repository : MCWorldExporter.GitHubRepository) {
			loadFromRepository(repository, foundRPs, items);
		}
		return items;
	}
	
	@Override
	public void setVisible(boolean b) {
		if(b) {
			
			availablePanel.removeAll();
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					for(ExampleResourcePackItem item : loadItems()) {
						availablePanel.add(item);
					}
				}
				
			});
		}
		super.setVisible(b);
	}
	
	private static class ExampleResourcePackItem extends JCheckBox{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String repository;
		
		public ExampleResourcePackItem(String name, String repository) {
			super(name);
			this.repository = repository;
		}
		
		public String getRepository() {
			return repository;
		}
		
	}
	
}
