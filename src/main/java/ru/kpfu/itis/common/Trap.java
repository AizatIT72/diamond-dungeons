package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Ловушка - объект, который атакует соседние клетки.
 * Ловушка НЕ находится в клетке поражения, а стреляет в направлении.
 */
public class Trap implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;  // Позиция самой ловушки (в стене или сбоку)
    public int y;
    
    public TrapType type;        // Тип активации (PRESSURE или TIMER)
    public TrapAttack attack;    // Тип атаки (ARROW или FIRE)
    public Direction direction;  // Направление выстрела (UP, DOWN, LEFT, RIGHT)
    
    public int range;                    // Дальность поражения (1 или 2 клетки)
    public long lastActivation;          // Время последней активации (для таймера)
    public long lastDamageTime;          // Время последнего нанесения урона (для кулдауна)
    public boolean active;               // Стреляет сейчас или нет
    
    // Для таймерных ловушек
    private static final long TIMER_INTERVAL = 3000;  // Интервал между выстрелами (мс)
    private static final long ACTIVE_DURATION = 300;  // Длительность выстрела (мс)
    public static final long DAMAGE_COOLDOWN = 1000;  // Кулдаун урона (мс) - как у мобов

    public Trap() {
        this.active = false;
        this.lastActivation = 0;
        this.lastDamageTime = 0;
    }

    public Trap(int x, int y, TrapType type, TrapAttack attack, Direction direction) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.attack = attack;
        this.direction = direction;
        this.range = attack.range;
        this.active = false;
        this.lastActivation = System.currentTimeMillis();
        this.lastDamageTime = 0;
    }

    /**
     * Проверяет, нужно ли активировать таймерную ловушку.
     */
    public boolean shouldActivate(long currentTime) {
        if (type == TrapType.TIMER) {
            return (currentTime - lastActivation) >= TIMER_INTERVAL;
        }
        return false;
    }

    /**
     * Активирует ловушку.
     */
    public void activate(long currentTime) {
        this.active = true;
        this.lastActivation = currentTime;
    }

    /**
     * Деактивирует ловушку после выстрела.
     */
    public void deactivate(long currentTime) {
        if (active && (currentTime - lastActivation) >= ACTIVE_DURATION) {
            this.active = false;
        }
    }

    @Override
    public String toString() {
        return "Trap{x=" + x + ", y=" + y + ", type=" + type + ", attack=" + attack + ", dir=" + direction + "}";
    }
}

