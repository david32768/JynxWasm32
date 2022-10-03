package parse;

import java.util.Arrays;
import java.util.StringJoiner;
import static parse.ValueType.*;

public class FnType {

    private final ValueType[] parm;

    public FnType(ValueType... parm) {
        int last;
        for (last = 1; last < parm.length;last++) {
            if (parm[last] == V00) break;
        }
        this.parm = Arrays.copyOf(parm, last);
    }

    public FnType(ValueType retvt,ValueType[] parm) {
        this.parm = new ValueType[parm.length+1];
        this.parm[0] = retvt;
        System.arraycopy(parm, 0, this.parm, 1, parm.length);
    }

    public FnType combine(FnType next) {
        ValueType[] nextvts = next.getParm();
        ValueType vt = nextvts[nextvts.length - 1];
        assert vt == this.getRtype();
        ValueType[] result = new ValueType[nextvts.length - 1 + this.numParms()];
        System.arraycopy(nextvts, 0, result, 0, nextvts.length - 1);
        System.arraycopy(parm, 1, result, nextvts.length - 1, parm.length - 1);
        return new FnType(result);
    }
    
    public int numParms() {
        return parm.length - 1;
    }
    
    public ValueType[] getParm() {
        return parm.clone();
    }

    public FnType append(ValueType vt) {
        ValueType[] parmp1 = Arrays.copyOf(parm, parm.length + 1);
        parmp1[parm.length] = vt;
        return new FnType(parmp1);
    }
    
    public ValueType[] getStackParm() {
        ValueType[] stackparms = new ValueType[parm.length -1];
        for (int i = 1; i < parm.length;++i) stackparms[i-1] = parm[parm.length - i];
        return stackparms;
    }

    public String wasmString() {
        StringJoiner sj = new StringJoiner(",","(",")");
        for (int i = 1; i < parm.length;i++) {
            ValueType vt = parm[i];
            sj.add(vt.toString());
        }
        String ret = parm[0] == V00?"()":parm[0].toString();
        return String.format("%s->%s",sj,ret);
    }

    public int adjustJVMStack() {
        int adjust = getRtype().getStackSize();
        for (ValueType parmvt:getStackParm()) {
            adjust -= parmvt.getStackSize();
        }
        return adjust;
    }

    @Override
    public String toString() {
        return wasmString();
    }
    
    public ValueType getRtype() {
        return parm[0];
    }

    public ValueType getType(int index) {
        if (index < parm.length) return parm[index];
        return V00;
    }
    
    public ValueType lastParm() {
        if (parm.length > 1) {
            return parm[parm.length - 1];
        }
        return V00;
    }

    public FnType changeType(ValueType from, ValueType to) {
        ValueType[] newparm = new ValueType[parm.length];
        for (int i = 0; i < parm.length; ++i) {
           newparm[i] = parm[i] == from?to:parm[i];
        }
        return new FnType(newparm);
    }
    
    public FnType changeRType(ValueType vt) {
        ValueType[] newparm = parm.clone();
        newparm[0] = vt;
        return new FnType(newparm);
    }

    public static FnType binary(ValueType vt) {
        return new FnType(vt,vt,vt);
    }
    
    public static FnType unary(ValueType vt) {
        return new FnType(vt,vt);
    }

    public static FnType compare(ValueType vt) {
        return new FnType(B32,vt,vt);
    }
    
    public static FnType transform(ValueType result, ValueType vt) {
        return new FnType(result,vt);
    }
    
    public static FnType produce(ValueType vt) {
        return new FnType(vt);
    }

    public static FnType consume(ValueType vt) {
        return new FnType(V00,vt);
    }
    
    private static FnType RUNABLE = new FnType(ValueType.V00,ValueType.V00);
    
    public boolean isRunable() {
        return this.equals(RUNABLE);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FnType) {
            FnType other = (FnType)obj;
            return Arrays.equals(parm, other.parm);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Arrays.deepHashCode(this.parm);
        return hash;
    }

}
