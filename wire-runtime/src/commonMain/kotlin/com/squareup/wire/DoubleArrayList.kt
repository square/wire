package com.squareup.wire

/**
 * Inspired by org.jetbrains.kotlin.utils.IntArrayList
 *
 * Offers a nice wrapper around DoubleArray, that handles resizing the underlying array as needed and
 * provides a trimToSize() method to truncate the underlying array to the current number of elements.
 */
class DoubleArrayList(initialCapacity: Int) {
  private var data = DoubleArray(initialCapacity)
  private var size = 0

  /**
   * Returns the underlying DoubleArray, truncating as necessary so that the returned array has
   * the same size as the number of elements in it.
   *
   * Because this method truncates the underlying array, it should only be called after all
   * elements have been added to the list, otherwise the next call to add() will cause the
   * array to be resized.
   */
  fun getTruncatedArray(): DoubleArray {
    if (size < data.size) {
      data = data.copyOf(size)
    }
    return data
  }

  private fun ensureCapacity(minCapacity: Int) {
    if (minCapacity > data.size) {
      data = data.copyOf(maxOf(data.size * 3 / 2 + 1, minCapacity))
    }
  }

  fun add(double: Double) {
    ensureCapacity(size + 1)
    data[size++] = double
  }

  override fun toString(): String =
    (0 until size).joinToString(", ", "[", "]") { data[it].toString() }
}
