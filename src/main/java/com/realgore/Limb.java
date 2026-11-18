package com.realgore;

/** Битовая маска отрубленных конечностей + соответствие индексам частей тела (GibEntity). */
public final class Limb {
    public static final int HEAD = 1;
    public static final int RIGHT_ARM = 2;
    public static final int LEFT_ARM = 4;
    public static final int RIGHT_LEG = 8;
    public static final int LEFT_LEG = 16;

    /** part: 0 голова, 1 торс, 2 правая рука, 3 левая рука, 4 правая нога, 5 левая нога */
    public static int bit(int part) {
        return switch (part) {
            case 0 -> HEAD;
            case 2 -> RIGHT_ARM;
            case 3 -> LEFT_ARM;
            case 4 -> RIGHT_LEG;
            case 5 -> LEFT_LEG;
            default -> 0;
        };
    }

    private Limb() {
    }
}
