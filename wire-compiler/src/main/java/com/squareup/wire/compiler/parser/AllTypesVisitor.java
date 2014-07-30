package com.squareup.wire.compiler.parser;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public class AllTypesVisitor {

  private final Collection<ProtoFile> protoFiles;

  public AllTypesVisitor(Collection<ProtoFile> protoFiles) {
    this.protoFiles = protoFiles;
  }

  public void visit() {
    // Seed the type resolution queue with all the top-level types from each proto file.
    Deque<Type> typeQueue = new ArrayDeque<>();
    for (ProtoFile protoFile : protoFiles) {
      typeQueue.addAll(protoFile.getTypes());
      for (ExtendDeclaration extendDeclaration : protoFile.getExtendDeclarations()) {
        visitExtendDeclaration(protoFile, extendDeclaration);
      }
    }

    while (!typeQueue.isEmpty()) {
      Type type = typeQueue.removeFirst();
      if (type instanceof MessageType) {
        visitMessageType((MessageType) type);
      } else if (type instanceof EnumType) {
        visitEnumType((EnumType) type);
      } else {
        throw new RuntimeException("Unknown type " + type.getFullyQualifiedName());
      }
      visitType(type);

      typeQueue.addAll(type.getNestedTypes());
    }
  }

  public void visitExtendDeclaration(ProtoFile protoFile, ExtendDeclaration extendDeclaration) {
  }

  public void visitEnumType(EnumType enumType) {
  }

  public void visitMessageType(MessageType messageType) {
  }

  public void visitType(Type type) {
  }
}
