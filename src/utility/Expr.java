package utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class Expr {

    private final Expr parent;
    private final StringBuilder before;
    private final StringBuilder after;
    private final String comments;
    private final ArrayList<Expr> children;

    private Expr(Expr parent, String comments) {
        this(parent, "", "", comments,new ArrayList<>());
    }

    private Expr(Expr parent, String before, String after, String comments,ArrayList<Expr> children) {
        this.parent = parent;
        this.before = new StringBuilder(before);
        this.after = new StringBuilder(after);
        this.children = children;
        this.comments = comments;
    }
    
    
    private static Expr getInstance(Expr parent, String comments) {
        return new Expr(parent,comments);
    }

    String comments() {
        return comments;
    }
    
    private boolean isText() {
        return before().isEmpty() && children.isEmpty();
    }
    
    private void addChild(Expr child) {
        assert child.parent == this;
        children.add(child);
    }

    Expr getChild(int index) {
        return children.get(index);
    }
    
    private void addBeforeChar(char c) {
        before.append(c);
    }

    private void addAfterChar(char c) {
        after.append(c);
    }

    private Expr getParent() {
        return parent;
    }
    
    String before() {
        return before.toString().trim();
    }
    
    String after() {
        return after.toString().trim();
    }
    
    private void setBefore(String str) {
        this.before.setLength(0);
        this.before.append(str);
    }
    
    private void setAfter(String str) {
        this.after.setLength(0);
        this.after.append(str);
    }
    
    String print() {
        return print(0);
    }
    
    private String print(int level) {
        char[] blanks = new char[2*level];
        Arrays.fill(blanks, ' ');
        String indent = String.valueOf(blanks);
        StringBuilder sb = new StringBuilder();
        if (isText()) {
            sb.append(after()).append("\n");
        } else if (children.isEmpty()) {
            assert after().isEmpty();
            sb.append(indent).append("(").append(before()).append(")\n");
        } else {
            sb.append(indent).append("(").append(before()).append("\n");
            for (Expr child:children) {
                sb.append(child.print(level + 1));
            }
            if (!after().isEmpty()) {
                sb.append(indent).append(after()).append("\n");
            }
            sb.append(indent).append(")\n");
        }
        return sb.toString();
    }

    boolean contains(TestSection section) {
        String str = before();
        Optional<TestSection> optsect = TestSection.getStartInstance(str);
        boolean contains = optsect.isPresent() && optsect.get() == section;
        for (Expr child:children) {
            contains |= child.contains(section);
        }
        return contains;
    }

    private static String removeComments(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        int level = 0;
        for (String line:lines) {
            line = line.trim();
            if (level == 0) {
                if (line.startsWith(";;")) {
                    sb.append('\r').append(line).append('\n');
                    continue;
                }
            }
            boolean linecomment = false;
            boolean quote = false;
            StringBuilder linesb = new StringBuilder();
            for (int i = 0; i < line.length(); ++i) {
                char c = line.charAt(i);
                assert c != '\r' &&c != '\n';
                char next = i + 1 == line.length()?'\n':line.charAt(i + 1);
                switch (c) {
                    case '\"':
                        if (level == 0) {
                            quote = !quote;
                        }
                        break;
                    case '\\':
                        if (quote && next != '\n') {
                            linesb.append(c);
                            ++i;
                            c = next;
                        }
                        break;
                    case ' ':
                        if (!quote && next == ' ') {
                            ++i;
                        }
                        break;
                    case '(':
                        if (!quote && next == ';') {
                            ++i;
                            ++level;
                            continue;
                        }
                        break;
                    case ';':
                        if (!quote && next == ')') {
                            ++i;
                            --level;
                            assert level >=0:line;
                            continue;
                        }
                        linecomment = !quote && level == 0 && next == ';';
                        break;
                }
                if (linecomment) {
                    break;
                }
                if (level == 0) {
                    linesb.append(c);
                }
            }
            assert !quote:line;
            String line2 = linesb.toString().trim();
            if (!line2.isEmpty()) {
                sb.append(line2).append(' ');
            }
        }
        return sb.toString();
    }
    
    static List<Expr> parse(List<String> lines) {
        String chars = removeComments(lines);
        List<Expr> parents = new ArrayList<>();
        Expr current = null;
        char lastc = ' ';
        boolean quote = false;
        boolean comment = false;
        StringBuilder sb = new StringBuilder();
        for (char c:chars.toCharArray()) {
            if (comment) {
                sb.append(c);
                if (c == '\n') {
                    comment = false;
                }
                continue;
            } else {
                if (c == '\r') {
                    comment = true;
                    continue;
                }
            }
            if (!quote && c == '(') {
                if (current == null) {
                    current = Expr.getInstance(null,sb.toString());
                } else {
                    Expr child = Expr.getInstance(current,"");
                    if (!current.after().isEmpty()) {
                        Expr text = new Expr(current,"");
                        text.setAfter(current.after());
                        current.setAfter("");
                        current.addChild(text);
                    }
                    current.addChild(child);
                    current = child;
                }
                lastc = c;
                sb.setLength(0);
                continue;
            }
            if (!quote && c == ')') {
                Expr parent = current.getParent();
                if (parent == null) {
                    parents.add(current);
                }
                current = parent;
                lastc = c;
                sb.setLength(0);
                continue;
            }
            if (current == null) {
                if (Character.isWhitespace(c)) {
                    continue;
                } else {
                    String msg = String.format("character = '%c'%n",c);
                    throw new AssertionError(msg);
                }
            }
            switch(lastc) {
                case '(':
                    current.addBeforeChar(c);
                    break;
                case ')':
                    current.addAfterChar(c);
                    break;
            }
            if (c == '\"') {
                quote = !quote;
            }
        }
        return parents;
    }

}
