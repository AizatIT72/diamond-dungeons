package ru.kpfu.itis.common;

import java.io.Serializable;

/**
 * Направление патрульного моба по оси.
 * POSITIVE - движение в положительную сторону оси (вправо/вниз)
 * NEGATIVE - движение в отрицательную сторону оси (влево/вверх)
 */
public enum PatrolDirection implements Serializable {
    POSITIVE,
    NEGATIVE;

    private static final long serialVersionUID = 1L;
}

