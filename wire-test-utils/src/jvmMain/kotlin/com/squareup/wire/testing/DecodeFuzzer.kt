/*
 * Copyright (C) 2026 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.testing

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import java.io.IOException
import java.util.Random
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString

/**
 * Feeds random but mostly well-formed wire bytes to a [ProtoAdapter] and checks the decode
 * contract: decoding may only throw [IOException] (this includes `ProtocolException` and
 * `EOFException`). Any other exception, including a [StackOverflowError], fails the test. When
 * decoding succeeds, the message must also encode again, and the encoded bytes must decode.
 *
 * The input is deterministic. Each input comes from a [Random] seeded with the base seed and the
 * input index, so a failure message with the seed and index is enough to reproduce it.
 *
 * Two environment variables change a run without a code change:
 *
 *  * `WIRE_FUZZ_SCALE` multiplies the number of inputs. Use `50` for a long local run, for example
 *    `WIRE_FUZZ_SCALE=50 ./gradlew :wire-tests:jvm-kotlin-interop:test --rerun --tests '*DecodeFuzzTest'`.
 *    Gradle does not track environment variables, so `--rerun` keeps it from reusing a cached run.
 *  * `WIRE_FUZZ_SEED` replaces the base seed, to explore new inputs.
 *
 * The generator reads the `@WireField` annotations of generated and reflective message classes to
 * pick field numbers and wire types, and sometimes breaks them on purpose. For other adapters, pass
 * [Config.tags] and the generator falls back to a random wire grammar. [Config.seeds] holds valid
 * encoded messages. The fuzzer mutates them, which reaches messages with required fields.
 *
 * Some inputs nest a message field 150 levels or more, and repeat the field at each level. Decoding
 * them must fail, because merged messages share the recursion limit with their parents.
 */
object DecodeFuzzer {
  class Config(
    /** Number of inputs at scale 1. Each input goes through both decode paths. */
    val inputs: Int = 5_000,
    /** Valid encoded messages that the fuzzer mutates. */
    val seeds: List<ByteString> = listOf(),
    /**
     * Field numbers to use besides the ones in the `@WireField` annotations. Use it for unknown
     * fields, extensions, and adapters that have no annotations.
     */
    val tags: List<Int> = (1..20).toList(),
    /**
     * Proto2 `required` fields that are missing on the wire are a documented, intended failure:
     * generated code throws an [IllegalStateException] named "Required field...". Set this to true
     * for message types that have required fields. No other [IllegalStateException] is accepted.
     */
    val missingRequiredFieldsAllowed: Boolean = false,
    /** The base seed. `WIRE_FUZZ_SEED` overrides it. */
    val seed: Long = 1L,
  )

  /** The required fields of `AllTypes` in the shared fixtures, with the tags 101 to 117. */
  val allTypesRequiredFields: ByteString = listOf(
    "a8066f", "b00670", "b806e201", "c50672000000", "cd0673000000", "d00674", "d80675",
    "e006ec01", "e9067700000000000000", "f1067800000000000000", "f80601", "85070000f442",
    "89070000000000c05e40", "920703313234", "9a07027de1", "a00701", "aa070308e707",
  ).joinToString(separator = "").decodeHex()

  class Case(
    val name: String,
    val adapter: ProtoAdapter<*>,
    val config: Config = Config(),
  )

  /** Fuzzes every case, then fails with one [AssertionError] that lists all violations. */
  fun fuzzAll(cases: List<Case>) {
    val messages = cases.mapNotNull { case ->
      try {
        fuzz(case.name, case.adapter, case.config)
        null
      } catch (e: AssertionError) {
        e.message
      }
    }
    if (messages.isNotEmpty()) throw AssertionError(messages.joinToString(separator = "\n\n"))
  }

  /** Fuzzes [adapter], fails with an [AssertionError] on the first contract violation per kind. */
  private fun fuzz(name: String, adapter: ProtoAdapter<*>, config: Config) {
    val scale = System.getenv("WIRE_FUZZ_SCALE")?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
    val baseSeed = System.getenv("WIRE_FUZZ_SEED")?.toLongOrNull() ?: config.seed
    val inputs = (config.inputs * scale).toInt()

    val generator = Generator(adapter, config)
    val failures = LinkedHashMap<String, Failure>()
    var decoded = 0
    val startNanos = System.nanoTime()

    for (index in 0 until inputs) {
      val random = Random(baseSeed * 1_000_003L + index)
      val input = generator.next(random, index)
      val bytes = input.bytes
      for (path in DecodePath.values()) {
        val value = try {
          path.decode(adapter, bytes)
        } catch (e: Throwable) {
          if (!isDocumented(e, config)) {
            failures.record(Failure("decode ${path.label}", e, name, baseSeed, index, bytes))
          }
          continue
        }
        if (input.deeperThanRecursionLimit) {
          val error = AssertionError("decode succeeded, but the message is nested deeper than the recursion limit")
          failures.record(Failure("decode ${path.label}", error, name, baseSeed, index, bytes))
        }
        if (path == DecodePath.Reader32) decoded++
        if (value == null) continue
        try {
          @Suppress("UNCHECKED_CAST")
          val encoded = (adapter as ProtoAdapter<Any>).encode(value)
          adapter.decode(encoded)
        } catch (e: Throwable) {
          failures.record(Failure("encode after ${path.label}", e, name, baseSeed, index, bytes))
        }
      }
    }

    val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000
    println("DecodeFuzzer $name: inputs=$inputs decoded=$decoded elapsedMillis=$elapsedMillis")

    if (failures.isNotEmpty()) {
      throw AssertionError(
        failures.values.joinToString(separator = "\n\n", prefix = "$name\n") { it.describe() },
      )
    }
    if (inputs > 0 && decoded == 0) {
      throw AssertionError("$name: no generated input decoded, so the fuzzer proves nothing")
    }
  }

  private fun isDocumented(e: Throwable, config: Config): Boolean = when {
    e is IOException -> true
    config.missingRequiredFieldsAllowed &&
      e is IllegalStateException &&
      e.message?.startsWith("Required field") == true -> true
    else -> false
  }

  private fun MutableMap<String, Failure>.record(failure: Failure) {
    putIfAbsent(failure.signature, failure)
  }

  private enum class DecodePath(val label: String) {
    /** `decode(ByteArray)` reads with a `ProtoReader32`. */
    Reader32("ProtoReader32") {
      override fun decode(adapter: ProtoAdapter<*>, bytes: ByteArray): Any? = adapter.decode(bytes)
    },

    /** `decode(BufferedSource)` reads with a `ProtoReader`. */
    Reader("ProtoReader") {
      override fun decode(adapter: ProtoAdapter<*>, bytes: ByteArray): Any? = adapter.decode(Buffer().write(bytes))
    },
    ;

    abstract fun decode(adapter: ProtoAdapter<*>, bytes: ByteArray): Any?
  }

  private class Failure(
    val stage: String,
    val error: Throwable,
    val name: String,
    val seed: Long,
    val index: Int,
    val bytes: ByteArray,
  ) {
    val signature: String
      get() {
        val frame = error.stackTrace.firstOrNull { it.className.startsWith("com.squareup.wire") && !it.className.startsWith("com.squareup.wire.testing") }
          ?: error.stackTrace.firstOrNull()
        return "$stage ${error.javaClass.name} at ${frame?.className}.${frame?.methodName}"
      }

    fun describe(): String {
      val hex = bytes.toByteString().hex()
      val shown = if (hex.length > 8192) hex.take(8192) + "... (${bytes.size} bytes in total)" else hex
      return buildString {
        append("$stage threw ${error.javaClass.name}: ${error.message?.take(200)}\n")
        append("  type=$name seed=$seed index=$index (set WIRE_FUZZ_SEED=$seed to repeat it)\n")
        append("  input=$shown\n")
        append(error.stackTrace.take(12).joinToString(separator = "\n") { "    at $it" })
      }
    }
  }

  /** Describes the fields of a message, from its `@WireField` annotations. */
  private class Shape {
    val fields = LinkedHashMap<Int, Field>()
    val entries: List<Map.Entry<Int, Field>> by lazy { fields.entries.toList() }
  }

  /**
   * The [wireType] is 0, 1, 2 or 5. A message or map entry field has a [message] shape. A packed
   * field has wire type 2 and holds elements of wire type [elementWireType].
   */
  private class Field(val wireType: Int, val message: Shape?, val packed: Boolean, val elementWireType: Int)

  /**
   * The bytes of one input. [deeperThanRecursionLimit] is true if the bytes nest messages deeper
   * than the recursion limit, through fields that the adapter reads as messages. Decoding such an
   * input must fail.
   */
  private class Input(val bytes: ByteArray, val deeperThanRecursionLimit: Boolean = false)

  private class Generator(adapter: ProtoAdapter<*>, val config: Config) {
    private val shape: Shape? = adapter.type?.java?.let { shapeOf(it, mutableMapOf()) }?.takeIf { it.fields.isNotEmpty() }
    private val tags: List<Int> = (shape?.fields?.keys?.toList() ?: listOf()) + config.tags

    fun next(random: Random, index: Int): Input {
      // Valid seeds come first, unchanged, so the encode path always runs.
      if (index < config.seeds.size) return Input(config.seeds[index].toByteArray())
      if (shape != null && random.nextInt(40) == 0) return mergeNesting(random, shape)
      return Input(
        when (random.nextInt(10)) {
          0 -> deepNesting(random)
          in 1..3 -> if (config.seeds.isEmpty()) message(random, shape, 0) else mutate(random, config.seeds[random.nextInt(config.seeds.size)].toByteArray())
          else -> message(random, shape, 0)
        },
      )
    }

    private fun message(random: Random, shape: Shape?, depth: Int): ByteArray {
      val buffer = Buffer()
      repeat(random.nextInt(8)) { field(buffer, random, shape, depth) }
      return buffer.readByteArray()
    }

    private fun field(buffer: Buffer, random: Random, shape: Shape?, depth: Int) {
      val known: Map.Entry<Int, Field>? = shape?.entries?.let { entries ->
        if (entries.isEmpty() || random.nextInt(15) == 0) null else entries[random.nextInt(entries.size)]
      }
      val tag = when {
        known != null -> known.key
        random.nextInt(25) == 0 -> random.nextInt(1 shl 20) + 1
        random.nextInt(60) == 0 -> (1 shl 29) - 1
        else -> config.tags[random.nextInt(config.tags.size)]
      }
      var wireType = known?.value?.wireType ?: random.nextInt(6)
      if (known != null && random.nextInt(8) == 0) wireType = random.nextInt(6)
      if (wireType == 4 && random.nextInt(8) != 0) wireType = 2
      varint(buffer, (tag.toLong() shl 3) or wireType.toLong())
      val field = known?.value
      when (wireType) {
        0 -> varint(buffer, varintValue(random))
        1 -> buffer.writeLongLe(
          when (random.nextInt(4)) {
            0 -> 0
            1 -> Long.MAX_VALUE
            2 -> Long.MIN_VALUE
            else -> random.nextLong()
          },
        )
        5 -> buffer.writeIntLe(
          when (random.nextInt(4)) {
            0 -> 0
            1 -> Int.MAX_VALUE
            2 -> Int.MIN_VALUE
            else -> random.nextInt()
          },
        )
        2 -> {
          val payload = payload(random, field, depth)
          val length = if (random.nextInt(12) == 0) {
            when (random.nextInt(4)) {
              0 -> Int.MAX_VALUE.toLong()
              1 -> payload.size + 1L
              2 -> 0xFFFFFFFFL
              else -> (payload.size + random.nextInt(5)).toLong()
            }
          } else {
            payload.size.toLong()
          }
          varint(buffer, length)
          buffer.write(payload)
        }
        3 -> {
          // A group: nested fields, then the matching end tag.
          buffer.write(if (depth < MAX_DEPTH) message(random, null, depth + 1) else ByteArray(0))
          if (random.nextInt(10) != 0) varint(buffer, (tag.toLong() shl 3) or 4L)
        }
        else -> {}
      }
    }

    private fun payload(random: Random, field: Field?, depth: Int): ByteArray {
      if (field != null && field.message != null && random.nextInt(10) != 0) {
        return if (depth < MAX_DEPTH) message(random, field.message, depth + 1) else ByteArray(0)
      }
      if (field != null && field.packed && random.nextInt(10) != 0) {
        return packed(random, field.elementWireType)
      }
      return when (random.nextInt(5)) {
        0 -> ByteArray(random.nextInt(10)) { random.nextInt().toByte() }
        1 -> if (depth < MAX_DEPTH) message(random, null, depth + 1) else ByteArray(0)
        2 -> packed(random, 0)
        3 -> packed(random, 5)
        else -> "hello".repeat(random.nextInt(3)).toByteArray()
      }
    }

    private fun packed(random: Random, wireType: Int): ByteArray {
      val buffer = Buffer()
      repeat(random.nextInt(5)) {
        when (wireType) {
          1 -> buffer.writeLongLe(random.nextLong())
          5 -> buffer.writeIntLe(random.nextInt())
          else -> varint(buffer, varintValue(random))
        }
      }
      if (random.nextInt(12) == 0) buffer.writeByte(random.nextInt(256))
      return buffer.readByteArray()
    }

    private fun varintValue(random: Random): Long = when (random.nextInt(6)) {
      0 -> 0
      1 -> 1
      2 -> -1L
      3 -> Long.MAX_VALUE
      4 -> random.nextLong() ushr random.nextInt(64)
      else -> random.nextInt(300).toLong()
    }

    /** Nests a message or a group thousands of levels deep, to find recursion limits. */
    private fun deepNesting(random: Random): ByteArray {
      val tag = tags[random.nextInt(tags.size)].toLong()
      val levels = intArrayOf(10, 100, 1_000, 5_000)[random.nextInt(4)]
      val buffer = Buffer()
      if (random.nextBoolean()) {
        repeat(levels) { varint(buffer, (tag shl 3) or 3L) }
        repeat(levels) { varint(buffer, (tag shl 3) or 4L) }
        return buffer.readByteArray()
      }
      val sizes = LongArray(levels + 1)
      for (level in 1..levels) {
        val inner = sizes[level - 1]
        sizes[level] = varintSize((tag shl 3) or 2L) + varintSize(inner) + inner
      }
      for (level in levels downTo 1) {
        varint(buffer, (tag shl 3) or 2L)
        varint(buffer, sizes[level - 1])
      }
      return buffer.readByteArray()
    }

    /**
     * Nests messages through message fields of [root], 150 levels or more. Every level holds the
     * same field twice: an empty message, then the message for the next level. Generated code keeps
     * the second occurrence as bytes and merges it into the first one after the last tag. The
     * merged messages must share the recursion limit with their parents. A type that has no
     * message field to follow stops early, and then the input has no expected failure.
     *
     * The merge budget is not tested here. It limits time and memory on much larger inputs than
     * a fuzzer can afford, and unit tests cover it.
     */
    private fun mergeNesting(random: Random, root: Shape): Input {
      val wanted = MERGE_NESTING_LEVELS + random.nextInt(50)
      val tags = ArrayList<Int>()
      var current = root
      while (tags.size < wanted) {
        val choices = current.entries.filter { it.value.message != null }
        if (choices.isEmpty()) break
        val choice = choices[random.nextInt(choices.size)]
        tags += choice.key
        current = choice.value.message!!
      }
      var bytes = ByteArray(0)
      for (tag in tags.asReversed()) {
        val buffer = Buffer()
        varint(buffer, (tag.toLong() shl 3) or 2L)
        varint(buffer, 0L)
        varint(buffer, (tag.toLong() shl 3) or 2L)
        varint(buffer, bytes.size.toLong())
        buffer.write(bytes)
        bytes = buffer.readByteArray()
      }
      return Input(bytes, deeperThanRecursionLimit = tags.size >= MERGE_NESTING_LEVELS)
    }

    private fun mutate(random: Random, seed: ByteArray): ByteArray {
      var bytes = seed
      repeat(1 + random.nextInt(4)) {
        val position = if (bytes.isEmpty()) 0 else random.nextInt(bytes.size)
        bytes = when (random.nextInt(6)) {
          0 -> bytes.copyOf().also { if (it.isNotEmpty()) it[position] = random.nextInt().toByte() }
          1 -> bytes.copyOf().also { if (it.isNotEmpty()) it[position] = (it[position].toInt() xor (1 shl random.nextInt(8))).toByte() }
          2 -> bytes.copyOfRange(0, position)
          3 -> bytes.copyOfRange(0, position) + bytes.copyOfRange(minOf(bytes.size, position + 1 + random.nextInt(8)), bytes.size)
          4 -> bytes.copyOfRange(0, position) + message(random, shape, MAX_DEPTH - 2) + bytes.copyOfRange(position, bytes.size)
          else -> bytes + bytes.copyOfRange(position, minOf(bytes.size, position + 1 + random.nextInt(32)))
        }
      }
      return bytes
    }

    private companion object {
      const val MAX_DEPTH = 6

      /** More than the default recursion limit of 100. */
      const val MERGE_NESTING_LEVELS = 150

      fun varint(buffer: Buffer, value: Long) {
        var v = value
        while (v and 0x7fL.inv() != 0L) {
          buffer.writeByte(((v and 0x7f) or 0x80).toInt())
          v = v ushr 7
        }
        buffer.writeByte(v.toInt())
      }

      fun varintSize(value: Long): Int {
        var v = value
        var size = 1
        while (v and 0x7fL.inv() != 0L) {
          size++
          v = v ushr 7
        }
        return size
      }

      fun shapeOf(type: Class<*>, cache: MutableMap<Class<*>, Shape>): Shape? {
        if (!Message::class.java.isAssignableFrom(type)) return null
        cache[type]?.let { return it }
        val shape = Shape()
        cache[type] = shape
        // The JVM does not fix the order of declared fields. Sort by tag so that a seed generates the same inputs on every JDK.
        val annotations = type.declaredFields.mapNotNull { it.getAnnotation(WireField::class.java) }
        for (annotation in annotations.sortedBy { it.tag }) {
          if (annotation.keyAdapter.isNotEmpty()) {
            val entry = Shape()
            entry.fields[1] = fieldOf(annotation.keyAdapter, false, cache)
            entry.fields[2] = fieldOf(annotation.adapter, false, cache)
            shape.fields[annotation.tag] = Field(2, entry, false, 2)
          } else {
            shape.fields[annotation.tag] = fieldOf(annotation.adapter, annotation.label.isPacked, cache)
          }
        }
        return shape
      }

      /** [name] looks like `com.squareup.wire.ProtoAdapter#INT32` or `com.example.Message#ADAPTER`. */
      fun fieldOf(name: String, packed: Boolean, cache: MutableMap<Class<*>, Shape>): Field {
        val className = name.substringBefore('#')
        val member = name.substringAfter('#')
        if (className == ProtoAdapter::class.java.name) {
          val wireType = when (member) {
            "INT32", "UINT32", "SINT32", "INT64", "UINT64", "SINT64", "BOOL" -> 0
            "FIXED32", "SFIXED32", "FLOAT" -> 5
            "FIXED64", "SFIXED64", "DOUBLE" -> 1
            else -> 2
          }
          return Field(if (packed) 2 else wireType, null, packed, wireType)
        }
        val type = try {
          Class.forName(className)
        } catch (_: ClassNotFoundException) {
          return Field(2, null, false, 2)
        }
        if (WireEnum::class.java.isAssignableFrom(type)) {
          return Field(if (packed) 2 else 0, null, packed, 0)
        }
        return Field(2, shapeOf(type, cache), false, 2)
      }
    }
  }
}
