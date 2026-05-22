package com.customweaponsfx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

public class CustomWeaponSfxPanel extends PluginPanel
{
	static final String BUNDLED_PREFIX = "bundled:";
	private static final String BUILTIN_SUFFIX = " (built-in)";

	static final String UNARMED_GROUPS_PREFIX  = "defaultAttack";
	static final String RECEIVED_GROUPS_PREFIX = "defaultReceived";
	static final String THRALL_GROUPS_PREFIX   = "defaultThrall";


	private List<String> bundledSounds = new ArrayList<>();
	private final Set<Integer> expandedWeapons = new HashSet<>();
	private final Set<String> expandedDefaults = new HashSet<>();

	private final ConfigManager configManager;
	private final ItemManager itemManager;
	private final Runnable onOpenSearch;
	private final Consumer<Integer> onRemoveWeapon;
	private final Runnable onRefreshSounds;
	private final BiConsumer<String, Integer> onTestSound;
	private final Runnable onReset;

	private final JPanel weaponListPanel;

	public CustomWeaponSfxPanel(ConfigManager configManager,
		ItemManager itemManager,
		Runnable onOpenSearch,
		Consumer<Integer> onRemoveWeapon,
		Runnable onRefreshSounds,
		BiConsumer<String, Integer> onTestSound,
		Runnable onReset)
	{
		this.configManager = configManager;
		this.itemManager = itemManager;
		this.onOpenSearch = onOpenSearch;
		this.onRemoveWeapon = onRemoveWeapon;
		this.onRefreshSounds = onRefreshSounds;
		this.onTestSound = onTestSound;
		this.onReset = onReset;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		weaponListPanel = new JPanel();
		weaponListPanel.setLayout(new javax.swing.BoxLayout(weaponListPanel, javax.swing.BoxLayout.Y_AXIS));

		JPanel content = new JPanel();
		content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
		content.add(buildTopPanel());
		content.add(weaponListPanel);

		add(content, BorderLayout.NORTH);
	}

	private JPanel buildTopPanel()
	{
		JPanel top = new JPanel();
		top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Custom Weapon SFX");
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(title);
		top.add(Box.createVerticalStrut(4));

		JLabel customSoundDirections = new JLabel("<html>Want a custom sfx?<br>" +
				"1. Place <b>.wav</b> files in <b>.runelite/customweaponsfx/</b><br>" +
				"2. Click Refesh Sounds<br>" +
				"3. Click Add Weapon and configure it</html>");
		customSoundDirections.setFont(customSoundDirections.getFont().deriveFont(14f));
		customSoundDirections.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(customSoundDirections);
		top.add(Box.createVerticalStrut(8));

		JPanel btnRow = new JPanel();
		btnRow.setLayout(new javax.swing.BoxLayout(btnRow, javax.swing.BoxLayout.X_AXIS));
		btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton addWeaponBtn = new JButton("Add Weapon");
		addWeaponBtn.setToolTipText("Search for a weapon to configure");
		addWeaponBtn.addActionListener(e -> onOpenSearch.run());
		btnRow.add(addWeaponBtn);

		btnRow.add(Box.createHorizontalStrut(4));

		JButton refreshBtn = new JButton("Refresh Sounds");
		refreshBtn.addActionListener(e -> onRefreshSounds.run());
		btnRow.add(refreshBtn);

		top.add(btnRow);
		top.add(Box.createVerticalStrut(4));

		JPanel resetRow = new JPanel();
		resetRow.setLayout(new javax.swing.BoxLayout(resetRow, javax.swing.BoxLayout.X_AXIS));
		resetRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton resetBtn = new JButton("Reset All Data");
		resetBtn.setForeground(Color.RED);
		resetBtn.setToolTipText("Wipe all saved weapon entries and sound groups, then restore defaults");
		resetBtn.addActionListener(e ->
		{
			int confirm = JOptionPane.showConfirmDialog(
				this,
				"Reset all weapons and sound groups back to defaults?",
				"Reset All Data",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
			);
			if (confirm == JOptionPane.YES_OPTION) onReset.run();
		});
		resetRow.add(resetBtn);

		top.add(resetRow);
		top.add(Box.createVerticalStrut(10));

		return top;
	}

	public void rebuild(List<WeaponEntry> weapons, List<String> availableSounds,
		List<String> bundledSounds, List<TriggerGroup> unarmedGroups,
		List<TriggerGroup> receivedGroups, List<TriggerGroup> thrallGroups)
	{
		this.bundledSounds = bundledSounds;
		SwingUtilities.invokeLater(() ->
		{
			weaponListPanel.removeAll();

			weaponListPanel.add(buildDefaultRowGroups(
				"Player Attacks (Unarmed)", UNARMED_GROUPS_PREFIX, unarmedGroups, availableSounds,
				EnumSet.of(Triggers.REGULAR_ZERO, Triggers.REGULAR_HIT, Triggers.REGULAR_MAX, Triggers.ALL)));
			weaponListPanel.add(Box.createVerticalStrut(4));

			weaponListPanel.add(buildDefaultRowGroups(
				"Received Attacks", RECEIVED_GROUPS_PREFIX, receivedGroups, availableSounds,
				EnumSet.of(Triggers.REGULAR_ZERO, Triggers.REGULAR_HIT, Triggers.ALL)));
			weaponListPanel.add(Box.createVerticalStrut(4));

			weaponListPanel.add(buildDefaultRowGroups(
				"Thrall Attacks", THRALL_GROUPS_PREFIX, thrallGroups, availableSounds,
				EnumSet.of(Triggers.THRALL_HIT)));
			weaponListPanel.add(Box.createVerticalStrut(8));

			for (int i = 0; i < weapons.size(); i++)
			{
				weaponListPanel.add(buildRow(weapons.get(i), availableSounds, i));
				weaponListPanel.add(Box.createVerticalStrut(8));
			}

			weaponListPanel.revalidate();
			weaponListPanel.repaint();
		});
	}

	private JPanel buildDefaultRowGroups(String label, String prefix,
		List<TriggerGroup> groups, List<String> availableSounds, Set<Triggers> visibleTriggers)
	{
		boolean collapsed = !expandedDefaults.contains(prefix);

		JPanel panel = new JPanel();
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE),
			new EmptyBorder(6, 6, 6, 6)
		));

		JPanel headerRow = new JPanel(new BorderLayout(6, 0));
		headerRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton collapseBtn = new JButton(collapsed ? "▶" : "▼");
		collapseBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
		collapseBtn.setToolTipText(collapsed ? "Expand" : "Minimize");
		headerRow.add(collapseBtn, BorderLayout.WEST);

		JLabel nameLabel = new JLabel(label);
		nameLabel.setForeground(ColorScheme.BRAND_ORANGE);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
		headerRow.add(nameLabel, BorderLayout.CENTER);

		panel.add(headerRow);

		Component strut = Box.createVerticalStrut(6);
		strut.setVisible(!collapsed);
		panel.add(strut);

		JPanel groupsHolder = new JPanel();
		groupsHolder.setLayout(new javax.swing.BoxLayout(groupsHolder, javax.swing.BoxLayout.Y_AXIS));
		groupsHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		groupsHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
		groupsHolder.setVisible(!collapsed);
		rebuildGroupsSection(groupsHolder, groups, availableSounds,
			() -> saveDefaultGroupsFromPanel(prefix, groups), visibleTriggers);
		panel.add(groupsHolder);

		collapseBtn.addActionListener(e ->
		{
			boolean nowCollapsed = expandedDefaults.contains(prefix);
			if (nowCollapsed)
				expandedDefaults.remove(prefix);
			else
				expandedDefaults.add(prefix);
			collapseBtn.setText(nowCollapsed ? "▶" : "▼");
			collapseBtn.setToolTipText(nowCollapsed ? "Expand" : "Minimize");
			strut.setVisible(!nowCollapsed);
			groupsHolder.setVisible(!nowCollapsed);
			panel.revalidate();
			panel.repaint();
		});

		return panel;
	}

	private static final Color ROW_COLOR_A = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color ROW_COLOR_B = new Color(40, 38, 35);

	private JPanel buildRow(WeaponEntry entry, List<String> availableSounds, int index)
	{
		Color bg = (index % 2 == 0) ? ROW_COLOR_A : ROW_COLOR_B;
		boolean collapsed = !expandedWeapons.contains(entry.getItemId());

		JPanel panel = new JPanel();
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		panel.setBackground(bg);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(6, 6, 6, 6)
		));

		JPanel headerRow = new JPanel(new BorderLayout(6, 0));
		headerRow.setBackground(bg);
		headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

		JButton collapseBtn = new JButton(collapsed ? "▶" : "▼");
		collapseBtn.setMargin(new java.awt.Insets(2, 4, 2, 4));
		collapseBtn.setToolTipText(collapsed ? "Expand" : "Minimize");

		JPanel westBlock = new JPanel(new BorderLayout(4, 0));
		westBlock.setBackground(bg);
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(32, 32));
		AsyncBufferedImage icon = itemManager.getImage(entry.getItemId());
		if (icon != null)
		{
			icon.addTo(iconLabel);
		}
		westBlock.add(collapseBtn, BorderLayout.WEST);
		westBlock.add(iconLabel, BorderLayout.EAST);
		headerRow.add(westBlock, BorderLayout.WEST);

		JPanel nameBlock = new JPanel();
		nameBlock.setLayout(new javax.swing.BoxLayout(nameBlock, javax.swing.BoxLayout.Y_AXIS));
		nameBlock.setBackground(bg);

		JLabel nameLabel = new JLabel(entry.getWeaponName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
		nameBlock.add(Box.createVerticalGlue());
		nameBlock.add(nameLabel);
		nameBlock.add(Box.createVerticalGlue());

		headerRow.add(nameBlock, BorderLayout.CENTER);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
		removeBtn.setForeground(Color.RED);
		removeBtn.addActionListener(e ->
		{
			int confirm = JOptionPane.showConfirmDialog(
				this,
				"Remove " + entry.getWeaponName() + " and all its sound groups?",
				"Remove Weapon",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
			);
			if (confirm == JOptionPane.YES_OPTION) onRemoveWeapon.accept(entry.getItemId());
		});
		headerRow.add(removeBtn, BorderLayout.EAST);

		panel.add(headerRow);

		Component strut = Box.createVerticalStrut(6);
		strut.setVisible(!collapsed);
		panel.add(strut);

		JPanel groupsHolder = new JPanel();
		groupsHolder.setLayout(new javax.swing.BoxLayout(groupsHolder, javax.swing.BoxLayout.Y_AXIS));
		groupsHolder.setBackground(bg);
		groupsHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
		groupsHolder.setVisible(!collapsed);
		rebuildGroupsSection(groupsHolder, entry.getGroups(), availableSounds,
			() -> saveWeaponGroupsFromPanel(entry),
			EnumSet.complementOf(EnumSet.of(Triggers.THRALL_HIT)));
		panel.add(groupsHolder);

		collapseBtn.addActionListener(e ->
		{
			boolean nowCollapsed = expandedWeapons.contains(entry.getItemId());
			if (nowCollapsed)
				expandedWeapons.remove(entry.getItemId());
			else
				expandedWeapons.add(entry.getItemId());
			collapseBtn.setText(nowCollapsed ? "▶" : "▼");
			collapseBtn.setToolTipText(nowCollapsed ? "Expand" : "Minimize");
			strut.setVisible(!nowCollapsed);
			groupsHolder.setVisible(!nowCollapsed);
			panel.revalidate();
			panel.repaint();
		});

		return panel;
	}

	private void rebuildGroupsSection(JPanel holder, List<TriggerGroup> groups,
		List<String> availableSounds, Runnable onSave, Set<Triggers> visibleTriggers)
	{
		holder.removeAll();

		for (int i = 0; i < groups.size(); i++)
		{
			final int idx = i;
			TriggerGroup group = groups.get(i);

			JPanel groupPanel = new JPanel();
			groupPanel.setLayout(new javax.swing.BoxLayout(groupPanel, javax.swing.BoxLayout.Y_AXIS));
			groupPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
			groupPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
				new EmptyBorder(4, 4, 4, 4)
			));
			groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

			JPanel groupHeader = new JPanel(new BorderLayout());
			groupHeader.setBackground(ColorScheme.DARK_GRAY_COLOR);
			groupHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

			JLabel groupLabel = new JLabel("Sound " + (i + 1));
			groupLabel.setForeground(ColorScheme.BRAND_ORANGE);
			groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD, 11f));
			groupHeader.add(groupLabel, BorderLayout.WEST);

			if (groups.size() > 1)
			{
				JButton removeGroupBtn = new JButton("Remove");
				removeGroupBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
				removeGroupBtn.setForeground(Color.RED);
				removeGroupBtn.addActionListener(e ->
				{
					int confirm = JOptionPane.showConfirmDialog(
						this,
						"Remove Sound " + (idx + 1) + "?",
						"Remove Sound Group",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE
					);
					if (confirm != JOptionPane.YES_OPTION) return;
					groups.remove(idx);
					onSave.run();
					rebuildGroupsSection(holder, groups, availableSounds, onSave, visibleTriggers);
				});
				groupHeader.add(removeGroupBtn, BorderLayout.EAST);
			}

			groupPanel.add(groupHeader);
			groupPanel.add(Box.createVerticalStrut(4));
			groupPanel.add(buildGroupSoundRow(group, availableSounds, onSave));
			groupPanel.add(Box.createVerticalStrut(4));
			groupPanel.add(buildVolumeRowGroup(group, onSave));
			groupPanel.add(Box.createVerticalStrut(4));
			groupPanel.add(buildTriggersPanel(group.getTriggers(), onSave, visibleTriggers));

			holder.add(groupPanel);
			if (i < groups.size() - 1)
				holder.add(Box.createVerticalStrut(4));
		}

		holder.add(Box.createVerticalStrut(4));
		JButton addGroupBtn = new JButton("+ Add Sound Group");
		addGroupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		addGroupBtn.addActionListener(e ->
		{
			groups.add(new TriggerGroup(EnumSet.noneOf(Triggers.class), "", 75));
			onSave.run();
			rebuildGroupsSection(holder, groups, availableSounds, onSave, visibleTriggers);
		});
		holder.add(addGroupBtn);

		holder.revalidate();
		holder.repaint();
	}

	private JPanel buildTriggersPanel(Set<Triggers> enabledTriggers, Runnable onSave, Set<Triggers> visibleTriggers)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel lbl = new JLabel("Triggers:");
		lbl.setForeground(Color.WHITE);
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(lbl);
		panel.add(Box.createVerticalStrut(2));

		for (Triggers trigger : Triggers.values())
		{
			if (!visibleTriggers.contains(trigger)) continue;
			JCheckBox box = new JCheckBox(trigger.getName());
			box.setForeground(Color.LIGHT_GRAY);
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setSelected(enabledTriggers.contains(trigger));
			box.setAlignmentX(Component.LEFT_ALIGNMENT);
			box.addActionListener(e ->
			{
				if (box.isSelected()) enabledTriggers.add(trigger);
				else enabledTriggers.remove(trigger);
				onSave.run();
			});
			panel.add(box);
		}

		return panel;
	}

	private JPanel buildGroupSoundRow(TriggerGroup group, List<String> availableSounds, Runnable onSave)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel lbl = new JLabel("Sound:");
		lbl.setForeground(Color.WHITE);
		row.add(lbl);

		String[] options = buildSoundOptions(availableSounds);
		JComboBox<String> box = new JComboBox<>(options);
		box.setPreferredSize(new Dimension(85, box.getPreferredSize().height));
		box.setSelectedItem(configToDisplay(group.getSoundFile()));

		box.addActionListener(e ->
		{
			String sel = (String) box.getSelectedItem();
			group.setSoundFile(displayToConfig(sel));
			onSave.run();
		});
		row.add(box);

		JButton testBtn = new JButton("▶");
		testBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
		testBtn.setToolTipText("Test sound");
		testBtn.addActionListener(e -> onTestSound.accept(displayToConfig((String) box.getSelectedItem()), group.getVolume()));
		row.add(testBtn);
		return row;
	}

	private JPanel buildVolumeRowGroup(TriggerGroup group, Runnable onSave)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel lbl = new JLabel("Volume:");
		lbl.setForeground(Color.WHITE);
		row.add(lbl);
		JSlider slider = new JSlider(0, 100, group.getVolume());
		slider.setPreferredSize(new Dimension(100, 20));
		JLabel val = new JLabel(group.getVolume() + "%");
		val.setForeground(Color.LIGHT_GRAY);
		slider.addChangeListener(e ->
		{
			int v = slider.getValue();
			val.setText(v + "%");
			if (!slider.getValueIsAdjusting())
			{
				group.setVolume(v);
				onSave.run();
			}
		});
		row.add(slider);
		row.add(val);
		return row;
	}

	private void saveWeaponGroupsFromPanel(WeaponEntry entry)
	{
		int itemId = entry.getItemId();
		List<TriggerGroup> groups = entry.getGroups();
		configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
			"specWeapon_" + itemId + "_groupCount", groups.size());
		for (int i = 0; i < groups.size(); i++)
		{
			TriggerGroup g = groups.get(i);
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				"specWeapon_" + itemId + "_group_" + i + "_triggers",
				TriggerGroup.serializeTriggers(g.getTriggers()));
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				"specWeapon_" + itemId + "_group_" + i + "_sound", g.getSoundFile());
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				"specWeapon_" + itemId + "_group_" + i + "_volume", g.getVolume());
		}
	}

	private void saveDefaultGroupsFromPanel(String prefix, List<TriggerGroup> groups)
	{
		configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
			prefix + "_groupCount", groups.size());
		for (int i = 0; i < groups.size(); i++)
		{
			TriggerGroup g = groups.get(i);
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				prefix + "_group_" + i + "_triggers",
				TriggerGroup.serializeTriggers(g.getTriggers()));
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				prefix + "_group_" + i + "_sound", g.getSoundFile());
			configManager.setConfiguration(CustomWeaponSfxPlugin.CONFIG_GROUP,
				prefix + "_group_" + i + "_volume", g.getVolume());
		}
	}

	private String[] buildSoundOptions(List<String> userSounds)
	{
		String[] options = new String[bundledSounds.size() + userSounds.size()];
		for (int i = 0; i < bundledSounds.size(); i++)
			options[i] = bundledSounds.get(i) + BUILTIN_SUFFIX;
		for (int i = 0; i < userSounds.size(); i++)
			options[bundledSounds.size() + i] = userSounds.get(i);
		return options;
	}

	private String configToDisplay(String configValue)
	{
		if (configValue == null || configValue.isEmpty())
			return "squeak" + BUILTIN_SUFFIX;
		if (configValue.startsWith(BUNDLED_PREFIX))
			return configValue.substring(BUNDLED_PREFIX.length()) + BUILTIN_SUFFIX;
		return configValue;
	}

	private static String displayToConfig(String display)
	{
		if (display == null) return BUNDLED_PREFIX + "squeak";
		if (display.endsWith(BUILTIN_SUFFIX))
			return BUNDLED_PREFIX + display.substring(0, display.length() - BUILTIN_SUFFIX.length());
		return display;
	}
}
