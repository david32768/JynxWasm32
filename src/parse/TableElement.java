package parse;

public class TableElement {

    private final ConstantExpression constexpr;
    private final WasmFunction[] functions;

    public TableElement(ConstantExpression constexpr, WasmFunction[] functions) {
        this.constexpr = constexpr;
        this.functions = functions;
    }

    public ConstantExpression getConstExpr() {
        return constexpr;
    }

    public WasmFunction[] getFunctions() {
        return functions;
    }

    
}
