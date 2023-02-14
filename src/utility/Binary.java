package utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

import static parse.Reason.M0;
import static parse.Reason.M107;
import static parse.Reason.M999;
import static utility.State.*;

import parse.ParseException;
import parse.Reason;

import parse.WasmModule;

public class Binary {
    
    private static int otherCount = 0;
    private static int OKCount = 0;
    private static int errorCount = 0;
    
    private static void log(Level loglevel, String name, String comments, Reason expected, Reason actual, String msg) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            pw.println(name);
            pw.print(comments);
            pw.println("expected - " + expected.reason());
            pw.println("actual - " + msg);
            if (actual == expected) {
                pw.println("successful");
            }
        }
        Logger.getGlobal().log(loglevel, sw.toString());
    }
    
    private static void check(String name, String comments, Reason expected, Reason actual, String msg) {
        Level loglevel = Level.INFO;
        if (actual == expected) {
            ++OKCount;
        } else if (expected == M0 || actual == M0) {
            ++errorCount;
            loglevel = Level.SEVERE;
        } else {
            ++otherCount;
            loglevel = Level.WARNING;
        }
        log(loglevel, name, comments, expected, actual, msg);
    }

    private static void test(String comments, String name, Reason expected, String modstr, ByteBuffer bb) {
        try {
            Logger.getGlobal().fine(String.format("%s***** %s ***** %s%n    %s",comments,name,expected.reason(),modstr));
            WasmModule module = WasmModule.getModule(name,bb); // syntax check only
            check(name, comments, expected, M0, M0.reason());
        } catch (ParseException ex) {
            check(name, comments, expected, ex.reason(), ex.getMessage());
        } catch (BufferUnderflowException ex) {
            check(name, comments, expected, M107, ex.toString());
        } catch (Exception ex) {
            check(name, comments, expected, M999, ex.toString());
        }
    }

    private static ByteBuffer combineQuoted(String modstr) {
        byte[] ba = new byte[modstr.length()];
        int next = 0;
        int x = 0;
        State state = BLANK;
        for (char ch:modstr.toCharArray()) {
            switch(state) {
                case BLANK:
                    if (ch == '\"') {
                        state = QUOTE;
                    } else if (!Character.isWhitespace(ch)) {
                        throw new AssertionError();
                    }
                    break;
                case QUOTE:
                    if (ch == '\"') {
                        state = BLANK;
                    } else if (ch == '\\') {
                        state = SLASH;
                    } else {
                        ba[next++] = (byte)ch;                
                    }
                    break;
                case SLASH:
                    x = Integer.valueOf("" + ch, 16);
                    state = SLASH2;
                    break;
                case SLASH2:
                    x <<= 4;
                    x += Integer.valueOf("" + ch, 16);
                    ba[next++] = (byte)x;
                    state = QUOTE;
                    break;
                default:
                    throw new EnumConstantNotPresentException(state.getClass(), state.name());
            }
        }
        return ByteBuffer.wrap(ba,0,next);
    }

    private static String dequote(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    
    public static void testFile(Path path) throws IOException {
        int notrelevent = 0;
        List<String> lines = Files.readAllLines(path);
        int testct = 0;
        for (Expr expr:Expr.parse(lines)) {
            if (!expr.contains(TestSection.MODULE_BINARY)) {
                ++notrelevent;
                continue;
            }
            String before = expr.before();
            Optional<TestSection> optsect = TestSection.getStartInstance(before);
            if (!optsect.isPresent()) {
                System.err.println("unknown test section - " + before);
                continue;
            }
            TestSection section = optsect.get();
            Reason reason = M0;
            String comments = expr.comments();
            switch(section) {
                case ASSERT_MALFORMED:
                    String reasonstr = dequote(expr.after());
                    Optional<Reason> optreason = Reason.getInstance(reasonstr);
                    if (!optreason.isPresent()) {
                        ++errorCount;
                        System.err.println("unknown reason - " + reasonstr);
                        continue;
                    }
                    reason = optreason.get();
                    before = expr.getChild(0).before();
                    optsect = TestSection.getStartInstance(before);
                    if (!optsect.isPresent() || optsect.get() != TestSection.MODULE_BINARY) {
                        continue;
                    }
                    // FALL THROUGH
                case MODULE_BINARY:
                    String modstr = before.substring(TestSection.MODULE_BINARY.name().length() + 1);
                    ByteBuffer bb = combineQuoted(modstr);
                    ++testct;
                    String name = "test" + testct;
                    test(comments, name, reason, modstr, bb);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        String endmsg = String.format("filename %s%n OK count = %d, errors = %d, different = %d, not relevant = %d%n",
                path, OKCount, errorCount, otherCount,notrelevent);
        Logger.getGlobal().warning(endmsg);
        System.out.format(endmsg);
    }
    
}
