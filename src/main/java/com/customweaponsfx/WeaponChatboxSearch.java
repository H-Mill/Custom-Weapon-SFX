package com.customweaponsfx;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextInput;
import net.runelite.client.ui.JagexColors;

@Singleton
public class WeaponChatboxSearch extends ChatboxTextInput
{
	private static final int ICON_HEIGHT = 32;
	private static final int ICON_WIDTH = 36;
	private static final int PADDING = 6;
	private static final int MAX_RESULTS = 24;
	private static final int FONT_SIZE = 16;
	private static final int HOVERED_OPACITY = 128;
	private static final int WEAPON_SLOT = EquipmentInventorySlot.WEAPON.getSlotIdx();

	private final ChatboxPanelManager chatboxPanelManager;
	private final ItemManager itemManager;
	private final Client client;

	private final Map<Integer, ItemComposition> results = new LinkedHashMap<>();
	private int index = -1;
	private Consumer<Integer> onItemSelected;

	@Inject
	WeaponChatboxSearch(ChatboxPanelManager chatboxPanelManager, ClientThread clientThread,
		ItemManager itemManager, Client client)
	{
		super(chatboxPanelManager, clientThread);
		this.chatboxPanelManager = chatboxPanelManager;
		this.itemManager = itemManager;
		this.client = client;

		lines(1);
		prompt("Search for a weapon");
		onChanged(searchString ->
			clientThread.invokeLater(() ->
			{
				filterResults();
				update();
			}));
	}

	@Override
	protected void update()
	{
		Widget container = chatboxPanelManager.getContainerWidget();
		container.deleteAllChildren();

		Widget promptWidget = container.createChild(-1, WidgetType.TEXT);
		promptWidget.setText(getPrompt());
		promptWidget.setTextColor(0x800000);
		promptWidget.setFontId(getFontID());
		promptWidget.setOriginalX(0);
		promptWidget.setOriginalY(5);
		promptWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		promptWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		promptWidget.setOriginalHeight(FONT_SIZE);
		promptWidget.setXTextAlignment(WidgetTextAlignment.CENTER);
		promptWidget.setYTextAlignment(WidgetTextAlignment.CENTER);
		promptWidget.setWidthMode(WidgetSizeMode.MINUS);
		promptWidget.revalidate();

		Widget cancelBtn = container.createChild(-1, WidgetType.TEXT);
		cancelBtn.setText("Cancel");
		cancelBtn.setTextColor(0xAA0000);
		cancelBtn.setFontId(getFontID());
		cancelBtn.setOriginalX(container.getWidth() - 56);
		cancelBtn.setOriginalY(5);
		cancelBtn.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		cancelBtn.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		cancelBtn.setOriginalHeight(FONT_SIZE);
		cancelBtn.setOriginalWidth(56);
		cancelBtn.setHasListener(true);
		cancelBtn.setAction(0, "Close");
		cancelBtn.setOnOpListener((JavaScriptCallback) ev -> chatboxPanelManager.close());
		cancelBtn.revalidate();

		buildEdit(0, 5 + FONT_SIZE, container.getWidth(), FONT_SIZE);

		Widget separator = container.createChild(-1, WidgetType.LINE);
		separator.setOriginalX(0);
		separator.setOriginalY(8 + (FONT_SIZE * 2));
		separator.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		separator.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		separator.setOriginalHeight(0);
		separator.setOriginalWidth(16);
		separator.setWidthMode(WidgetSizeMode.MINUS);
		separator.setTextColor(0x666666);
		separator.revalidate();

		int x = PADDING;
		int y = PADDING * 3;
		int idx = 0;
		for (ItemComposition itemComposition : results.values())
		{
			Widget item = container.createChild(-1, WidgetType.GRAPHIC);
			item.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			item.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			item.setOriginalX(x);
			item.setOriginalY(y + FONT_SIZE * 2);
			item.setOriginalHeight(ICON_HEIGHT);
			item.setOriginalWidth(ICON_WIDTH);
			item.setName(JagexColors.MENU_TARGET_TAG + itemComposition.getName());
			item.setItemId(itemComposition.getId());
			item.setItemQuantity(10000);
			item.setItemQuantityMode(ItemQuantityMode.NEVER);
			item.setBorderType(1);
			item.setAction(0, "Add weapon");
			item.setHasListener(true);

			if (index == idx)
			{
				item.setOpacity(HOVERED_OPACITY);
			}
			else
			{
				item.setOnMouseOverListener((JavaScriptCallback) ev -> item.setOpacity(HOVERED_OPACITY));
				item.setOnMouseLeaveListener((JavaScriptCallback) ev -> item.setOpacity(0));
			}

			item.setOnOpListener((JavaScriptCallback) ev ->
			{
				if (onItemSelected != null)
				{
					onItemSelected.accept(itemComposition.getId());
				}
				chatboxPanelManager.close();
			});

			x += ICON_WIDTH + PADDING;
			if (x + ICON_WIDTH >= container.getWidth())
			{
				y += ICON_HEIGHT + PADDING;
				x = PADDING;
			}

			item.revalidate();
			++idx;
		}
	}

	@Override
	public void keyPressed(KeyEvent ev)
	{
		if (!chatboxPanelManager.shouldTakeInput()) return;

		switch (ev.getKeyCode())
		{
			case KeyEvent.VK_ENTER:
				ev.consume();
				if (index > -1 && onItemSelected != null)
				{
					onItemSelected.accept(results.keySet().toArray(new Integer[0])[index]);
					chatboxPanelManager.close();
				}
				break;
			default:
				super.keyPressed(ev);
		}
	}

	@Override
	protected void close()
	{
		value("");
		results.clear();
		index = -1;
		super.close();
	}

	public WeaponChatboxSearch onItemSelected(Consumer<Integer> onItemSelected)
	{
		this.onItemSelected = onItemSelected;
		return this;
	}

	private void filterResults()
	{
		results.clear();
		index = -1;

		String search = getValue().toLowerCase();
		if (search.isEmpty()) return;

		for (int i = 0; i < client.getItemCount() && results.size() < MAX_RESULTS; i++)
		{
			int canonical = itemManager.canonicalize(i);
			if (canonical != i) continue;

			ItemComposition comp = itemManager.getItemComposition(canonical);
			String name = comp.getName().toLowerCase();
			if (name.equals("null") || !name.contains(search)) continue;

			ItemStats stats = itemManager.getItemStats(canonical);
			if (stats == null || stats.getEquipment() == null) continue;
			if (stats.getEquipment().getSlot() != WEAPON_SLOT) continue;

			if (!results.containsKey(comp.getId()))
			{
				results.put(comp.getId(), comp);
			}
		}
	}
}
