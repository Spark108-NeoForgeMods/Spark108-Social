package ru.spark108.social;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Server-side extension point for text displayed beside the hovered player's green outline. */
public final class HaloApi {
    private static final Map<String, Provider> PROVIDERS = new LinkedHashMap<>();

    private HaloApi() {}

    public record Field(HaloPosition position, String name, String value, int color) {
        public Field {
            Objects.requireNonNull(position);
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
        }
    }

    @FunctionalInterface
    public interface Provider {
        /** Return no fields when the viewer is not authorized to see this data. */
        List<Field> fields(ServerPlayer viewer, ServerPlayer target);
    }

    /** Register a stable provider id during mod initialization. Later registrations with the same id replace it. */
    public static synchronized void register(String id, Provider provider) {
        PROVIDERS.put(Objects.requireNonNull(id), Objects.requireNonNull(provider));
    }

    static synchronized List<Field> fields(ServerPlayer viewer, ServerPlayer target) {
        List<Field> result = new ArrayList<>();
        for (Provider provider : PROVIDERS.values()) {
            List<Field> provided = provider.fields(viewer, target);
            if (provided == null) continue;
            for (Field field : provided) {
                if (field == null) continue;
                if (result.size() == 64) return List.copyOf(result);
                result.add(field);
            }
        }
        return List.copyOf(result);
    }
}
