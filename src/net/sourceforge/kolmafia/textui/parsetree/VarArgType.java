package net.sourceforge.kolmafia.textui.parsetree;

import org.eclipse.lsp4j.Location;

public class VarArgType extends AggregateType {
  public VarArgType(final Type dataType) {
    this(dataType, null);
  }

  public VarArgType(final Type dataType, final Location location) {
    super("vararg", dataType, 0, location);
  }

  @Override
  public String toString() {
    return this.dataType.toString() + " ...";
  }

  @Override
  public VarArgType reference(final Location location) {
    return new VarArgTypeReference(location);
  }

  private class VarArgTypeReference extends VarArgType {
    public VarArgTypeReference(final Location location) {
      super(VarArgType.this.dataType, location);
    }

    @Override
    public Location getDefinitionLocation() {
      return VarArgType.this.getDefinitionLocation();
    }
  }
}
