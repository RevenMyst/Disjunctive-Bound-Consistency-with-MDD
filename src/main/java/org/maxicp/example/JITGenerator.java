package org.maxicp.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class JITGenerator {

    static void generator(int n, int d, int w, int seed) {
        Random r = new Random(seed);

        int [] duration = new int[n];
        int [] initialStartMin = new int[n];
        int [] endMax = new int[n];            // end = start + duration (original)
        int [] startMin = new int[n];
        int [] endObj = new int[n];
        int [] endMaxAdjusted = new int[n];    // corrected endMax to respect the window

        // -----------------------------
        // 1. Generate original timeline
        // -----------------------------
        for (int i = 0; i < n; i++) {
            initialStartMin[i] = (i == 0) ? 0 : endMax[i - 1];
            duration[i] = r.nextInt(d) + 1;
            endMax[i] = initialStartMin[i] + duration[i];
        }

        // -----------------------------
        // 2. Compute constraints (window w)
        // -----------------------------
        for (int i = 0; i < n; i++) {

            // startMin from initialStartMin with window
            startMin[i] = initialStartMin[Math.max(i - w, 0)];

            // corrected endMax — DO NOT overwrite the original endMax!
            if (i > n - w) {
                endMaxAdjusted[i] = n * d;  // upper bound limit
            } else {
                endMaxAdjusted[i] = endMax[Math.min(i + w, n - 1)];
            }

            int min = startMin[i] + duration[i];
            int max = endMaxAdjusted[i];
            endObj[i] = r.nextInt(max - min + 1) + min;
        }

        // -----------------------------
        // 3. Shuffle consistently
        // -----------------------------
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;

        Collections.shuffle(Arrays.asList(order));

        int[] durationShuffled = new int[n];
        int[] startMinShuffled = new int[n];
        int[] endObjShuffled = new int[n];
        int[] endMaxShuffled = new int[n]; // shuffled adjusted endMax

        for (int i = 0; i < n; i++) {
            int idx = order[i];
            durationShuffled[i] = duration[idx];
            startMinShuffled[i] = startMin[idx];
            endObjShuffled[i] = endObj[idx];
            endMaxShuffled[i] = endMaxAdjusted[idx];
        }

        // overwrite
        duration = durationShuffled;
        startMin = startMinShuffled;
        endObj = endObjShuffled;
        endMaxAdjusted = endMaxShuffled;

        // -----------------------------
        // 4. File output
        // -----------------------------
        String filename = "data/JIT/jit" + n + "/JIT-" + n + "-" + d + "-" + w + "-" + seed + ".txt";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {

            bw.write(n + " " + d + " " + w);
            bw.newLine();

            for (int i = 0; i < n; i++) {
                bw.write(startMin[i] + " " + duration[i] + " " + endMaxAdjusted[i] + " " + endObj[i]);
                bw.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        // for n = 15, 30 ,60 ,120
        // for w = 1, 2, 3
        // for 20 seeds
        int[] ns = {18,25,30,40};
        int[] ws = {2};
        int d = 25;
        for(int n : ns){
            for(int w : ws){
                for(int seed = 0; seed < 20; seed++){
                    generator(n,d,w,seed);
                }
            }
        }
    }
}
