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

import org.bukkit.configuration.InvalidConfigurationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PosterMap extends ImageMap
{
    protected final int[] mapsIDs;
    protected final int columnCount;
    protected final int rowCount;
    
    public PosterMap(UUID userUUID, int[] mapsIDs, String id, String name, int columnCount, int rowCount)
    {
        super(userUUID, Type.POSTER, id, name);
        this.mapsIDs = mapsIDs;
        this.columnCount = Math.max(columnCount, 0);
        this.rowCount = Math.max(rowCount, 0);
    }
    
    public PosterMap(UUID userUUID, int[] mapsIDs, int columnCount, int rowCount)
    {
        this(userUUID, mapsIDs, null, null, columnCount, rowCount);
    }
    
    @Override
    public int[] getMapsIDs()
    {
        return mapsIDs;
    }

    @Override
    public boolean managesMap(int mapID)
    {
        for(int i = 0; i < mapsIDs.length; i++)
        {
            if(mapsIDs[i] == mapID) return true;
        }
        
        return false;
    }

    /* ====== Serialization methods ====== */
    
    public PosterMap(Map<String, Object> map, UUID userUUID) throws InvalidConfigurationException
    {
        super(map, userUUID, Type.POSTER);
        
        columnCount = getFieldValue(map, "columns");
        rowCount = getFieldValue(map, "rows");
        
        List<Integer> idList = getFieldValue(map, "mapsIDs");
        mapsIDs = new int[idList.size()];
        for(int i = 0, c = idList.size(); i < c; i++)
        {
            mapsIDs[i] = (short) ((int) idList.get(i));
        }
    }
    
    @Override
    protected void postSerialize(Map<String, Object> map)
    {
        map.put("columns", columnCount);
        map.put("rows", rowCount);
        map.put("mapsIDs", mapsIDs);
    }
    
    /* ====== Getters & Setters ====== */
    
    /**
     * Returns the amount of columns in the poster map
     * @return The number of columns, or 0 if this data is missing
     */
    public int getColumnCount()
    {
        return columnCount;
    }
    
    /**
     * Returns the amount of rows in the poster map
     * @return The number of rows, or 0 if this data is missing
     */
    public int getRowCount()
    {
        return rowCount;
    }
    
    public int getColumnAt(int i)
    {
        if(columnCount == 0) return 0;
        return (i % columnCount);
    }
    
    public int getRowAt(int i)
    {
        if(columnCount == 0) return 0;
        return (i / columnCount);
    }
    
    public int getIndexAt(int col, int row)
    {
        return columnCount * row + col;
    }

    /**
     * Returns the map id at the given column and line.
     *
     * @param x The x coordinate. Starts at 0.
     * @param y The y coordinate. Starts at 0.
     * @return The Minecraft map ID.
     *
     * @throws ArrayIndexOutOfBoundsException if the given coordinates are too big (out of the poster).
     */
    public int getMapIdAt(int x, int y)
    {
        return mapsIDs[y * columnCount + x];
    }
    
    public int getMapIdAtReverseY(int index)
    {
        int x = index % (columnCount);
        int y = index / (columnCount);
        return getMapIdAt(x, rowCount - y - 1);
    }
    
    public int getMapIdAt(int index)
    {
        return mapsIDs[index];
    }
    
    public boolean hasColumnData()
    {
        return rowCount != 0 && columnCount != 0;
    }

    @Override
    public int getMapCount()
    {
        return mapsIDs.length;
    }
    
    public int getIndex(int mapID)
    {
        for(int i = 0; i < mapsIDs.length; i++)
        {
            if(mapsIDs[i] == mapID) return i;
        }
        
        throw new IllegalArgumentException("Invalid map ID");
    }

}
