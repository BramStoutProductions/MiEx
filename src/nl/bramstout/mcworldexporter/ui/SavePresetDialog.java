package nl.bramstout.mcworldexporter.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.MCWorldExporter;
import nl.bramstout.mcworldexporter.Preset;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePack;
import nl.bramstout.mcworldexporter.resourcepack.ResourcePacks;

public class SavePresetDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JTextField nameInput;
	private JComboBox<String> resourcePacks;
	
	public SavePresetDialog() {
		super(MCWorldExporter.getApp().getUI());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
		root.setBorder(new EmptyBorder(8, 8, 8, 8));
		add(root);
		
		JLabel nameInputLabel = new JLabel(" Preset Name:");
		nameInputLabel.setPreferredSize(new Dimension(300-16, 16));
		nameInputLabel.setMinimumSize(nameInputLabel.getPreferredSize());
		nameInputLabel.setMaximumSize(nameInputLabel.getPreferredSize());
		nameInputLabel.setAlignmentX(0f);
		root.add(nameInputLabel);
		nameInput = new JTextField();
		nameInput.setPreferredSize(new Dimension(300-16, 24));
		nameInput.setMinimumSize(nameInput.getPreferredSize());
		nameInput.setMaximumSize(nameInput.getPreferredSize());
		nameInput.setAlignmentX(0f);
		ToolTips.registerTooltip(nameInput, ToolTips.ATLAS_CREATOR_DIALOG_SAVE_TO);
		root.add(nameInput);
		
		
		JPanel separator1 = new JPanel();
		separator1.setPreferredSize(new Dimension(300-16, 12));
		separator1.setMinimumSize(separator1.getPreferredSize());
		separator1.setMaximumSize(separator1.getPreferredSize());
		separator1.setAlignmentX(0f);
		root.add(separator1);
		
		
		JLabel resourcePacksLabel = new JLabel(" Save To Pack:");
		resourcePacksLabel.setPreferredSize(new Dimension(300-16, 16));
		resourcePacksLabel.setMinimumSize(resourcePacksLabel.getPreferredSize());
		resourcePacksLabel.setMaximumSize(resourcePacksLabel.getPreferredSize());
		resourcePacksLabel.setAlignmentX(0f);
		root.add(resourcePacksLabel);
		resourcePacks = new JComboBox<String>();
		resourcePacks.setPreferredSize(new Dimension(300-16, 24));
		resourcePacks.setMinimumSize(resourcePacks.getPreferredSize());
		resourcePacks.setMaximumSize(resourcePacks.getPreferredSize());
		resourcePacks.setAlignmentX(0f);
		resourcePacks.setEditable(false);
		root.add(resourcePacks);
		
		
		JPanel separator2 = new JPanel();
		separator2.setPreferredSize(new Dimension(300-16, 24));
		separator2.setMinimumSize(separator2.getPreferredSize());
		separator2.setMaximumSize(separator2.getPreferredSize());
		separator2.setAlignmentX(0f);
		root.add(separator2);
		
		
		JButton createButton = new JButton("Save Preset");
		createButton.setPreferredSize(new Dimension(300-16, 24));
		createButton.setMinimumSize(createButton.getPreferredSize());
		createButton.setMaximumSize(createButton.getPreferredSize());
		createButton.setAlignmentX(0.0f);
		root.add(createButton);
		
		setSize(300, 208);
		setTitle("Save Preset");
		
		createButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(nameInput.getText().trim().isEmpty()) {
					JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), 
							"Please specify a name for the preset.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String presetName = nameInput.getText();
				String parentName = (String) resourcePacks.getSelectedItem();
				Preset preset = new Preset(presetName, parentName, parentName);
				preset.fromApp();
				Preset.addPreset(preset);
				
				JOptionPane.showMessageDialog(MCWorldExporter.getApp().getUI(), 
						"Preset saved successfully", "Done", JOptionPane.PLAIN_MESSAGE);
				setVisible(false);
			}
			
		});
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(b) {
			nameInput.setText("");
			
			resourcePacks.removeAllItems();
			
			Set<String> existingNames = new HashSet<String>();
			for(ResourcePack pack : ResourcePacks.getResourcePacks()) {
				if(existingNames.contains(pack.getName()))
					continue;
				resourcePacks.addItem(pack.getName());
			}
			
			for(int i = 0; i < resourcePacks.getItemCount(); ++i) {
				if(resourcePacks.getItemAt(i).equals(ResourcePacks.getBaseResourcePack().getName())) {
					resourcePacks.setSelectedIndex(i);
					break;
				}
			}
		}
	}

}
