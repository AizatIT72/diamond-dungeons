package ru.kpfu.itis.common;

import java.io.Serializable;

public class PatrolEnemy implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;
    public int y;
    public PatrolDirection direction;
    public PatrolAxis axis;
    public long lastMoveTime;
    public long lastAttackTime;  

    public PatrolEnemy() {
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAttackTime = 0;  
    }

    public PatrolEnemy(int x, int y, PatrolAxis axis, PatrolDirection direction) {
        this.x = x;
        this.y = y;
        this.axis = axis;
        this.direction = direction;
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAttackTime = 0;  
    }

    @Override
    public String toString() {
        return "PatrolEnemy{x=" + x + ", y=" + y + ", axis=" + axis + ", dir=" + direction + "}";
    }
}
