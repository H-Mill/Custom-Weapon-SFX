package com.squeakytoy;

import com.google.inject.Provides;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Squeaky Toy",
	description = "Plays a squeaky toy sound on configurable player actions",
	tags = {"squeaky", "toy", "sound", "sfx"}
)
public class SqueakyToyPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SqueakyToyConfig config;

	private ExecutorService executor;
	private final Random random = new Random();

	private byte[] squeakPcmBytes;
	private AudioFormat squeakPcmFormat;

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadExecutor();
		loadSqueakAudio();
		log.debug("Squeaky Toy started!");
	}

	@Override
	protected void shutDown()
	{
		executor.shutdownNow();
		executor = null;
		squeakPcmBytes = null;
		squeakPcmFormat = null;
		log.debug("Squeaky Toy stopped!");
	}

	private void loadSqueakAudio()
	{
		try (InputStream raw = SqueakyToyPlugin.class.getResourceAsStream("squeak.mp3"))
		{
			if (raw == null)
			{
				log.debug("squeak.mp3 not found in resources");
				return;
			}

			AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
			AudioFormat mp3Format = mp3Stream.getFormat();

			AudioFormat pcmFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				mp3Format.getSampleRate(),
				16,
				mp3Format.getChannels(),
				mp3Format.getChannels() * 2,
				mp3Format.getSampleRate(),
				false
			);

			try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, mp3Stream))
			{
				squeakPcmBytes = pcmStream.readAllBytes();
				squeakPcmFormat = pcmFormat;
			}
		}
		catch (Exception e)
		{
			log.debug("Failed to load squeak audio", e);
		}
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
		if (executor == null || executor.isShutdown() || squeakPcmBytes == null)
		{
			return;
		}

		int volume = config.volume();
		if (volume == 0)
		{
			return;
		}

		// Small random pitch variation: ±10%
		float pitchFactor = 0.9f + random.nextFloat() * 0.2f;

		executor.submit(() ->
		{
			try
			{
				playPcm(volume, pitchFactor);
			}
			catch (Exception e)
			{
				log.debug("Failed to play squeak", e);
			}
		});
	}

	private void playPcm(int volume, float pitchFactor) throws LineUnavailableException
	{
		// Scale sample amplitudes for volume
		byte[] samples = squeakPcmBytes.clone();
		float gain = volume / 100.0f;
		for (int i = 0; i < samples.length - 1; i += 2)
		{
			short s = (short) (((samples[i + 1] & 0xFF) << 8) | (samples[i] & 0xFF));
			s = (short) (s * gain);
			samples[i] = (byte) (s & 0xFF);
			samples[i + 1] = (byte) ((s >> 8) & 0xFF);
		}

		// Vary claimed sample rate to shift pitch (±10% changes perceived pitch by ~±1.7 semitones)
		float playbackRate = squeakPcmFormat.getSampleRate() * pitchFactor;
		int channels = squeakPcmFormat.getChannels();
		AudioFormat playFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED,
			playbackRate,
			16,
			channels,
			channels * 2,
			playbackRate,
			false
		);

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFormat);
		if (!AudioSystem.isLineSupported(info))
		{
			log.debug("Audio line not supported on this system");
			return;
		}

		try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info))
		{
			line.open(playFormat);
			line.start();
			line.write(samples, 0, samples.length);
			line.drain();
		}
	}

	@Provides
	SqueakyToyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SqueakyToyConfig.class);
	}
}
