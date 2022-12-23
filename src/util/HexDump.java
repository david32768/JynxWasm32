package util;

import java.nio.ByteBuffer;

public class HexDump {

    private static final int WIDTH = 32;
    
    public static String printHex(ByteBuffer bb,long start) {
        StringBuilder sb = new StringBuilder();
        while(bb.hasRemaining()) {
            int pos = bb.position();
            int width = Math.min(WIDTH, bb.remaining());
            sb.append(String.format("%08x   ",bb.position() + start));
            for (int i = 0; i < width; ++i) {
                if (i != 0 && i % 4 == 0) sb.append(" ");
                byte bi = bb.get();
                String bs = String.format("%02x", Byte.toUnsignedInt(bi));
                sb.append(bs);
            }
            for (int i = width; i < WIDTH; ++i) {
                if (i != 0 && i % 4 == 0) sb.append(" ");
                sb.append("  ");
            }
            sb.append("  ");
            bb.position(pos);
            for (int i = 0; i < width; ++i) {
                byte bi = bb.get();
                if (bi < 0x20 || bi > 0x7f) bi = '.';
                sb.append((char)bi);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
}
