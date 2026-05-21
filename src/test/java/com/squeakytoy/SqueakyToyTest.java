package com.squeakytoy;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SqueakyToyTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SqueakyToyPlugin.class);
		RuneLite.main(args);
	}
}
