package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Патрульный моб - движется по прямой линии (горизонтально или вертикально),
 * разворачивается при столкновении со стеной.
 * НЕ реагирует на игрока, просто наказывает за ошибку.
 */
public class PatrolEnemy implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;
    public int y;
    public PatrolDirection direction;
    public PatrolAxis axis;
    public long lastMoveTime;
    public long lastAttackTime;  // Время последней атаки для кулдауна

    public PatrolEnemy() {
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAttackTime = 0;  // Инициализируем как 0, чтобы первая атака была возможна
    }

    public PatrolEnemy(int x, int y, PatrolAxis axis, PatrolDirection direction) {
        this.x = x;
        this.y = y;
        this.axis = axis;
        this.direction = direction;
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAttackTime = 0;  // Инициализируем как 0, чтобы первая атака была возможна
    }

    @Override
    public String toString() {
        return "PatrolEnemy{x=" + x + ", y=" + y + ", axis=" + axis + ", dir=" + direction + "}";
    }
}

