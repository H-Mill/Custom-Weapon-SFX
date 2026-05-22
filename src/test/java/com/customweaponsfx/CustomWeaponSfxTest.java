package com.customweaponsfx;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CustomWeaponSfxTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CustomWeaponSfxPlugin.class);
		RuneLite.main(args);
	}
}
