package ru.kpfu.itis.common;

public class Protocol {
    public static final int CONNECT = 1;           
    public static final int DISCONNECT = 2;        
    public static final int PLAYER_MOVE = 3;       
    public static final int GAME_STATE = 4;        
    public static final int PLAYER_ACTION = 5;     
    public static final int CHAT_MESSAGE = 6;      

    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    public static final int ACTION_COLLECT = 1;    
    public static final int ACTION_USE = 2;        
    public static final int ACTION_DOOR = 3;       

    public static final String DELIMITER = "|";

    public static final int MAX_PLAYERS = 3;

    public static final int FIELD_WIDTH = 20;
    public static final int FIELD_HEIGHT = 20;

    public static final int CELL_SIZE = 30;
}
