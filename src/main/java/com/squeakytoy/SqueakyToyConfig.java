package com.squeakytoy;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("squeakytoy")
public interface SqueakyToyConfig extends Config
{
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Volume of the squeak sound (0–100)",
		position = 0
	)
	@Range(min = 0, max = 100)
	default int volume()
	{
		return 75;
	}

	@ConfigItem(
		keyName = "attackSqueakMode",
		name = "Player Attacks",
		description = "When to play a squeak for player attacks",
		position = 1
	)
	default AttackSqueakMode attackSqueakMode()
	{
		return AttackSqueakMode.ON_ZERO;
	}

	@ConfigItem(
		keyName = "receivedAttackSqueakMode",
		name = "Attacks on Player",
		description = "When to play a squeak for attacks received by the player",
		position = 2
	)
	default AttackSqueakMode receivedAttackSqueakMode()
	{
		return AttackSqueakMode.DISABLED;
	}
}
