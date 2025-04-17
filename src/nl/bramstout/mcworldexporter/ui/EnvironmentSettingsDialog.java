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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.Environment;
import nl.bramstout.mcworldexporter.Environment.EnvironmentVariable;
import nl.bramstout.mcworldexporter.FileUtil;
import nl.bramstout.mcworldexporter.MCWorldExporter;

public class EnvironmentSettingsDialog extends JDialog implements ComponentListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JScrollPane variablesScrollPane;
	private JPanel variablesPanel;
	
	public EnvironmentSettingsDialog() {
		super(MCWorldExporter.getApp().getUI());
		setModalityType(ModalityType.APPLICATION_MODAL);
		
		JPanel root = new JPanel();
		root.setLayout(new DialogLayout());
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(root);
		
		variablesPanel = new JPanel();
		variablesPanel.setLayout(new ListLayout());
		variablesPanel.setBackground(getBackground().brighter());
		variablesScrollPane = new JScrollPane(variablesPanel);
		variablesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		variablesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		variablesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		root.add(variablesScrollPane);
		
		JButton saveButton = new JButton("Save");
		root.add(saveButton);
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				complete(false);
			}
			
		});
		
		JButton closeButton = new JButton("Close");
		root.add(closeButton);
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				complete(true);
			}
			
		});
		
		setSize(900, 600);
		setTitle("Environment Settings");
		
		addComponentListener(this);
	}
	
	@Override
	public void setVisible(boolean v) {
		if(v) {
			variablesPanel.removeAll();
			for(EnvironmentVariable var : Environment.ENVIRONMENT_VARIABLES) {
				variablesPanel.add(new VariableWidget(var));
			}
		}
		super.setVisible(v);
	}
	
	public void complete(boolean cancelled) {
		if(!cancelled) {
			Map<String, String> values = new HashMap<String, String>();
			for(Component comp : variablesPanel.getComponents()) {
				if(!(comp instanceof VariableWidget))
					continue;
				VariableWidget widget = (VariableWidget) comp;
				if(widget.isEnabled()) {
					String value = widget.getValue();
					if(value == null || value.isEmpty())
						continue;
					values.put(widget.getVariable().getName(), widget.getValue());
				}
			}
			
			if(Environment.hasChanged(values)) {
				Environment.setEnv(values);
				Environment.saveToEnvFile();
			}
			JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), "The new settings will go in effect the next time you launch MiEx.", "Saved", JOptionPane.PLAIN_MESSAGE);
		}
		if(isVisible())
			setVisible(false);
	}
	
	@Override
	public void componentHidden(ComponentEvent e) {
		complete(true);
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
	
	private static class DialogLayout implements LayoutManager{

		@Override
		public void addLayoutComponent(String name, Component comp) {
			
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			int width = 900;
			int height = 600;
			return new Dimension(width, height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		@Override
		public void layoutContainer(Container parent) {
			int padding = 8;
			int buttonHeight = 24;
			
			int i = 0;
			int numButtons = parent.getComponents().length - 1;
			int buttonsWidth = parent.getWidth() / 3;
			int buttonWidth = (buttonsWidth - (padding * numButtons)) / numButtons;
			int buttonX = parent.getWidth() - buttonsWidth;
			for(Component comp : parent.getComponents()) {
				if(i == 0) {
					comp.setBounds(padding, padding, parent.getWidth() - padding - padding, parent.getHeight() - buttonHeight - padding * 3);
				}else{
					comp.setBounds(buttonX, parent.getHeight() - buttonHeight - padding, buttonWidth, buttonHeight);
					buttonX += buttonWidth + padding;
				}
				i++;
			}
		}
	
	}
	
	private static class ListLayout implements LayoutManager{

		@Override
		public void addLayoutComponent(String name, Component comp) {
			
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			int padding = 2;
			
			int width = 0;
			int height = padding;
			for(Component comp : parent.getComponents()) {
				width = Math.max(width, comp.getMinimumSize().width);
				height += comp.getPreferredSize().height + padding;
			}
			return new Dimension(width, height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		@Override
		public void layoutContainer(Container parent) {
			int padding = 2;
			
			int y = padding;
			for(Component comp : parent.getComponents()) {
				int height = comp.getPreferredSize().height;
				comp.setBounds(0, y, parent.getWidth(), height);
				y += height + padding;
			}
		}
	
	}
	
	public static class VariableWidget extends JPanel{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private EnvironmentVariable variable;
		private JCheckBox enabledCheckbox;
		private VariableEditor editor;
		
		public VariableWidget(EnvironmentVariable variable) {
			this.variable = variable;
			
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new EmptyBorder(4, 4, 4, 4));
			setToolTipText(variable.getDescription());
			
			enabledCheckbox = new JCheckBox();
			enabledCheckbox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					_update();
				}
				
			});
			add(enabledCheckbox);
			
			JLabel nameLabel = new JLabel(variable.getName());
			nameLabel.setMinimumSize(new Dimension(250, 16));
			nameLabel.setPreferredSize(new Dimension(250, 16));
			nameLabel.setMaximumSize(new Dimension(250, 16));
			add(nameLabel);
			
			switch(variable.getType()) {
			case BOOLEAN:
				editor = new VariableEditorBoolean();
				break;
			case FILE:
				editor = new VariableEditorPath(false);
				break;
			case FILE_ARRAY:
				editor = new VariableEditorPathList(false);
				break;
			case FLOAT:
				editor = null;
				break;
			case FOLDER:
				editor = new VariableEditorPath(true);
				break;
			case FOLDER_ARRAY:
				editor = new VariableEditorPathList(true);
				break;
			case INTEGER:
				editor = new VariableEditorInteger();
				break;
			case STRING:
				editor = new VariableEditorString();
				break;
			case STRING_ARRAY:
				editor = null;
				break;
			}
			if(editor != null) {
				add((Component) editor);
				String value = Environment.getEnv(variable.getName());
				if(value == null)
					value = variable.getDefaultValue();
				editor.setFromStringValue(value);
			}
			
			enabledCheckbox.setSelected(Environment.hasEditedInEnvFile(variable.getName()));
			_update();
		}
		
		public void _update() {
			setEnabled(isEnabled());
			if(editor != null)
				((Component) editor).setEnabled(isEnabled());
		}
		
		public boolean isEnabled() {
			return enabledCheckbox.isSelected();
		}
		
		public String getValue() {
			if(editor == null)
				return null;
			return editor.getStringValue();
		}
		
		public EnvironmentVariable getVariable() {
			return variable;
		}
		
	}
	
	public static interface VariableEditor{
		
		public void setFromStringValue(String value);
		
		public String getStringValue();
		
	}
	
	public static class VariableEditorString extends JTextField implements VariableEditor{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void setFromStringValue(String value) {
			setText(value);
		}

		@Override
		public String getStringValue() {
			return getText();
		}
		
	}
	
	public static class VariableEditorInteger extends JSpinner implements VariableEditor{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void setFromStringValue(String value) {
			try {
				setValue(Integer.parseInt(value));
			}catch(Exception ex) {
				
			}
		}

		@Override
		public String getStringValue() {
			return Integer.toString((Integer) getValue());
		}
		
	}
	
	public static class VariableEditorBoolean extends JCheckBox implements VariableEditor{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void setFromStringValue(String value) {
			boolean boolVal = value.startsWith("t") || value.startsWith("1");
			setSelected(boolVal);
		}

		@Override
		public String getStringValue() {
			return isSelected() ? "1" : "0";
		}
		
	}
	
	public static class VariableEditorPath extends JPanel implements VariableEditor{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private JTextField textInput;
		private JButton browseButton;
		
		public VariableEditorPath(boolean isDirectory) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new EmptyBorder(0, 0, 0, 0));
			
			this.textInput = new JTextField();
			add(textInput);
			
			this.browseButton = new JButton("...");
			this.browseButton.setMargin(new Insets(0, 0, 0, 0));
			this.browseButton.setMinimumSize(new Dimension(24, 24));
			this.browseButton.setMaximumSize(new Dimension(24, 24));
			this.browseButton.setPreferredSize(new Dimension(24, 24));
			add(this.browseButton);
			
			this.browseButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					chooser.setApproveButtonText("Select");
					chooser.setDialogTitle(isDirectory ? "Select Directory" : "Select File");
					chooser.setFileSelectionMode(isDirectory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
					File dir = new File(textInput.getText()).getParentFile();
					if(dir == null || !dir.exists())
						dir = new File(FileUtil.getHomeDir());
					chooser.setCurrentDirectory(dir);
					
					int result = chooser.showOpenDialog(MCWorldExporter.getApp().getUI());
					if (result == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						
						setFromStringValue(file.getPath());
					}
				}
				
			});
		}

		@Override
		public void setFromStringValue(String value) {
			this.textInput.setText(value);
		}

		@Override
		public String getStringValue() {
			return this.textInput.getText();
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			this.textInput.setEnabled(enabled);
			this.browseButton.setEnabled(enabled);
		}
		
	}
	
	public static class ListItem extends JPanel{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Component item;
		private JButton deleteButton;
		
		public ListItem(Component item) {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setBorder(new EmptyBorder(0,0,0,0));
			
			this.item = item;
			add(item);
			
			deleteButton = new JButton("X");
			deleteButton.setMargin(new Insets(0, 0, 0, 0));
			deleteButton.setMinimumSize(new Dimension(24, 24));
			deleteButton.setMaximumSize(new Dimension(24, 24));
			deleteButton.setPreferredSize(new Dimension(24, 24));
			add(deleteButton);
			
			deleteButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Container parent = getParent();
					getParent().remove(ListItem.this);
					parent.invalidate();
					parent.getParent().invalidate();
					parent.getParent().validate();
				}
				
			});
		}
		
		public Component getItem() {
			return item;
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			item.setEnabled(enabled);
			deleteButton.setEnabled(enabled);
		}
		
	}
	
	public static class VariableEditorPathList extends JScrollPane implements VariableEditor{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private boolean isDirectory;
		private JPanel root;
		private JButton addButton;
		
		public VariableEditorPathList(boolean isDirectory) {
			super();
			this.isDirectory = isDirectory;
			setBorder(new EmptyBorder(0, 0, 0, 0));
			
			setMinimumSize(new Dimension(20, 150));
			setPreferredSize(new Dimension(200, 150));
			
			root = new JPanel();
			root.setLayout(new ListLayout());
			root.setBackground(getBackground().brighter());
			setViewportView(root);
			setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			
			addButton = new JButton("Add Item");
			addButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					root.add(new ListItem(new VariableEditorPath(isDirectory)), root.getComponentCount()-1);
					root.invalidate();
					invalidate();
					validate();
				}
				
			});
			setFromStringValue(null);
		}

		@Override
		public void setFromStringValue(String value) {
			root.removeAll();
			if(value == null) {
				root.add(addButton);
				return;
			}
			
			String[] items = value.split(";");
			for(String item : items) {
				VariableEditorPath editor = new VariableEditorPath(isDirectory);
				editor.setFromStringValue(item);
				root.add(new ListItem(editor));
			}
			root.add(addButton);
			setEnabled(isEnabled());
		}

		@Override
		public String getStringValue() {
			String str = null;
			for(Component comp : root.getComponents()) {
				if(comp instanceof ListItem) {
					if(((ListItem) comp).getItem() instanceof VariableEditor) {
						String editorVal = ((VariableEditor) ((ListItem) comp).getItem()).getStringValue();
						if(str == null)
							str = editorVal;
						else
							str = str + ";" + editorVal;
					}
				}
			}
			return str;
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			root.setEnabled(enabled);
			for(Component comp : root.getComponents())
				comp.setEnabled(enabled);
		}
		
	}

}
