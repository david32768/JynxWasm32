package jynxwasm32;

import java.util.stream.Collectors;
import parse.Kind;

public class JavaName {

    private final boolean standard;

    public JavaName(boolean standard) {
        this.standard = standard;
    }
    
    private static String changeInvalidChar(Integer I) {
        int i = I;
        if (Character.isJavaIdentifierPart(i)) {
            return String.valueOf((char)i);
        } else if (i == '-') {
            return "_";
        } else if (i == '.') {
            return "$DOT$";
        } else {
            return "$" + Integer.toHexString(i) + "$";
        }
    }
    
    public String simpleName(String name) {
        if (!Character.isJavaIdentifierStart(name.codePointAt(0))) {
            name = "_"+ name;
        }
        return name
                .codePoints()
                .boxed()
                .map(JavaName::changeInvalidChar)
                .collect(Collectors.joining());
    }

    public String ownerName(String name) {
        String[] parts = name.split("/");
        if (standard) {
            String classname = parts[parts.length - 1];
            char first = classname.charAt(0);
            if (!Character.isUpperCase(first)) {
                classname = Character.toUpperCase(first) + classname.substring(1);
            }
            parts[parts.length - 1] = classname;
        }
        StringBuilder sb = new StringBuilder(name.length());
        boolean first = true;
        for (String part:parts) {
            if (first) {
                first = false;
            } else {
                sb.append("/");
            }
            sb.append(simpleName(part));
        }
        return sb.toString();
    }
    
    public String of(Kind kind) {
        String modname = kind.getModuleName();
        String module = ownerName(modname);
        String field = simpleName(kind.getFieldName());
        String name = module + "." + field;
        return name;
    }

    public String simpleName(Kind kind) {
        return JavaName.this.simpleName(kind.getFieldName());
    }
    
    public String localName(Kind kind) {
        if (kind.isImported()) {
            return of(kind);
        } else {
            return simpleName(kind);
        }
    }
    
    public boolean is(String name) {
        return ownerName(name).equals(name);
    }
}
