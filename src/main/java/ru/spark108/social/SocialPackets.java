package ru.spark108.social;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public final class SocialPackets {
    private SocialPackets() {}

    public record ClientSettings(boolean spectatorInteraction, List<HaloAlignment> alignments)
            implements CustomPacketPayload {
        public static final Type<ClientSettings> TYPE = new Type<>(id("client_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientSettings> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeBoolean(packet.spectatorInteraction);
                    for (HaloAlignment alignment : packet.alignments) buf.writeVarInt(alignment.ordinal());
                }, buf -> {
                    boolean spectatorInteraction = buf.readBoolean();
                    List<HaloAlignment> alignments = new ArrayList<>(HaloPosition.values().length);
                    for (HaloPosition ignored : HaloPosition.values()) {
                        int id = buf.readVarInt();
                        if (id < 0 || id >= HaloAlignment.values().length)
                            throw new IllegalArgumentException("Invalid halo alignment");
                        alignments.add(HaloAlignment.values()[id]);
                    }
                    return new ClientSettings(spectatorInteraction, List.copyOf(alignments));
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HaloRequest(UUID target) implements CustomPacketPayload {
        public static final Type<HaloRequest> TYPE = new Type<>(id("halo_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HaloRequest> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeUUID(packet.target), buf -> new HaloRequest(buf.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record HaloLine(HaloPosition position, String name, String value, int color) {}

    public record HaloReply(UUID target, List<HaloLine> lines) implements CustomPacketPayload {
        public static final Type<HaloReply> TYPE = new Type<>(id("halo_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HaloReply> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeUUID(packet.target);
                    buf.writeVarInt(packet.lines.size());
                    for (HaloLine line : packet.lines) {
                        buf.writeVarInt(line.position.ordinal());
                        buf.writeUtf(line.name, 64);
                        buf.writeUtf(line.value, 256);
                        buf.writeInt(line.color);
                    }
                }, buf -> {
                    UUID target = buf.readUUID();
                    int count = buf.readVarInt();
                    if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid halo field count");
                    List<HaloLine> lines = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        int position = buf.readVarInt();
                        if (position < 0 || position >= HaloPosition.values().length)
                            throw new IllegalArgumentException("Invalid halo position");
                        lines.add(new HaloLine(HaloPosition.values()[position],
                                buf.readUtf(64), buf.readUtf(256), buf.readInt()));
                    }
                    return new HaloReply(target, List.copyOf(lines));
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ActionsRequest(UUID target) implements CustomPacketPayload {
        public static final Type<ActionsRequest> TYPE = new Type<>(id("actions_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionsRequest> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeUUID(packet.target), buf -> new ActionsRequest(buf.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MenuButton(ResourceLocation id, Component title) {}

    public record MenuActionRequest(UUID target, ResourceLocation action) implements CustomPacketPayload {
        public static final Type<MenuActionRequest> TYPE = new Type<>(id("menu_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionRequest> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeUUID(packet.target); buf.writeResourceLocation(packet.action); },
                buf -> new MenuActionRequest(buf.readUUID(), buf.readResourceLocation()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ActionsReply(UUID target, boolean transfer, boolean trade, boolean spectatorBlocked,
                               List<MenuButton> buttons) implements CustomPacketPayload {
        public static final Type<ActionsReply> TYPE = new Type<>(id("actions_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionsReply> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeUUID(packet.target); buf.writeBoolean(packet.transfer);
                    buf.writeBoolean(packet.trade); buf.writeBoolean(packet.spectatorBlocked);
                    buf.writeVarInt(packet.buttons.size());
                    for (MenuButton button : packet.buttons) {
                        buf.writeResourceLocation(button.id);
                        ComponentSerialization.STREAM_CODEC.encode(buf, button.title);
                    }
                }, buf -> {
                    UUID target = buf.readUUID();
                    boolean transfer = buf.readBoolean(), trade = buf.readBoolean(), blocked = buf.readBoolean();
                    int count = buf.readVarInt();
                    if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid menu button count");
                    List<MenuButton> buttons = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) buttons.add(new MenuButton(buf.readResourceLocation(),
                            ComponentSerialization.STREAM_CODEC.decode(buf)));
                    return new ActionsReply(target, transfer, trade, blocked, List.copyOf(buttons));
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BalanceRequest(UUID target) implements CustomPacketPayload {
        public static final Type<BalanceRequest> TYPE = new Type<>(id("balance_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BalanceRequest> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeUUID(packet.target), buf -> new BalanceRequest(buf.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BalanceReply(UUID target, boolean success, double balance, String currency, String message)
            implements CustomPacketPayload {
        public static final Type<BalanceReply> TYPE = new Type<>(id("balance_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BalanceReply> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeUUID(packet.target); buf.writeBoolean(packet.success); buf.writeDouble(packet.balance);
                    buf.writeUtf(packet.currency, 64); buf.writeUtf(packet.message, 128);
                }, buf -> new BalanceReply(buf.readUUID(), buf.readBoolean(), buf.readDouble(),
                        buf.readUtf(64), buf.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TransferRequest(UUID target, String amount) implements CustomPacketPayload {
        public static final Type<TransferRequest> TYPE = new Type<>(id("transfer_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TransferRequest> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeUUID(packet.target); buf.writeUtf(packet.amount, 32); },
                buf -> new TransferRequest(buf.readUUID(), buf.readUtf(32)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TransferReply(UUID target, boolean success, double balance, String message)
            implements CustomPacketPayload {
        public static final Type<TransferReply> TYPE = new Type<>(id("transfer_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TransferReply> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeUUID(packet.target); buf.writeBoolean(packet.success); buf.writeDouble(packet.balance);
                    buf.writeUtf(packet.message, 128);
                }, buf -> new TransferReply(buf.readUUID(), buf.readBoolean(), buf.readDouble(), buf.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TradeOpenRequest(UUID target) implements CustomPacketPayload {
        public static final Type<TradeOpenRequest> TYPE = new Type<>(id("trade_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeOpenRequest> CODEC = StreamCodec.of(
                (buf, packet) -> buf.writeUUID(packet.target), buf -> new TradeOpenRequest(buf.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TradeDecision(UUID tradeId, boolean confirmed) implements CustomPacketPayload {
        public static final Type<TradeDecision> TYPE = new Type<>(id("trade_decision"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeDecision> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeUUID(packet.tradeId); buf.writeBoolean(packet.confirmed); },
                buf -> new TradeDecision(buf.readUUID(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TradeState(UUID tradeId, boolean selfConfirmed, boolean otherConfirmed, int secondsLeft)
            implements CustomPacketPayload {
        public static final Type<TradeState> TYPE = new Type<>(id("trade_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeState> CODEC = StreamCodec.of(
                (buf, packet) -> {
                    buf.writeUUID(packet.tradeId); buf.writeBoolean(packet.selfConfirmed);
                    buf.writeBoolean(packet.otherConfirmed); buf.writeVarInt(packet.secondsLeft);
                }, buf -> new TradeState(buf.readUUID(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SocialMod.ID, path);
    }
}
