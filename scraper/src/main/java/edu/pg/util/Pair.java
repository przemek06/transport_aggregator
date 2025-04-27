package edu.pg.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Pair<T, U> {
        private final T first;
        private final U second;

        public static <T, U> Pair<T, U> of(T first, U second) {
            return new Pair<>(first, second);
        }
    }