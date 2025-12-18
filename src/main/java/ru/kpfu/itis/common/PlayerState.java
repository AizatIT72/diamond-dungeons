package ru.kpfu.itis.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String characterType;
    public int x, y;
    public int lives;  
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
        this.lives = 3;  
        this.diamonds = 0;
        this.hasKey = false;
        this.isAlive = true;
        this.lastMoveTime = System.currentTimeMillis();
        this.inventory = new HashMap<>();
    }

    public void loseLife() {
        if (lives > 0) {
            lives--;

            if (lives <= 0) {
                isAlive = false;
            }
        } else {
            isAlive = false;
        }
    }

    public void addDiamond() {
        diamonds++;
    }

    public boolean canMove() {

        return lives > 0 && System.currentTimeMillis() - lastMoveTime > 100;
    }

    public int getMoveSpeed() {
        switch (characterType) {
            case "Зеленый плут": return 2;
            default: return 1;
        }
    }
}