/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is JTransforms.
 *
 * The Initial Developer of the Original Code is
 * Piotr Wendykier, Emory University.
 * Portions created by the Initial Developer are Copyright (C) 2007-2009
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package edu.emory.mathcs.jtransforms.dct;

import java.util.concurrent.Future;

import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Computes 3D Discrete Cosine Transform (DCT) of single precision data. The
 * sizes of all three dimensions can be arbitrary numbers. This is a parallel
 * implementation of split-radix and mixed-radix algorithms optimized for SMP
 * systems. <br>
 * <br>
 * Part of code is derived from General Purpose FFT Package written by Takuya Ooura
 * (http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html)
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class FloatDCT_3D {

    private int slices;

    private int rows;

    private int columns;

    private int sliceStride;

    private int rowStride;

    private float[] t;

    private FloatDCT_1D dctSlices, dctRows, dctColumns;

    private int oldNthreads;

    private int nt;

    private boolean isPowerOfTwo = false;

    private boolean useThreads = false;

    /**
     * Creates new instance of FloatDCT_3D.
     * 
     * @param slices
     *            number of slices
     * @param rows
     *            number of rows
     * @param columns
     *            number of columns
     */
    public FloatDCT_3D(int slices, int rows, int columns) {
        if (slices <= 1 || rows <= 1 || columns <= 1) {
            throw new IllegalArgumentException("slices, rows and columns must be greater than 1");
        }
        this.slices = slices;
        this.rows = rows;
        this.columns = columns;
        this.sliceStride = rows * columns;
        this.rowStride = columns;
        if (slices * rows * columns >= ConcurrencyUtils.getThreadsBeginN_3D()) {
            this.useThreads = true;
        }
        if (ConcurrencyUtils.isPowerOf2(slices) && ConcurrencyUtils.isPowerOf2(rows) && ConcurrencyUtils.isPowerOf2(columns)) {
            isPowerOfTwo = true;

            oldNthreads = ConcurrencyUtils.getNumberOfThreads();
            nt = slices;
            if (nt < rows) {
                nt = rows;
            }
            nt *= 4;
            if (oldNthreads > 1) {
                nt *= oldNthreads;
            }
            if (columns == 2) {
                nt >>= 1;
            }
            t = new float[nt];
        }
        dctSlices = new FloatDCT_1D(slices);
        if (slices == rows) {
            dctRows = dctSlices;
        } else {
            dctRows = new FloatDCT_1D(rows);
        }
        if (slices == columns) {
            dctColumns = dctSlices;
        } else if (rows == columns) {
            dctColumns = dctRows;
        } else {
            dctColumns = new FloatDCT_1D(columns);
        }
    }

    /**
     * Computes the 3D forward DCT (DCT-II) leaving the result in <code>a</code>
     * . The data is stored in 1D array addressed in slice-major, then
     * row-major, then column-major, in order of significance, i.e. the element
     * (i,j,k) of 3D array x[slices][rows][columns] is stored in a[i*sliceStride
     * + j*rowStride + k], where sliceStride = rows * columns and rowStride =
     * columns.
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void forward(final float[] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 4;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 2) {
                    nt >>= 1;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                ddxt3da_subth(-1, a, scale);
                ddxt3db_subth(-1, a, scale);
            } else {
                ddxt3da_sub(-1, a, scale);
                ddxt3db_sub(-1, a, scale);
            }
        } else {
            if ((nthreads > 1) && useThreads && (slices >= nthreads) && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                int idx1 = s * sliceStride;
                                for (int r = 0; r < rows; r++) {
                                    dctColumns.forward(a, idx1 + r * rowStride, scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                int idx1 = s * sliceStride;
                                for (int c = 0; c < columns; c++) {
                                    for (int r = 0; r < rows; r++) {
                                        int idx3 = idx1 + r * rowStride + c;
                                        temp[r] = a[idx3];
                                    }
                                    dctRows.forward(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        int idx3 = idx1 + r * rowStride + c;
                                        a[idx3] = temp[r];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                int idx1 = r * rowStride;
                                for (int c = 0; c < columns; c++) {
                                    for (int s = 0; s < slices; s++) {
                                        int idx3 = s * sliceStride + idx1 + c;
                                        temp[s] = a[idx3];
                                    }
                                    dctSlices.forward(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        int idx3 = s * sliceStride + idx1 + c;
                                        a[idx3] = temp[s];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    int idx1 = s * sliceStride;
                    for (int r = 0; r < rows; r++) {
                        dctColumns.forward(a, idx1 + r * rowStride, scale);
                    }
                }
                float[] temp = new float[rows];
                for (int s = 0; s < slices; s++) {
                    int idx1 = s * sliceStride;
                    for (int c = 0; c < columns; c++) {
                        for (int r = 0; r < rows; r++) {
                            int idx3 = idx1 + r * rowStride + c;
                            temp[r] = a[idx3];
                        }
                        dctRows.forward(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            int idx3 = idx1 + r * rowStride + c;
                            a[idx3] = temp[r];
                        }
                    }
                }
                temp = new float[slices];
                for (int r = 0; r < rows; r++) {
                    int idx1 = r * rowStride;
                    for (int c = 0; c < columns; c++) {
                        for (int s = 0; s < slices; s++) {
                            int idx3 = s * sliceStride + idx1 + c;
                            temp[s] = a[idx3];
                        }
                        dctSlices.forward(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            int idx3 = s * sliceStride + idx1 + c;
                            a[idx3] = temp[s];
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes the 3D forward DCT (DCT-II) leaving the result in <code>a</code>
     * . The data is stored in 3D array
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void forward(final float[][][] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 4;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 2) {
                    nt >>= 1;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                ddxt3da_subth(-1, a, scale);
                ddxt3db_subth(-1, a, scale);
            } else {
                ddxt3da_sub(-1, a, scale);
                ddxt3db_sub(-1, a, scale);
            }
        } else {
            if ((nthreads > 1) && useThreads && (slices >= nthreads) && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int r = 0; r < rows; r++) {
                                    dctColumns.forward(a[s][r], scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int c = 0; c < columns; c++) {
                                    for (int r = 0; r < rows; r++) {
                                        temp[r] = a[s][r][c];
                                    }
                                    dctRows.forward(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        a[s][r][c] = temp[r];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                for (int c = 0; c < columns; c++) {
                                    for (int s = 0; s < slices; s++) {
                                        temp[s] = a[s][r][c];
                                    }
                                    dctSlices.forward(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        a[s][r][c] = temp[s];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    for (int r = 0; r < rows; r++) {
                        dctColumns.forward(a[s][r], scale);
                    }
                }
                float[] temp = new float[rows];
                for (int s = 0; s < slices; s++) {
                    for (int c = 0; c < columns; c++) {
                        for (int r = 0; r < rows; r++) {
                            temp[r] = a[s][r][c];
                        }
                        dctRows.forward(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            a[s][r][c] = temp[r];
                        }
                    }
                }
                temp = new float[slices];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        for (int s = 0; s < slices; s++) {
                            temp[s] = a[s][r][c];
                        }
                        dctSlices.forward(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            a[s][r][c] = temp[s];
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes the 3D inverse DCT (DCT-III) leaving the result in
     * <code>a</code>. The data is stored in 1D array addressed in slice-major,
     * then row-major, then column-major, in order of significance, i.e. the
     * element (i,j,k) of 3D array x[slices][rows][columns] is stored in
     * a[i*sliceStride + j*rowStride + k], where sliceStride = rows * columns
     * and rowStride = columns.
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final float[] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 4;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 2) {
                    nt >>= 1;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                ddxt3da_subth(1, a, scale);
                ddxt3db_subth(1, a, scale);
            } else {
                ddxt3da_sub(1, a, scale);
                ddxt3db_sub(1, a, scale);
            }
        } else {
            if ((nthreads > 1) && useThreads && (slices >= nthreads) && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                int idx1 = s * sliceStride;
                                for (int r = 0; r < rows; r++) {
                                    dctColumns.inverse(a, idx1 + r * rowStride, scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                int idx1 = s * sliceStride;
                                for (int c = 0; c < columns; c++) {
                                    for (int r = 0; r < rows; r++) {
                                        int idx3 = idx1 + r * rowStride + c;
                                        temp[r] = a[idx3];
                                    }
                                    dctRows.inverse(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        int idx3 = idx1 + r * rowStride + c;
                                        a[idx3] = temp[r];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                int idx1 = r * rowStride;
                                for (int c = 0; c < columns; c++) {
                                    for (int s = 0; s < slices; s++) {
                                        int idx3 = s * sliceStride + idx1 + c;
                                        temp[s] = a[idx3];
                                    }
                                    dctSlices.inverse(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        int idx3 = s * sliceStride + idx1 + c;
                                        a[idx3] = temp[s];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    int idx1 = s * sliceStride;
                    for (int r = 0; r < rows; r++) {
                        dctColumns.inverse(a, idx1 + r * rowStride, scale);
                    }
                }
                float[] temp = new float[rows];
                for (int s = 0; s < slices; s++) {
                    int idx1 = s * sliceStride;
                    for (int c = 0; c < columns; c++) {
                        for (int r = 0; r < rows; r++) {
                            int idx3 = idx1 + r * rowStride + c;
                            temp[r] = a[idx3];
                        }
                        dctRows.inverse(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            int idx3 = idx1 + r * rowStride + c;
                            a[idx3] = temp[r];
                        }
                    }
                }
                temp = new float[slices];
                for (int r = 0; r < rows; r++) {
                    int idx1 = r * rowStride;
                    for (int c = 0; c < columns; c++) {
                        for (int s = 0; s < slices; s++) {
                            int idx3 = s * sliceStride + idx1 + c;
                            temp[s] = a[idx3];
                        }
                        dctSlices.inverse(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            int idx3 = s * sliceStride + idx1 + c;
                            a[idx3] = temp[s];
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes the 3D inverse DCT (DCT-III) leaving the result in
     * <code>a</code>. The data is stored in 3D array.
     * 
     * @param a
     *            data to transform
     * @param scale
     *            if true then scaling is performed
     */
    public void inverse(final float[][][] a, final boolean scale) {
        int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (isPowerOfTwo) {
            if (nthreads != oldNthreads) {
                nt = slices;
                if (nt < rows) {
                    nt = rows;
                }
                nt *= 4;
                if (nthreads > 1) {
                    nt *= nthreads;
                }
                if (columns == 2) {
                    nt >>= 1;
                }
                t = new float[nt];
                oldNthreads = nthreads;
            }
            if ((nthreads > 1) && useThreads) {
                ddxt3da_subth(1, a, scale);
                ddxt3db_subth(1, a, scale);
            } else {
                ddxt3da_sub(1, a, scale);
                ddxt3db_sub(1, a, scale);
            }
        } else {
            if ((nthreads > 1) && useThreads && (slices >= nthreads) && (rows >= nthreads) && (columns >= nthreads)) {
                Future<?>[] futures = new Future[nthreads];
                int p = slices / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int r = 0; r < rows; r++) {
                                    dctColumns.inverse(a[s][r], scale);
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                for (int l = 0; l < nthreads; l++) {
                    final int firstSlice = l * p;
                    final int lastSlice = (l == (nthreads - 1)) ? slices : firstSlice + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[rows];
                            for (int s = firstSlice; s < lastSlice; s++) {
                                for (int c = 0; c < columns; c++) {
                                    for (int r = 0; r < rows; r++) {
                                        temp[r] = a[s][r][c];
                                    }
                                    dctRows.inverse(temp, scale);
                                    for (int r = 0; r < rows; r++) {
                                        a[s][r][c] = temp[r];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

                p = rows / nthreads;
                for (int l = 0; l < nthreads; l++) {
                    final int firstRow = l * p;
                    final int lastRow = (l == (nthreads - 1)) ? rows : firstRow + p;
                    futures[l] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            float[] temp = new float[slices];
                            for (int r = firstRow; r < lastRow; r++) {
                                for (int c = 0; c < columns; c++) {
                                    for (int s = 0; s < slices; s++) {
                                        temp[s] = a[s][r][c];
                                    }
                                    dctSlices.inverse(temp, scale);
                                    for (int s = 0; s < slices; s++) {
                                        a[s][r][c] = temp[s];
                                    }
                                }
                            }
                        }
                    });
                }
                ConcurrencyUtils.waitForCompletion(futures);

            } else {
                for (int s = 0; s < slices; s++) {
                    for (int r = 0; r < rows; r++) {
                        dctColumns.inverse(a[s][r], scale);
                    }
                }
                float[] temp = new float[rows];
                for (int s = 0; s < slices; s++) {
                    for (int c = 0; c < columns; c++) {
                        for (int r = 0; r < rows; r++) {
                            temp[r] = a[s][r][c];
                        }
                        dctRows.inverse(temp, scale);
                        for (int r = 0; r < rows; r++) {
                            a[s][r][c] = temp[r];
                        }
                    }
                }
                temp = new float[slices];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        for (int s = 0; s < slices; s++) {
                            temp[s] = a[s][r][c];
                        }
                        dctSlices.inverse(temp, scale);
                        for (int s = 0; s < slices; s++) {
                            a[s][r][c] = temp[s];
                        }
                    }
                }
            }
        }
    }

    private void ddxt3da_sub(int isgn, float[] a, boolean scale) {
        int idx0, idx1, idx2;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                for (int r = 0; r < rows; r++) {
                    dctColumns.forward(a, idx0 + r * rowStride, scale);
                }
                if (columns > 2) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = rows + r;
                            t[r] = a[idx1];
                            t[idx2] = a[idx1 + 1];
                            t[idx2 + rows] = a[idx1 + 2];
                            t[idx2 + 2 * rows] = a[idx1 + 3];
                        }
                        dctRows.forward(t, 0, scale);
                        dctRows.forward(t, rows, scale);
                        dctRows.forward(t, 2 * rows, scale);
                        dctRows.forward(t, 3 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = rows + r;
                            a[idx1] = t[r];
                            a[idx1 + 1] = t[idx2];
                            a[idx1 + 2] = t[idx2 + rows];
                            a[idx1 + 3] = t[idx2 + 2 * rows];
                        }
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        t[r] = a[idx1];
                        t[rows + r] = a[idx1 + 1];
                    }
                    dctRows.forward(t, 0, scale);
                    dctRows.forward(t, rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        a[idx1] = t[r];
                        a[idx1 + 1] = t[rows + r];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                idx0 = s * sliceStride;
                for (int r = 0; r < rows; r++) {
                    dctColumns.inverse(a, idx0 + r * rowStride, scale);
                }
                if (columns > 2) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = rows + r;
                            t[r] = a[idx1];
                            t[idx2] = a[idx1 + 1];
                            t[idx2 + rows] = a[idx1 + 2];
                            t[idx2 + 2 * rows] = a[idx1 + 3];
                        }
                        dctRows.inverse(t, 0, scale);
                        dctRows.inverse(t, rows, scale);
                        dctRows.inverse(t, 2 * rows, scale);
                        dctRows.inverse(t, 3 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx1 = idx0 + r * rowStride + c;
                            idx2 = rows + r;
                            a[idx1] = t[r];
                            a[idx1 + 1] = t[idx2];
                            a[idx1 + 2] = t[idx2 + rows];
                            a[idx1 + 3] = t[idx2 + 2 * rows];
                        }
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        t[r] = a[idx1];
                        t[rows + r] = a[idx1 + 1];
                    }
                    dctRows.inverse(t, 0, scale);
                    dctRows.inverse(t, rows, scale);
                    for (int r = 0; r < rows; r++) {
                        idx1 = idx0 + r * rowStride;
                        a[idx1] = t[r];
                        a[idx1 + 1] = t[rows + r];
                    }
                }
            }
        }
    }

    private void ddxt3da_sub(int isgn, float[][][] a, boolean scale) {
        int idx2;

        if (isgn == -1) {
            for (int s = 0; s < slices; s++) {
                for (int r = 0; r < rows; r++) {
                    dctColumns.forward(a[s][r], scale);
                }
                if (columns > 2) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = rows + r;
                            t[r] = a[s][r][c];
                            t[idx2] = a[s][r][c + 1];
                            t[idx2 + rows] = a[s][r][c + 2];
                            t[idx2 + 2 * rows] = a[s][r][c + 3];
                        }
                        dctRows.forward(t, 0, scale);
                        dctRows.forward(t, rows, scale);
                        dctRows.forward(t, 2 * rows, scale);
                        dctRows.forward(t, 3 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx2 = rows + r;
                            a[s][r][c] = t[r];
                            a[s][r][c + 1] = t[idx2];
                            a[s][r][c + 2] = t[idx2 + rows];
                            a[s][r][c + 3] = t[idx2 + 2 * rows];
                        }
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        t[r] = a[s][r][0];
                        t[rows + r] = a[s][r][1];
                    }
                    dctRows.forward(t, 0, scale);
                    dctRows.forward(t, rows, scale);
                    for (int r = 0; r < rows; r++) {
                        a[s][r][0] = t[r];
                        a[s][r][1] = t[rows + r];
                    }
                }
            }
        } else {
            for (int s = 0; s < slices; s++) {
                for (int r = 0; r < rows; r++) {
                    dctColumns.inverse(a[s][r], scale);
                }
                if (columns > 2) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int r = 0; r < rows; r++) {
                            idx2 = rows + r;
                            t[r] = a[s][r][c];
                            t[idx2] = a[s][r][c + 1];
                            t[idx2 + rows] = a[s][r][c + 2];
                            t[idx2 + 2 * rows] = a[s][r][c + 3];
                        }
                        dctRows.inverse(t, 0, scale);
                        dctRows.inverse(t, rows, scale);
                        dctRows.inverse(t, 2 * rows, scale);
                        dctRows.inverse(t, 3 * rows, scale);
                        for (int r = 0; r < rows; r++) {
                            idx2 = rows + r;
                            a[s][r][c] = t[r];
                            a[s][r][c + 1] = t[idx2];
                            a[s][r][c + 2] = t[idx2 + rows];
                            a[s][r][c + 3] = t[idx2 + 2 * rows];
                        }
                    }
                } else if (columns == 2) {
                    for (int r = 0; r < rows; r++) {
                        t[r] = a[s][r][0];
                        t[rows + r] = a[s][r][1];
                    }
                    dctRows.inverse(t, 0, scale);
                    dctRows.inverse(t, rows, scale);
                    for (int r = 0; r < rows; r++) {
                        a[s][r][0] = t[r];
                        a[s][r][1] = t[rows + r];
                    }
                }
            }
        }
    }

    private void ddxt3db_sub(int isgn, float[] a, boolean scale) {
        int idx0, idx1, idx2;

        if (isgn == -1) {
            if (columns > 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int c = 0; c < columns; c += 4) {
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = slices + s;
                            t[s] = a[idx1];
                            t[idx2] = a[idx1 + 1];
                            t[idx2 + slices] = a[idx1 + 2];
                            t[idx2 + 2 * slices] = a[idx1 + 3];
                        }
                        dctSlices.forward(t, 0, scale);
                        dctSlices.forward(t, slices, scale);
                        dctSlices.forward(t, 2 * slices, scale);
                        dctSlices.forward(t, 3 * slices, scale);
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = slices + s;
                            a[idx1] = t[s];
                            a[idx1 + 1] = t[idx2];
                            a[idx1 + 2] = t[idx2 + slices];
                            a[idx1 + 3] = t[idx2 + 2 * slices];
                        }
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        t[s] = a[idx1];
                        t[slices + s] = a[idx1 + 1];
                    }
                    dctSlices.forward(t, 0, scale);
                    dctSlices.forward(t, slices, scale);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        a[idx1] = t[s];
                        a[idx1 + 1] = t[slices + s];
                    }
                }
            }
        } else {
            if (columns > 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int c = 0; c < columns; c += 4) {
                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = slices + s;
                            t[s] = a[idx1];
                            t[idx2] = a[idx1 + 1];
                            t[idx2 + slices] = a[idx1 + 2];
                            t[idx2 + 2 * slices] = a[idx1 + 3];
                        }
                        dctSlices.inverse(t, 0, scale);
                        dctSlices.inverse(t, slices, scale);
                        dctSlices.inverse(t, 2 * slices, scale);
                        dctSlices.inverse(t, 3 * slices, scale);

                        for (int s = 0; s < slices; s++) {
                            idx1 = s * sliceStride + idx0 + c;
                            idx2 = slices + s;
                            a[idx1] = t[s];
                            a[idx1 + 1] = t[idx2];
                            a[idx1 + 2] = t[idx2 + slices];
                            a[idx1 + 3] = t[idx2 + 2 * slices];
                        }
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    idx0 = r * rowStride;
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        t[s] = a[idx1];
                        t[slices + s] = a[idx1 + 1];
                    }
                    dctSlices.inverse(t, 0, scale);
                    dctSlices.inverse(t, slices, scale);
                    for (int s = 0; s < slices; s++) {
                        idx1 = s * sliceStride + idx0;
                        a[idx1] = t[s];
                        a[idx1 + 1] = t[slices + s];
                    }
                }
            }
        }
    }

    private void ddxt3db_sub(int isgn, float[][][] a, boolean scale) {
        int idx2;

        if (isgn == -1) {
            if (columns > 2) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int s = 0; s < slices; s++) {
                            idx2 = slices + s;
                            t[s] = a[s][r][c];
                            t[idx2] = a[s][r][c + 1];
                            t[idx2 + slices] = a[s][r][c + 2];
                            t[idx2 + 2 * slices] = a[s][r][c + 3];
                        }
                        dctSlices.forward(t, 0, scale);
                        dctSlices.forward(t, slices, scale);
                        dctSlices.forward(t, 2 * slices, scale);
                        dctSlices.forward(t, 3 * slices, scale);
                        for (int s = 0; s < slices; s++) {
                            idx2 = slices + s;
                            a[s][r][c] = t[s];
                            a[s][r][c + 1] = t[idx2];
                            a[s][r][c + 2] = t[idx2 + slices];
                            a[s][r][c + 3] = t[idx2 + 2 * slices];
                        }
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        t[s] = a[s][r][0];
                        t[slices + s] = a[s][r][1];
                    }
                    dctSlices.forward(t, 0, scale);
                    dctSlices.forward(t, slices, scale);
                    for (int s = 0; s < slices; s++) {
                        a[s][r][0] = t[s];
                        a[s][r][1] = t[slices + s];
                    }
                }
            }
        } else {
            if (columns > 2) {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c += 4) {
                        for (int s = 0; s < slices; s++) {
                            idx2 = slices + s;
                            t[s] = a[s][r][c];
                            t[idx2] = a[s][r][c + 1];
                            t[idx2 + slices] = a[s][r][c + 2];
                            t[idx2 + 2 * slices] = a[s][r][c + 3];
                        }
                        dctSlices.inverse(t, 0, scale);
                        dctSlices.inverse(t, slices, scale);
                        dctSlices.inverse(t, 2 * slices, scale);
                        dctSlices.inverse(t, 3 * slices, scale);

                        for (int s = 0; s < slices; s++) {
                            idx2 = slices + s;
                            a[s][r][c] = t[s];
                            a[s][r][c + 1] = t[idx2];
                            a[s][r][c + 2] = t[idx2 + slices];
                            a[s][r][c + 3] = t[idx2 + 2 * slices];
                        }
                    }
                }
            } else if (columns == 2) {
                for (int r = 0; r < rows; r++) {
                    for (int s = 0; s < slices; s++) {
                        t[s] = a[s][r][0];
                        t[slices + s] = a[s][r][1];
                    }
                    dctSlices.inverse(t, 0, scale);
                    dctSlices.inverse(t, slices, scale);
                    for (int s = 0; s < slices; s++) {
                        a[s][r][0] = t[s];
                        a[s][r][1] = t[slices + s];
                    }
                }
            }
        }
    }

    private void ddxt3da_subth(final int isgn, final float[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > slices ? slices : ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * rows;
        if (columns == 2) {
            nt >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx0, idx1, idx2;
                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            for (int r = 0; r < rows; r++) {
                                dctColumns.forward(a, idx0 + r * rowStride, scale);
                            }
                            if (columns > 2) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + rows + r;
                                        t[startt + r] = a[idx1];
                                        t[idx2] = a[idx1 + 1];
                                        t[idx2 + rows] = a[idx1 + 2];
                                        t[idx2 + 2 * rows] = a[idx1 + 3];
                                    }
                                    dctRows.forward(t, startt, scale);
                                    dctRows.forward(t, startt + rows, scale);
                                    dctRows.forward(t, startt + 2 * rows, scale);
                                    dctRows.forward(t, startt + 3 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx1 = idx0 + r * rowStride + c;
                                        idx2 = startt + rows + r;
                                        a[idx1] = t[startt + r];
                                        a[idx1 + 1] = t[idx2];
                                        a[idx1 + 2] = t[idx2 + rows];
                                        a[idx1 + 3] = t[idx2 + 2 * rows];
                                    }
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    t[startt + r] = a[idx1];
                                    t[startt + rows + r] = a[idx1 + 1];
                                }
                                dctRows.forward(t, startt, scale);
                                dctRows.forward(t, startt + rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    a[idx1] = t[startt + r];
                                    a[idx1 + 1] = t[startt + rows + r];
                                }
                            }
                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            idx0 = s * sliceStride;
                            for (int r = 0; r < rows; r++) {
                                dctColumns.inverse(a, idx0 + r * rowStride, scale);
                            }
                            if (columns > 2) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int j = 0; j < rows; j++) {
                                        idx1 = idx0 + j * rowStride + c;
                                        idx2 = startt + rows + j;
                                        t[startt + j] = a[idx1];
                                        t[idx2] = a[idx1 + 1];
                                        t[idx2 + rows] = a[idx1 + 2];
                                        t[idx2 + 2 * rows] = a[idx1 + 3];
                                    }
                                    dctRows.inverse(t, startt, scale);
                                    dctRows.inverse(t, startt + rows, scale);
                                    dctRows.inverse(t, startt + 2 * rows, scale);
                                    dctRows.inverse(t, startt + 3 * rows, scale);
                                    for (int j = 0; j < rows; j++) {
                                        idx1 = idx0 + j * rowStride + c;
                                        idx2 = startt + rows + j;
                                        a[idx1] = t[startt + j];
                                        a[idx1 + 1] = t[idx2];
                                        a[idx1 + 2] = t[idx2 + rows];
                                        a[idx1 + 3] = t[idx2 + 2 * rows];
                                    }
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    t[startt + r] = a[idx1];
                                    t[startt + rows + r] = a[idx1 + 1];
                                }
                                dctRows.inverse(t, startt, scale);
                                dctRows.inverse(t, startt + rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    idx1 = idx0 + r * rowStride;
                                    a[idx1] = t[startt + r];
                                    a[idx1 + 1] = t[startt + rows + r];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt3da_subth(final int isgn, final float[][][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > slices ? slices : ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * rows;
        if (columns == 2) {
            nt >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx2;
                    if (isgn == -1) {
                        for (int s = n0; s < slices; s += nthreads) {
                            for (int r = 0; r < rows; r++) {
                                dctColumns.forward(a[s][r], scale);
                            }
                            if (columns > 2) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + rows + r;
                                        t[startt + r] = a[s][r][c];
                                        t[idx2] = a[s][r][c + 1];
                                        t[idx2 + rows] = a[s][r][c + 2];
                                        t[idx2 + 2 * rows] = a[s][r][c + 3];
                                    }
                                    dctRows.forward(t, startt, scale);
                                    dctRows.forward(t, startt + rows, scale);
                                    dctRows.forward(t, startt + 2 * rows, scale);
                                    dctRows.forward(t, startt + 3 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + rows + r;
                                        a[s][r][c] = t[startt + r];
                                        a[s][r][c + 1] = t[idx2];
                                        a[s][r][c + 2] = t[idx2 + rows];
                                        a[s][r][c + 3] = t[idx2 + 2 * rows];
                                    }
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    t[startt + r] = a[s][r][0];
                                    t[startt + rows + r] = a[s][r][1];
                                }
                                dctRows.forward(t, startt, scale);
                                dctRows.forward(t, startt + rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    a[s][r][0] = t[startt + r];
                                    a[s][r][1] = t[startt + rows + r];
                                }
                            }
                        }
                    } else {
                        for (int s = n0; s < slices; s += nthreads) {
                            for (int r = 0; r < rows; r++) {
                                dctColumns.inverse(a[s][r], scale);
                            }
                            if (columns > 2) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + rows + r;
                                        t[startt + r] = a[s][r][c];
                                        t[idx2] = a[s][r][c + 1];
                                        t[idx2 + rows] = a[s][r][c + 2];
                                        t[idx2 + 2 * rows] = a[s][r][c + 3];
                                    }
                                    dctRows.inverse(t, startt, scale);
                                    dctRows.inverse(t, startt + rows, scale);
                                    dctRows.inverse(t, startt + 2 * rows, scale);
                                    dctRows.inverse(t, startt + 3 * rows, scale);
                                    for (int r = 0; r < rows; r++) {
                                        idx2 = startt + rows + r;
                                        a[s][r][c] = t[startt + r];
                                        a[s][r][c + 1] = t[idx2];
                                        a[s][r][c + 2] = t[idx2 + rows];
                                        a[s][r][c + 3] = t[idx2 + 2 * rows];
                                    }
                                }
                            } else if (columns == 2) {
                                for (int r = 0; r < rows; r++) {
                                    t[startt + r] = a[s][r][0];
                                    t[startt + rows + r] = a[s][r][1];
                                }
                                dctRows.inverse(t, startt, scale);
                                dctRows.inverse(t, startt + rows, scale);
                                for (int r = 0; r < rows; r++) {
                                    a[s][r][0] = t[startt + r];
                                    a[s][r][1] = t[startt + rows + r];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt3db_subth(final int isgn, final float[] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * slices;
        if (columns == 2) {
            nt >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];
        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx0, idx1, idx2;
                    if (isgn == -1) {
                        if (columns > 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int c = 0; c < columns; c += 4) {
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + slices + s;
                                        t[startt + s] = a[idx1];
                                        t[idx2] = a[idx1 + 1];
                                        t[idx2 + slices] = a[idx1 + 2];
                                        t[idx2 + 2 * slices] = a[idx1 + 3];
                                    }
                                    dctSlices.forward(t, startt, scale);
                                    dctSlices.forward(t, startt + slices, scale);
                                    dctSlices.forward(t, startt + 2 * slices, scale);
                                    dctSlices.forward(t, startt + 3 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + slices + s;
                                        a[idx1] = t[startt + s];
                                        a[idx1 + 1] = t[idx2];
                                        a[idx1 + 2] = t[idx2 + slices];
                                        a[idx1 + 3] = t[idx2 + 2 * slices];
                                    }
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    t[startt + s] = a[idx1];
                                    t[startt + slices + s] = a[idx1 + 1];
                                }
                                dctSlices.forward(t, startt, scale);
                                dctSlices.forward(t, startt + slices, scale);
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    a[idx1] = t[startt + s];
                                    a[idx1 + 1] = t[startt + slices + s];
                                }
                            }
                        }
                    } else {
                        if (columns > 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int c = 0; c < columns; c += 4) {
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + slices + s;
                                        t[startt + s] = a[idx1];
                                        t[idx2] = a[idx1 + 1];
                                        t[idx2 + slices] = a[idx1 + 2];
                                        t[idx2 + 2 * slices] = a[idx1 + 3];
                                    }
                                    dctSlices.inverse(t, startt, scale);
                                    dctSlices.inverse(t, startt + slices, scale);
                                    dctSlices.inverse(t, startt + 2 * slices, scale);
                                    dctSlices.inverse(t, startt + 3 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx1 = s * sliceStride + idx0 + c;
                                        idx2 = startt + slices + s;
                                        a[idx1] = t[startt + s];
                                        a[idx1 + 1] = t[idx2];
                                        a[idx1 + 2] = t[idx2 + slices];
                                        a[idx1 + 3] = t[idx2 + 2 * slices];
                                    }
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                idx0 = r * rowStride;
                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    t[startt + s] = a[idx1];
                                    t[startt + slices + s] = a[idx1 + 1];
                                }
                                dctSlices.inverse(t, startt, scale);
                                dctSlices.inverse(t, startt + slices, scale);

                                for (int s = 0; s < slices; s++) {
                                    idx1 = s * sliceStride + idx0;
                                    a[idx1] = t[startt + s];
                                    a[idx1 + 1] = t[startt + slices + s];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }

    private void ddxt3db_subth(final int isgn, final float[][][] a, final boolean scale) {
        final int nthreads = ConcurrencyUtils.getNumberOfThreads() > rows ? rows : ConcurrencyUtils.getNumberOfThreads();
        int nt = 4 * slices;
        if (columns == 2) {
            nt >>= 1;
        }
        Future<?>[] futures = new Future[nthreads];

        for (int i = 0; i < nthreads; i++) {
            final int n0 = i;
            final int startt = nt * i;
            futures[i] = ConcurrencyUtils.submit(new Runnable() {

                public void run() {
                    int idx2;
                    if (isgn == -1) {
                        if (columns > 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + slices + s;
                                        t[startt + s] = a[s][r][c];
                                        t[idx2] = a[s][r][c + 1];
                                        t[idx2 + slices] = a[s][r][c + 2];
                                        t[idx2 + 2 * slices] = a[s][r][c + 3];
                                    }
                                    dctSlices.forward(t, startt, scale);
                                    dctSlices.forward(t, startt + slices, scale);
                                    dctSlices.forward(t, startt + 2 * slices, scale);
                                    dctSlices.forward(t, startt + 3 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + slices + s;
                                        a[s][r][c] = t[startt + s];
                                        a[s][r][c + 1] = t[idx2];
                                        a[s][r][c + 2] = t[idx2 + slices];
                                        a[s][r][c + 3] = t[idx2 + 2 * slices];
                                    }
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    t[startt + s] = a[s][r][0];
                                    t[startt + slices + s] = a[s][r][1];
                                }
                                dctSlices.forward(t, startt, scale);
                                dctSlices.forward(t, startt + slices, scale);
                                for (int s = 0; s < slices; s++) {
                                    a[s][r][0] = t[startt + s];
                                    a[s][r][1] = t[startt + slices + s];
                                }
                            }
                        }
                    } else {
                        if (columns > 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int c = 0; c < columns; c += 4) {
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + slices + s;
                                        t[startt + s] = a[s][r][c];
                                        t[idx2] = a[s][r][c + 1];
                                        t[idx2 + slices] = a[s][r][c + 2];
                                        t[idx2 + 2 * slices] = a[s][r][c + 3];
                                    }
                                    dctSlices.inverse(t, startt, scale);
                                    dctSlices.inverse(t, startt + slices, scale);
                                    dctSlices.inverse(t, startt + 2 * slices, scale);
                                    dctSlices.inverse(t, startt + 3 * slices, scale);
                                    for (int s = 0; s < slices; s++) {
                                        idx2 = startt + slices + s;
                                        a[s][r][c] = t[startt + s];
                                        a[s][r][c + 1] = t[idx2];
                                        a[s][r][c + 2] = t[idx2 + slices];
                                        a[s][r][c + 3] = t[idx2 + 2 * slices];
                                    }
                                }
                            }
                        } else if (columns == 2) {
                            for (int r = n0; r < rows; r += nthreads) {
                                for (int s = 0; s < slices; s++) {
                                    t[startt + s] = a[s][r][0];
                                    t[startt + slices + s] = a[s][r][1];
                                }
                                dctSlices.inverse(t, startt, scale);
                                dctSlices.inverse(t, startt + slices, scale);

                                for (int s = 0; s < slices; s++) {
                                    a[s][r][0] = t[startt + s];
                                    a[s][r][1] = t[startt + slices + s];
                                }
                            }
                        }
                    }
                }
            });
        }
        ConcurrencyUtils.waitForCompletion(futures);
    }
}
