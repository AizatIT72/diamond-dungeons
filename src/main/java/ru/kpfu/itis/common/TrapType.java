package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Тип активации ловушки.
 * PRESSURE - активируется при наступлении игрока на триггерную клетку
 * TIMER - активируется периодически по таймеру
 */
public enum TrapType implements Serializable {
    PRESSURE,    // Контактная - по наступлению
    TIMER;       // Периодическая - по таймеру

    private static final long serialVersionUID = 1L;
}

