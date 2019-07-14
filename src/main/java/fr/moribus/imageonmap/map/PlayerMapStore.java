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

package fr.moribus.imageonmap.map;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.MapManagerException.Reason;
import fr.zcraft.zlib.tools.PluginLogger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerMapStore implements ConfigurationSerializable
{
    private final UUID playerUUID;
    private final ArrayList<ImageMap> mapList = new ArrayList<ImageMap>();
    private boolean modified = false;
    private int mapCount = 0;
    
    public PlayerMapStore(UUID playerUUID)
    {
        this.playerUUID = playerUUID;
    }
    
    public synchronized boolean managesMap(int mapID)
    {
        for(ImageMap map : mapList)
        {
            if(map.managesMap(mapID)) return true;
        }
        return false;
    }
    
    public synchronized boolean managesMap(ItemStack item)
    {
        if(item == null) return false;
        if(item.getType() != Material.MAP) return false;
        
        for(ImageMap map : mapList)
        {
            if(map.managesMap(item)) return true;
        }
        return false;
    }
    
    public synchronized void addMap(ImageMap map) throws MapManagerException
    {
        checkMapLimit(map);
        insertMap(map);
    }
    
    public synchronized void insertMap(ImageMap map)
    {
        _addMap(map);
        notifyModification();
    }
    
    private void _addMap(ImageMap map)
    {
        mapList.add(map);
        mapCount += map.getMapCount();
    }
    
    public synchronized void deleteMap(ImageMap map) throws MapManagerException
    {
        _removeMap(map);
        notifyModification();
    }
    
    private void _removeMap(ImageMap map) throws MapManagerException
    {
        if(!mapList.remove(map))
        {
            throw new MapManagerException(Reason.IMAGEMAP_DOES_NOT_EXIST);
        }
        mapCount -= map.getMapCount();
    }
    
    public synchronized boolean mapExists(String id)
    {
        for(ImageMap map : mapList)
        {
            if(map.getId().equals(id)) return true;
        }
        
        return false;
    }
    
    public String getNextAvailableMapID(String mapId)
    {
        if(!mapExists(mapId)) return mapId;
        int id = 0;
        
        do
        {
            id++;
        }while(mapExists(mapId + "-" + id));
        
        return mapId + "-" + id;
    }
    
    public synchronized List<ImageMap> getMapList()
    {
        return new ArrayList(mapList);
    }
    
    public synchronized ImageMap[] getMaps()
    {
        return mapList.toArray(new ImageMap[mapList.size()]);
    }
    
    public synchronized ImageMap getMap(String mapId)
    {
        for(ImageMap map : mapList)
        {
            if(map.getId().equals(mapId)) return map;
        }
        
        return null;
    }
    
    public void checkMapLimit(ImageMap map) throws MapManagerException
    {
        checkMapLimit(map.getMapCount());
    }
    
    public void checkMapLimit(int newMapsCount) throws MapManagerException
    {
        int limit = PluginConfiguration.MAP_PLAYER_LIMIT.get();
        if(limit <= 0) return;
        
        if(getMapCount() + newMapsCount > limit)
            throw new MapManagerException(Reason.MAXIMUM_PLAYER_MAPS_EXCEEDED, limit);
    }
    
    /* ===== Getters & Setters ===== */
    
    public UUID getUUID()
    {
        return playerUUID;
    }
    
    public synchronized boolean isModified()
    {
        return modified;
    }
    
    public synchronized void notifyModification()
    {
        this.modified = true;
    }
    
    public synchronized int getMapCount()
    {
        return this.mapCount;
    }
    
    /* ****** Serializing ***** */
    
    @Override
    public Map<String, Object> serialize() 
    {
        Map<String, Object> map = new HashMap<String, Object>();
        ArrayList<Map> list = new ArrayList<Map>();
        synchronized(this)
        {
            for(ImageMap tMap : mapList)
            {
                list.add(tMap.serialize());
            }
        }
        map.put("mapList", list);
        return map;
    }
    
    private void loadFromConfig(ConfigurationSection section)
    {
        if(section == null) return;
        List<Map<String, Object>> list = (List<Map<String, Object>>) section.getList("mapList");
        if(list == null) return;
        
        for(Map<String, Object> tMap : list)
        {
            try
            {
                ImageMap newMap = ImageMap.fromConfig(tMap, playerUUID);
                synchronized(this) {_addMap(newMap);}
            }
            catch(InvalidConfigurationException ex)
            {
                PluginLogger.warning("Could not load map data : ", ex);
            }
        }
        
        try { checkMapLimit(0); }
        catch(MapManagerException ex)
        {
            PluginLogger.warning("Map limit exceeded for player {0} ({1} maps loaded)",
                    playerUUID.toString(),mapList.size());
        }
    }
    
    /* ****** Configuration Files management ***** */
    
    private FileConfiguration mapConfig = null;
    private File mapsFile = null;
    
    private FileConfiguration getToolConfig()
    {
        if(mapConfig == null) load();
        
        return mapConfig;
    }
    
    public void load()
    {
        if(mapsFile == null)
        {
            mapsFile = new File(ImageOnMap.getPlugin().getMapsDirectory(), playerUUID.toString() + ".yml");
            if(!mapsFile.exists()) save();
        }
        mapConfig = YamlConfiguration.loadConfiguration(mapsFile);
        loadFromConfig(getToolConfig().getConfigurationSection("PlayerMapStore"));
    }
    
    public void save()
    {
        if(mapsFile == null || mapConfig == null) return;
        getToolConfig().set("PlayerMapStore", this.serialize());
        try 
        {
            getToolConfig().save(mapsFile);
        } 
        catch (IOException ex) 
        {
            PluginLogger.error("Could not save maps file for player '{0}'", ex, playerUUID.toString());
        }
        synchronized(this) {modified = false;}
    }
}
