/*
 * Copyright 1997-2014 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.array;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.ojalgo.access.Access1D;
import org.ojalgo.access.AccessUtils;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.NullaryFunction;
import org.ojalgo.function.ParameterFunction;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.function.VoidFunction;
import org.ojalgo.machine.JavaType;
import org.ojalgo.scalar.PrimitiveScalar;

/**
 * A one- and/or arbitrary-dimensional array of {@linkplain org.ojalgo.scalar.double}.
 *
 * @author apete
 */
public class BufferArray extends DenseArray<Double> {

    static final long MAX = 1L << 8;

    static final long ELEMENT_SIZE = JavaType.DOUBLE.memory();

    public static final Array1D<Double> make(final File file, final long count) {
        return BufferArray.create(file, count).asArray1D();
    }

    public static final Array2D<Double> make(final File file, final long rows, final long columns) {
        return BufferArray.create(file, rows, columns).asArray2D(rows);
    }

    public static final ArrayAnyD<Double> make(final File file, final long... structure) {
        return BufferArray.create(file, structure).asArrayAnyD(structure);
    }

    private static BasicArray<Double> create(final File file, final long... structure) {

        final long tmpCount = AccessUtils.count(structure);

        DoubleBuffer tmpDoubleBuffer = null;

        try {

            final RandomAccessFile tmpRandomAccessFile = new RandomAccessFile(file, "rw");

            final FileChannel tmpFileChannel = tmpRandomAccessFile.getChannel();

            final long tmpSize = ELEMENT_SIZE * tmpCount;

            if (tmpCount > MAX) {

                final DenseFactory<Double> tmpFactory = new DenseFactory<Double>() {

                    long offset = 0L;

                    @Override
                    long getElementSize() {
                        return ELEMENT_SIZE;
                    }

                    @Override
                    DenseArray<Double> make(final int size) {

                        final long tmpSize2 = size * ELEMENT_SIZE;
                        try {

                            final MappedByteBuffer tmpMap = tmpFileChannel.map(MapMode.READ_WRITE, offset, tmpSize2);
                            tmpMap.order(ByteOrder.nativeOrder());
                            return new BufferArray(tmpMap.asDoubleBuffer(), tmpRandomAccessFile);
                        } catch (final IOException exception) {
                            throw new RuntimeException(exception);
                        } finally {
                            offset += tmpSize2;
                        }
                    }

                    @Override
                    PrimitiveScalar zero() {
                        return PrimitiveScalar.ZERO;
                    }

                };

                return SegmentedArray.make(tmpFactory, structure);

            } else {

                final MappedByteBuffer tmpMappedByteBuffer = tmpFileChannel.map(FileChannel.MapMode.READ_WRITE, 0L, tmpSize);
                tmpMappedByteBuffer.order(ByteOrder.nativeOrder());

                tmpDoubleBuffer = tmpMappedByteBuffer.asDoubleBuffer();

                return new BufferArray(tmpDoubleBuffer, tmpRandomAccessFile);
            }

        } catch (final FileNotFoundException exception) {
            throw new RuntimeException(exception);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static final BasicArray<Double> make(final int capacity) {
        return new BufferArray(DoubleBuffer.allocate(capacity), null);
    }

    public static final BufferArray wrap(final DoubleBuffer data) {
        return new BufferArray(data, null);
    }

    protected static void fill(final DoubleBuffer data, final Access1D<?> value) {
        final int tmpLimit = (int) Math.min(data.capacity(), value.count());
        for (int i = 0; i < tmpLimit; i++) {
            data.put(i, value.doubleValue(i));
        }
    }

    protected static void fill(final DoubleBuffer data, final int first, final int limit, final int step, final double value) {
        for (int i = first; i < limit; i += step) {
            data.put(i, value);
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final Access1D<Double> left,
            final BinaryFunction<Double> function, final Access1D<Double> right) {
        for (int i = first; i < limit; i += step) {
            data.put(i, function.invoke(left.get(i), right.get(i)));
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final Access1D<Double> left,
            final BinaryFunction<Double> function, final double right) {
        for (int i = first; i < limit; i += step) {
            data.put(i, function.invoke(left.doubleValue(i), right));
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final Access1D<Double> value,
            final ParameterFunction<Double> function, final int aParam) {
        for (int i = first; i < limit; i += step) {
            data.put(i, function.invoke(value.doubleValue(i), aParam));
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final Access1D<Double> value,
            final UnaryFunction<Double> function) {
        for (int i = first; i < limit; i += step) {
            data.put(i, function.invoke(value.doubleValue(i)));
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final double left,
            final BinaryFunction<Double> function, final Access1D<Double> right) {
        for (int i = first; i < limit; i += step) {
            data.put(i, function.invoke(left, right.doubleValue(i)));
        }
    }

    protected static void invoke(final DoubleBuffer data, final int first, final int limit, final int step, final VoidFunction<Double> visitor) {
        for (int i = first; i < limit; i += step) {
            visitor.invoke(data.get(i));
        }
    }

    private final DoubleBuffer myBuffer;
    private final RandomAccessFile myFile;

    private BufferArray(final DoubleBuffer buffer, final RandomAccessFile file) {

        super();

        myBuffer = buffer;
        myFile = file;
    }

    public void close() {
        if (myFile != null) {
            try {
                myFile.close();
            } catch (final IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    protected double doubleValue(final int index) {
        return myBuffer.get(index);
    }

    @Override
    protected void exchange(final int firstA, final int firstB, final int step, final int count) {
        // TODO Auto-generated method stub

    }

    @Override
    protected final void fill(final int first, final int limit, final Access1D<Double> left, final BinaryFunction<Double> function, final Access1D<Double> right) {
        BufferArray.invoke(myBuffer, first, limit, 1, left, function, right);
    }

    @Override
    protected void fill(final int first, final int limit, final Access1D<Double> left, final BinaryFunction<Double> function, final Double right) {

    }

    @Override
    protected void fill(final int first, final int limit, final Double left, final BinaryFunction<Double> function, final Access1D<Double> right) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void fill(final int first, final int limit, final int step, final Double value) {
        BufferArray.fill(myBuffer, first, limit, step, value);
    }

    @Override
    protected void fill(final int first, final int limit, final int step, final NullaryFunction<Double> supplier) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void finalize() throws Throwable {

        super.finalize();

        if (myFile != null) {
            this.close();
        }
    }

    @Override
    protected Double get(final int index) {
        return myBuffer.get(index);
    }

    @Override
    protected int indexOfLargest(final int first, final int limit, final int step) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected boolean isAbsolute(final int index) {
        return PrimitiveScalar.isAbsolute(myBuffer.get(index));
    }

    @Override
    protected boolean isSmall(final int index, final double comparedTo) {
        return PrimitiveScalar.isSmall(comparedTo, myBuffer.get(index));
    }

    @Override
    protected void modify(final int index, final Access1D<Double> left, final BinaryFunction<Double> function) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void modify(final int index, final BinaryFunction<Double> function, final Access1D<Double> right) {
        // TODO Auto-generated method stub
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final Access1D<Double> left, final BinaryFunction<Double> function) {
        BufferArray.invoke(myBuffer, first, limit, step, left, function, this);
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final BinaryFunction<Double> function, final Access1D<Double> right) {
        BufferArray.invoke(myBuffer, first, limit, step, this, function, right);
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final BinaryFunction<Double> function, final Double right) {
        BufferArray.invoke(myBuffer, first, limit, step, this, function, right);
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final Double left, final BinaryFunction<Double> function) {
        BufferArray.invoke(myBuffer, first, limit, step, left, function, this);
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final ParameterFunction<Double> function, final int parameter) {
        BufferArray.invoke(myBuffer, first, limit, step, this, function, parameter);
    }

    @Override
    protected final void modify(final int first, final int limit, final int step, final UnaryFunction<Double> function) {
        BufferArray.invoke(myBuffer, first, limit, step, this, function);
    }

    @Override
    protected void modify(final int index, final UnaryFunction<Double> function) {
        myBuffer.put(index, function.invoke(myBuffer.get(index)));
    }

    @Override
    protected void modifyOne(final int index, final UnaryFunction<Double> function) {
        this.set(index, function.invoke(this.doubleValue(index)));

    }

    @Override
    protected int searchAscending(final Double number) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected final void set(final int index, final double value) {
        myBuffer.put(index, value);
    }

    @Override
    protected final void set(final int index, final Number value) {
        myBuffer.put(index, value.doubleValue());
    }

    @Override
    protected int size() {
        return myBuffer.capacity();
    }

    @Override
    protected final void sortAscending() {

    }

    @Override
    protected final PrimitiveScalar toScalar(final long index) {
        return PrimitiveScalar.valueOf(myBuffer.get((int) index));
    }

    @Override
    protected final void visit(final int first, final int limit, final int step, final VoidFunction<Double> visitor) {
        BufferArray.invoke(myBuffer, first, limit, step, visitor);
    }

    @Override
    protected final void visit(final int index, final VoidFunction<Double> visitor) {
        visitor.invoke(myBuffer.get(index));
    }

    @Override
    boolean isPrimitive() {
        return true;
    }

    @Override
    DenseArray<Double> newInstance(final int capacity) {
        return null;
        // return new MyTestArray(capacity);
    }

}
