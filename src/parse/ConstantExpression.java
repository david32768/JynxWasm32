package parse;

import java.util.ArrayList;
import wasm.Instruction;
import wasm.ObjectInstruction;
import wasm.OpCode;
import wasm.OpType;

public class ConstantExpression {

    private final Instruction constinst;

    public ConstantExpression(Instruction constinst) {
        this.constinst = constinst;
    }
    
    public boolean usesGlobal() {
        return constinst.getOpCode() == OpCode.GLOBAL_GET;
    }
    
    public Number evalConstant()  {
        OpCode opcode = constinst.getOpCode();
        if (opcode == OpCode.GLOBAL_GET) {
            assert false;
            return null;
        }
        assert opcode.getOpType() == OpType.CONST;
        return constinst.getImm1();
    }
        
    public Instruction getConstInst() {
        return constinst;
    }
    
    public String getConstantString()  {
        OpCode opcode = constinst.getOpCode();
        if (opcode == OpCode.GLOBAL_GET) {
            return String.format("(%s)",constinst);
        }
        assert opcode.getOpType() == OpType.CONST;
        return constinst.getImm1().toString();
    }
        
    public static ConstantExpression parseConstantExpression(WasmModule module, Section section)  {
        TypeStack typestack = new TypeStack(FnType.produce(ValueType.X32), new ArrayList<>(), section, module);
        ArrayList<Instruction> insts = new ArrayList<>();
        Op op = section.getop();
        Instruction inst = op.getInstruction(typestack);
        insts.add(inst);
        while (op.getOpCode() != OpCode.END) {
            op = section.getop();
            inst = op.getInstruction(typestack);
            insts.add(inst);
        }
        if (insts.size() != 2) {
            throw new IllegalArgumentException("Invalid initial expression - length not 2 but is " + insts.size());
        }
        Instruction constinst = insts.get(0);
        OpCode opcode = constinst.getOpCode();
        if (opcode == OpCode.GLOBAL_GET) {
            ObjectInstruction objinst = (ObjectInstruction)constinst;
            Global global = (Global)objinst.getObject();
            if (!global.isImported()) {
                String msg = String.format("global used in a constant expressio must be imported: %s",global);
                throw new IllegalArgumentException(msg);
            }
        } else if (opcode.getOpType() != OpType.CONST) {
            String msg = String.format("Invalid initial expression - not constant expression instruction but is %s", opcode);
            throw new IllegalArgumentException(msg);
        }
        return new ConstantExpression(constinst);
    }

}
