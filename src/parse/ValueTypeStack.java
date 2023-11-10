package parse;

import static parse.ValueType.V00;
import util.LIFOStack;

public class ValueTypeStack {

    private final LIFOStack<ValueType> stack;
    
    private int floor;
    private int currentsz;
    private int maxsz;
    
    public ValueTypeStack() {
        this.stack = new LIFOStack<>();
        this.floor = 0;
        this.currentsz = 0;
        this.maxsz = 0;
    }

    public int getMaxsz() {
        return maxsz;
    }

    public int addedSize() {
        return stack.size() - floor;
    }

    public int size() {
        return stack.size();
    }
    
    public boolean addedEmpty() {
        return stack.size() == floor;
    }
    
    public int setFloor() {
        int previous = floor;
        floor = stack.size();
        return previous;
    }

    public void resetFloor(int newfloor) {
        if (newfloor > floor) {
            throw new IllegalArgumentException();
        }
        floor = newfloor;
    }

    
    public ValueType pop() {
        if (stack.size() <= floor) {
            throw new StackStateException();
        }
        ValueType result = stack.pop();
        currentsz -= result.getStackSize();
        return result;
    }

    public ValueType peek() {
        if (stack.size() <= floor) {
            throw new StackStateException();
        }
        return stack.peek();
    }
    
    public ValueType peekIf(ValueType vt) {
        if (vt == V00) return V00;
        return peek();
    }
    
    public ValueType peekNext() {
        if (stack.size() <= floor + 1) {
            throw new StackStateException();
        }
        return stack.peek(1);
    }

    private void push(ValueType vt) {
        currentsz += vt.getStackSize();
        maxsz = Math.max(maxsz, currentsz);
        stack.push(vt);
    }

    private void pushIf(ValueType vt) {
        if (vt != V00) push(vt);
    }
    
    public void adjustStack(FnType optype) {
        ValueType[] stackparms = optype.getStackParm();
        for (ValueType parmvt:stackparms) {
            ValueType stackvt = pop();
            if (!stackvt.isCompatible(parmvt)) {
                    String msg = String.format("%nstack value = %s parm value = %s%n",stackvt,parmvt);
                    throw new StackStateException(msg);
            }
        }
        ValueType vtr = optype.getRtype();
        pushIf(vtr);
    }

    public FnType getUnwind(ValueType vtr, int from, boolean fromtos) {
        int to = stack.size();
        if (!fromtos) --to;
        ValueType[] parms = new ValueType[to - from + 1];
        parms[0] = vtr;
        for (int i = from; i < to;++i) {
            parms[i - from + 1] = stack.at(i);
        }
        return new FnType(parms);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stack.size(); ++i) {
            if (i != 0 && i == floor) sb.append(") ");
            ValueType vt = stack.at(i);
            sb.append(vt.name());
            sb.append(' ');
        }
        return sb.toString();
    }

}
