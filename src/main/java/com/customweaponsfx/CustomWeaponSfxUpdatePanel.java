package com.customweaponsfx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class CustomWeaponSfxUpdatePanel extends JPanel
{
	CustomWeaponSfxUpdatePanel(String version, String patchNotes, Runnable onDismiss)
	{
		JLabel titleLabel = new JLabel("Custom Weapon SFX " + version);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setHorizontalAlignment(JLabel.CENTER);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel patchLabel = new JLabel("What's New");
		patchLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		patchLabel.setHorizontalAlignment(JLabel.CENTER);
		patchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JSeparator sepTop = new JSeparator();
		sepTop.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		sepTop.setBackground(ColorScheme.LIGHT_GRAY_COLOR);

		JSeparator sepBottom = new JSeparator();
		sepBottom.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		sepBottom.setBackground(ColorScheme.LIGHT_GRAY_COLOR);

		JTextArea notes = new JTextArea(patchNotes);
		notes.setFont(FontManager.getRunescapeSmallFont());
		notes.setWrapStyleWord(true);
		notes.setLineWrap(true);
		notes.setOpaque(false);
		notes.setEditable(false);
		notes.setFocusable(false);
		notes.setBackground(ColorScheme.DARK_GRAY_COLOR);
		notes.setBorder(new EmptyBorder(0, 0, 0, 0));

		JPanel notesPanel = new JPanel();
		notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
		notesPanel.add(patchLabel);
		notesPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		notesPanel.add(sepTop);
		notesPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		notesPanel.add(notes);
		notesPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		notesPanel.add(sepBottom);
		// prevents the panel from expanding horizontally past its preferred size
		notesPanel.getPreferredSize();

		JButton dismissBtn = new JButton("Got it!");
		dismissBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		dismissBtn.addActionListener(e -> onDismiss.run());

		JLabel dismissHint = new JLabel("Won't show again until the next update");
		dismissHint.setFont(FontManager.getRunescapeSmallFont());
		dismissHint.setHorizontalAlignment(JLabel.CENTER);
		dismissHint.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.add(titleLabel);
		content.add(Box.createRigidArea(new Dimension(0, 8)));
		content.add(notesPanel);
		content.add(Box.createRigidArea(new Dimension(0, 10)));
		content.add(dismissBtn);
		content.add(Box.createRigidArea(new Dimension(0, 4)));
		content.add(dismissHint);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(5, 0, 10, 0));
		add(content, BorderLayout.NORTH);
	}
}
