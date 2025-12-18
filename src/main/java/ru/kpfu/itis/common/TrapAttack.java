package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Тип атаки ловушки.
 * ARROW - стрела (дальность 2 клетки)
 * FIRE - пламя (дальность 1 клетка)
 */
public enum TrapAttack implements Serializable {
    ARROW(2),  // Стрела - дальность 2 клетки
    FIRE(1);   // Пламя - дальность 1 клетка

    public final int range;

    TrapAttack(int range) {
        this.range = range;
    }

    private static final long serialVersionUID = 1L;
}

