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
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.ReleaseChecker;

public class AboutDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AboutDialog() {
		super(MCWorldExporter.getApp().getUI(), Dialog.ModalityType.APPLICATION_MODAL);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(root);
		
		JLabel versionLabel = new JLabel("MiEx " + ReleaseChecker.CURRENT_VERSION);
		versionLabel.setAlignmentX(0.5f);
		versionLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
		root.add(versionLabel);
		
		JLabel wikiLabel = new JLabel("For help on how to use MiEx, visit the Wiki");
		wikiLabel.setForeground(new Color(0f, 0.1f, 0.35f));
		wikiLabel.setAlignmentX(0.5f);
		wikiLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
		wikiLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		wikiLabel.addMouseListener(new MouseAdapter() {
			 
			@Override
			public void mousePressed(MouseEvent e) {
				try {
		            Desktop.getDesktop().browse(new URI("https://github.com/BramStoutProductions/MiEx/wiki/08.-Usage"));
		        }catch(Exception e1) {
		            e1.printStackTrace();
		        }
			}
		 
		    @Override
		    public void mouseEntered(MouseEvent e) {
		    	wikiLabel.setForeground(new Color(0f, 0.2f, 0.66f));
		    }
		 
		    @Override
		    public void mouseExited(MouseEvent e) {
		    	wikiLabel.setForeground(new Color(0f, 0.1f, 0.35f));
		    }
		});
		root.add(wikiLabel);
		
		try {
			JEditorPane editorPane = new JEditorPane(AboutDialog.class.getClassLoader().getResource("about.txt"));
			editorPane.setEditable(false);
			JScrollPane editorScrollPane = new JScrollPane(editorPane);
			editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			root.add(editorScrollPane);
		}catch(Exception ex) {
			ex.printStackTrace();
			root.add(new JLabel("Could not load about text"));
		}
		
		setSize(800, 600);
		setTitle("About");
	}

}
