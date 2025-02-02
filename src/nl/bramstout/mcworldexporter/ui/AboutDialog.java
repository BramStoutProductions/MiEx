package nl.bramstout.mcworldexporter.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
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
		super(MCWorldExporter.getApp().getUI());
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
