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
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import nl.bramstout.mcworldexporter.export.Noise;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.ui.MainWindow;
import nl.bramstout.mcworldexporter.world.World;
import nl.bramstout.mcworldexporter.world.anvil.WorldAnvil;

public class MCWorldExporter {
	
	private static MCWorldExporter instance;
	public static String forceOutputPath = null;
	public static String forceOpenWorld = null;
	public static List<String> defaultResourcePacks = new ArrayList<String>();
	
	public static MCWorldExporter getApp() {
		return instance;
	}
	
	
	
	
	private World world;
	private MainWindow ui;
	private ExportBounds exportBounds;
	private List<String> fgChunks;
	
	public MCWorldExporter() {
		Noise.init();
		Config.load();
		Atlas.readAtlasConfig();
		
		instance = this;
		world = null;
		exportBounds = new ExportBounds();
		fgChunks = new ArrayList<String>();
		ui = new MainWindow();
		ui.setLocationRelativeTo(null);
		ui.setVisible(true);
		
		ResourcePack.setupDefaults();
		
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if(forceOpenWorld != null)
					setWorld(new File(forceOpenWorld));
			}
			
		});
	}
	
	public void setWorld(File worldFolder) {
		world = new WorldAnvil(worldFolder);
		ui.reset();
	}
	
	public World getWorld() {
		return world;
	}
	
	public MainWindow getUI() {
		return ui;
	}
	
	public ExportBounds getExportBounds() {
		return exportBounds;
	}
	
	public List<String> getFGChunks(){
		return fgChunks;
	}

	
	
	
	
	public static void main(String[] args) {
		try {
			for(int i = 0; i < args.length; ++i) {
				if(args[i].equalsIgnoreCase("-homeDir"))
					FileUtil.homeDir = args[i+1];
				else if(args[i].equalsIgnoreCase("-rpDir"))
					FileUtil.resourcePackDir = args[i+1];
				else if(args[i].equalsIgnoreCase("-rpUSDPrefix"))
					FileUtil.resourcePackUSDPrefix = args[i+1];
				else if(args[i].equalsIgnoreCase("-usdcatExe")) {
					if(new File(args[i+1]).exists())
						FileUtil.usdCatExe = args[i+1];
				}else if(args[i].equalsIgnoreCase("-resourcePack")) {
					defaultResourcePacks.add(args[i+1]);
				}
				else if(args[i].equalsIgnoreCase("-output")) {
					forceOutputPath = args[i+1];
					if(!forceOutputPath.endsWith(".usd"))
						forceOutputPath = forceOutputPath + ".usd";
				}
				else if(args[i].equalsIgnoreCase("-world")) {
					forceOpenWorld = args[i+1];
					if(!(new File(forceOpenWorld).exists())) {
						forceOpenWorld = FileUtil.getMinecraftSavesDir() + "/" + forceOpenWorld;
						if(!(new File(forceOpenWorld).exists()))
							forceOpenWorld = null;
					}
				}
			}
		}catch(Exception ex) {
			System.out.println("Could not correctly parse command line arguments!");
		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		new MCWorldExporter();
	}

}
