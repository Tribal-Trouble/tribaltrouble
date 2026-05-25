package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

public final class LinearSolver {

    private static final float THRESHOLD = 0.001f;

    public static void solve(float @NonNull [] @NonNull [] eq_system_orig, float @NonNull [] solution) {
        float[][] eq_system = copyEquation(eq_system_orig);
//System.out.println("orig system:");
//dumpEquation(eq_system);
        int pivot_row = 0;
        for (int column = 0; column < eq_system[0].length - 1; column++) {
            if (solve(column, pivot_row, eq_system))
                pivot_row++;
        }
//dumpEquation(eq_system);
        reduce(eq_system);
        /*System.out.println("reduced system:");
        dumpEquation(eq_system);*/
        assert checkEquation(eq_system);
        assignSolutions(eq_system, solution);
//dumpSolution(solution);
        assert checkSolution(eq_system_orig, solution);
    }

    private static float[][] copyEquation(float @NonNull [] @NonNull [] orig_eq) {
        float[][] result = new float[orig_eq.length][orig_eq[0].length];
        for (int row = 0; row < result.length; row++) {
            System.arraycopy(orig_eq[row], 0, result[row], 0, result[0].length);
        }
        return result;
    }

    private static boolean checkSolution(float @NonNull [] @NonNull [] eq_system, float @NonNull [] solution) {
        for (float[] eq_system1 : eq_system) {
            float right_side = 0f;
            for (int column = 0; column < solution.length; column++) {
                right_side += eq_system1[column] * solution[column];
            }
            float left_side = eq_system1[eq_system[0].length - 1];
            if (!isEqual(right_side, left_side)) {
                IO.println("mismatch " + right_side + " != " + left_side);
                return false;
            }
        }
        return true;
    }

    private static void assignSolutions(float @NonNull [] @NonNull [] eq_system, float[] solution) {
        int row = 0;
        for (int column = 0; column < eq_system[0].length - 1; column++) {
            float pivot = eq_system[row][column];
            if (!isZero(pivot)) {
                solution[column] = eq_system[row][eq_system[0].length - 1];
                row++;
                if (row == eq_system.length)
                    break;
            }
        }
    }

    private static void reduce(float @NonNull [] @NonNull [] eq_system) {
        for (int row = 1; row < eq_system.length; row++) {
            int column;
            for (column = 0; column < eq_system[0].length; column++) {
                float pivot = eq_system[row][column];
                if (!isZero(pivot))
                    break;
            }
            if (column == eq_system[0].length)
                continue;
            for (int inner_row = 0; inner_row < row; inner_row++) {
                float row_pivot = eq_system[inner_row][column];
                subtractRow(row_pivot, eq_system[inner_row], eq_system[row]);
            }
        }
    }

    private static boolean checkEquation(float @NonNull [] @NonNull [] eq_system) {
        for (float[] eq_system1 : eq_system) {
            int num_non_zero = 0;
            for (int column = 0; column < eq_system[0].length; column++) {
                if (!isZero(eq_system1[column])) {
                    num_non_zero++;
                }
            }
            if (num_non_zero == 1 && !isZero(eq_system1[eq_system[0].length - 1])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEqual(float val1, float val2) {
        return Math.abs(val1 - val2) <= THRESHOLD;
    }

    public static boolean isZero(float val) {
        return isEqual(val, 0f);
    }

    private static void swapRows(int row_index1, int row_index2, float @NonNull [] @NonNull [] eq_system) {
        float[] row = eq_system[row_index1];
        eq_system[row_index1] = eq_system[row_index2];
        eq_system[row_index2] = row;
//System.out.println("swapping rows " + row_index1 + " " + row_index2);
    }

    private static void subtractRow(float scale, float @NonNull [] row1, float[] row2) {
        for (int i = 0; i < row1.length; i++) {
            row1[i] -= scale * row2[i];
        }
    }

    private static void scaleRow(float scale, float @NonNull [] row) {
        for (int column = 0; column < row.length; column++) {
            row[column] *= scale;
        }
    }

    private static boolean solve(int column, int pivot_row, float @NonNull [] @NonNull [] eq_system) {
        for (int row = pivot_row; row < eq_system.length; row++) {
            float pivot = eq_system[row][column];
            if (!isZero(pivot)) {
                swapRows(pivot_row, row, eq_system);
                scaleRow(1f / pivot, eq_system[pivot_row]);
//System.out.println("pivot " + pivot);
                for (int inner_row = pivot_row + 1; inner_row < eq_system.length; inner_row++) {
                    float row_pivot = eq_system[inner_row][column];
//System.out.println("row pivot " + row_pivot);
                    subtractRow(row_pivot, eq_system[inner_row], eq_system[pivot_row]);
                }
                return true;
            }
        }
        return false;
    }

    /*	private static final void dumpEquation(float[][] eq_system) {
    	for (int row = 0; row < eq_system.length; row++)
    		dumpSolution(eq_system[row]);
    	System.out.println();
    }
     */
    public static void dumpSolution(float @NonNull [] solution) {
        for (float v : solution) {
            IO.print(v + " ");
        }
        IO.println();
    }

    /*	public static final void main(String[] args) {
    	float[][] system0 = new float[][]{{1, 2, 1, 3}, {3, -1, -3, -1}, {2, 3, 1, 4}};
    	float[][] system1 = new float[][]{{0, -1, -1, 1, 0}, {1, 1, 1, 1, 6}, {2, 4, 1, -2, -1}, {3, 1, -2, 2, 3}};
    	float[][] system2 = new float[][]{{1, 1, 1, 1, 1, 1}, {-1, -1, 0, 0, 1, -1}, {-2, -2, 0, 0, 3, 1}, {0, 0, 1, 1, 3, -1}, {1, 1, 2, 2, 4, 1}};
    	float[][] system3 = new float[][]{{1, 1, 1, 1, 1, 1}, {-1, -1, 0, 0, 1, -1}, {-2, -2, 0, 0, 3, 1}, {0, 0, 1, 1, 3, 3}, {1, 1, 2, 2, 4, 4}};
    	float[] solution0 = new float[system0[0].length - 1];
    	float[] solution1 = new float[system1[0].length - 1];
    	float[] solution2 = new float[system2[0].length - 1];
    	float[] solution3 = new float[system3[0].length - 1];
    	solve(system0, solution0);
    	solve(system1, solution1);
    //		solve(system2, solution2);
    	solve(system3, solution3);
    }*/
    private LinearSolver() {
    }
}
