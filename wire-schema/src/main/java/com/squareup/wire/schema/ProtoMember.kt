/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema;

/**
 * Identifies a field, enum or RPC on a declaring type. Members are encoded as strings containing a
 * type name, a hash, and a member name, like {@code squareup.dinosaurs.Dinosaur#length_meters}.
 *
 * <p>A member's name is typically a simple name like "length_meters" or "packed". If the member
 * field is an extension to its type, that name is prefixed with its enclosing package. This yields
 * a member name with two packages, like {@code google.protobuf.FieldOptions#squareup.units.unit}.
 */
public final class ProtoMember {
  private final ProtoType type;
  private final String member;

  private ProtoMember(ProtoType type, String member) {
    this.type = type;
    this.member = member;
  }

  public static ProtoMember get(String typeAndMember) {
    int hash = typeAndMember.indexOf('#');
    if (hash == -1) throw new IllegalArgumentException("expected a '#' in " + typeAndMember);
    ProtoType type = ProtoType.get(typeAndMember.substring(0, hash));
    String member = typeAndMember.substring(hash + 1);
    return new ProtoMember(type, member);
  }

  public static ProtoMember get(ProtoType type, String member) {
    return new ProtoMember(type, member);
  }

  public static ProtoMember get(ProtoType type, Field field) {
    String member = field.isExtension() ? field.qualifiedName() : field.name();
    return new ProtoMember(type, member);
  }

  public ProtoType type() {
    return type;
  }

  public String member() {
    return member;
  }

  @Override public boolean equals(Object o) {
    return o instanceof ProtoMember
        && type.equals(((ProtoMember) o).type)
        && member.equals(((ProtoMember) o).member);
  }

  @Override public int hashCode() {
    return type.hashCode() * 37 + member.hashCode();
  }

  @Override public String toString() {
    return type + "#" + member;
  }
}
