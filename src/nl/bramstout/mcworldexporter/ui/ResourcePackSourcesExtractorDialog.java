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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackDefaults;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePackSource;
import nl.bramstout.mcworldexporter.world.World;

public class ResourcePackSourcesExtractorDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ResourcePackSourcesExtractorDialog(World world, List<ResourcePackSource> sources) {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(root);
		

		JLabel infoLabel = new JLabel("Could not find the necessary resource packs installed!");
		infoLabel.setAlignmentX(0f);
		root.add(infoLabel);
		JLabel infoLabel2 = new JLabel("You can install the resource packs using this dialog.");
		infoLabel2.setAlignmentX(0f);
		root.add(infoLabel2);

		root.add(new JPanel());
		
		JLabel sourcesLabel = new JLabel("Sources");
		sourcesLabel.setAlignmentX(0f);
		root.add(sourcesLabel);
		JPanel sourcesPanel = new JPanel();
		sourcesPanel.setLayout(new BoxLayout(sourcesPanel, BoxLayout.Y_AXIS));
		sourcesPanel.setBackground(getBackground().brighter());
		JScrollPane sourcesScrollPane = new JScrollPane(sourcesPanel);
		sourcesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sourcesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		sourcesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		sourcesScrollPane.setPreferredSize(new Dimension(400, 400));
		sourcesScrollPane.setAlignmentX(0f);
		root.add(sourcesScrollPane);
		
		for(ResourcePackSource source : sources) {
			sourcesPanel.add(new SourceComponent(source));
		}
		
		root.add(new JPanel());
		
		JLabel resourcePackLabel = new JLabel("Resource Pack Name");
		resourcePackLabel.setAlignmentX(0f);
		root.add(resourcePackLabel);
		JTextField resourcePackInput = new JTextField();
		resourcePackInput.setAlignmentX(0f);
		root.add(resourcePackInput);
		String rpName = world.getWorldDir().getName();
		for(int i = 2; i < 10000; ++i) {
			if(!new File(FileUtil.getResourcePackDir(), rpName).exists()) {
				break;
			}
			rpName = world.getWorldDir().getName() + "_" + Integer.toString(i);
		}
		resourcePackInput.setText(rpName);
		
		root.add(new JPanel());
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout(0, 0));
		buttonPanel.setAlignmentX(0f);
		root.add(buttonPanel);
		JButton createButton = new JButton("Install");
		createButton.setPreferredSize(new Dimension(174, 24));
		buttonPanel.add(createButton, BorderLayout.WEST);
		JButton closeButton = new JButton("Close");
		closeButton.setPreferredSize(new Dimension(174, 24));
		buttonPanel.add(closeButton, BorderLayout.EAST);
		
		
		setSize(400, 400);
		setTitle("Install World Resource Pack");
				
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean hasAllSelected = true;
				
				List<ResourcePackSource> sourcesToInstall = new ArrayList<ResourcePackSource>();
				for(Component component : sourcesPanel.getComponents()) {
					if(!(component instanceof SourceComponent))
						continue;
					SourceComponent sourceComp = (SourceComponent)component;
					
					if(sourceComp.isSelected())
						sourcesToInstall.add(sourceComp.getSource());
					else
						hasAllSelected = false;
				}
				if(!sourcesToInstall.isEmpty()) {
					ResourcePackDefaults.extractSourcesIntoResourcePack(sources, 
							new File(FileUtil.getResourcePackDir(), resourcePackInput.getText()));
				}
				
				if(hasAllSelected)
					// If all sources were installed,
					// then there isn't really any use for this dialog
					// anymore and the user most likely will expect
					// the dialog to disappear, so let's hide it.
					// But, if not all sources were selected,
					// it could be that the user wants to install them
					// into separate resource packs, so let's not hide
					// the dialog then, so that the user can do that.
					setVisible(false);
			}
			
		});
		
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});
	}
	
	private static class SourceComponent extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JCheckBox enabledCheckbox;
		private JLabel label;
		private ResourcePackSource source;
		
		public SourceComponent(ResourcePackSource source) {
			this.source = source;
			setLayout(new BorderLayout(8, 0));
			
			this.enabledCheckbox = new JCheckBox();
			this.enabledCheckbox.setSelected(true);
			add(this.enabledCheckbox, BorderLayout.WEST);
			
			this.label = new JLabel(source.getName());
			add(this.label, BorderLayout.CENTER);
			
			setPreferredSize(new Dimension(350, 22));
			setMinimumSize(new Dimension(250, 22));
			setMaximumSize(new Dimension(400, 22));
		}
		
		public boolean isSelected() {
			return this.enabledCheckbox.isSelected();
		}
		
		public ResourcePackSource getSource() {
			return source;
		}
		
	}

}
