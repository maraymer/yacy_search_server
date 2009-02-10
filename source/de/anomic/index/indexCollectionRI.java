// indexCollectionRI.java
// (C) 2006 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 03.07.2006 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate: 2006-04-02 22:40:07 +0200 (So, 02 Apr 2006) $
// $LastChangedRevision: 1986 $
// $LastChangedBy: orbiter $
//
// LICENSE
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.anomic.index;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.anomic.kelondro.kelondroCollectionIndex;
import de.anomic.kelondro.index.Row;
import de.anomic.kelondro.index.RowSet;
import de.anomic.kelondro.order.Base64Order;
import de.anomic.kelondro.order.CloneableIterator;
import de.anomic.kelondro.util.kelondroOutOfLimitsException;
import de.anomic.kelondro.util.Log;

public class indexCollectionRI implements indexRI {

    kelondroCollectionIndex collectionIndex;
    
    public indexCollectionRI(final File path, final String filenameStub, final int maxpartition, final Row payloadrow, boolean useCommons) {
        try {
            collectionIndex = new kelondroCollectionIndex(
                    path,
                    filenameStub,
                    12 /*keyLength*/,
                    Base64Order.enhancedCoder,
                    4 /*loadfactor*/,
                    maxpartition,
                    payloadrow,
                    useCommons);
        } catch (final IOException e) {
            Log.logSevere("PLASMA", "unable to open collection index at " + path.toString() + ":" + e.getMessage());
        }
    }
    
    public void clear() {
        try {
            collectionIndex.clear();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
    
    public int size() {
        return collectionIndex.size();
    }

    public int minMem() {
        // calculate a minimum amount of memory that is necessary to use the index
        // during runtime (after the object was initialized)
        return 100 * 1024 /* overhead here */ + collectionIndex.minMem();
    }

    public synchronized CloneableIterator<indexContainer> wordContainerIterator(final String startWordHash, final boolean rot, final boolean ram) {
        return new wordContainersIterator(startWordHash, rot);
    }

    public class wordContainersIterator implements CloneableIterator<indexContainer> {

        private final Iterator<Object[]> wci;
        private final boolean rot;
        
        public wordContainersIterator(final String startWordHash, final boolean rot) {
            this.rot = rot;
            this.wci = collectionIndex.keycollections(startWordHash.getBytes(), Base64Order.zero(startWordHash.length()), rot);
        }
        
        public wordContainersIterator clone(final Object secondWordHash) {
            return new wordContainersIterator((String) secondWordHash, rot);
        }
        
        public boolean hasNext() {
            return wci.hasNext();
        }

        public indexContainer next() {
            final Object[] oo = wci.next();
            if (oo == null) return null;
            final byte[] key = (byte[]) oo[0];
            final RowSet collection = (RowSet) oo[1];
            if (collection == null) return null;
            return new indexContainer(new String(key), collection);
        }
        
        public void remove() {
            wci.remove();
        }

    }

    public boolean hasContainer(final String wordHash) {
        return collectionIndex.has(wordHash.getBytes());
    }
    
    public indexContainer getContainer(final String wordHash, final Set<String> urlselection) {
        try {
            final RowSet collection = collectionIndex.get(wordHash.getBytes());
            if (collection != null) collection.select(urlselection);
            if ((collection == null) || (collection.size() == 0)) return null;
            return new indexContainer(wordHash, collection);
        } catch (final IOException e) {
            return null;
        }
    }

    public indexContainer deleteContainer(final String wordHash) {
        try {
            final RowSet collection = collectionIndex.delete(wordHash.getBytes());
            if (collection == null) return null;
            return new indexContainer(wordHash, collection);
        } catch (final IOException e) {
            return null;
        }
    }

    public boolean removeEntry(final String wordHash, final String urlHash) {
        final HashSet<String> hs = new HashSet<String>();
        hs.add(urlHash);
        return removeEntries(wordHash, hs) == 1;
    }
    
    public int removeEntries(final String wordHash, final Set<String> urlHashes) {
        try {
            return collectionIndex.remove(wordHash.getBytes(), urlHashes);
        } catch (final kelondroOutOfLimitsException e) {
            e.printStackTrace();
            return 0;
        } catch (final IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void addEntries(final indexContainer newEntries) {
    	if (newEntries == null) return;
        try {
            collectionIndex.merge(newEntries);
        } catch (final kelondroOutOfLimitsException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        collectionIndex.close();
    }

    public int sizeEntry(String key) {
        try {
            final RowSet collection = collectionIndex.get(key.getBytes());
            if (collection == null) return 0;
            return collection.size();
        } catch (final IOException e) {
            return 0;
        }
    }
    
}
