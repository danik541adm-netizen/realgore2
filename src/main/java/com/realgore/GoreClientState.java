package com.realgore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентская копия масок отрубленных конечностей: id сущности -> маска. Без клиентских импортов. */
public final class GoreClientState {
    private static final Map<Integer, Integer> MASKS = new ConcurrentHashMap<>();

    public static void set(int entityId, int mask) {
        if (mask == 0) {
            MASKS.remove(entityId);
        } else {
            MASKS.put(entityId, mask);
        }
    }

    public static int get(int entityId) {
        return MASKS.getOrDefault(entityId, 0);
    }

    public static void clear() {
        MASKS.clear();
    }

    private GoreClientState() {
    }
}
