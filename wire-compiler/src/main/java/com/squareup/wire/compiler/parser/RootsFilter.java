package com.squareup.wire.compiler.parser;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Service;
import com.squareup.protoparser.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.protoparser.ScalarTypes.isScalarType;

/**
 * Filter a set of proto file objects to only include the specified types and their transitive
 * dependencies.
 * <p>
 * This is done in three steps:
 * <ol>
 * <li>Decompose the proto file object hierarchy into a tree of nodes that represents each type and
 * service.</li>
 * <li>For each root type, find its corresponding node and mark it to be kept. Doing so will also
 * mark all of its parent types (if any) and its dependencies (if any) to be kept as well.</li>
 * <li>Re-assemble only the kept nodes back into proto file objects.</li>
 * </ol>
 */
final class RootsFilter {

  public static final String EXTEND = "extend@";

  /** Filter the protos to only include the specified types and their transitive dependencies. */
  public static Set<ProtoFile> filter(Set<ProtoFile> protoFiles, Set<String> roots) {
    Set<String> kept = new LinkedHashSet<>();

    //roots = new LinkedHashSet<>(roots);
    //
    //// Preserve extensions used by options.
    //roots.add("google.protobuf.EnumOptions");
    //roots.add("google.protobuf.FieldOptions");
    //roots.add("google.protobuf.MessageOptions");
    //roots.add("google.protobuf.EnumValueOptions");
    //roots.add(EXTEND + "google.protobuf.EnumOptions");
    //roots.add(EXTEND + "google.protobuf.FieldOptions");
    //roots.add(EXTEND + "google.protobuf.MessageOptions");
    //roots.add(EXTEND + "google.protobuf.EnumValueOptions");

    Set<String> serviceMethodRoots = new LinkedHashSet<>();
    for (String root : roots) {
      if (root.contains("#")) {
        serviceMethodRoots.add(root);
      }
    }

    // Transform the set of proto files into a tree of nodes.
    RootNode rootNode = new RootNode(protoFiles, serviceMethodRoots);
    Map<String, Node<?>> nodeMap = rootNode.asNodeMap();

    // Collect nodes to keep by starting at the supplied roots and transitively iterating out.
    Set<Node<?>> typesToKeep = new LinkedHashSet<>();
    for (String root : roots) {
      int hashIndex = root.indexOf('#');
      if (hashIndex != -1) {
        root = root.substring(0, hashIndex);
      }
      if (!nodeMap.containsKey(root)) {
        throw new IllegalStateException("Unknown type " + root);
      }
      keep(root, typesToKeep, nodeMap, kept);
    }

    // Re-assemble all of the marked nodes back into a set of proto files.
    return rootNode.collectKeptNodes(typesToKeep);
  }

  private static Node<?> nodeForType(Node<?> parent, Type type) {
    if (type instanceof MessageType) {
      return new MessageTypeNode(parent, (MessageType) type);
    }
    if (type instanceof EnumType) {
      return new EnumTypeNode(parent, (EnumType) type);
    }
    throw new IllegalArgumentException("Unknown type " + type.getClass().getCanonicalName());
  }

  private abstract static class Node<T> {
    final Node<?> parent;
    final String type;
    final T obj;
    final List<Node<?>> children;

    Node(Node<?> parent, String type, T obj) {
      this.parent = parent;
      this.type = type;
      this.obj = obj;

      children = new ArrayList<>();
    }

    /** Flatten this type and the types of any children into a map to their corresponding nodes. */
    final Map<String, Node<?>> asNodeMap() {
      Map<String, Node<?>> typeMap = new LinkedHashMap<>();
      if (type != null) {
        typeMap.put(type, this);
      }
      for (Node<?> child : children) {
        typeMap.putAll(child.asNodeMap());
      }
      return typeMap;
    }

    /** Create a real proto object of this type and any children present in the supplied set. */
    abstract T collectKeptNodes(Set<Node<?>> typesToKeep);

    /** Mark this node to be kept. This method should be overriden to keep any dependencies. */
    void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap, Set<String> kept) {
      if (typesToKeep.contains(this)) {
        return;
      }
      typesToKeep.add(this);
      if (parent != null) {
        parent.keepNodes(typesToKeep, nodeMap, kept);
      }
    }
  }

  /** The root node which represents set of {@link ProtoFile} objects. */
  private static class RootNode extends Node<Set<ProtoFile>> {
    RootNode(Set<ProtoFile> protoFiles, Set<String> serviceMethodRoots) {
      super(null, null, protoFiles);

      for (ProtoFile protoFile : protoFiles) {
        children.add(new ProtoFileNode(this, protoFile, serviceMethodRoots));
      }
    }

    @Override public Set<ProtoFile> collectKeptNodes(Set<Node<?>> typesToKeep) {
      Set<ProtoFile> protoFiles = new LinkedHashSet<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          protoFiles.add((ProtoFile) child.collectKeptNodes(typesToKeep));
        }
      }
      return protoFiles;
    }
  }

  private static class ProtoFileNode extends Node<ProtoFile> {
    ProtoFileNode(RootNode parent, ProtoFile protoFile, Set<String> serviceMethodRoots) {
      super(parent, null, protoFile);

      for (Type type : protoFile.getTypes()) {
        children.add(nodeForType(this, type));
      }
      for (Service service : protoFile.getServices()) {
        if (shouldEmitService(serviceMethodRoots, service.getFullyQualifiedName())) {
          children.add(new ServiceNode(this, service, serviceMethodRoots));
        }
      }
      for (ExtendDeclaration extendDeclaration : protoFile.getExtendDeclarations()) {
        children.add(new ExtendDeclarationNode(this, extendDeclaration));
      }
    }

    @Override ProtoFile collectKeptNodes(Set<Node<?>> typesToKeep) {
      List<Type> markedTypes = new ArrayList<>();
      List<Service> markedServices = new ArrayList<>();
      List<ExtendDeclaration> markedExtendDeclarations = new ArrayList<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          if (child instanceof ServiceNode) {
            markedServices.add((Service) child.collectKeptNodes(typesToKeep));
          } else if (child instanceof ExtendDeclarationNode) {
            markedExtendDeclarations.add((ExtendDeclaration) child.collectKeptNodes(typesToKeep));
          } else if (child instanceof MessageTypeNode || child instanceof EnumTypeNode) {
            markedTypes.add((Type) child.collectKeptNodes(typesToKeep));
          } else {
            throw new RuntimeException("Unknown child type " + child.getClass().getName());
          }
        } else {
          child = child;
        }
      }
      return new ProtoFile(obj.getFileName(), obj.getPackageName(), obj.getDependencies(),
          obj.getPublicDependencies(), markedTypes, markedServices, obj.getOptions(),
          markedExtendDeclarations);
    }

    private boolean shouldEmitService(Set<String> serviceMethodRoots, String fullyQualifiedName) {
      if (serviceMethodRoots.isEmpty()) {
        return true;
      }
      for (String serviceMethodRoot : serviceMethodRoots) {
        if (serviceMethodRoot.equals(fullyQualifiedName) ||
            serviceMethodRoot.startsWith(fullyQualifiedName + "#")) {
          return true;
        }
      }
      return false;
    }
  }

  private static void keep(String type, Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap,
      Set<String> kept) {
    if (isScalarType(type)) {
      return;
    }
    if (!kept.contains(type)) {
      kept.add(type);
      System.out.println("keep " + type);
      nodeMap.get(type).keepNodes(typesToKeep, nodeMap, kept);
    }
    if (!kept.contains(EXTEND + type) && nodeMap.containsKey(EXTEND + type)) {
      kept.add(EXTEND + type);
      System.out.println("keep " + EXTEND + type);
      nodeMap.get(EXTEND + type).keepNodes(typesToKeep, nodeMap, kept);
    }
  }

  private static class ServiceNode extends Node<Service> {
    private final Set<String> serviceMethodRoots;

    ServiceNode(Node<?> parent, Service type, Set<String> serviceMethodRoots) {
      super(parent, type.getFullyQualifiedName(), type);
      this.serviceMethodRoots = serviceMethodRoots;
    }

    @Override void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap,
        Set<String> kept) {
      super.keepNodes(typesToKeep, nodeMap, kept);

      for (Service.Method method : obj.getMethods()) {
        if (serviceMethodRoots.isEmpty() ||
            serviceMethodRoots.contains(type) ||
            serviceMethodRoots.contains(type + "#" + method.getName())) {
          keep(method.getRequestType(), typesToKeep, nodeMap, kept);
          keep(method.getResponseType(), typesToKeep, nodeMap, kept);
        }
      }
    }

    @Override Service collectKeptNodes(Set<Node<?>> typesToKeep) {
      List<Service.Method> methodsToKeep = new ArrayList<>();
      for (Service.Method method : obj.getMethods()) {
        if (serviceMethodRoots.isEmpty() ||
            serviceMethodRoots.contains(type) ||
            serviceMethodRoots.contains(type + "#" + method.getName())) {
          methodsToKeep.add(method);
        }
      }
      return new Service(obj.getName(), obj.getFullyQualifiedName(),
          obj.getDocumentation(), obj.getOptions(), methodsToKeep);
    }
  }

  private static class ExtendDeclarationNode extends Node<ExtendDeclaration> {
    ExtendDeclarationNode(Node<?> parent, ExtendDeclaration extendDeclaration) {
      super(parent, EXTEND + extendDeclaration.getFullyQualifiedName(), extendDeclaration);
    }

    @Override void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap,
        Set<String> kept) {
      super.keepNodes(typesToKeep, nodeMap, kept);
      for (MessageType.Field extensionField : obj.getFields()) {
        keep(extensionField.getType(), typesToKeep, nodeMap, kept);
      }
    }

    @Override ExtendDeclaration collectKeptNodes(Set<Node<?>> typesToKeep) {
      return obj;
    }
  }

  private static class MessageTypeNode extends Node<MessageType> {
    MessageTypeNode(Node<?> parent, MessageType type) {
      super(parent, type.getFullyQualifiedName(), type);

      for (Type nestedType : type.getNestedTypes()) {
        children.add(nodeForType(this, nestedType));
      }
    }

    @Override void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap,
        Set<String> kept) {
      super.keepNodes(typesToKeep, nodeMap, kept);

      for (MessageType.Field field : obj.getFields()) {
        keep(field.getType(), typesToKeep, nodeMap, kept);
      }
    }

    @Override MessageType collectKeptNodes(Set<Node<?>> typesToKeep) {
      List<Type> markedNestedTypes = new ArrayList<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          markedNestedTypes.add((Type) child.collectKeptNodes(typesToKeep));
        }
      }
      return new MessageType(obj.getName(), obj.getFullyQualifiedName(), obj.getDocumentation(),
          obj.getFields(), markedNestedTypes, obj.getExtensions(), obj.getOptions());
    }
  }

  private static class EnumTypeNode extends Node<EnumType> {
    EnumTypeNode(Node<?> parent, EnumType type) {
      super(parent, type.getFullyQualifiedName(), type);
    }

    @Override EnumType collectKeptNodes(Set<Node<?>> typesToKeep) {
      return obj; // No child types that could possibly be filtered. Return the original.
    }
  }


  private RootsFilter() {
    throw new AssertionError("No instances.");
  }
}
