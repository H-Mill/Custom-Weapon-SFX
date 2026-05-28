package com.customweaponsfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
		name = "Custom Weapon SFX",
		description = "Plays a custom sound effect on configurable player actions",
		tags = {"custom", "weapon", "sound", "sfx"},
		configName = "SqueakyToyPlugin"
)
public class CustomWeaponSfxPlugin extends Plugin
{
	static final String CONFIG_GROUP = "customweaponsfx";
	static final File SOUNDS_DIR = new File(RuneLite.RUNELITE_DIR, "customweaponsfx");

	private static final int VARP_SPEC_PERCENT = 300;
	private static final int PENDING_SPEC_TIMEOUT_TICKS = 10;
	private static final int RECEIVED_KEY = -2;

	@Inject private Client client;
	@Inject private AudioPlayer audioPlayer;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private ItemManager itemManager;
	@Inject private WeaponChatboxSearch weaponSearch;

	private ExecutorService executor;
	private CustomWeaponSfxPanel panel;
	private NavigationButton navButton;

	private final List<WeaponEntry> weaponEntries = new CopyOnWriteArrayList<>();
	private final List<TriggerGroup> receivedGroups = new CopyOnWriteArrayList<>();
	private List<String> bundledSounds = new ArrayList<>();
	private List<String> availableSounds = new ArrayList<>();

	private int lastSpecPct = -1;
	private int pendingSpecItemId = -1;
	private int pendingSpecTick = -1;

	// Hitsplats are batched per tick so multi-hit attacks are evaluated together.
	private final Map<Integer, PendingAttack> pendingAttacks = new HashMap<>();
	private int pendingAttackTick = -1;

	private final List<DeferredHit> deferredHits = new ArrayList<>();

	@Override
	protected void startUp()
	{
		SOUNDS_DIR.mkdirs();
		executor = Executors.newSingleThreadExecutor();

		loadWeaponEntries();
		loadDefaultGroups(receivedGroups, CustomWeaponSfxPanel.RECEIVED_GROUPS_PREFIX);
		seedFirstRunGroups(receivedGroups, CustomWeaponSfxPanel.RECEIVED_GROUPS_PREFIX, EnumSet.of(Triggers.REGULAR_ZERO));
		bundledSounds = scanBundledSounds();
		availableSounds = scanSounds();

		panel = new CustomWeaponSfxPanel(configManager, itemManager, this::openWeaponSearch, this::addEquippedWeapon, this::removeWeapon, this::refreshSounds, this::playSoundFile, this::resetAllData);

		navButton = NavigationButton.builder()
			.tooltip("Custom Weapon SFX")
			.icon(loadIcon())
			.panel(panel)
			.priority(5)
			.build();
		clientToolbar.addNavigation(navButton);

		panel.rebuild(new ArrayList<>(weaponEntries), availableSounds, bundledSounds, receivedGroups);

		clientThread.invoke(() -> lastSpecPct = client.getVarpValue(VARP_SPEC_PERCENT));

		log.debug("Custom Weapon SFX started!");
	}

	@Override
	protected void shutDown()
	{
		executor.shutdownNow();
		executor = null;

		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;

		receivedGroups.clear();
		bundledSounds.clear();

		lastSpecPct = -1;
		pendingSpecItemId = -1;
		pendingSpecTick = -1;

		pendingAttacks.clear();
		pendingAttackTick = -1;

		deferredHits.clear();

		log.debug("Custom Weapon SFX stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int nowSpec = client.getVarpValue(VARP_SPEC_PERCENT);

		if (lastSpecPct >= 0 && nowSpec < lastSpecPct)
		{
			onSpecFired();
		}
		lastSpecPct = nowSpec;

		if (pendingSpecItemId >= 0
			&& client.getTickCount() - pendingSpecTick > PENDING_SPEC_TIMEOUT_TICKS)
		{
			pendingSpecItemId = -1;
			pendingSpecTick = -1;
		}

		if (!deferredHits.isEmpty())
		{
			for (DeferredHit h : deferredHits)
				buffer(h.key, h.groups, h.wasSpec, h.amount, h.isMax, h.tick);
			deferredHits.clear();
		}

		if (!pendingAttacks.isEmpty())
		{
			boolean specHitsplatSeen = false;
			for (PendingAttack attack : pendingAttacks.values())
			{
				boolean allMax  = attack.isMaxList.stream().allMatch(b -> b);
				boolean anyHit  = attack.amounts.stream().anyMatch(a -> a > 0);
				boolean allZero = attack.amounts.stream().allMatch(a -> a == 0);
				fireMatchingGroups(attack.groups, attack.wasSpec, anyHit, allZero, allMax);
				if (attack.wasSpec) specHitsplatSeen = true;
			}
			pendingAttacks.clear();
			pendingAttackTick = -1;
			// Clear the spec window as soon as the spec's hitsplat has been processed
			// so the next normal attack isn't silently swallowed by the wasSpec flag.
			if (specHitsplatSeen)
			{
				pendingSpecItemId = -1;
				pendingSpecTick   = -1;
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		int amount  = event.getHitsplat().getAmount();
		int tick    = client.getTickCount();

		if (actor == client.getLocalPlayer())
		{
			buffer(RECEIVED_KEY, receivedGroups, false, amount, false, tick);
			return;
		}

		if (!event.getHitsplat().isMine()) return;

		boolean isMax   = isMaxHit(event.getHitsplat().getHitsplatType());
		boolean wasSpec = pendingSpecItemId >= 0;

		int weaponId = getEquippedWeaponId();
		if (weaponId < 0) return;

		WeaponEntry entry = getWeaponEntry(weaponId);
		if (entry != null)
			deferredHits.add(new DeferredHit(weaponId, entry.getGroups(), wasSpec, amount, isMax, tick));
	}

	private void resetAllData()
	{
		for (WeaponEntry entry : weaponEntries)
		{
			int itemId = entry.getItemId();
			for (int i = 0; i < entry.getGroups().size(); i++)
			{
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_triggers");
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_sound");
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_volume");
			}
			configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_name");
			configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_groupCount");
		}
		configManager.unsetConfiguration(CONFIG_GROUP, "specWeaponIds");
		weaponEntries.clear();

		clearDefaultGroupConfig(CustomWeaponSfxPanel.RECEIVED_GROUPS_PREFIX, receivedGroups.size());
		receivedGroups.clear();

		addDefaultGroup(receivedGroups, CustomWeaponSfxPanel.RECEIVED_GROUPS_PREFIX, EnumSet.of(Triggers.REGULAR_ZERO));

		rebuildPanel();
	}

	private void clearDefaultGroupConfig(String prefix, int count)
	{
		configManager.unsetConfiguration(CONFIG_GROUP, prefix + "_groupCount");
		for (int i = 0; i < count; i++)
		{
			configManager.unsetConfiguration(CONFIG_GROUP, prefix + "_group_" + i + "_triggers");
			configManager.unsetConfiguration(CONFIG_GROUP, prefix + "_group_" + i + "_sound");
			configManager.unsetConfiguration(CONFIG_GROUP, prefix + "_group_" + i + "_volume");
		}
	}

	private void buffer(int key, List<TriggerGroup> groups,
						boolean wasSpec, int amount, boolean isMax, int tick)
	{
		if (tick != pendingAttackTick)
		{
			pendingAttacks.clear();
			pendingAttackTick = tick;
		}

		PendingAttack attack = pendingAttacks.get(key);
		if (attack == null)
		{
			attack = new PendingAttack(groups);
			attack.wasSpec = wasSpec;
			pendingAttacks.put(key, attack);
		}
		attack.amounts.add(amount);
		attack.isMaxList.add(isMax);
	}

	private void onSpecFired()
	{
		int weaponId = getEquippedWeaponId();
		if (weaponId < 0) return;

		WeaponEntry entry = getWeaponEntry(weaponId);
		if (entry == null || entry.getGroups().isEmpty()) return;

		pendingSpecItemId = weaponId;
		pendingSpecTick = client.getTickCount();
	}

	private int getEquippedWeaponId()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) return -1;
		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		return weapon == null ? -1 : weapon.getId();
	}

	private void openWeaponSearch()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				SwingUtilities.invokeLater(() ->
					JOptionPane.showMessageDialog(
						null,
						"You must be logged in to search for weapons.",
						"Not Logged In",
						JOptionPane.WARNING_MESSAGE
					)
				);
				return;
			}
			weaponSearch
				.onItemSelected(itemId ->
				{
					String name = client.getItemDefinition(itemId).getName();
					addWeapon(itemId, name);
				})
				.build();
		});
		client.getCanvas().requestFocusInWindow();
	}

	private void addEquippedWeapon()
	{
		clientThread.invoke(() ->
		{
			int weaponId = getEquippedWeaponId();
			if (weaponId < 0) return;
			String name = client.getItemDefinition(weaponId).getName();
			addWeapon(weaponId, name);
		});
	}

	private void addWeapon(int itemId, String name)
	{
		if (getWeaponEntry(itemId) != null) return;

		List<TriggerGroup> groups = new ArrayList<>();
		groups.add(new TriggerGroup(EnumSet.noneOf(Triggers.class), "", 75));
		weaponEntries.add(new WeaponEntry(itemId, name, groups));

		configManager.setConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_name", name);
		saveWeaponGroups(itemId, groups);
		saveWeaponIds();

		rebuildPanel();
	}

	private void removeWeapon(int itemId)
	{
		WeaponEntry entry = getWeaponEntry(itemId);
		if (entry != null)
		{
			for (int i = 0; i < entry.getGroups().size(); i++)
			{
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_triggers");
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_sound");
				configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_volume");
			}
		}
		weaponEntries.removeIf(e -> e.getItemId() == itemId);
		configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_name");
		configManager.unsetConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_groupCount");
		saveWeaponIds();

		rebuildPanel();
	}

	private void refreshSounds()
	{
		availableSounds = scanSounds();
		rebuildPanel();
	}

	private void rebuildPanel()
	{
		CustomWeaponSfxPanel p = panel;
		if (p == null) return;
		List<WeaponEntry> snapshot = new ArrayList<>(weaponEntries);
		List<String> sounds = availableSounds;
		List<String> bundled = bundledSounds;
		SwingUtilities.invokeLater(() -> p.rebuild(snapshot, sounds, bundled, receivedGroups));
	}

	private WeaponEntry getWeaponEntry(int itemId)
	{
		for (WeaponEntry entry : weaponEntries)
		{
			if (entry.getItemId() == itemId) return entry;
		}
		return null;
	}

	private List<String> scanSounds()
	{
		List<String> sounds = new ArrayList<>();
		File[] files = SOUNDS_DIR.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".wav"));
		if (files != null)
		{
			for (File f : files)
			{
				String name = f.getName();
				sounds.add(name.substring(0, name.length() - 4));
			}
			sounds.sort(String.CASE_INSENSITIVE_ORDER);
		}
		return sounds;
	}

	private void loadWeaponEntries()
	{
		weaponEntries.clear();
		String idsStr = configManager.getConfiguration(CONFIG_GROUP, "specWeaponIds");
		if (idsStr == null || idsStr.isBlank()) return;

		for (String idStr : idsStr.split(","))
		{
			try
			{
				int itemId = Integer.parseInt(idStr.trim());
				String name = configManager.getConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_name");
				String countStr = configManager.getConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_groupCount");
				if (countStr == null || countStr.isBlank()) continue;

				List<TriggerGroup> groups = new ArrayList<>();
				try
				{
					int count = Integer.parseInt(countStr.trim());
					for (int i = 0; i < count; i++)
					{
						groups.add(loadGroup("specWeapon_" + itemId + "_group_" + i));
					}
				}
				catch (NumberFormatException e)
				{
					log.debug("Skipping invalid groupCount for weapon {}", itemId);
					continue;
				}

				weaponEntries.add(new WeaponEntry(
					itemId,
					name != null ? name : "Weapon #" + itemId,
					groups
				));
			}
			catch (NumberFormatException e)
			{
				log.debug("Skipping invalid weapon ID in config: {}", idStr);
			}
		}
	}

	private void loadDefaultGroups(List<TriggerGroup> list, String prefix)
	{
		list.clear();
		String countStr = configManager.getConfiguration(CONFIG_GROUP, prefix + "_groupCount");
		if (countStr == null || countStr.isBlank()) return;

		try
		{
			int count = Integer.parseInt(countStr.trim());
			for (int i = 0; i < count; i++)
			{
				list.add(loadGroup(prefix + "_group_" + i));
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Skipping invalid groupCount for {}", prefix);
		}
	}

	private void seedFirstRunGroups(List<TriggerGroup> list, String prefix, Set<Triggers> defaultTriggers)
	{
		if (!list.isEmpty()) return;
		if (configManager.getConfiguration(CONFIG_GROUP, prefix + "_groupCount") != null) return;
		addDefaultGroup(list, prefix, defaultTriggers);
	}

	private void addDefaultGroup(List<TriggerGroup> list, String prefix, Set<Triggers> defaultTriggers)
	{
		TriggerGroup group = new TriggerGroup(defaultTriggers, "", 75);
		list.add(group);
		configManager.setConfiguration(CONFIG_GROUP, prefix + "_groupCount", 1);
		configManager.setConfiguration(CONFIG_GROUP, prefix + "_group_0_triggers",
			TriggerGroup.serializeTriggers(group.getTriggers()));
		configManager.setConfiguration(CONFIG_GROUP, prefix + "_group_0_sound", group.getSoundFile());
		configManager.setConfiguration(CONFIG_GROUP, prefix + "_group_0_volume", group.getVolume());
	}

	private TriggerGroup loadGroup(String keyPrefix)
	{
		String triggersStr = configManager.getConfiguration(CONFIG_GROUP, keyPrefix + "_triggers");
		String sound = configManager.getConfiguration(CONFIG_GROUP, keyPrefix + "_sound");
		String volStr = configManager.getConfiguration(CONFIG_GROUP, keyPrefix + "_volume");

		Set<Triggers> triggers = TriggerGroup.deserializeTriggers(triggersStr);

		int vol = 75;
		if (volStr != null)
		{
			try { vol = Integer.parseInt(volStr); }
			catch (NumberFormatException ignored) {}
		}

		return new TriggerGroup(triggers, sound != null ? sound : "", vol);
	}

	private void saveWeaponIds()
	{
		String ids = weaponEntries.stream()
			.map(e -> String.valueOf(e.getItemId()))
			.collect(Collectors.joining(","));
		configManager.setConfiguration(CONFIG_GROUP, "specWeaponIds", ids);
	}

	private void fireMatchingGroups(List<TriggerGroup> groups, boolean wasSpec,
									boolean anyHit, boolean allZero, boolean allMax)
	{
		for (TriggerGroup group : groups)
		{
			Set<Triggers> triggers = group.getTriggers();
			if (triggers.isEmpty()) continue;

			boolean matches = false;
			for (Triggers trigger : triggers)
			{
				if (matchesTrigger(trigger, wasSpec, anyHit, allZero, allMax))
				{
					matches = true;
					break;
				}
			}
			if (!matches) continue;
			playSoundFile(group.getSoundFile(), group.getVolume());
		}
	}

	private static boolean matchesTrigger(Triggers trigger, boolean wasSpec,
										   boolean anyHit, boolean allZero, boolean allMax)
	{
		switch (trigger)
		{
			case REGULAR_ZERO: return !wasSpec && allZero;
			case REGULAR_HIT:  return !wasSpec && anyHit && !allMax;
			case REGULAR_MAX:  return !wasSpec && allMax;
			case SPECIAL_ZERO: return wasSpec && allZero;
			case SPECIAL_HIT:  return wasSpec && anyHit && !allMax;
			case SPECIAL_MAX:  return wasSpec && allMax;
			case ALL:          return anyHit;
			default:           return false;
		}
	}

	private static boolean isMaxHit(int hitsplatType)
	{
		return hitsplatType == HitsplatID.DAMAGE_MAX_ME
			|| hitsplatType == HitsplatID.DAMAGE_MAX_ME_CYAN
			|| hitsplatType == HitsplatID.DAMAGE_MAX_ME_ORANGE
			|| hitsplatType == HitsplatID.DAMAGE_MAX_ME_YELLOW
			|| hitsplatType == HitsplatID.DAMAGE_MAX_ME_WHITE
			|| hitsplatType == HitsplatID.DAMAGE_MAX_ME_POISE;
	}

	private void saveWeaponGroups(int itemId, List<TriggerGroup> groups)
	{
		configManager.setConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_groupCount", groups.size());
		for (int i = 0; i < groups.size(); i++)
		{
			TriggerGroup g = groups.get(i);
			configManager.setConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_triggers",
				TriggerGroup.serializeTriggers(g.getTriggers()));
			configManager.setConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_sound", g.getSoundFile());
			configManager.setConfiguration(CONFIG_GROUP, "specWeapon_" + itemId + "_group_" + i + "_volume", g.getVolume());
		}
	}

	private void playSoundFile(String soundFile, int volume)
	{
		if (executor == null || executor.isShutdown()) return;
		if (volume == 0) return;
		float gain = volumeToGain(volume);

		if (soundFile == null || soundFile.isEmpty() || soundFile.startsWith(CustomWeaponSfxPanel.BUNDLED_PREFIX))
		{
			String name = (soundFile == null || soundFile.isEmpty())
				? "squeak.wav"
				: soundFile.substring(CustomWeaponSfxPanel.BUNDLED_PREFIX.length()) + ".wav";
			executor.submit(() ->
			{
				try { audioPlayer.play(CustomWeaponSfxPlugin.class, name, gain); }
				catch (Exception e) { log.debug("Failed to play bundled sound {}", name, e); }
			});
		}
		else
		{
			File f = new File(SOUNDS_DIR, soundFile + ".wav");
			if (!f.exists())
			{
				log.debug("Sound file missing: {}", f.getAbsolutePath());
				return;
			}
			executor.submit(() ->
			{
				try { audioPlayer.play(f, gain); }
				catch (Exception e) { log.debug("Failed to play {}", soundFile, e); }
			});
		}
	}

	private List<String> scanBundledSounds()
	{
		List<String> sounds = new ArrayList<>();
		URL anchor = CustomWeaponSfxPlugin.class.getResource("squeak.wav");
		if (anchor == null) return sounds;
		try
		{
			if ("file".equals(anchor.getProtocol()))
			{
				File dir = new File(anchor.toURI()).getParentFile();
				File[] files = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".wav"));
				if (files != null)
				{
					for (File f : files)
						sounds.add(f.getName().substring(0, f.getName().length() - 4));
				}
			}
			else if ("jar".equals(anchor.getProtocol()))
			{
				JarURLConnection conn = (JarURLConnection) anchor.openConnection();
				String entryName = conn.getEntryName();
				String prefix = entryName.substring(0, entryName.lastIndexOf('/') + 1);
				try (JarFile jar = conn.getJarFile())
				{
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements())
					{
						JarEntry entry = entries.nextElement();
						String name = entry.getName();
						if (name.startsWith(prefix) && name.endsWith(".wav") && !entry.isDirectory()
							&& !name.substring(prefix.length()).contains("/"))
						{
							sounds.add(name.substring(prefix.length(), name.length() - 4));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Failed to scan bundled sounds", e);
		}
		sounds.sort(String.CASE_INSENSITIVE_ORDER);
		return sounds;
	}

	private static float volumeToGain(int volume)
	{
		if (volume <= 0) return -80f;
		return 20.0f * (float) Math.log10(volume / 100.0f);
	}


	private static class DeferredHit
	{
		final int              key;
		final List<TriggerGroup> groups;
		final boolean          wasSpec;
		final int              amount;
		final boolean          isMax;
		final int              tick;

		DeferredHit(int key, List<TriggerGroup> groups, boolean wasSpec, int amount, boolean isMax, int tick)
		{
			this.key     = key;
			this.groups  = groups;
			this.wasSpec = wasSpec;
			this.amount  = amount;
			this.isMax   = isMax;
			this.tick    = tick;
		}
	}

	private static class PendingAttack
	{
		final List<TriggerGroup> groups;
		final List<Integer>      amounts   = new ArrayList<>();
		final List<Boolean>      isMaxList = new ArrayList<>();
		boolean                  wasSpec   = false;

		PendingAttack(List<TriggerGroup> groups) { this.groups = groups; }
	}

	private static BufferedImage loadIcon()
	{
		try
		{
			return ImageUtil.loadImageResource(CustomWeaponSfxPlugin.class, "icon.png");
		}
		catch (Exception ignored) {}

		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(255, 153, 0));
		g.fillOval(0, 0, 15, 15);
		g.dispose();
		return img;
	}

}
