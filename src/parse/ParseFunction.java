package parse;

@FunctionalInterface
public interface ParseFunction<T> {

    public void parse(T obj, Section section, int index);
    
}
