package parse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import wasm.OpCode;
import wasm.OpType;

public final class Section {
    
    private final SectionType type;
    private final ByteBuffer sectionbb;
    private final int payload_len;
    private final int position;
    private final int limit;

    // getinstance
    private Section(SectionType type,ByteBuffer buffer) {
        this.type = type;
        this.sectionbb = buffer.asReadOnlyBuffer();
        this.sectionbb.order(ByteOrder.LITTLE_ENDIAN);
        this.payload_len = getU32();
        this.position = this.sectionbb.position();
        int newlimit = this.sectionbb.position() + payload_len;
        if (newlimit > buffer.limit()) {
            String msg = String.format("unexpected end of section or function%n newlimit (%d) > buffer limit (%d)",
                    newlimit,buffer.limit());
            throw new IllegalArgumentException(msg);
        }
        this.sectionbb.limit(newlimit);
        buffer.position(newlimit);  // after this section
        this.limit = newlimit;
    }

    public static Section getInstance(SectionType type,ByteBuffer buffer) {
        return new Section(type,buffer);
    }

    public static Section getSubSection(Section section) {
        return new Section(section.type,section.sectionbb);
    }

    public ByteBuffer getByteBuffer() {
        return sectionbb;
    }

    public int getPayload_len() {
        return payload_len;
    }

    public int position() {
        return sectionbb.position();
    }
    
    public void position(int newpos) {
        sectionbb.position(newpos);
    }

    public void discardRemaining() {
        sectionbb.position(limit);
    }
    
    public boolean hasRemaining() {
        return sectionbb.hasRemaining();
    }
    
    public void reset() {
        sectionbb.clear();    // leaves data unchanged
        sectionbb.position(position);
        sectionbb.limit(limit);
    }
    
    private float getF32() {
        return sectionbb.getFloat();
    }
    
    private double getF64() {
        return sectionbb.getDouble();
    }

    private static final int FC_EXTENSION = 0xfc;
    
    private OpCode getOpCode() {
        int inst = getUByte();
        if (inst == FC_EXTENSION) {
            int extension = getU32(); 
            if (extension <= 0xffff) {
                inst = (inst << 16) | extension;
            } else {
                
            }
        }
        return OpCode.getInstance(inst);
    }
    
    public Op getop() {
        OpCode opcode = getOpCode();
        OpType optype = opcode.getOpType();
        return optype.getOp(opcode, this);
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    // spec 5.2.4
    public String getName() {
        int len = vecsz();
        byte[] str = new byte[len];
        sectionbb.get(str);
        String name = new String(str,UTF8); //malformed chenged to \ufffd
        if (name.contains("\ufffd")) {
            throw new IllegalArgumentException("invalid UTF-8 encoding");
        }
        return name;
    }
    
    public byte[] byteArray(int length) {
        byte[] ba = new byte[length];
        sectionbb.get(ba, 0, length);
        return ba;
    }
    
    public int getUByte() {
        return Byte.toUnsignedInt(sectionbb.get());
    }
    
    private byte unusedLEB(int N, boolean signed) {
        int bits = signed? N - 1:N;
        int result = (-1 << (bits % 7));
        result &= 0x7f;
        return (byte)result;
    }
    
    // LEB128 endcoding - see wikipedia
    private long LEBint(int N, boolean signed) {
        assert N <= 64;
        long result = 0;
        for (int i = 0;i < N;i+=7) {   // limit to reading ceil(N/7) bytes - WebAssembly spec 5.2.2
            byte bit8 = sectionbb.get();
            result |= Integer.toUnsignedLong(bit8 & 0x7f) << i;
            if (bit8 >= 0) { // positive means last byte
                int shift = 64 - (i + 7);
                if (signed && shift > 0) {
                    result <<= shift;
                    result >>= shift;
                }
                if (i > N - 7) {
                    byte mask = unusedLEB(N,signed);
                    byte unused = signed && result < 0?mask:0;
                    if ((bit8 & mask) != unused) {
                        String msg = String.format("integer too large%n"
                                + " unused bits%nresult = %d bit8 = %02x mask = %016x",
                                result,bit8,mask);
                        throw new IllegalArgumentException(msg);
                    }
                }
                return result;
            }
        }
        String msg = String.format("integer representation too long%n expected %s%d",signed?"":"U",N);
        throw new IllegalArgumentException(msg);
    }

    public boolean getFlag() {
        int flag = sectionbb.get();
        if (flag != 0 && flag != 1) {
            throw new IllegalArgumentException("flag not zero or one: " + flag);
        }
        return flag == 1;
    }
    
    public boolean getMutability() {
        try {
            return getFlag();
        } catch (IllegalArgumentException ex) {
            String msg = String.format("invalid mutability%n%s",ex.getMessage());
            throw new IllegalArgumentException(msg,ex);
        }
    }
    
    // LEB128 endcoding - 5.2.2
    public int getU32() {
        int result = (int)LEBint(32, false);
        if (result < 0) {
            throw new UnsupportedOperationException("unsigned integer too large to be held in int");
        }
        return result;
    }

    // LEB128 endcoding - 5.2.2
    public long getU32L() {
        return LEBint(32, false);
    }

    public int vecsz() {
        return getU32();
    }

    // 5.1.1
    public int typeidx() {
        return getU32();
    }

    // 5.1.1
    public int funcidx() {
        return getU32();
    }

    // 5.1.1
    public int tableidx() {
        return getU32();
    }

    // 5.1.1
    public int memidx() {
        return getU32();
    }

    // 5.1.1
    public int globalidx() {
        return getU32();
    }

    // 5.1.1
    public int localidx() {
        return getU32();
    }
    
    // 5.1.1
    public int labelidx() {
        return getU32();
    }

    // LEB128 endcoding - 5.2.2
    public int varint7() {
        return (int)LEBint(7,true);
    }

    // LEB128 endcoding - 5.2.2
    private int getI32() {
        return (int)LEBint(32,true);
    }

    // LEB128 endcoding - 5.2.2
    private long getI64() {
        return LEBint(64,true);
    }

    public Number getImm(ValueType type) {
        Number value = 0;
        switch (type) {
            case I32:
                value = getI32();
                break;
            case I64:
                value = getI64();
                break;
            case F32:
                value = getF32();
                break;
            case F64:
                value = getF64();
                break;
            default:
                throw new AssertionError();
        }
        return value;
    }

    public ValueType getValueType() {
        int encoding = getUByte();
        return WasmType.getInstance(encoding).getValueType();
    }
    
    public ValueType getBlockType() {
        int encoding = getUByte();
        WasmType wasmtype = WasmType.getInstance(encoding);
        return wasmtype.getBlockType();
    }
    
    public void expectWasmType(WasmType required) {
        int encoding = getUByte();
        WasmType wasmtype = WasmType.getInstance(encoding);
        if (wasmtype != required) {
            String msg = String.format("WasmType expected is %s but is %s",required,wasmtype);
            throw new IllegalArgumentException(msg);
        }
    }
}
