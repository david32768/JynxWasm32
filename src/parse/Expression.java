package parse;

import java.util.ArrayList;
import wasm.Instruction;
import wasm.ObjectInstruction;
import wasm.OpCode;
import wasm.OpType;

public class Expression {

    public static Number parseConstant(WasmModule module, Section section)  {
        Instruction constinst = parseInstruction(module, section);
        return evalConstant(constinst);
    }
        
    public static Number evalConstant(Instruction constinst)  {
        OpCode opcode = constinst.getOpCode();
        if (opcode == OpCode.GLOBAL_GET) {
            ObjectInstruction objinst = (ObjectInstruction)constinst;
            Global global = (Global)objinst.getObject();
            return global.getValue();
        }
        assert opcode.getOpType() == OpType.CONST;
        return constinst.getImm1();
    }
        
    public static Instruction parseInstruction(WasmModule module, Section section)  {
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
        if (opcode != OpCode.GLOBAL_GET && opcode.getOpType() != OpType.CONST) {
            throw new IllegalArgumentException("Invalid initial expression - not CONST instruction but is "
                    + opcode);
        }
        return constinst;
    }

}
