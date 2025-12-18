package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Ось движения патрульного моба.
 * HORIZONTAL - движение по горизонтали (влево/вправо)
 * VERTICAL - движение по вертикали (вверх/вниз)
 */
public enum PatrolAxis implements Serializable {
    HORIZONTAL,
    VERTICAL;

    private static final long serialVersionUID = 1L;
}

