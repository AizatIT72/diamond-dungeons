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
    public int lives;  // Количество жизней (заменило health/maxHealth)
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
        this.lives = 3;  // 3 жизни по умолчанию
        this.diamonds = 0;
        this.hasKey = false;
        this.isAlive = true;
        this.lastMoveTime = System.currentTimeMillis();
        this.inventory = new HashMap<>();
    }

    /**
     * Отнимает жизнь у игрока.
     * Игрок теряет 1 жизнь, но продолжает играть на месте.
     * Если жизни закончились - помечает как мёртвого для возрождения.
     */
    public void loseLife() {
        if (lives > 0) {
            lives--;
            // Если жизни закончились - игрок мёртв и будет возрождён
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
        // Игрок может двигаться, если у него есть жизни и прошло достаточно времени с последнего движения
        return lives > 0 && System.currentTimeMillis() - lastMoveTime > 100;
    }

    public int getMoveSpeed() {
        switch (characterType) {
            case "Зеленый плут": return 2;
            default: return 1;
        }
    }
}