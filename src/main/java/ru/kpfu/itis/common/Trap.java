package ru.kpfu.itis.common;

import java.io.Serializable;

public class Trap implements Serializable {
    private static final long serialVersionUID = 1L;

    public int x;  
    public int y;

    public TrapType type;        
    public TrapAttack attack;    
    public Direction direction;  

    public int range;                    
    public long lastActivation;          
    public long lastDamageTime;          
    public boolean active;               

    private static final long TIMER_INTERVAL = 3000;  
    private static final long ACTIVE_DURATION = 300;  
    public static final long DAMAGE_COOLDOWN = 1000;  

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

    public boolean shouldActivate(long currentTime) {
        if (type == TrapType.TIMER) {
            return (currentTime - lastActivation) >= TIMER_INTERVAL;
        }
        return false;
    }

    public void activate(long currentTime) {
        this.active = true;
        this.lastActivation = currentTime;
    }

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
