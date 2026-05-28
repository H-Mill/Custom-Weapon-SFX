package com.customweaponsfx;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class WeaponEntry
{
	private final int itemId;
	private String weaponName;
	private final List<TriggerGroup> groups;
	@Setter private boolean enabled;

	public WeaponEntry(int itemId, String weaponName, List<TriggerGroup> groups)
	{
		this.itemId = itemId;
		this.weaponName = weaponName;
		this.groups = groups != null ? groups : new ArrayList<>();
		this.enabled = true;
	}
}
