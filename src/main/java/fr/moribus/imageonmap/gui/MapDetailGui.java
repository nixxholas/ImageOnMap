/*
 * Copyright (C) 2013 Moribus
 * Copyright (C) 2015 ProkopyL <prokopylmc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.moribus.imageonmap.gui;

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.PosterMap;
import fr.moribus.imageonmap.map.SingleMap;
import fr.moribus.imageonmap.ui.MapItemManager;
import fr.zcraft.zlib.components.gui.ExplorerGui;
import fr.zcraft.zlib.components.gui.Gui;
import fr.zcraft.zlib.components.gui.GuiAction;
import fr.zcraft.zlib.components.gui.PromptGui;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.tools.Callback;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class MapDetailGui extends ExplorerGui<Integer>
{
    private final ImageMap map;

    public MapDetailGui(ImageMap map)
    {
        this.map = map;
    }

    @Override
    protected ItemStack getViewItem(int x, int y)
    {
        final Material partMaterial = y % 2 == x % 2 ? Material.LEGACY_EMPTY_MAP : Material.PAPER;

        final ItemStackBuilder builder = new ItemStackBuilder(partMaterial)
                .title(I.t(getPlayerLocale(), "{green}Map part"))
                .lore(I.t(getPlayerLocale(), "{gray}Row: {white}{0}", y + 1))
                .lore(I.t(getPlayerLocale(), "{gray}Column: {white}{0}", x + 1));

        if (Permissions.GET.grantedTo(getPlayer()))
            builder.loreLine().lore(I.t(getPlayerLocale(), "{gray}» {white}Click{gray} to get only this part"));

        return builder.item();
    }
    
    @Override
    protected ItemStack getViewItem(Integer mapId)
    {
        final int index = ((PosterMap) map).getIndex(mapId);
        final Material partMaterial = index % 2 == 0 ? Material.LEGACY_EMPTY_MAP : Material.PAPER;

        final ItemStackBuilder builder = new ItemStackBuilder(partMaterial)
                .title(I.t(getPlayerLocale(), "{green}Map part"))
                .lore(I.t(getPlayerLocale(), "{gray}Part: {white}{0}", index + 1));

        if (Permissions.GET.grantedTo(getPlayer()))
            builder.loreLine().lore(I.t(getPlayerLocale(), "{gray}» {white}Click{gray} to get only this part"));

        return builder.item();
    }
    
    @Override
    protected ItemStack getPickedUpItem(int x, int y)
    {
        if (!Permissions.GET.grantedTo(getPlayer()))
            return null;

        if (map instanceof SingleMap)
        {
            return MapItemManager.createMapItem((SingleMap)map);
        }
        else if (map instanceof PosterMap)
        {
            return MapItemManager.createMapItem((PosterMap)map, x, y);
        }
        
        throw new IllegalStateException("Unsupported map type: " + map.getType());
    }

    @Override
    protected ItemStack getPickedUpItem(Integer mapId)
    {
        if (!Permissions.GET.grantedTo(getPlayer()))
            return null;

        final PosterMap poster = (PosterMap) map;
        return MapItemManager.createMapItem(poster, poster.getIndex(mapId));
    }
    
    @Override
    protected ItemStack getEmptyViewItem()
    {
        if (map instanceof SingleMap)
        {
            return getViewItem(0, 0);
        }
        else return super.getEmptyViewItem();
    }

    @Override
    protected void onUpdate()
    {
        /// Title of the map details GUI
        setTitle(I.t(getPlayerLocale(), "Your maps » {black}{0}", map.getName()));
        setKeepHorizontalScrollingSpace(true);

        if (map instanceof PosterMap)
        {
            PosterMap poster = (PosterMap) map;
            if(poster.hasColumnData())
            {
                setDataShape(poster.getColumnCount(), poster.getRowCount());
            }
            else
            {
                setData(ArrayUtils.toObject(poster.getMapsIDs()));
            }
        }
        else
        {
            setDataShape(1,1); 
        }

        final boolean canRename = Permissions.RENAME.grantedTo(getPlayer());
        final boolean canDelete = Permissions.DELETE.grantedTo(getPlayer());

        int renameSlot = getSize() - 7;
        int deleteSlot = getSize() - 6;

        if (!canRename)
            deleteSlot--;

        if (canRename)
        {
            action("rename", renameSlot, new ItemStackBuilder(Material.LEGACY_BOOK_AND_QUILL)
                    .title(I.t(getPlayerLocale(), "{blue}Rename this image"))
                    .longLore(I.t(getPlayerLocale(), "{gray}Click here to rename this image; this is used for your own organization."))
            );
        }

        if (canDelete)
        {
            action("delete", deleteSlot, new ItemStackBuilder(Material.BARRIER)
                    .title(I.t(getPlayerLocale(), "{red}Delete this image"))
                    .longLore(I.t(getPlayerLocale(), "{gray}Deletes this map {white}forever{gray}. This action cannot be undone!"))
                    .loreLine()
                    .longLore(I.t(getPlayerLocale(), "{gray}You will be asked to confirm your choice if you click here."))
            );
        }


        // To keep the controls centered, the back button is shifted to the right when the
        // arrow isn't displayed, so when the map fit on the grid without sliders.
        int backSlot = getSize() - 4;

        if (!canRename && !canDelete)
            backSlot = getSize() - 5;
        else if (map instanceof PosterMap && ((PosterMap) map).getColumnCount() <= INVENTORY_ROW_SIZE)
            backSlot++;

        action("back", backSlot, new ItemStackBuilder(Material.EMERALD)
                .title(I.t(getPlayerLocale(), "{green}« Back"))
                .lore(I.t(getPlayerLocale(), "{gray}Go back to the list."))
        );
    }


    @GuiAction ("rename")
    public void rename()
    {
        if (!Permissions.RENAME.grantedTo(getPlayer()))
        {
            I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
            update();
            return;
        }

        PromptGui.prompt(getPlayer(), new Callback<String>()
        {
            @Override
            public void call(String newName)
            {
                if (!Permissions.RENAME.grantedTo(getPlayer()))
                {
                    I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
                    return;
                }

                if (newName == null || newName.isEmpty())
                {
                    I.sendT(getPlayer(), "{ce}Map names can't be empty.");
                    return;
                }

                map.rename(newName);
                I.sendT(getPlayer(), "{cs}Map successfully renamed.");
            }
        }, map.getName(), this);
    }

    @GuiAction ("delete")
    public void delete()
    {
        if (!Permissions.DELETE.grantedTo(getPlayer()))
        {
            I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
            update();
            return;
        }

        Gui.open(getPlayer(), new ConfirmDeleteMapGui(map), this);
    }

    @GuiAction ("back")
    public void back()
    {
        close();
    }
}
