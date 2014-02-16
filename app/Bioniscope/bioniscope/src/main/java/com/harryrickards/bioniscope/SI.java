package com.harryrickards.bioniscope;

/**
 * Helpful SI formatting methods
 */
public class SI {
    // Formatting SI numbers with 3SF
    // e.g. 100,000 -> 100k
    public static String formatSI(double num) {
        // Handle 0
        if (num < 1e-4 && num > -1e-4) { return "0.00 "; }

        // Handle negative numbers
        if (num < 0) { return "-"+formatSI(-num); }

        int exp = (int) Math.floor(Math.log10(num)/3);

        // Handle tiny/massive numbers
        if (exp < -4 || exp > 4) { return String.format("%.3e", num); }

        char si = "pnum kMGT".charAt(exp+4);
        return String.format("%.3g%s", num/Math.pow(10,exp*3), si);
    }
}
