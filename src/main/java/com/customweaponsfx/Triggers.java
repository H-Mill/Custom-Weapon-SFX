package com.customweaponsfx;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Triggers
{
	REGULAR_ZERO("Regular attack zero"),
	REGULAR_HIT("Regular attack hit"),
	REGULAR_MAX("Regular attack max"),
	SPECIAL_ZERO("Special attack zero"),
	SPECIAL_HIT("Special attack hit"),
	SPECIAL_MAX("Special attack max"),
	THRALL_HIT("Thrall attack hit"),
	ALL("All attacks");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
