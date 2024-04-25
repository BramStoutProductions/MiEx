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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.world.Player;

public class TeleportDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public TeleportDialog() {
		super(MCWorldExporter.getApp().getUI());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
		root.setBorder(new EmptyBorder(16, 16, 16, 16));
		add(root);
		
		
		
		JPanel coordinatePanel = new JPanel();
		coordinatePanel.setLayout(new BorderLayout(8,8));
		coordinatePanel.setBorder(new EmptyBorder(0, 0, 0, 16));
		coordinatePanel.setPreferredSize(new Dimension(200, 80));
		coordinatePanel.setMinimumSize(coordinatePanel.getPreferredSize());
		coordinatePanel.setMaximumSize(coordinatePanel.getPreferredSize());
		JLabel coordinateLabel = new JLabel("Coordinates (x,z)");
		coordinatePanel.add(coordinateLabel, BorderLayout.NORTH);
		JTextField coordinateInput = new JTextField("0,0");
		coordinatePanel.add(coordinateInput, BorderLayout.CENTER);
		JButton coordinateTeleportButton = new JButton("Teleport");
		coordinatePanel.add(coordinateTeleportButton, BorderLayout.SOUTH);
		root.add(coordinatePanel);
		coordinateTeleportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String inputValue = coordinateInput.getText();
					if(inputValue == null)
						return;
					if (!inputValue.contains(","))
						return;
					String[] tokens = inputValue.split(",");
					int x = Integer.parseInt(tokens[0].trim());
					int z = Integer.parseInt(tokens[1].trim());
					MCWorldExporter.getApp().getUI().getViewer().teleport(x, z);
					setVisible(false);
				}catch(Exception ex) {}
			}
			
		});
		
		
		JPanel playerPanel = new JPanel();
		playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
		playerPanel.setBorder(new EmptyBorder(0, 16, 0, 0));
		playerPanel.setPreferredSize(new Dimension(300, 250));
		playerPanel.setMinimumSize(playerPanel.getPreferredSize());
		playerPanel.setMaximumSize(playerPanel.getPreferredSize());
		JPanel playerScrollPanel = new JPanel();
		playerScrollPanel.setLayout(new BoxLayout(playerScrollPanel, BoxLayout.Y_AXIS));
		playerScrollPanel.setBackground(getBackground().brighter());
		JScrollPane playerScrollPane = new JScrollPane(playerScrollPanel);
		playerScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		playerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		playerPanel.add(playerScrollPane);
		
		if(MCWorldExporter.getApp().getWorld() != null) {
			for(Player player : MCWorldExporter.getApp().getWorld().getPlayers()) {
				playerScrollPanel.add(new PlayerItem(this, player));
			}
		}
		
		root.add(playerPanel);
		
		
		setSize(550, 300);
		setTitle("Teleport");
	}
	
	private static class PlayerItem extends JPanel{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public PlayerItem(TeleportDialog parent, Player player) {
			setLayout(new BorderLayout());
			
			setPreferredSize(new Dimension(250, 48));
			setMinimumSize(new Dimension(250, 48));
			setMaximumSize(new Dimension(350, 48));

			setBorder(new CompoundBorder(new LineBorder(Color.gray, 1), new EmptyBorder(5, 5, 5, 5)));
			
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
			infoPanel.add(new JLabel(player.getName()));
			infoPanel.add(new JLabel(player.getDimension()));
			add(infoPanel, BorderLayout.CENTER);
			
			JButton teleportButton = new JButton("TP");
			add(teleportButton, BorderLayout.EAST);
			
			teleportButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					MCWorldExporter.getApp().getUI().getViewer().teleport((int) player.getX(), (int)player.getZ());
					parent.setVisible(false);
				}
				
			});
		}
		
	}

}
