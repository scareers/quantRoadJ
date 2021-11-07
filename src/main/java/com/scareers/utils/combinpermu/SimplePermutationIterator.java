/**
 * Combinatorics Library 3
 * Copyright 2009-2016 Dmytro Paukov d.paukov@gmail.com
 */
package com.scareers.utils.combinpermu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Iterator for the permutation generator
 *
 * @author Dmytro Paukov
 * @version 3.0
 * @see SimplePermutationGenerator
 * @param <T>
 *            Type of elements in the permutations
 */
class SimplePermutationIterator<T> implements Iterator<List<T>> {

    final SimplePermutationGenerator<T> generator;
    final List<T> currentPermutation;
    final int length;
    long currentIndex = 0;

    private int[] pZ = null;
    private int[] pP = null;
    private int[] pD = null;
    private int m = 0;
    private int w = 0;
    private int pm = 0;
    private int dm = 0;
    private int zpm = 0;

    SimplePermutationIterator(SimplePermutationGenerator<T> generator) {
        this.generator = generator;
        length = generator.originalVector.size();
        currentPermutation = new ArrayList<>(generator.originalVector);
        pZ = new int[length + 2];
        pP = new int[length + 2];
        pD = new int[length + 2];

        currentIndex = 0;

        m = 0;
        w = 0;
        pm = 0;
        dm = 0;
        zpm = 0;

        for (int i = 1; i <= length; i++) {
            pP[i] = i;
            pZ[i] = i;
            pD[i] = -1;
        }
        pD[1] = 0;
        pZ[length + 1] = m = length + 1;
        pZ[0] = pZ[length + 1];

    }


    @Override
    public boolean hasNext() {
        return m != 1;
    }


    @Override
    public List<T> next() {

        for (int i = 1; i <= length; i++) {
            int index = pZ[i] - 1;
            currentPermutation.set(i-1, generator.originalVector.get(index));
        }
        m = length;
        while (pZ[pP[m] + pD[m]] > m) {
            pD[m] = -pD[m];
            m--;
        }
        pm = pP[m];
        dm = pm + pD[m];
        w = pZ[pm];
        pZ[pm] = pZ[dm];
        pZ[dm] = w;
        zpm = pZ[pm];
        w = pP[zpm];
        pP[zpm] = pm;
        pP[m] = w;
        currentIndex++;

        return new ArrayList<>(currentPermutation);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


    @Override
    public String toString() {
        return "SimplePermutationIterator=[#" + currentIndex + ", " + currentPermutation + "]";
    }
}
