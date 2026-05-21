package com.squeakytoy;

import com.google.inject.Provides;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Squeaky Toy",
	description = "Plays a squeaky toy sound effect on configurable player actions",
	tags = {"squeaky", "toy", "sound", "sfx"}
)
public class SqueakyToyPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SqueakyToyConfig config;

	@Inject
	private AudioPlayer audioPlayer;

	private ExecutorService executor;

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadExecutor();
		log.debug("Squeaky Toy started!");
	}

	@Override
	protected void shutDown()
	{
		executor.shutdownNow();
		executor = null;
		log.debug("Squeaky Toy stopped!");
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		int amount = event.getHitsplat().getAmount();

		// Hitsplat received by the local player
		if (actor == client.getLocalPlayer())
		{
			AttackSqueakMode receivedMode = config.receivedAttackSqueakMode();
			if (receivedMode != AttackSqueakMode.DISABLED
				&& (receivedMode == AttackSqueakMode.ALL_ATTACKS || amount == 0))
			{
				playSqueak();
			}
			return;
		}

		// Outgoing attack hitsplats
		AttackSqueakMode mode = config.attackSqueakMode();
		if (mode == AttackSqueakMode.DISABLED || !event.getHitsplat().isMine())
		{
			return;
		}

		if (mode == AttackSqueakMode.ON_ZERO && amount != 0)
		{
			return;
		}

		playSqueak();
	}

	private void playSqueak()
	{
		if (executor == null || executor.isShutdown())
		{
			return;
		}

		int volume = config.volume();
		if (volume == 0)
		{
			return;
		}

		// Convert 0-100 volume to decibel gain (0 dB = full volume)
		float gain = 20.0f * (float) Math.log10(volume / 100.0f);

		executor.submit(() ->
		{
			try
			{
				audioPlayer.play(SqueakyToyPlugin.class, "squeak.wav", gain);
			}
			catch (Exception e)
			{
				log.debug("Failed to play squeak", e);
			}
		});
	}

	@Provides
	SqueakyToyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SqueakyToyConfig.class);
	}
}
