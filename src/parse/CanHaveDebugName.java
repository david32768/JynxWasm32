package parse;

public interface CanHaveDebugName {

    public void setDebugName(String name);

    default String getDesc() {
        return "";
    }
}
