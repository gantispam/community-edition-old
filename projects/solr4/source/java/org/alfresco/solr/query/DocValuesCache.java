/*
 * #%L
 * Alfresco Solr 4
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.solr.query;

import java.io.IOException;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.NumericDocValues;


/**
 * The DocValuesCache is an in-memory uncompressed numeric DocValues cache. It is designed to provide the fastest
 * possible access to numeric docValues. The DocValuesCache can be used instead of the Direct DocValues format which also
 * provides uncompressed in-memory docValues. The DocValuesCache can be used in situations when it is not
 * practical to re-index to use Direct docValues.
 **/

public class DocValuesCache
{
    private static Map<String, WeakHashMap<Object, NumericDocValues>> cache = new HashMap();

    public static synchronized NumericDocValues getNumericDocValues(String field, AtomicReader reader) throws IOException
    {
        WeakHashMap<Object, NumericDocValues> fieldCache = cache.get(field);

        if(fieldCache == null)
        {
            fieldCache = new WeakHashMap();
            cache.put(field, fieldCache);
        }

        Object cacheKey = reader.getCoreCacheKey();
        NumericDocValues cachedValues = fieldCache.get(cacheKey);

        if(cachedValues == null)
        {
            NumericDocValues fieldValues = reader.getNumericDocValues(field);
            if(fieldValues == null)
            {
                return null;
            }
            else
            {
                int maxDoc = reader.maxDoc();
                boolean longs = false;
                int[] intValues = new int[maxDoc]; //Always start off with an int array.
                SettableDocValues settableValues = new IntValues(intValues);

                for(int i=0; i<maxDoc; i++)
                {
                    long value = fieldValues.get(i);
                    if(value > Integer.MAX_VALUE && !longs)
                    {
                        longs = true;
                        settableValues = new LongValues(intValues);
                    }

                    settableValues.set(i, value);
                }
                fieldCache.put(cacheKey, settableValues);
                return settableValues;
            }
        }
        else
        {
            return cachedValues;
        }
    }

    private static abstract class SettableDocValues extends NumericDocValues
    {
        public abstract void set(int index, long value);
    }

    private static class IntValues extends SettableDocValues
    {
        private int[] values;

        public IntValues(int[] values)
        {
            this.values = values;
        }

        public void set(int index, long value)
        {
            this.values[index] = (int)value;
        }

        public long get(int index) {
            return values[index];
        }
    }

    private static class LongValues extends SettableDocValues
    {
        private long[] values;

        public LongValues(int[] ivalues)
        {
            values = new long[ivalues.length];
            //TODO: Validate to you can copy an int[] into a long[].
            System.arraycopy(ivalues, 0, values, 0, ivalues.length);
        }

        public void set(int index, long value)
        {
            this.values[index] = value;
        }

        public long get(int index)
        {
            return values[index];
        }
    }
}