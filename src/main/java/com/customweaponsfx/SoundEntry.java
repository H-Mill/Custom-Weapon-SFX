package com.customweaponsfx;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SoundEntry
{
	@Setter private String soundFile;
	@Setter private int volume;

	public SoundEntry(String soundFile, int volume)
	{
		this.soundFile = soundFile;
		this.volume = volume;
	}
}
