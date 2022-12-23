package parse;

import java.util.ArrayList;
import java.util.logging.Logger;

public class Local implements CanHaveDebugName {

    private final ValueType type;
    private final int jvmnum;
    private final int relnum;
    private final boolean parm;
    
    private Number value;
    private String name;
    
    private Local(ValueType type, int jvmnum, int relnum, boolean parm) {
        this.type = type;
        this.jvmnum = jvmnum;
        this.relnum = relnum;
        this.parm = parm;
        this.value = type.getZero();
    }

    private Local(ValueType type, int jvmnum, int relnum) {
        this(type,jvmnum,relnum,false);
    }

    public Number getValue() {
        return value;
    }

    public ValueType getType() {
        return type;
    }

    @Override
    public void setDebugName(String name) {
        if (this.name == null) {
            this.name = name;
        } else {
            String msg = String.format("cannot set name to %s as already set to %s",name,this.name);
            Logger.getGlobal().warning(msg);
        }
    }
    
    public String getName() {
        return name == null?String.valueOf(relnum):name;
    }
    
    public String getDebugName() {
        return name;
    }
    
    public boolean isParm() {
        return parm;
    }

    public int getNumber() {
        return relnum;
    }

    public int getNextJvmnum() {
        return jvmnum + type.getStackSize();
    }

    @Override
    public String toString() {
        return String.format("%s %d %s",type,jvmnum,parm?"parameter":"");
    }

    public Number setValue(Number value) {
        if (!type.isInstance(value)) throw new RuntimeException("Local type mismatch");
        this.value = value;
        return value;
    }
    
    
/*
    #### Local Entry

Each local entry declares a number of local variables of a given type.
It is legal to have several entries with the same type.

| Field | Type         | Description                                     |
| ----- | ------------ | ----------------------------------------------- |
| count | `varuint32`  | number of local variables of the following type |
| type  | `value_type` | type of the variables                           |

    */    
    
    public static ArrayList<Local> parse(Section section, LocalFunction fn) {
        // parameters are parms 0 ->
        FnType fntype = fn.getFnType();
        ValueType[] parms = fntype.getParm();
        ArrayList<Local> result = new ArrayList<>();
        int jvmnum = 0;
        int relnum = 0;
        for (int i = 1; i < parms.length;++i) {
            ValueType vt = parms[i];
            Local local = new Local(vt,jvmnum,relnum,true);
            result.add(local);
            jvmnum = local.getNextJvmnum();
            ++relnum;
        }

        int local_count = section.vecsz();
        Logger.getGlobal().fine(String.format("locals has %d sections", local_count));
        for (int i = 0; i < local_count;i++) {
            int count = section.vecsz();
            ValueType type = section.getValueType();
            Logger.getGlobal().finer(String.format("%d locals of type %s", count,type));
            for (int j = 0; j < count;j++) {
                Local local = new Local(type,jvmnum,relnum);
                result.add(local);
                jvmnum = local.getNextJvmnum();
                ++relnum;
            }
        }
        return result;
    }

}
