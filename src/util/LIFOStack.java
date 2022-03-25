package util;

import java.util.ArrayList;

public class LIFOStack<T> {

    private final ArrayList<T> stack = new ArrayList<>();

    public LIFOStack() {
    }

    public void push(T element) {
        stack.add(element);
    }
    
    public boolean isEmpty() {
        return stack.isEmpty();
    }
    
    public int size() {
        return stack.size();
    }
    
    private int last() {
        return stack.size() - 1;
    }
    
    public T peek() {
        return stack.isEmpty()?null:stack.get(last());
    }
    
    public T pop() {
        return stack.remove(last());
    }
    
    public T peek(int index) {
        return stack.get(last() - index);
    }
    
    public T at(int index) {
        return stack.get(index);
    }

}
