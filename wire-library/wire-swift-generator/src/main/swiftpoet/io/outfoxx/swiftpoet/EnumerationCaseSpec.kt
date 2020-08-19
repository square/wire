package io.outfoxx.swiftpoet

class EnumerationCaseSpec private constructor(
  builder: Builder
) : AttributedSpec(builder.attributes) {

  val name = builder.name
  val typeOrConstant = builder.typeOrConstant
  val kdoc = builder.kdoc.build()

  fun toBuilder(): Builder {
    val builder = Builder(name, typeOrConstant)
    builder.kdoc.add(kdoc)
    builder.attributes += attributes
    return builder
  }

  internal fun emit(codeWriter: CodeWriter) {

    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAttributes(attributes)
    codeWriter.emitCode("case %L", escapeIfKeyword(name))
    when (typeOrConstant) {
      null -> {}
      is CodeBlock -> codeWriter.emitCode(" = %L", typeOrConstant)
      is TupleTypeName -> typeOrConstant.emit(codeWriter)
      else -> throw IllegalStateException("Invalid enum type of constant")
    }
  }

  class Builder internal constructor(
    internal var name: String,
    internal var typeOrConstant: Any?
  ) {

    internal val attributes = mutableListOf<AttributeSpec>()
    internal val kdoc = CodeBlock.builder()

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAttribute(attribute: AttributeSpec) = apply {
      this.attributes += attribute
    }

    fun addAttribute(name: String, vararg arguments: String) = apply {
      this.attributes += AttributeSpec.builder(name).addArguments(arguments.toList()).build()
    }

    fun build(): EnumerationCaseSpec {
      return EnumerationCaseSpec(this)
    }
  }

  companion object {
    @JvmStatic fun builder(name: String) = Builder(name, null)

    @JvmStatic fun builder(name: String, type: TypeName) = Builder(name, TupleTypeName.of("" to type))

    @JvmStatic fun builder(name: String, type: TupleTypeName) = Builder(name, type)

    @JvmStatic fun builder(name: String, constant: CodeBlock) = Builder(name, constant)

    @JvmStatic fun builder(name: String, constant: String) = Builder(name, CodeBlock.of("%S", constant))

    @JvmStatic fun builder(name: String, constant: Int) = Builder(name, CodeBlock.of("%L", constant.toString()))
  }

}
