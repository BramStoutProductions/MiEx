package nl.bramstout.mcworldexporter.ui;

import java.awt.Component;
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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;

public class ExampleResourcePackDownloader extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel availablePanel;

	public ExampleResourcePackDownloader() {
		super(MCWorldExporter.getApp().getUI());
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
				List<String> packs = new ArrayList<String>();
				for(Component comp : availablePanel.getComponents()) {
					if(comp instanceof JCheckBox) {
						if(((JCheckBox) comp).isSelected())
							packs.add(((JCheckBox) comp).getText());
					}
				}
				if(packs.isEmpty())
					return;
				
				MCWorldExporter.getApp().getUI().getProgressBar().setText("Downloading resource packs");
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0.25f);
				
				HttpURLConnection connection = null;
				InputStream stream = null;
				byte[] buffer = new byte[4096];
				try {
					URL url = new URI("https://api.github.com/repos/BramStoutProductions/MiEx/zipball").toURL();
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
				
				MCWorldExporter.getApp().getUI().getProgressBar().setText("");
				MCWorldExporter.getApp().getUI().getProgressBar().setProgress(0f);
				
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "Resource Packs successfully downloaded!", "Done", JOptionPane.PLAIN_MESSAGE);
				setVisible(false);
				
				// Reload resource packs.
				List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
				List<String> activeResourcePackUUIDS = new ArrayList<String>();
				for(ResourcePack pack : activeResourcePacks)
					activeResourcePackUUIDS.add(pack.getUUID());
				
				ResourcePacks.load();
				ResourcePacks.setActiveResourcePacks(activeResourcePacks);
				
				MCWorldExporter.getApp().getUI().getResourcePackManager().reset(false);
				MCWorldExporter.getApp().getUI().getResourcePackManager().enableResourcePack(activeResourcePackUUIDS);
				
				Atlas.readAtlasConfig();
				Config.load();
				BlockStateRegistry.clearBlockStateRegistry();
				ModelRegistry.clearModelRegistry();
				BiomeRegistry.recalculateTints();
				MCWorldExporter.getApp().getUI().update();
				MCWorldExporter.getApp().getUI().fullReRender();
			}
			
		});
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(b) {
			
			availablePanel.removeAll();
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					HttpURLConnection connection = null;
					InputStream stream = null;
					try {
						URL url = new URI("https://api.github.com/repos/BramStoutProductions/MiEx/contents/extras/example_resource_packs").toURL();
						connection = (HttpURLConnection) url.openConnection();
						stream = connection.getInputStream();
						
						JsonArray packs = JsonParser.parseReader(new JsonReader(new BufferedReader(
											new InputStreamReader(stream)))).getAsJsonArray();
						for(JsonElement el : packs.asList()) {
							JsonObject pack = el.getAsJsonObject();
							if(!pack.has("name"))
								continue;
							String name = pack.get("name").getAsString();
							availablePanel.add(new JCheckBox(name));
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
				
			});
		}
	}
	
}
