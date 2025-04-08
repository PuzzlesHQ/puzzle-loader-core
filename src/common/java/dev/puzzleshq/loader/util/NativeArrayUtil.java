package dev.puzzleshq.loader.util;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A basic array util for class wrapper arrays to native arrays.
 *
 * @author Mr Zombii
 * @since 1.0.0
 */
public class NativeArrayUtil {

    /**
     * Converts a Byte[] array to a native byte[] array.
     */
    @Contract(pure = true)
    public static byte[] toNativeArray(Byte[] array) {
        byte[] pArray = new byte[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Short[] array to a native short[] array.
     */
    @Contract(pure = true)
    public static short[] toNativeArray(Short[] array) {
        short[] pArray = new short[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts an Integer[] array to a native int[] array.
     */
    @Contract(pure = true)
    public static int[] toNativeArray(Integer[] array) {
        int[] pArray = new int[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Long[] array to a native long[] array.
     */
    @Contract(pure = true)
    public static long[] toNativeArray(Long[] array) {
        long[] pArray = new long[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Float[] array to a native float[] array.
     */
    @Contract(pure = true)
    public static float[] toNativeArray(Float[] array) {
        float[] pArray = new float[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Double[] array to a native double[] array.
     */
    @Contract(pure = true)
    public static double[] toNativeArray(Double[] array) {
        double[] pArray = new double[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Boolean[] array to a native boolean[] array.
     */
    @Contract(pure = true)
    public static boolean[] toNativeArray(Boolean[] array) {
        boolean[] pArray = new boolean[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a Character[] array to a native char[] array.
     */
    @Contract(pure = true)
    public static char[] toNativeArray(Character[] array) {
        char[] pArray = new char[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a byte[] array to a Byte[] array.
     */
    @Contract(pure = true)
    public static Byte[] toObjectLikeArray(byte[] array) {
        Byte[] pArray = new Byte[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a short[] array to a Short[] array.
     */
    @Contract(pure = true)
    public static Short[] toObjectLikeArray(short[] array) {
        Short[] pArray = new Short[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts an int[] array to a Integer[] array.
     */
    @Contract(pure = true)
    public static Integer[] toObjectLikeArray(int[] array) {
        Integer[] pArray = new Integer[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a long[] array to a Long[] array.
     */
    @Contract(pure = true)
    public static Long[] toObjectLikeArray(long[] array) {
        Long[] pArray = new Long[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a float[] array to a Float[] array.
     */
    @Contract(pure = true)
    public static Float[] toObjectLikeArray(float[] array) {
        Float[] pArray = new Float[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a double[] array to a Double[] array.
     */
    @Contract(pure = true)
    public static Double[] toObjectLikeArray(double[] array) {
        Double[] pArray = new Double[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a boolean[] array to a Boolean[] array.
     */
    @Contract(pure = true)
    public static Boolean[] toObjectLikeArray(boolean[] array) {
        Boolean[] pArray = new Boolean[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    /**
     * Converts a char[] array to a Character[] array.
     */
    @Contract(pure = true)
    public static Character[] toObjectLikeArray(char[] array) {
        Character[] pArray = new Character[array.length];
        for (int i = 0; i < pArray.length; i++) pArray[i] = array[i];
        return pArray;
    }

    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Method ripped straight from JDK 11+
     * <br>
     * <br>
     * <p>
     * Reads up to a specified number of bytes from the input stream. This
     * method blocks until the requested number of bytes has been read, end
     * of stream is detected, or an exception is thrown. This method does not
     * close the input stream.
     *
     * <p> The length of the returned array equals the number of bytes read
     * from the stream. If {@code len} is zero, then no bytes are read and
     * an empty byte array is returned. Otherwise, up to {@code len} bytes
     * are read from the stream. Fewer than {@code len} bytes may be read if
     * end of stream is encountered.
     *
     * <p> When this stream reaches end of stream, further invocations of this
     * method will return an empty byte array.
     *
     * <p> Note that this method is intended for simple cases where it is
     * convenient to read the specified number of bytes into a byte array. The
     * total amount of memory allocated by this method is proportional to the
     * number of bytes read from the stream which is bounded by {@code len}.
     * Therefore, the method may be safely called with very large values of
     * {@code len} provided sufficient memory is available.
     *
     * <p> The behavior for the case where the input stream is <i>asynchronously
     * closed</i>, or the thread interrupted during the read, is highly input
     * stream specific, and therefore not specified.
     *
     * <p> If an I/O error occurs reading from the input stream, then it may do
     * so after some, but not all, bytes have been read. Consequently the input
     * stream may not be at end of stream and may be in an inconsistent state.
     * It is strongly recommended that the stream be promptly closed if an I/O
     * error occurs.
     *
     * @implNote
     * The number of bytes allocated to read data from this stream and return
     * the result is bounded by {@code 2*(long)len}, inclusive.
     *
     * @param len the maximum number of bytes to read
     * @return a byte array containing the bytes read from this input stream
     * @throws IllegalArgumentException if {@code length} is negative
     * @throws IOException if an I/O error occurs
     * @throws OutOfMemoryError if an array of the required size cannot be
     *         allocated.
     *
     * @since 11
     */
    public static byte[] readNBytes(InputStream stream, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = stream.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

}