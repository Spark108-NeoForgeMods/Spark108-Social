package ru.spark108.social;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/** Server-side buttons for the player interaction menu. Register during mod initialization. */
public final class PlayerMenuApi {
    private static final Map<ResourceLocation, Action> ACTIONS = new LinkedHashMap<>();

    private PlayerMenuApi() {}

    private record Action(Component title, BiPredicate<ServerPlayer, ServerPlayer> available,
                          BiConsumer<ServerPlayer, ServerPlayer> execute) {}

    /** Both callbacks run on the server thread; arguments are the viewer and the targeted player.
     * Availability is checked when opening the menu and again before execution.
     * Registering an existing id replaces its action without changing its order. */
    public static synchronized void register(ResourceLocation id, Component title,
                                             BiPredicate<ServerPlayer, ServerPlayer> available,
                                             BiConsumer<ServerPlayer, ServerPlayer> execute) {
        ACTIONS.put(Objects.requireNonNull(id), new Action(Objects.requireNonNull(title).copy(),
                Objects.requireNonNull(available), Objects.requireNonNull(execute)));
    }

    public static synchronized void unregister(ResourceLocation id) {
        ACTIONS.remove(id);
    }

    static List<SocialPackets.MenuButton> buttons(ServerPlayer viewer, ServerPlayer target) {
        Map<ResourceLocation, Action> snapshot;
        synchronized (PlayerMenuApi.class) { snapshot = new LinkedHashMap<>(ACTIONS); }
        return snapshot.entrySet().stream().filter(entry -> entry.getValue().available.test(viewer, target))
                .limit(64).map(entry -> new SocialPackets.MenuButton(entry.getKey(), entry.getValue().title.copy()))
                .toList();
    }

    static void execute(ResourceLocation id, ServerPlayer viewer, ServerPlayer target) {
        Action action;
        synchronized (PlayerMenuApi.class) { action = ACTIONS.get(id); }
        if (action != null && action.available.test(viewer, target)) action.execute.accept(viewer, target);
    }
}
