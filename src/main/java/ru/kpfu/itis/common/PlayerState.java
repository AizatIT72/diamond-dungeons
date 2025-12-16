package ru.kpfu.itis.common;

import java.io.Serializable;

public class PlayerState implements Serializable {

    public int id;
    public String name;

    public int x;
    public int y;

    public int hp;
    public int maxHp;

    public boolean hasKey;
    public boolean atDoor;

    public PlayerState(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.maxHp = 100;
        this.hp = maxHp;
        this.hasKey = false;
        this.atDoor = false;
    }
}
