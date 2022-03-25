package parse;

public class ImportedFunction implements WasmFunction {

    private final FnType fntype;

    private KindName kindName;
    
    public ImportedFunction(FnType fntype,KindName kindName) {
        this.fntype = fntype;
        this.kindName = kindName;
    }

    @Override
    public FnType getFnType() {
        return fntype;
    }

    @Override
    public KindName getKindName() {
        return kindName;
    }

    @Override
    public void exportNames(String module_name,String field_name) {
        kindName = kindName.exportNames(module_name, field_name);
    }

   @Override
    public String toString() {
        return String.format("Function %s %s %s",
                kindName.getModuleName(), kindName.getFieldName(), fntype);
    }

}
