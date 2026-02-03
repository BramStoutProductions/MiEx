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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import nl.bramstout.mcworldexporter.atlas.Atlas;
import nl.bramstout.mcworldexporter.export.GeneratedTextures;
import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.launcher.Launcher;
import nl.bramstout.mcworldexporter.launcher.LauncherRegistry;
import nl.bramstout.mcworldexporter.launcher.MinecraftSave;
import nl.bramstout.mcworldexporter.model.BlockStateRegistry;
import nl.bramstout.mcworldexporter.model.ModelRegistry;
import nl.bramstout.mcworldexporter.parallel.ReadWriteMutex;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;
import nl.bramstout.mcworldexporter.ui.MainWindow;
import nl.bramstout.mcworldexporter.ui.ResourcePackSourcesExtractorDialog;
import nl.bramstout.mcworldexporter.world.BiomeRegistry;
import nl.bramstout.mcworldexporter.world.World;
import nl.bramstout.mcworldexporter.world.anvil.WorldAnvil;
import nl.bramstout.mcworldexporter.world.bedrock.WorldBedrock;
import nl.bramstout.mcworldexporter.world.hytale.WorldHytale;

public class MCWorldExporter {
	
	private static MCWorldExporter instance;
	public static String forceOutputPath = null;
	public static String forceOpenWorld = null;
	public static List<String> defaultResourcePacks = new ArrayList<String>();
	public static int numUIThreads = 4;
	public static boolean portableExports = false;
	public static ReadWriteMutex worldMutex = new ReadWriteMutex();
	public static String GitHubRepository = "BramStoutProductions/MiEx";
	public static boolean offlineMode = false;
	
	public static MCWorldExporter getApp() {
		return instance;
	}
	
	
	
	
	private World world;
	private File lastExportFileOpened;
	private MainWindow ui;
	private List<ExportBounds> exportBounds;
	private int activeExportBoundsIndex;
	
	public MCWorldExporter() {
		instance = this;
		LauncherRegistry.initLaunchers();
		
		world = null;
		lastExportFileOpened = null;
		exportBounds = new ArrayList<ExportBounds>();
		exportBounds.add(new ExportBounds("Region 1"));
		activeExportBoundsIndex = 0;
		ui = new MainWindow();
		ui.setLocationRelativeTo(null);
		ui.setVisible(true);
		ui.setEnabled(false);
		
		try {
			ResourcePackDefaults.setupDefaults();
			
			ResourcePacks.init();
			Noise.init();
			Config.load();
			Atlas.readAtlasConfig();
			
			resetExportBounds();
			
			BlockStateRegistry.clearBlockStateRegistry();
			ModelRegistry.clearModelRegistry();
			BiomeRegistry.recalculateTints();
			ResourcePacks.doPostLoad();
			MCWorldExporter.getApp().getUI().update();
			MCWorldExporter.getApp().getUI().fullReRender();
			MCWorldExporter.getApp().getUI().getResourcePackManager().reset(true);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		ui.setEnabled(true);
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if(forceOpenWorld != null) {
					File worldFolder = new File(forceOpenWorld);
					if(worldFolder.exists() && worldFolder.isDirectory())
						setWorld(worldFolder, worldFolder.getName(), null);
				}
			}
			
		});
	}
	
	public void setWorld(File worldFolder, String name, Launcher launcher) {
		MCWorldExporter.getApp().getUI().getEntityDialog().noDefaultSelection = false;
		worldMutex.acquireWrite();
		
		try {
			if(world != null)
				world.unload();
			world = null;
			if(worldFolder != null) {
				if(launcher == null)
					launcher = LauncherRegistry.getLauncherForWorld(worldFolder);
				World tmpWorld = null;
				if(WorldAnvil.supportsWorld(worldFolder)) {
					System.out.println("Opening world as Minecraft Anvil format: " + worldFolder.getPath());
					tmpWorld = new WorldAnvil(worldFolder, name, launcher);
				}else if(WorldBedrock.supportsWorld(worldFolder)) {
					System.out.println("Opening world as Minecraft Bedrock Edition format: " + worldFolder.getPath());
					tmpWorld = new WorldBedrock(worldFolder, name, launcher);
				}else if(WorldHytale.supportsWorld(worldFolder)) {
					System.out.println("Opening world as Hytale format: " + worldFolder.getPath());
					tmpWorld = new WorldHytale(worldFolder, name, launcher);
				}else {
					System.out.println("Opening world as unknown format: " + worldFolder.getPath());
					SwingUtilities.invokeLater(new Runnable() {
		
						@Override
						public void run() {
							JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "The selected folder does not contain a world.", "Error", JOptionPane.ERROR_MESSAGE);
						}
						
					});
					ui.reset();
					worldMutex.releaseWrite();
					return;
				}
				
				
				List<String> requiredResourcePacks = tmpWorld.getRequiredResourcePacks();
				for(String rp : requiredResourcePacks) {
					System.out.println("World requires resource pack: " + rp);
				}
				List<String> missingRequired = new ArrayList<String>(requiredResourcePacks);
				for(ResourcePack pack : ResourcePacks.getActiveResourcePacks()) {
					if(requiredResourcePacks.contains(pack.getUUID()))
						missingRequired.remove(pack.getUUID());
				}
				if(!missingRequired.isEmpty()) {
					// We have some resource packs that are absolutely required, so let's load them.
					List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
					Iterator<String> it = missingRequired.iterator();
					while(it.hasNext()) {
						String packUuid = it.next();
						ResourcePack pack = ResourcePacks.getResourcePack(packUuid);
						if(pack != null) {
							it.remove();
							activeResourcePacks.add(pack);
						}
					}
					// If the missing resource packs are certain base_resource_packs, then try setting them up.
					if(missingRequired.contains("base_resource_pack_hytale")) {
						// No base_resource_pack_hytale, so make it.
						int option = JOptionPane.showConfirmDialog(MCWorldExporter.getApp().getUI(), 
								"No base_resource_pack_hytale so this world cannot be loaded. Would you like to install the base_resource_pack_hytale?", 
								"Error", JOptionPane.YES_NO_OPTION);
						
						if(option == 0) {
							ResourcePackDefaults.updateBaseResourcePackHytale(true);
							ResourcePacks.load();
							MCWorldExporter.getApp().getUI().getResourcePackManager().syncWithResourcePacks();
							
							ResourcePack pack = ResourcePacks.getResourcePack("base_resource_pack_hytale");
							if(pack != null) {
								activeResourcePacks.add(pack);
								missingRequired.remove("base_resource_pack_hytale");
							}
						}
					}
					
					
					if(missingRequired.isEmpty()) {
						// We got all required resource packs, so set those as active.
						System.out.println("Setting active resource packs to:");
						for(ResourcePack rp : activeResourcePacks)
							System.out.println("  " + rp.getUUID());
						ResourcePacks.setActiveResourcePacks(activeResourcePacks);
						getUI().getResourcePackManager().syncWithResourcePacks();
					}else {
						// We are still missing some resource packs,
						// so let's refuse to load this world.
						String listStr = "";
						for(String missingUuid : missingRequired) {
							System.out.println("Missing required resource pack: " + missingUuid);
							if(!listStr.isEmpty())
								listStr += ", ";
							listStr += missingUuid;
						}
						final String finalListStr = listStr;
						
						SwingUtilities.invokeLater(new Runnable() {
							
							@Override
							public void run() {
								JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "This world requires the following resource packs to be installed: " + finalListStr, "Error", JOptionPane.ERROR_MESSAGE);
							}
							
						});
						ui.reset();
						worldMutex.releaseWrite();
						return;
					}
				}
				
				
				
				// Handle dependent resource packs.
				// The world might have been created using certain resource packs,
				// data packs, mods, etc. If so, let's ask the user if they want MiEx
				// to load it.
				List<ResourcePackSource> dependentRPs = tmpWorld.getDependentResourcePacks();
				if(!dependentRPs.isEmpty()) {
					System.out.println("World contains dependent resource packs:");
					for(ResourcePackSource rp : dependentRPs) {
						System.out.println("  " + rp.getName() + ": ");
						for(int i = 0; i < rp.getSources().size(); ++i) {
							System.out.println("    " + rp.getSourceUuids().get(i) + ": " + rp.getSources().get(i));
						}
					}
					System.out.println();
					
					List<String> sourceUuids = new ArrayList<String>();
					for(ResourcePackSource source : dependentRPs)
						sourceUuids.addAll(source.getSourceUuids());
					List<ResourcePack> neededResourcePacks = ResourcePacks.getResourcePacksForSources(sourceUuids);
					
					boolean hasLoadedEverything = false;
					if(neededResourcePacks != null) {
						hasLoadedEverything = true;
						for(ResourcePack rp : neededResourcePacks) {
							boolean isActive = false;
							for(ResourcePack rp2 : ResourcePacks.getActiveResourcePacks()) {
								if(rp2.getUUID().equals(rp.getUUID())) {
									isActive = true;
									break;
								}
							}
							if(!isActive)
								hasLoadedEverything = false;
						}
					}
					
					if(!hasLoadedEverything) {
						int option = JOptionPane.showConfirmDialog(MCWorldExporter.getApp().getUI(), 
								"This world makes use of resource packs and/or mods. Would you like to enable the resource packs for it?", 
								"Load Resource Packs?", JOptionPane.YES_NO_OPTION);
						
						if(option == 0) {
							if(neededResourcePacks != null) {
								List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
								for(ResourcePack rp : neededResourcePacks) {
									boolean hasAlready = false;
									for(ResourcePack rp2 : activeResourcePacks) {
										if(rp2.getUUID() == rp.getUUID()) {
											hasAlready = true;
											break;
										}
									}
									if(hasAlready)
										continue;
									activeResourcePacks.add(0, rp);
								}
								ResourcePacks.setActiveResourcePacks(activeResourcePacks);
								getUI().getResourcePackManager().syncWithResourcePacks();
							}else {
								// We don't have all of the resource packs needed, so let's show the user the dialog
								// so that they can extract it.
								ResourcePackSourcesExtractorDialog dialog = new ResourcePackSourcesExtractorDialog(tmpWorld, dependentRPs);
								dialog.setLocationRelativeTo(MCWorldExporter.getApp().getUI());
								dialog.setVisible(true);
								
								ResourcePacks.load();
								MCWorldExporter.getApp().getUI().getResourcePackManager().syncWithResourcePacks();
								
								// Let's try it again, in the hopes that we now have everything.
								// Otherwise, just load in the world.
								neededResourcePacks = ResourcePacks.getResourcePacksForSources(sourceUuids);
								if(neededResourcePacks != null) {
									List<ResourcePack> activeResourcePacks = new ArrayList<ResourcePack>(ResourcePacks.getActiveResourcePacks());
									for(ResourcePack rp : neededResourcePacks) {
										boolean hasAlready = false;
										for(ResourcePack rp2 : activeResourcePacks) {
											if(rp2.getUUID() == rp.getUUID()) {
												hasAlready = true;
												break;
											}
										}
										if(hasAlready)
											continue;
										activeResourcePacks.add(0, rp);
									}
									ResourcePacks.setActiveResourcePacks(activeResourcePacks);
									getUI().getResourcePackManager().syncWithResourcePacks();
								}
							}
						}
					}
				}
				
				
				
				
				
				int resourcePackVersion = ResourcePacks.getBaseResourcePack().getWorldVersion();
				int worldVersion = tmpWorld.getWorldVersion();
				System.out.println("World version: " + Integer.toString(worldVersion));
				if(worldVersion > resourcePackVersion && worldVersion > 0) {
					int option = JOptionPane.showConfirmDialog(MCWorldExporter.getApp().getUI(), 
							"The base resource pack is outdated. Please update the base resource pack via the Tools button. Are you sure you still want to load the world?", 
							"Warning", JOptionPane.YES_NO_OPTION);
					
					if(option != 0) {
						ui.reset();
						worldMutex.releaseWrite();
						return;
					}
				}
				
				if(tmpWorld != null)
					tmpWorld.setWorldDir(worldFolder);
				world = tmpWorld;
				System.out.println("World loaded.");
			}
			ui.reset();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		worldMutex.releaseWrite();
	}
	
	public World getWorld() {
		return world;
	}
	
	public MainWindow getUI() {
		return ui;
	}
	
	public List<ExportBounds> getExportBoundsList() {
		return exportBounds;
	}
	
	public void setExportBounds(List<ExportBounds> bounds) {
		this.exportBounds.clear();
		for(ExportBounds bound : bounds) {
			this.exportBounds.add(bound.copy());
		}
		setActiveExportBoundsIndex(activeExportBoundsIndex);
	}
	
	public void resetExportBounds() {
		exportBounds = new ArrayList<ExportBounds>();
		exportBounds.add(new ExportBounds("Region 1"));
		activeExportBoundsIndex = 0;
		this.getUI().update();
	}
	
	public ExportBounds getActiveExportBounds() {
		if(exportBounds.size() == 0)
			exportBounds.add(new ExportBounds("Region 1"));
		if(activeExportBoundsIndex < 0 || activeExportBoundsIndex >= exportBounds.size())
			activeExportBoundsIndex = 0;
		return exportBounds.get(activeExportBoundsIndex);
	}
	
	public int getActiveExportBoundsIndex() {
		if(exportBounds.size() == 0)
			exportBounds.add(new ExportBounds("Region 1"));
		if(activeExportBoundsIndex < 0 || activeExportBoundsIndex >= exportBounds.size())
			activeExportBoundsIndex = 0;
		return activeExportBoundsIndex;
	}
	
	public void setActiveExportBoundsIndex(int activeExportBoundsIndex) {
		if(exportBounds.size() == 0)
			exportBounds.add(new ExportBounds("Region 1"));
		if(activeExportBoundsIndex < 0 || activeExportBoundsIndex >= exportBounds.size())
			activeExportBoundsIndex = 0;
		this.activeExportBoundsIndex = activeExportBoundsIndex;
		this.getUI().update();
	}
	
	public void setActiveExportBounds(String name) {
		int index = -1;
		for(int i = 0; i < exportBounds.size(); ++i) {
			if(exportBounds.get(i).getName().equals(name)) {
				index = i;
				break;
			}
		}
		if(index == -1)
			return;
		setActiveExportBoundsIndex(index);
	}
	
	public void addExportBounds() {
		String name = "";
		for(int i = this.exportBounds.size()+1; i < 10000; i++) {
			name = "Region " + Integer.toString(i);
			if(!hasExportBounds(name))
				break;
		}
		this.exportBounds.add(new ExportBounds(name));
		this.setActiveExportBoundsIndex(exportBounds.size()-1);
		this.getUI().update();
	}
	
	public void deleteExportBounds(String name) {
		int index = -1;
		for(int i = 0; i < exportBounds.size(); ++i) {
			if(exportBounds.get(i).getName().equals(name)) {
				index = i;
				break;
			}
		}
		if(index == -1)
			return;
		exportBounds.remove(index);
		if(activeExportBoundsIndex >= index)
			// We removed an export bounds in front of your active one,
			// meaning that the index of our active one changed, so
			// make sure to update the index to compensate.
			setActiveExportBoundsIndex(activeExportBoundsIndex-1);
		this.getUI().update();
	}
	
	public boolean hasExportBounds(String name) {
		for(ExportBounds bounds : exportBounds) {
			if(bounds.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public File getLastExportFileOpened() {
		return lastExportFileOpened;
	}
	
	public void setLastExportFileOpened(File lastExportFileOpened) {
		this.lastExportFileOpened = lastExportFileOpened;
	}

	
	
	
	
	public static void main(String[] args) {
		
		/*{
			BufferedImage cornerAtlas = Occlusion.generateEdgeNormalMapAtlas(1);
			try {
				ImageIO.write(cornerAtlas, "PNG", new File("./src/default_data/base_resource_pack/assets/miex/textures/corner_atlas.png"));
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			cornerAtlas = Occlusion.generateEdgeNormalMapAtlas(2);
			try {
				ImageIO.write(cornerAtlas, "PNG", new File("./src/default_data/base_resource_pack/assets/miex/textures/corner_atlas_small.png"));
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			cornerAtlas = Occlusion.generateEdgeNormalMapAtlas(4);
			try {
				ImageIO.write(cornerAtlas, "PNG", new File("./src/default_data/base_resource_pack/assets/miex/textures/corner_atlas_smaller.png"));
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}*/
		
		Environment.loadFromEnvFile();
		
		try {
			String numUIThreadsEnvVar = Environment.getEnv("MIEX_NUM_UI_THREADS");
			if(numUIThreadsEnvVar != null) {
				Integer val = Integer.parseInt(numUIThreadsEnvVar);
				numUIThreads = val.intValue();
			}
		}catch(Exception ex) {}
		
		try {
			String portableExportsEnvVar = Environment.getEnv("MIEX_PORTABLE_EXPORTS");
			if(portableExportsEnvVar != null) {
				portableExports = portableExportsEnvVar.toLowerCase().startsWith("t") || portableExportsEnvVar.startsWith("1");
			}
		}catch(Exception ex) {}
		
		try {
			String gitHubRepositoryEnvVar = Environment.getEnv("MIEX_GITHUB_REPO");
			if(gitHubRepositoryEnvVar != null)
				GitHubRepository = gitHubRepositoryEnvVar;
		}catch(Exception ex) {}
		
		try {
			String offlineModeEnvVar = Environment.getEnv("MIEX_OFFLINE_MODE");
			if(offlineModeEnvVar != null) {
				offlineMode = offlineModeEnvVar.toLowerCase().startsWith("t") || offlineModeEnvVar.startsWith("1");
			}
		}catch(Exception ex) {}
		
		try {
			String storeGenTexturesInExportEnvVar = Environment.getEnv("MIEX_STORE_GEN_TEX_IN_EXPORT");
			if(storeGenTexturesInExportEnvVar != null) {
				GeneratedTextures.storeGeneratedTexturesInExport = storeGenTexturesInExportEnvVar.toLowerCase().startsWith("t") || 
						storeGenTexturesInExportEnvVar.startsWith("1");
			}
		}catch(Exception ex) {}
		
		try {
			String genTexRPNameEnvVar = Environment.getEnv("MIEX_GEN_TEX_RP_NAME");
			if(genTexRPNameEnvVar != null) {
				GeneratedTextures.generatedTexturesResourcePackName = genTexRPNameEnvVar;
			}
		}catch(Exception ex) {}
		
		try {
			for(int i = 0; i < args.length; ++i) {
				if(args[i].equalsIgnoreCase("-homeDir"))
					FileUtil.homeDir = args[i+1];
				else if(args[i].equalsIgnoreCase("-rpDir")) {
					FileUtil.resourcePackDir = args[i+1].replace('\\', '/');
					if(!FileUtil.resourcePackDir.endsWith("/"))
						FileUtil.resourcePackDir = FileUtil.resourcePackDir + "/";
				}else if(args[i].equalsIgnoreCase("-rpUSDPrefix")) {
					FileUtil.resourcePackUSDPrefix = args[i+1];
					if(!FileUtil.resourcePackUSDPrefix.endsWith("/"))
						FileUtil.resourcePackUSDPrefix = FileUtil.resourcePackUSDPrefix + "/";
				}else if(args[i].equalsIgnoreCase("-rpMTLXPrefix")) {
					FileUtil.resourcePackMTLXPrefix = args[i+1];
					if(!FileUtil.resourcePackMTLXPrefix.endsWith("/"))
						FileUtil.resourcePackMTLXPrefix = FileUtil.resourcePackMTLXPrefix + "/";
				}else if(args[i].equalsIgnoreCase("-rpJSONPrefix")) {
					FileUtil.resourcePackJSONPrefix = args[i+1];
					if(!FileUtil.resourcePackJSONPrefix.endsWith("/"))
						FileUtil.resourcePackJSONPrefix = FileUtil.resourcePackJSONPrefix + "/";
				}else if(args[i].equalsIgnoreCase("-mcRootDir")) {
					FileUtil.multiMCRootDir = args[i+1].replace('\\', '/');
					if(!FileUtil.multiMCRootDir.endsWith("/"))
						FileUtil.multiMCRootDir = FileUtil.multiMCRootDir + "/";
				}else if(args[i].equalsIgnoreCase("-multimcRootDir")) {
					FileUtil.multiMCRootDir = args[i+1].replace('\\', '/');
					if(!FileUtil.multiMCRootDir.endsWith("/"))
						FileUtil.multiMCRootDir = FileUtil.multiMCRootDir + "/";
				}else if(args[i].equalsIgnoreCase("-technicRootDir")) {
					FileUtil.technicRootDir = args[i+1].replace('\\', '/');
					if(!FileUtil.technicRootDir.endsWith("/"))
						FileUtil.technicRootDir = FileUtil.technicRootDir + "/";
				}else if(args[i].equalsIgnoreCase("-modrinthRootDir")) {
					FileUtil.modrinthRootDir = args[i+1].replace('\\', '/');
					if(!FileUtil.modrinthRootDir.endsWith("/"))
						FileUtil.modrinthRootDir = FileUtil.modrinthRootDir + "/";
				}else if(args[i].equalsIgnoreCase("-hytaleRootDir")) {
					FileUtil.hytaleRootDir = args[i+1].replace('\\', '/');
					if(!FileUtil.hytaleRootDir.endsWith("/"))
						FileUtil.hytaleRootDir = FileUtil.hytaleRootDir + "/";
				}else if(args[i].equalsIgnoreCase("-additionalSaveDirs")) {
					FileUtil.additionalSaveDirs = new String[] {};
					
					String pathsStr = args[i+1];
					String[] paths = pathsStr.split(";");
					for(String str : paths) {
						FileUtil.additionalSaveDirs = Arrays.copyOf(FileUtil.additionalSaveDirs, 
								FileUtil.additionalSaveDirs.length + 1);
						FileUtil.additionalSaveDirs[FileUtil.additionalSaveDirs.length-1] = str;
					}
				}else if(args[i].equalsIgnoreCase("-usdcatExe")) {
					if(new File(args[i+1]).exists())
						FileUtil.usdCatExe = args[i+1];
				}else if(args[i].equalsIgnoreCase("-log")) {
					if(new File(args[i+1]).exists())
						FileUtil.logFile = args[i+1];
				}else if(args[i].equalsIgnoreCase("-resourcePack")) {
					defaultResourcePacks.add(args[i+1]);
				}else if(args[i].equalsIgnoreCase("-numUIThreads")) {
					try {
						Integer val = Integer.parseInt(args[i+1]);
						numUIThreads = val.intValue();
					}catch(Exception ex) {}
				}
				else if(args[i].equalsIgnoreCase("-portableExports")) {
					portableExports = true;
				}
				else if(args[i].equalsIgnoreCase("-offlineMode")) {
					offlineMode = true;
				}
				else if(args[i].equalsIgnoreCase("-githubRepo")) {
					GitHubRepository = args[i+1];
				}
				else if(args[i].equalsIgnoreCase("-storeGenTexInExport")) {
					GeneratedTextures.storeGeneratedTexturesInExport = true;
				}
				else if(args[i].equalsIgnoreCase("-genTexRpName")) {
					GeneratedTextures.generatedTexturesResourcePackName = args[i+1];
				}
				else if(args[i].equalsIgnoreCase("-output")) {
					forceOutputPath = args[i+1];
					if(!forceOutputPath.endsWith(".usd"))
						forceOutputPath = forceOutputPath + ".usd";
				}
				else if(args[i].equalsIgnoreCase("-world")) {
					forceOpenWorld = args[i+1];
					if(!(new File(forceOpenWorld).exists())) {
						for(Launcher launcher : LauncherRegistry.getLaunchers()) {
							boolean foundFile = false;
							for(MinecraftSave save : launcher.getSaves()) {
								if(save.getLabel().equals(forceOpenWorld)) {
									forceOpenWorld = save.getWorldFolder().getPath();
									foundFile = true;
									break;
								}
								if(foundFile)
									break;
							}
						}
					}
				}
			}
		}catch(Exception ex) {
			System.out.println("Could not correctly parse command line arguments!");
		}
		
		ReleaseChecker.checkRelease();
		
		Logger.init();
		System.setOut(new Logger(System.out, false));
		System.setErr(new Logger(System.err, true));
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		// Only support 64 bit Java
		String archModel = System.getProperty("sun.arch.data.model");
		if(!archModel.contains("64")) {
			JOptionPane.showMessageDialog(null, "MiEx only supports 64-bit Java. Please make sure that MiEx is launched with 64-bit Java", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		new MCWorldExporter();
	}

}
