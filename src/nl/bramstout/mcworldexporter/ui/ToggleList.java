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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

public class ToggleList extends JPanel{

	public static interface SelectionListener{
		
		public void onSelectionChanged(List<Entry<String, Boolean>> items);
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JLabel label;
	private ToggleListView view;
	private JScrollPane scrollPane;
	private JButton selectAllButton;
	private JButton deselectAllButton;
	private SelectionListener listener;
	
	public ToggleList(String labelText) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.listener = null;
		
		label = new JLabel(labelText);
		label.setAlignmentX(0.5f);
		add(label);
		
		view = new ToggleListView();
		
		scrollPane = new JScrollPane(view);
		scrollPane.setBorder(new EmptyBorder(8, 0, 8, 0));
		add(scrollPane);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setMaximumSize(new Dimension(10000, 32));
		buttonPanel.setMinimumSize(new Dimension(0, 32));
		buttonPanel.setPreferredSize(new Dimension(100, 32));
		buttonPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		add(buttonPanel);
		
		selectAllButton = new JButton("Select All");
		selectAllButton.setMaximumSize(new Dimension(10000, 32));
		buttonPanel.add(selectAllButton);
		
		JPanel buttonSeparator = new JPanel();
		buttonSeparator.setMaximumSize(new Dimension(8, 0));
		buttonPanel.add(buttonSeparator);
		
		deselectAllButton = new JButton("Deselect All");
		deselectAllButton.setMaximumSize(new Dimension(10000, 32));
		buttonPanel.add(deselectAllButton);
		
		selectAllButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				view.selectAll();
			}
			
		});
		
		deselectAllButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				view.deselectAll();
			}
			
		});
	}
	
	public List<Entry<String, Boolean>> getItems(){
		return view.getItems();
	}
	
	public void setItems(List<String> items, boolean selected) {
		view.setItems(items, selected);
	}
	
	public void setItemsPreserveSelection(List<String> items) {
		view.setItemsPreserveSelection(items);
	}
	
	public void deselect(String item) {
		view.deselect(item);
	}
	
	public void select(String item) {
		view.select(item);
	}
	
	public void deselectAll() {
		view.deselectAll();
	}
	
	public void selectAll() {
		view.selectAll();
	}
	
	public void setSelection(List<String> selection) {
		view.setSelection(selection);
	}
	
	public void addSelectionListener(SelectionListener listener) {
		this.listener = listener;
		this.view.addSelectionListener(listener);
	}
	
	public SelectionListener getSelectionListener() {
		return listener;
	}
	
	public List<String> getSelection(){
		return view.getSelection();
	}
	

}
