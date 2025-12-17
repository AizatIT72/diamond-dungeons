package ru.kpfu.itis.common;

public enum TileType {
    FLOOR(0, true),
    WALL(1, false),
    DIAMOND(2, true),
    TRAP(3, true),
    CHEST(4, true),
    DOOR(5, true),
    START(6, true),
    ENEMY_SPAWN(7, true),
    BUTTON(8, true);

    private final int id;
    private final boolean walkable;

    TileType(int id, boolean walkable) {
        this.id = id;
        this.walkable = walkable;
    }

    public int getId() { return id; }
    public boolean isWalkable() { return walkable; }

    public static TileType fromId(int id) {
        for (TileType type : values()) {
            if (type.id == id) return type;
        }
        return FLOOR;
    }
}
