package com.squareup.wire.protocwire

import com.google.protobuf.DescriptorProtos
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.internal.parser.OptionElement

internal data class LocationAndComments(val comment: String, val loc: Location)

internal class SourceInfo(
  private val helper: SourceCodeHelper,
  val descriptorSource: Plugin.DescriptorSource,
  path: List<Int> = emptyList(),
) {
  constructor(
      fileDescriptor: DescriptorProtos.FileDescriptorProto,
      descriptorSource: Plugin.DescriptorSource,
      path: List<Int> = emptyList()
  ) : this(SourceCodeHelper(fileDescriptor), descriptorSource, path)

  private val path = listOf(*path.toTypedArray())

  fun push(value: Int): SourceInfo {
    return SourceInfo(helper, descriptorSource, path.plus(value))
  }

  fun info(): LocationAndComments {
    return helper.getLocation(path)
  }

  fun infoContaining(loc: Location): LocationAndComments {
    return helper.findLocationContaining(path, loc)
  }
}

/**
 * Creates an index for the source locations.
 */
internal class SourceCodeHelper(
  fileDescriptorProto: DescriptorProtos.FileDescriptorProto
) {
  private val locations: Map<List<Int>, List<DescriptorProtos.SourceCodeInfo.Location>> = makeLocationMap(fileDescriptorProto.sourceCodeInfo.locationList)
  private val baseLoc: Location = Location.get(fileDescriptorProto.name)

  /**
   * The location of the first span associated with the given path.
   */
  fun getLocation(path: List<Int>): LocationAndComments {
    val location = locations[path]?.firstOrNull()
    return toLocationAndComments(location)
  }

  /**
   * Looks through all spans associated with a given path to find the span that
   * encloses the given location.
   *
   * @returns the starting location of the span.
   */
  fun findLocationContaining(path: List<Int>, other: Location): LocationAndComments {
    val matches = locations[path]
    if (!matches.isNullOrEmpty()) {
      for (location: DescriptorProtos.SourceCodeInfo.Location in matches) {
        val startLine = location.getSpan(0) + 1
        val startCol = location.getSpan(1) + 1
        val endLine = if (location.spanCount > 3) location.getSpan(2) + 1 else startLine
        val endCol = location.getSpan(if (location.spanCount > 3) 3 else 2) + 1
        if (
          (startLine < other.line || (startLine == other.line && startCol <= other.column))
          && (endLine > other.line || (endLine == other.line && endCol >= other.column))
        ) {
          // found a location that contains the given one
          return toLocationAndComments(location)
        }
      }
    }
    // No match found.
    return toLocationAndComments(null)
  }

  private fun toLocationAndComments(location: DescriptorProtos.SourceCodeInfo.Location?): LocationAndComments {
    val loc = if (location == null) baseLoc else baseLoc.at(location.getSpan(0) + 1, location.getSpan(1) + 1)
    var comment = location?.leadingComments
    if (comment.isNullOrBlank()) {
      comment = location?.trailingComments
    }
    return LocationAndComments(comment ?: "", loc)
  }

  private fun makeLocationMap(locationList: List<DescriptorProtos.SourceCodeInfo.Location>): Map<List<Int>, List<DescriptorProtos.SourceCodeInfo.Location>> {
    val locationMap = mutableMapOf<List<Int>, MutableList<DescriptorProtos.SourceCodeInfo.Location>>()
    for (location in locationList) {
      val path = mutableListOf<Int>()
      for (pathElem in location.pathList) {
        path.add(pathElem)
      }
      val locList = locationMap.getOrPut(path) { mutableListOf() }
      locList.add(location)
    }
    return locationMap
  }
}

/**
 * Primarily for associating source information for a specific elements.
 *
 * @param parentSourceInfo source information for the parent element.
 * @param childTag tag of the field in the parent that corresponds with the list of child element.
 * @return zipped list of source information associated with each element in the list.
 */
internal fun <T> List<T>.withSourceInfo(parentSourceInfo: SourceInfo, childTag: Int): List<Pair<SourceInfo, T>> {
  val baseSource = parentSourceInfo.push(childTag)
  val result = mutableListOf<Pair<SourceInfo, T>>()
  for ((index, elem) in withIndex()) {
    val newSource = baseSource.push(index)
    result.add(newSource to elem)
  }
  return result
}
