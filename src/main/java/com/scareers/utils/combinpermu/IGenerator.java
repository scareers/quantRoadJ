/**
 * Combinatorics Library 3
 * Copyright 2009-2016 Dmytro Paukov d.paukov@gmail.com
 */
package com.scareers.utils.combinpermu;

import java.util.stream.Stream;

/**
 * This interface is implemented by all generic generators in the library
 * 
 * @author Dmytro Paukov
 * @version 3.0
 * @param <T>
 *            The type of the elements
 */
public interface IGenerator<T> extends Iterable<T> {

	Stream<T> stream();
}
