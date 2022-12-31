package wasm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static parse.ValueType.*;
import static wasm.OpType.*;

import parse.ValueType;

public enum OpCode {

    // control flow
    UNREACHABLE(0x00,CONTROL),
    NOP(0x01,PARAMETRIC),
    BLOCK(0x02,CONTROL),
    LOOP(0x03,CONTROL),
    IF(0x04,CONTROL),
    ELSE(0x05,CONTROL),

    END(0x0b,CONTROL),
    BR(0x0c,BRANCH),
    BR_IF(0x0d, BRANCH),
    BR_TABLE(0x0e, BRANCH_TABLE),
    RETURN(0x0f, CONTROL),

    CALL(0x10,VARIABLE),
    CALL_INDIRECT(0x11, VARIABLE),

    // parametric operators
    DROP(0x1a, PARAMETRIC),
    SELECT(0x1b,PARAMETRIC),

    // variable access
    LOCAL_GET(0x20, VARIABLE), // pushes value on stack
    LOCAL_SET(0x21, VARIABLE), // pops value off stack
    LOCAL_TEE(0x22, VARIABLE), // pops and pushes value on stack

    GLOBAL_GET(0x23, VARIABLE), // pushes object on stack
    GLOBAL_SET(0x24, VARIABLE), // pops object off stack

    // memory related
    I32_LOAD(0x28, MEMLOAD),
    I64_LOAD(0x29, MEMLOAD),
    F32_LOAD(0x2a, MEMLOAD),
    F64_LOAD(0x2b , MEMLOAD),

    I32_LOAD8_S(0x2c, MEMLOAD),
    I32_LOAD8_U(0x2d, MEMLOAD),
    I32_LOAD16_S(0x2e, MEMLOAD),
    I32_LOAD16_U(0x2f, MEMLOAD),

    I64_LOAD8_S(0x30, MEMLOAD),
    I64_LOAD8_U(0x31, MEMLOAD),
    I64_LOAD16_S(0x32, MEMLOAD),
    I64_LOAD16_U(0x33, MEMLOAD),
    I64_LOAD32_S(0x34, MEMLOAD),
    I64_LOAD32_U(0x35, MEMLOAD),

    I32_STORE(0x36, MEMSTORE),
    I64_STORE(0x37, MEMSTORE),
    F32_STORE(0x38, MEMSTORE),
    F64_STORE(0x39, MEMSTORE),

    I32_STORE8(0x3a, MEMSTORE),
    I32_STORE16(0x3b, MEMSTORE),
    I64_STORE8(0x3c, MEMSTORE),
    I64_STORE16(0x3d, MEMSTORE),
    I64_STORE32(0x3e, MEMSTORE),

    MEMORY_SIZE(0x3f, MEMFN),
    MEMORY_GROW(0x40, MEMFN),
    MEMORY_COPY(0xfc000a,MEMFN),
    MEMORY_FILL(0xfc000b,MEMFN),
    // constants
    I32_CONST(0x41, CONST),
    I64_CONST(0x42, CONST),
    F32_CONST(0x43, CONST),
    F64_CONST(0x44, CONST),

    // comparison operators
    I32_EQZ(0x45, COMPARE),
    I32_EQ(0x46, COMPARE),
    I32_NE(0x47, COMPARE),
    I32_LT_S(0x48, COMPARE),
    I32_LT_U(0x49, COMPARE),
    I32_GT_S(0x4a, COMPARE),
    I32_GT_U(0x4b, COMPARE),
    I32_LE_S(0x4c, COMPARE),
    I32_LE_U(0x4d, COMPARE),
    I32_GE_S(0x4e, COMPARE),
    I32_GE_U(0x4f, COMPARE),

    I64_EQZ(0x50, COMPARE),
    I64_EQ(0x51, COMPARE),
    I64_NE(0x52, COMPARE),
    I64_LT_S(0x53, COMPARE),
    I64_LT_U(0x54, COMPARE),
    I64_GT_S(0x55, COMPARE),
    I64_GT_U(0x56, COMPARE),
    I64_LE_S(0x57, COMPARE),
    I64_LE_U(0x58, COMPARE),
    I64_GE_S(0x59, COMPARE),
    I64_GE_U(0x5a, COMPARE),

    F32_EQ(0x5b, COMPARE),
    F32_NE(0x5c, COMPARE),
    F32_LT(0x5d, COMPARE),
    F32_GT(0x5e, COMPARE),
    F32_LE(0x5f, COMPARE),
    F32_GE(0x60, COMPARE),

    F64_EQ(0x61, COMPARE),
    F64_NE(0x62, COMPARE),
    F64_LT(0x63, COMPARE),
    F64_GT(0x64, COMPARE),
    F64_LE(0x65, COMPARE),
    F64_GE(0x66, COMPARE),
    
    // numeric operators
    I32_CLZ(0x67, UNARY),
    I32_CTZ(0x68, UNARY),
    I32_POPCNT(0x69, UNARY),

    I32_ADD(0x6a, BINARY),
    I32_SUB(0x6b, BINARY),
    I32_MUL(0x6c, BINARY),
    I32_DIV_S(0x6d, BINARY),
    I32_DIV_U(0x6e, BINARY),
    I32_REM_S(0x6f, BINARY),
    I32_REM_U(0x70, BINARY),
    
    I32_AND(0x71, BINARY),
    I32_OR(0x72, BINARY),
    I32_XOR(0x73, BINARY),
    
    I32_SHL(0x74, BINARY),
    I32_SHR_S(0x75, BINARY),
    I32_SHR_U(0x76, BINARY),
    I32_ROTL(0x77, BINARY),
    I32_ROTR(0x78, BINARY),
    
    I64_CLZ(0x79, UNARY),
    I64_CTZ(0x7a, UNARY),
    I64_POPCNT(0x7b, UNARY),

    I64_ADD(0x7c, BINARY),
    I64_SUB(0x7d, BINARY),
    I64_MUL(0x7e, BINARY),
    I64_DIV_S(0x7f, BINARY),
    I64_DIV_U(0x80, BINARY),
    I64_REM_S(0x81, BINARY),
    I64_REM_U(0x82, BINARY),
    
    I64_AND(0x83, BINARY),
    I64_OR(0x84, BINARY),
    I64_XOR(0x85, BINARY),
    
    I64_SHL(0x86, BINARY),
    I64_SHR_S(0x87, BINARY),
    I64_SHR_U(0x88, BINARY),
    I64_ROTL(0x89, BINARY),
    I64_ROTR(0x8a, BINARY),
    
    F32_ABS(0x8b, UNARY),
    F32_NEG(0x8c, UNARY),
    F32_CEIL(0x8d, UNARY),
    F32_FLOOR(0x8e, UNARY),
    F32_TRUNC(0x8f, UNARY),
    F32_NEAREST(0x90, UNARY),
    F32_SQRT(0x91, UNARY),

    F32_ADD(0x92, BINARY),
    F32_SUB(0x93, BINARY),
    F32_MUL(0x94, BINARY),
    F32_DIV(0x95, BINARY),
    F32_MIN(0x96, BINARY),
    F32_MAX(0x97, BINARY),
    F32_COPYSIGN(0x98, BINARY),

    F64_ABS(0x99, UNARY),
    F64_NEG(0x9a, UNARY),
    F64_CEIL(0x9b, UNARY),
    F64_FLOOR(0x9c,UNARY),
    F64_TRUNC(0x9d, UNARY),
    F64_NEAREST(0x9e, UNARY),
    F64_SQRT(0x9f, UNARY),

    F64_ADD(0xa0, BINARY),
    F64_SUB(0xa1, BINARY),
    F64_MUL(0xa2, BINARY),
    F64_DIV(0xa3, BINARY),
    F64_MIN(0xa4, BINARY),
    F64_MAX(0xa5, BINARY),
    F64_COPYSIGN(0xa6, BINARY),
    
    // conversions
    I32_WRAP_I64(0xa7, TRANSFORM),
    I32_TRUNC_S_F32(0xa8, TRANSFORM),
    I32_TRUNC_U_F32(0xa9, TRANSFORM),
    I32_TRUNC_S_F64(0xaa, TRANSFORM),
    I32_TRUNC_U_F64(0xab, TRANSFORM),
    I32_TRUNC_SAT_S_F32(0xfc0000, TRANSFORM),
    I32_TRUNC_SAT_U_F32(0xfc0001, TRANSFORM),
    I32_TRUNC_SAT_S_F64(0xfc0002, TRANSFORM),
    I32_TRUNC_SAT_U_F64(0xfc0003, TRANSFORM),

    I64_EXTEND_S_I32(0xac, TRANSFORM),
    I64_EXTEND_U_I32(0xad, TRANSFORM),
    I64_TRUNC_S_F32(0xae, TRANSFORM),
    I64_TRUNC_U_F32(0xaf, TRANSFORM),
    I64_TRUNC_S_F64(0xb0, TRANSFORM),
    I64_TRUNC_U_F64(0xb1, TRANSFORM),
    I64_TRUNC_SAT_S_F32(0xfc0004, TRANSFORM),
    I64_TRUNC_SAT_U_F32(0xfc0005, TRANSFORM),
    I64_TRUNC_SAT_S_F64(0xfc0006, TRANSFORM),
    I64_TRUNC_SAT_U_F64(0xfc0007, TRANSFORM),

    F32_CONVERT_S_I32(0xb2, TRANSFORM),
    F32_CONVERT_U_I32(0xb3, TRANSFORM),
    F32_CONVERT_S_I64(0xb4, TRANSFORM),
    F32_CONVERT_U_I64(0xb5, TRANSFORM),
    F32_DEMOTE_F64(0xb6, TRANSFORM),
    
    F64_CONVERT_S_I32(0xb7, TRANSFORM),
    F64_CONVERT_U_I32(0xb8, TRANSFORM),
    F64_CONVERT_S_I64(0xb9, TRANSFORM),
    F64_CONVERT_U_I64(0xba, TRANSFORM),
    F64_PROMOTE_F32(0xbb, TRANSFORM),
    // reinterpret
    I32_REINTERPRET_F32(0xbc, TRANSFORM),
    I64_REINTERPRET_F64(0xbd, TRANSFORM),
    F32_REINTERPRET_I32(0xbe, TRANSFORM),
    F64_REINTERPRET_I64(0xbf, TRANSFORM),

    // sign extension 2.0
    I32_EXTEND8_S(0xc0,UNARY),
    I32_EXTEND16_S(0xc1,UNARY),
    I64_EXTEND8_S(0xc2,UNARY),
    I64_EXTEND16_S(0xc3,UNARY),
    I64_EXTEND32_S(0xc4,UNARY),

    // extensions
        // optimized comparison operators
    I32_IFEQZ(0x4504, COMPAREIF),
    I32_IFEQ(0x4604, COMPAREIF),
    I32_IFNE(0x4704, COMPAREIF),
    I32_IFLT_S(0x4804, COMPAREIF),
    I32_IFLT_U(0x4904, COMPAREIF),
    I32_IFGT_S(0x4a04, COMPAREIF),
    I32_IFGT_U(0x4b04, COMPAREIF),
    I32_IFLE_S(0x4c04, COMPAREIF),
    I32_IFLE_U(0x4d04, COMPAREIF),
    I32_IFGE_S(0x4e04, COMPAREIF),
    I32_IFGE_U(0x4f04, COMPAREIF),

    I64_IFEQZ(0x5004, COMPAREIF),
    I64_IFEQ(0x5104, COMPAREIF),
    I64_IFNE(0x5204, COMPAREIF),
    I64_IFLT_S(0x5304, COMPAREIF),
    I64_IFLT_U(0x5404, COMPAREIF),
    I64_IFGT_S(0x5504, COMPAREIF),
    I64_IFGT_U(0x5604, COMPAREIF),
    I64_IFLE_S(0x5704, COMPAREIF),
    I64_IFLE_U(0x5804, COMPAREIF),
    I64_IFGE_S(0x5904, COMPAREIF),
    I64_IFGE_IFU(0x5a04, COMPAREIF),

    F32_IFEQ(0x5b04, COMPAREIF),
    F32_IFNE(0x5c04, COMPAREIF),
    F32_IFLT(0x5d04, COMPAREIF),
    F32_IFGT(0x5e04, COMPAREIF),
    F32_IFLE(0x5f04, COMPAREIF),
    F32_IFGE(0x6004, COMPAREIF),

    F64_IFEQ(0x6104, COMPAREIF),
    F64_IFNE(0x6204, COMPAREIF),
    F64_IFLT(0x6304, COMPAREIF),
    F64_IFGT(0x6404, COMPAREIF),
    F64_IFLE(0x6504, COMPAREIF),
    F64_IFGE(0x6604, COMPAREIF),
    
    I32_BR_IFEQZ(0x450d, COMPAREBRIF),
    I32_BR_IFEQ(0x460d, COMPAREBRIF),
    I32_BR_IFNE(0x470d, COMPAREBRIF),
    I32_BR_IFLT_S(0x480d, COMPAREBRIF),
    I32_BR_IFLT_U(0x490d, COMPAREBRIF),
    I32_BR_IFGT_S(0x4a0d, COMPAREBRIF),
    I32_BR_IFGT_U(0x4b0d, COMPAREBRIF),
    I32_BR_IFLE_S(0x4c0d, COMPAREBRIF),
    I32_BR_IFLE_U(0x4d0d, COMPAREBRIF),
    I32_BR_IFGE_S(0x4e0d, COMPAREBRIF),
    I32_BR_IFGE_U(0x4f0d, COMPAREBRIF),

    I64_BR_IFEQZ(0x500d, COMPAREBRIF),
    I64_BR_IFEQ(0x510d, COMPAREBRIF),
    I64_BR_IFNE(0x520d, COMPAREBRIF),
    I64_BR_IFLT_S(0x530d, COMPAREBRIF),
    I64_BR_IFLT_U(0x540d, COMPAREBRIF),
    I64_BR_IFGT_S(0x550d, COMPAREBRIF),
    I64_BR_IFGT_U(0x560d, COMPAREBRIF),
    I64_BR_IFLE_S(0x570d, COMPAREBRIF),
    I64_BR_IFLE_U(0x580d, COMPAREBRIF),
    I64_BR_IFGE_S(0x590d, COMPAREBRIF),
    I64_BR_IFGE_U(0x5a0d, COMPAREBRIF),

    F32_BR_IFEQ(0x5b0d, COMPAREBRIF),
    F32_BR_IFNE(0x5c0d, COMPAREBRIF),
    F32_BR_IFLT(0x5d0d, COMPAREBRIF),
    F32_BR_IFGT(0x5e0d, COMPAREBRIF),
    F32_BR_IFLE(0x5f0d, COMPAREBRIF),
    F32_BR_IFGE(0x600d, COMPAREBRIF),

    F64_BR_IFEQ(0x610d, COMPAREBRIF),
    F64_BR_IFNE(0x620d, COMPAREBRIF),
    F64_BR_IFLT(0x630d, COMPAREBRIF),
    F64_BR_IFGT(0x640d, COMPAREBRIF),
    F64_BR_IFLE(0x650d, COMPAREBRIF),
    F64_BR_IFGE(0x660d, COMPAREBRIF),
    
    I32_SELECTEQZ(0x451b, COMPARESELECT),
    I32_SELECTEQ(0x461b, COMPARESELECT),
    I32_SELECTNE(0x471b, COMPARESELECT),
    I32_SELECTLT_S(0x481b, COMPARESELECT),
    I32_SELECTLT_U(0x491b, COMPARESELECT),
    I32_SELECTGT_S(0x4a1b, COMPARESELECT),
    I32_SELECTGT_U(0x4b1b, COMPARESELECT),
    I32_SELECTLE_S(0x4c1b, COMPARESELECT),
    I32_SELECTLE_U(0x4d1b, COMPARESELECT),
    I32_SELECTGE_S(0x4e1b, COMPARESELECT),
    I32_SELECTGE_U(0x4f1b, COMPARESELECT),

    I64_SELECTEQZ(0x501b, COMPARESELECT),
    I64_SELECTEQ(0x511b, COMPARESELECT),
    I64_SELECTNE(0x521b, COMPARESELECT),
    I64_SELECTLT_S(0x531b, COMPARESELECT),
    I64_SELECTLT_U(0x541b, COMPARESELECT),
    I64_SELECTGT_S(0x551b, COMPARESELECT),
    I64_SELECTGT_U(0x561b, COMPARESELECT),
    I64_SELECTLE_S(0x571b, COMPARESELECT),
    I64_SELECTLE_U(0x581b, COMPARESELECT),
    I64_SELECTGE_S(0x591b, COMPARESELECT),
    I64_SELECTGE_SELECTU(0x5a1b, COMPARESELECT),

    F32_SELECTEQ(0x5b1b, COMPARESELECT),
    F32_SELECTNE(0x5c1b, COMPARESELECT),
    F32_SELECTLT(0x5d1b, COMPARESELECT),
    F32_SELECTGT(0x5e1b, COMPARESELECT),
    F32_SELECTLE(0x5f1b, COMPARESELECT),
    F32_SELECTGE(0x601b, COMPARESELECT),

    F64_SELECTEQ(0x611b, COMPARESELECT),
    F64_SELECTNE(0x621b, COMPARESELECT),
    F64_SELECTLT(0x631b, COMPARESELECT),
    F64_SELECTGT(0x641b, COMPARESELECT),
    F64_SELECTLE(0x651b, COMPARESELECT),
    F64_SELECTGE(0x661b, COMPARESELECT),
    
    ;
    
    private final Integer code;
    private final OpType optype;

    private OpCode(Integer code, OpType optype) {
        this.code = code;
        this.optype = optype;
    }

    private final static Map<Integer,OpCode> table;
    
    static {
        table = new HashMap<>();
        for (OpCode opc:values()) {
            Integer index = opc.code;
            if (index == null) { // generated
                continue;
            }
            OpCode dup = table.put(index,opc);
            if (dup != null) {
                String msg = String.format("duplicate code = %d for %s and %s%n",
                        index, opc,dup);
                throw new AssertionError(msg);
            }
        }
    }

    public int getCode() {
        return code;
    }

    private static final EnumSet<OpCode> transfer = EnumSet.of(BR,BR_TABLE,UNREACHABLE,RETURN);
    
    public boolean isTransfer() {
        return transfer.contains(this);
    }

    public static OpCode getInstance(int inst) {
        OpCode opcode = table.get(inst);
        if (opcode == null) {
            throw new IllegalArgumentException("unknown opcode - " + Integer.toHexString(inst));
        }
        return opcode;
    }

    public int levelChange() {
        switch(this) {
            case BLOCK:case LOOP:case IF:
                return 1;
            case END:
                return -1;
            default: return 0;
        }
    }

    public OpType getOpType() {
        return optype;
    }
    
    public boolean isUnsigned() {
        return name().endsWith("_U") || name().contains("_U_");
    }
    
    public ValueType getPrefix() {
        int index = name().indexOf('_');
        if (index >= 0) {
            String prefix = name().substring(0, index);
            return ValueType.getInstance(prefix);
        }
        return null;
    }
    
    public ValueType getSignedPrefix() {
        ValueType vt = getPrefix();
        if (vt != null) {
            boolean unsigned = name().endsWith("_U");
            if (unsigned) vt = vt.getUnsigned();
        }
        return vt;
    }
    
    public ValueType getSuffix() {
        String suffix = name();
        if (suffix.endsWith("_S") || suffix.endsWith("_U")) {
            suffix = suffix.substring(0, suffix.length() - 2);
        }
        int index = suffix.lastIndexOf('_');
        if (index >= 0) {
            suffix = suffix.substring(index + 1);
            return ValueType.getInstance(suffix);
        }
        return null;
    }

    public ValueType getMemType() {
        boolean signed = name().endsWith("_S");
        boolean unsigned = name().endsWith("_U");
        String memtype = name();
        if (signed || unsigned) memtype = memtype.substring(0, memtype.length() - 2);
        ValueType result = getPrefix();
        if (memtype.endsWith("8")) {
            result = I08;
        } else if (memtype.endsWith("16")) {
            result = I16;
        } else if (memtype.endsWith("32")) {
            result = I32;
        }
        if (unsigned) result = result.getUnsigned();
        return result;
    }
    
    public String getCompareName() {
        assert optype == COMPARE;
        String cmpname = name();
        if (getPrefix() != null) {
            cmpname = cmpname.substring(4);
        }
        if (cmpname.endsWith("_U") || cmpname.endsWith("_S")) {
            cmpname = cmpname.substring(0, cmpname.length() - 2);
        }
        return cmpname.toLowerCase();
    }

    public String getWasmOp() {
        String wasmop = name();
        switch(this) {
            case BR_IF:
            case BR_TABLE:
            case CALL_INDIRECT:
                break;
            default:
                int index = wasmop.indexOf('_');
                if (index >= 0) {
                    wasmop = wasmop.substring(0,index) + '.' + wasmop.substring(index+1);
                }
                break;
        }
        return wasmop.toLowerCase();
    }

    public static OpCode fromWasmOp(String wasmop) {
        String opcode = wasmop.replace(".", "_").toUpperCase();
        return OpCode.valueOf(opcode);
    }
    
    public String getOpName() {
        String opname = name();
        if (getPrefix() != null) {
            opname = opname.substring(4);
        }
        int index = opname.lastIndexOf('_');
        if (index >= 0) {
            String suffix = opname.substring(index + 1);
            if (ValueType.getInstance(suffix) != null) {
                opname = opname.substring(0, index);
            }
        }
        return opname;
    }
    
}
