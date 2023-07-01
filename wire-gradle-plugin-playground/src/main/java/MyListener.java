import com.squareup.wire.schema.EmittingRules;
import com.squareup.wire.schema.EventListener;
import com.squareup.wire.schema.PruningRules;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaHandler;
import com.squareup.wire.schema.WireRun;
import com.squareup.wire.schema.internal.TypeMover;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MyListener extends EventListener {
  @Override public void runStart(@NotNull WireRun wireRun) {
    super.runStart(wireRun);
  }

  @Override public void runSuccess(@NotNull WireRun wireRun) {
    super.runSuccess(wireRun);
  }

  @Override public void runFailed(@NotNull List<String> errors) {
    super.runFailed(errors);
  }

  @Override public void loadSchemaStart() {
    super.loadSchemaStart();
  }

  @Override public void loadSchemaSuccess(@NotNull Schema schema) {
    super.loadSchemaSuccess(schema);
  }

  @Override public void treeShakeStart(@NotNull Schema schema, @NotNull PruningRules pruningRules) {
    super.treeShakeStart(schema, pruningRules);
  }

  @Override
  public void treeShakeEnd(@NotNull Schema refactoredSchema, @NotNull PruningRules pruningRules) {
    super.treeShakeEnd(refactoredSchema, pruningRules);
  }

  @Override
  public void moveTypesStart(@NotNull Schema schema, @NotNull List<TypeMover.Move> moves) {
    super.moveTypesStart(schema, moves);
  }

  @Override
  public void moveTypesEnd(@NotNull Schema refactoredSchema, @NotNull List<TypeMover.Move> moves) {
    super.moveTypesEnd(refactoredSchema, moves);
  }

  @Override public void schemaHandlersStart() {
    super.schemaHandlersStart();
  }

  @Override public void schemaHandlersEnd() {
    super.schemaHandlersEnd();
  }

  @Override public void schemaHandlerStart(@NotNull SchemaHandler schemaHandler,
      @NotNull EmittingRules emittingRules) {
    super.schemaHandlerStart(schemaHandler, emittingRules);
  }

  @Override public void schemaHandlerEnd(@NotNull SchemaHandler schemaHandler,
      @NotNull EmittingRules emittingRules) {
    super.schemaHandlerEnd(schemaHandler, emittingRules);
  }
}
