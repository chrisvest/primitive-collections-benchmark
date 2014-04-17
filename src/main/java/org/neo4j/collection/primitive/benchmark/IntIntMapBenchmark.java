/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.neo4j.collection.primitive.benchmark;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import cern.colt.function.IntIntProcedure;
import cern.colt.function.IntProcedure;
import cern.colt.map.OpenIntIntHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import javolution.util.FastMap;
import javolution.util.function.Reducers;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import org.neo4j.collection.primitive.Primitive;
import org.neo4j.collection.primitive.PrimitiveLongIntMap;
import org.neo4j.collection.primitive.PrimitiveLongIntVisitor;
import org.neo4j.collection.primitive.PrimitiveLongVisitor;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
@State(Scope.Benchmark)
public abstract class IntIntMapBenchmark
{
    @Param({"10", "100", "1000", "10000", "100000"})
    private int size;

    private ThreadLocalRandom tlr;

    @Setup(Level.Trial)
    public void prepareTrial()
    {
        tlr = ThreadLocalRandom.current();
    }

    @Setup(Level.Iteration)
    public void prepareMap()
    {
        // Create a map that has been filled and then about half of the entries removed
        clearMap();
        for ( int i = 0; i < size; i++ )
        {
            putInt( i, tlr.nextInt() );
        }
        for ( int i = 0; i < size; i++ )
        {
            if ( tlr.nextBoolean() )
            {
                removeInt( i );
            }
        }
    }

    public abstract void clearMap();

    public abstract int getInt( int key );

    public abstract int removeInt( int key );

    public abstract void putInt( int key, int value );

    public abstract int sumValues();

    public abstract int sumKeys();

    @GenerateMicroBenchmark
    public int get()
    {
        return getInt( tlr.nextInt( size ) );
    }

    @GenerateMicroBenchmark
    public void put()
    {
        putInt( tlr.nextInt( size ), tlr.nextInt() );
    }

    @GenerateMicroBenchmark
    public int removeAndPut()
    {
        int i = removeInt( tlr.nextInt( size ) );
        putInt( tlr.nextInt( size ), i );
        return i;
    }

    @GenerateMicroBenchmark
    public int iterateValues()
    {
        return sumValues();
    }

    @GenerateMicroBenchmark
    public int iterateKeys()
    {
        return sumValues();
    }

    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Threads(1)
    @State(Scope.Benchmark)
    public static class PrngBaseline
    {
        private ThreadLocalRandom tlr;

        @Setup(Level.Trial)
        public void prepareTrial()
        {
            tlr = ThreadLocalRandom.current();
        }

        @GenerateMicroBenchmark
        public int prngBaseline()
        {
            return tlr.nextInt( 1000 );
        }
    }

    public static class Colt extends IntIntMapBenchmark
    {
        private OpenIntIntHashMap map;

        private static class SumKeys implements IntProcedure
        {
            private int sum = 0;

            @Override
            public boolean apply( int key )
            {
                sum += key;
                return true;
            }
        }

        private static class SumValues implements IntIntProcedure
        {
            private int sum = 0;

            @Override
            public boolean apply( int key, int value )
            {
                sum += value;
                return true;
            }
        }

        @Override
        public void clearMap()
        {
            map = new OpenIntIntHashMap();
        }

        @Override
        public int getInt( int key )
        {
            return map.get( key );
        }

        @Override
        public int removeInt( int key )
        {
            map.removeKey( key );
            return 0; // Colt does not return the removed value
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            SumValues sum = new SumValues();
            map.forEachPair( sum );
            return sum.sum;
        }

        @Override
        public int sumKeys()
        {
            SumKeys sum = new SumKeys();
            map.forEachKey( sum );
            return sum.sum;
        }
    }

    public static class Trove extends IntIntMapBenchmark
    {
        private TIntIntHashMap map;

        private static class Sum implements TIntProcedure
        {
            private int sum = 0;

            @Override
            public boolean execute( int value )
            {
                sum += value;
                return true;
            }
        }

        @Override
        public void clearMap()
        {
            map = new TIntIntHashMap();
        }

        @Override
        public int getInt( int key )
        {
            return map.get( key );
        }

        @Override
        public int removeInt( int key )
        {
            return map.remove( key );
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            Sum sum = new Sum();
            map.forEachValue( sum );
            return sum.sum;
        }

        @Override
        public int sumKeys()
        {
            Sum sum = new Sum();
            map.forEachKey( sum );
            return (int) sum.sum;
        }
    }

    public static class Fastutil extends IntIntMapBenchmark
    {
        private Int2IntOpenHashMap map;

        @Override
        public void clearMap()
        {
            map = new Int2IntOpenHashMap();
        }

        @Override
        public int getInt( int key )
        {
            return map.get( key );
        }

        @Override
        public int removeInt( int key )
        {
            return map.remove( key );
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            int sum = 0;
            IntIterator iterator = map.values().iterator();
            for ( int i = 0; i < map.size(); i++ )
            {
                sum += iterator.nextInt();
            }
            return sum;
        }

        @Override
        public int sumKeys()
        {
            int sum = 0;
            IntIterator iterator = map.keySet().iterator();
            for ( int i = 0; i < map.size(); i++ )
            {
                sum += iterator.nextInt();
            }
            return sum;
        }
    }

    public static class JavaHashMap extends IntIntMapBenchmark
    {
        private HashMap<Integer, Integer> map;

        @Override
        public void clearMap()
        {
            map = new HashMap<>();
        }

        @Override
        public int getInt( int key )
        {
            Integer integer = map.get( key );
            return integer == null? 0 : integer;
        }

        @Override
        public int removeInt( int key )
        {
            Integer remove = map.remove( key );
            return remove == null? 0 : remove;
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            return map.entrySet().stream().unordered().reduce(
                    0,
                    (x, e) -> x + e.getValue(),
                    (a, b) -> a + b);
        }

        @Override
        public int sumKeys()
        {
            return map.entrySet().stream().unordered().reduce(
                    0,
                    (x, e) -> x + e.getKey(),
                    (a, b) -> a + b );
        }
    }

    public static class Javolution extends IntIntMapBenchmark
    {
        private FastMap<Integer, Integer> map;

        @Override
        public void clearMap()
        {
            map = new FastMap<>();
        }

        @Override
        public int getInt( int key )
        {
            Integer integer = map.get( key );
            return integer == null? 0 : integer;
        }

        @Override
        public int removeInt( int key )
        {
            Integer remove = map.remove( key );
            return remove == null? 0 : remove;
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            return map.values().reduce( Reducers.sum() );
        }

        @Override
        public int sumKeys()
        {
            return map.keySet().reduce( Reducers.sum() );
        }
    }

    public static class NeoPrimitive extends IntIntMapBenchmark
    {
        private PrimitiveLongIntMap map;

        private static class SumKeys implements PrimitiveLongVisitor
        {
            private long sum;

            @Override
            public void visited( long key )
            {
                sum += key;
            }
        }

        private static class SumValues implements PrimitiveLongIntVisitor
        {
            private int sum = 0;

            @Override
            public void visited( long key, int value )
            {
                sum += value;
            }
        }

        @Override
        public void clearMap()
        {
            map = Primitive.longIntMap();
        }

        @Override
        public int getInt( int key )
        {
            return map.get( key );
        }

        @Override
        public int removeInt( int key )
        {
            return map.remove( key );
        }

        @Override
        public void putInt( int key, int value )
        {
            map.put( key, value );
        }

        @Override
        public int sumValues()
        {
            SumValues sum = new SumValues();
            map.visitEntries( sum );
            return sum.sum;
        }

        @Override
        public int sumKeys()
        {
            SumKeys sum = new SumKeys();
            map.visitKeys( sum );
            return (int) sum.sum;
        }
    }
}
