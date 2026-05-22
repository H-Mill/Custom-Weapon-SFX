package com.customweaponsfx;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class WeaponEntry
{
	private final int itemId;
	private String weaponName;
	private final List<TriggerGroup> groups;

	public WeaponEntry(int itemId, String weaponName, List<TriggerGroup> groups)
	{
		this.itemId = itemId;
		this.weaponName = weaponName;
		this.groups = groups != null ? groups : new ArrayList<>();
	}
}
