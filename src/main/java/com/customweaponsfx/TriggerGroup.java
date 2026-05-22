package com.customweaponsfx;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
public class TriggerGroup
{
	private final Set<Triggers> triggers;
	@Setter private String soundFile;
	@Setter private int volume;

	public TriggerGroup(Set<Triggers> triggers, String soundFile, int volume)
	{
		this.triggers = triggers != null ? triggers : EnumSet.noneOf(Triggers.class);
		this.soundFile = soundFile;
		this.volume = volume;
	}

	public static String serializeTriggers(Set<Triggers> triggers)
	{
		if (triggers == null || triggers.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (Triggers t : triggers)
		{
			if (sb.length() > 0) sb.append(',');
			sb.append(t.name());
		}
		return sb.toString();
	}

	public static Set<Triggers> deserializeTriggers(String s)
	{
		Set<Triggers> set = EnumSet.noneOf(Triggers.class);
		if (s == null || s.isBlank()) return set;
		for (String part : s.split(","))
		{
			part = part.trim();
			if (part.isEmpty()) continue;
			try { set.add(Triggers.valueOf(part)); }
			catch (IllegalArgumentException ignored) {}
		}
		return set;
	}
}
