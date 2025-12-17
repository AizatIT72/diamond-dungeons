package ru.kpfu.itis.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PlayerState implements Serializable {
    // ДОБАВИТЬ ЭТУ СТРОЧКУ
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String characterType;
    public int x, y;
    public int health;
    public int maxHealth;
    public int diamonds;
    public boolean hasKey;
    public boolean isAlive;
    public long lastMoveTime;
    public Map<String, Integer> inventory;

    public PlayerState(int id, String name, String characterType) {
        this.id = id;
        this.name = name;
        this.characterType = characterType;
        this.x = 1;
        this.y = 1;
        this.health = 100;
        this.maxHealth = 100;
        this.diamonds = 0;
        this.hasKey = false;
        this.isAlive = true;
        this.lastMoveTime = System.currentTimeMillis();
        this.inventory = new HashMap<>();
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            isAlive = false;
        }
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void addDiamond() {
        diamonds++;
    }

    public boolean canMove() {
        return isAlive && System.currentTimeMillis() - lastMoveTime > 100;
    }

    public int getMoveSpeed() {
        switch (characterType) {
            case "Зеленый плут": return 2;
            default: return 1;
        }
    }
}