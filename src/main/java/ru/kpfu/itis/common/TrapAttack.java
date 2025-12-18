package ru.kpfu.itis.common;

import java.io.Serializable;

public enum TrapAttack implements Serializable {
    ARROW(2),  
    FIRE(1);   

    public final int range;

    TrapAttack(int range) {
        this.range = range;
    }

    private static final long serialVersionUID = 1L;
}
