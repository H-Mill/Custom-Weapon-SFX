package com.squeakytoy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttackSqueakMode
{
	DISABLED("Disabled"),
	ON_ZERO("On zero"),
	ALL_ATTACKS("All attacks");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
