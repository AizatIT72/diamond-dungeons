package ru.kpfu.itis.common;

import java.io.Serializable;
import java.util.List;

public class Enemy implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum EnemyType {
        BAT(1, 10, 2, 2, "Летучая мышь"),
        SKELETON(2, 30, 5, 1, "Скелет"),
        GHOST(3, 20, 3, 3, "Призрак"),
        TRAP(4, 1, 15, 0, "Ловушка");

        public final int id;
        public final int health;
        public final int damage;
        public final int speed;
        public final String name;

        EnemyType(int id, int health, int damage, int speed, String name) {
            this.id = id;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public int id;
    public EnemyType type;
    public int x, y;
    public int health;
    public Direction direction;
    public long lastMoveTime;
    public long lastAttackTime;  
    public boolean isActive;

    public Enemy(int id, EnemyType type, int x, int y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.health = type.health;
        this.direction = Direction.DOWN;
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAttackTime = 0;  
        this.isActive = true;
    }

    public void move(TileType[][] map, List<PlayerState> players) {
        if (type.speed == 0 || !isActive) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 1000 / type.speed) return;

        Direction[] directions = Direction.values();
        Direction newDir = directions[(int)(Math.random() * directions.length)];

        int newX = x + newDir.dx;
        int newY = y + newDir.dy;

        if (newX >= 0 && newX < map[0].length &&
                newY >= 0 && newY < map.length &&
                map[newY][newX].isWalkable()) {
            x = newX;
            y = newY;
            direction = newDir;
        }

        lastMoveTime = currentTime;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            isActive = false;
        }
    }

    @Override
    public String toString() {
        return type.name + " (HP: " + health + ")";
    }
}