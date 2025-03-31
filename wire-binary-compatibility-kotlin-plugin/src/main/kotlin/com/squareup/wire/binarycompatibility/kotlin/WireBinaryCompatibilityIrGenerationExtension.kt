/*
 * Copyright (C) 2025 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.squareup.wire.binarycompatibility.kotlin

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class WireConstructorCallRewriter(
  private val pluginContext: IrPluginContext,
  val constructorCall: IrConstructorCall,
) {
  /** Returns the rewrite, or null if we don't want to rewrite this one. */
  @OptIn(FirIncompatiblePluginAPI::class)
  fun rewrite(): IrExpression? {
    // Validate that constructorCall has the shape we want: it's a Wire class
    // Validate that the target class has a nested Builder class
    // Validate that the target class is not within a Builder class

    val messageClassId = constructorCall.type.getClass()?.classId ?: return null
    val builderClassId = messageClassId.createNestedClassId(Name.identifier("Builder"))
    val builderSymbol: IrClassSymbol = pluginContext.referenceClass(builderClassId) ?: return null

    val buildFunction =
      builderSymbol.functions.find { it.owner.valueParameters.isEmpty() && it.owner.name.identifier == "build" }
        ?: return null

    // Create a block
    val bodyBuilder = IrBlockBuilder(
      startOffset = constructorCall.startOffset,
      endOffset = constructorCall.endOffset,
      context = pluginContext,
      scope = Scope(constructorCall.symbol),
    )

    return bodyBuilder.block {
      // First statement:
      //   val builder = Money.Builder()
      val builder = irTemporary(
        value = this.irCallConstructor(
          callee = builderSymbol.constructors.find { it.owner.valueParameters.isEmpty() }!!,
          listOf(),
        ),
        nameHint = "builder",
        isMutable = false,
      ).apply {
        origin = IrDeclarationOrigin.DEFINED
      }

      // builder.amount(5)
      for (i in 0 until constructorCall.valueArguments.size) {
        val valueArgument = constructorCall.valueArguments[i] ?: continue // Skip default parameters
        val valueParameter = constructorCall.symbol.owner.valueParameters[i]
        val parameterFunction = builderSymbol.functions.find { it.owner.valueParameters.size == 1 && it.owner.name == valueParameter.name } ?: return null


        +irCall(
          callee = parameterFunction,
        ).apply {
          this.dispatchReceiver = irGet(builder)
          this.putValueArgument(0, valueArgument)
        }
      }

      //   return builder.build()
      +irCall(
        callee = buildFunction,
      ).apply {
        this.dispatchReceiver = irGet(builder)
      }
    }
  }
}

private val wirePackage = FqName("com.squareup.wire")
private val wireMessage = ClassId(wirePackage, Name.identifier("Message"))
private val wireMessageBuilder = wireMessage.createNestedClassId(Name.identifier("Builder"))
fun IrFunction.isDeclaredByWireMessageOrBuilder(): Boolean {
  return (this.parent as? IrClass)?.superTypes?.any { it.isWireMessageOrBuilder() } ?: false
}

fun IrType.isWireMessageOrBuilder() : Boolean {
  val classId = this.getClass()?.classId ?: return false
  return classId == wireMessage || classId == wireMessageBuilder
}

@UnsafeDuringIrConstructionAPI // To use IrDeclarationContainer.declarations.
class WireBinaryCompatibilityIrGenerationExtension(
  private val messageCollector: MessageCollector,
) : IrGenerationExtension {

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    pluginContext.referenceClass(wireMessage)
      ?: return // Don't do anything if Wire isn't on the classpath. There's no code to rewrite here.

    val transformer = object : IrElementTransformerVoidWithContext() {
      override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        // When we visit any code generated by Wire (subtypes of Message and Message.Builder), do not rewrite this
        // for binary compatibility! Otherwise, we'll recurse forever and stack overflow. We don't need to worry about
        // this case in practice because Wire-generated code is by definition binary-compatible with itself.
        if (declaration.isDeclaredByWireMessageOrBuilder()) return declaration

        return super.visitFunctionNew(declaration)
      }

      override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructorCall = super.visitConstructorCall(expression)
        if (constructorCall !is IrConstructorCall) return constructorCall
        val rewrite = WireConstructorCallRewriter(pluginContext, constructorCall).rewrite()
        return rewrite ?: constructorCall
      }
    }

    moduleFragment.transform(transformer, null)

    moduleFragment.patchDeclarationParents()
  }
}
