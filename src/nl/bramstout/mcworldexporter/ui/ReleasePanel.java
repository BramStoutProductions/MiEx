package nl.bramstout.mcworldexporter.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import nl.bramstout.mcworldexporter.ReleaseChecker;

public class ReleasePanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JLabel label;
	JButton closeButton;
	
	public ReleasePanel() {
		super();
		setBackground(new Color(220, 210, 180));
		setLayout(new BorderLayout());
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		label = new JLabel("New release is available: " + ReleaseChecker.LATEST_VERSION);
		label.setBorder(new EmptyBorder(0, 8, 0, 0));
		add(label, BorderLayout.CENTER);
		
		closeButton = new JButton("X");
		closeButton.setOpaque(false);
		add(closeButton, BorderLayout.EAST);
		
		closeButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
			
		});
		
		setVisible(ReleaseChecker.hasNewRelease());
		
		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if(!Desktop.isDesktopSupported())
					return;
				try {
					Desktop.getDesktop().browse(new URI(ReleaseChecker.LATEST_VERSION_URL));
				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
			
		});
	}

}
