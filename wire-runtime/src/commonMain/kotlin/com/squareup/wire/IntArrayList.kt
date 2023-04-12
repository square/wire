package com.squareup.wire

/**
 * Inspired by org.jetbrains.kotlin.utils.IntArrayList
 *
 * Offers a nice wrapper around IntArray, that handles resizing the underlying array as needed and
 * provides a trimToSize() method to truncate the underlying array to the current number of elements.
 */
class IntArrayList(initialCapacity: Int) {
  private var data = IntArray(initialCapacity)
  private var size = 0

  /**
   * Returns the underlying IntArray, truncating as necessary so that the returned array has
   * the same size as the number of elements in it.
   *
   * Because this method truncates the underlying array, it should only be called after all
   * elements have been added to the list, otherwise the next call to add() will cause the
   * array to be resized.
   */
  fun getTruncatedArray(): IntArray {
    if (size < data.size) {
      data = data.copyOf(size)
    }
    return data
  }

  fun add(int: Int) {
    ensureCapacity(size + 1)
    data[size++] = int
  }

  fun isNotEmpty() : Boolean = size > 0

  private fun ensureCapacity(minCapacity: Int) {
    if (minCapacity > data.size) {
      data = data.copyOf(maxOf(data.size * 3 / 2 + 1, minCapacity))
    }
  }

  override fun toString(): String =
    (0 until size).joinToString(", ", "[", "]") { data[it].toString() }
}
