package ru.spark108.social;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public final class SocialClient {
    private static final int HALO_SIDE_GAP = 28;
    private static final int HALO_LINE_SPACING = 12;
    private static final float HALO_TEXT_SCALE = 0.015f;
    private static UUID haloTarget;
    private static List<SocialPackets.HaloLine> haloLines = List.of();
    private static long haloRequestAt;
    private static long haloReceivedAt;
    private static boolean spectatorInteraction;
    private static final Map<HaloPosition, HaloAlignment> serverAlignments = new EnumMap<>(HaloPosition.class);

    private SocialClient() {}

    public static boolean isHovered(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.screen == null
                && entity instanceof Player player && player != minecraft.player && player.isAlive()
                && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() == entity;
    }

    static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isSpectator() && !spectatorInteraction) return;
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND
                || minecraft.screen != null || minecraft.player == null
                || !(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof Player target)
                || target == minecraft.player || !target.isAlive()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        minecraft.setScreen(new SocialMenuScreen(target.getUUID(), target.getName().getString()));
    }

    static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        spectatorInteraction = false;
        haloTarget = null;
        haloLines = List.of();
        serverAlignments.clear();
    }

    static void settings(SocialPackets.ClientSettings packet) {
        spectatorInteraction = packet.spectatorInteraction();
        for (HaloPosition position : HaloPosition.values())
            serverAlignments.put(position, packet.alignments().get(position.ordinal()));
    }

    static void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (!isHovered(event.getEntity())) return;
        UUID target = event.getEntity().getUUID();
        long now = System.currentTimeMillis();
        if (!target.equals(haloTarget)) {
            haloTarget = target;
            haloLines = List.of();
            haloReceivedAt = 0;
            haloRequestAt = 0;
        }
        if (now - haloRequestAt >= 1000) {
            haloRequestAt = now;
            PacketDistributor.sendToServer(new SocialPackets.HaloRequest(target));
        }
        if (now - haloReceivedAt > 1300 || haloLines.isEmpty()) return;

        Map<HaloPosition, List<SocialPackets.HaloLine>> groups = new EnumMap<>(HaloPosition.class);
        for (SocialPackets.HaloLine line : haloLines)
            groups.computeIfAbsent(line.position(), ignored -> new ArrayList<>()).add(line);
        for (HaloPosition position : HaloPosition.values()) {
            List<SocialPackets.HaloLine> lines = groups.get(position);
            if (lines != null && !lines.isEmpty()) renderHaloBlock(event, position, lines);
        }
    }

    private static void renderHaloBlock(RenderPlayerEvent.Post event, HaloPosition position,
                                        List<SocialPackets.HaloLine> lines) {
        var pose = event.getPoseStack();
        var font = Minecraft.getInstance().font;
        float height = event.getEntity().getBbHeight();
        float anchor = switch (position) {
            case HEAD_LEFT, HEAD_RIGHT -> height * 0.87f;
            case BODY_LEFT, BODY_RIGHT -> height * 0.56f;
            case LEGS_LEFT, LEGS_RIGHT -> height * 0.23f;
        };
        int rows = lines.size();
        int maxWidth = 0;
        float maxEdge = 0;
        List<Component> texts = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            SocialPackets.HaloLine line = lines.get(row);
            Component text = Component.literal(line.name() + ": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(line.value()).withStyle(style -> style.withColor(line.color())));
            texts.add(text);
            maxWidth = Math.max(maxWidth, font.width(text));
            float y = (row - (rows - 1) / 2.0f) * HALO_LINE_SPACING;
            maxEdge = Math.max(maxEdge, contourEdge(event.getEntity(), anchor - y * HALO_TEXT_SCALE));
        }
        float blockEdge = maxEdge / HALO_TEXT_SCALE + HALO_SIDE_GAP;
        float blockLeft = position.isLeft() ? -blockEdge - maxWidth : blockEdge;
        HaloAlignment alignment = serverAlignments.getOrDefault(position, SocialConfig.alignment(position));
        pose.pushPose();
        pose.translate(0, anchor, 0);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(HALO_TEXT_SCALE, -HALO_TEXT_SCALE, HALO_TEXT_SCALE);
        for (int row = 0; row < rows; row++) {
            Component text = texts.get(row);
            int width = font.width(text);
            float y = (row - (rows - 1) / 2.0f) * HALO_LINE_SPACING;
            float edge = contourEdge(event.getEntity(), anchor - y * HALO_TEXT_SCALE)
                    / HALO_TEXT_SCALE + HALO_SIDE_GAP;
            float x = switch (alignment) {
                case CONTOUR -> position.isLeft() ? -edge - width : edge;
                case CENTER -> blockLeft + (maxWidth - width) / 2.0f;
                case LEFT -> blockLeft;
                case RIGHT -> blockLeft + maxWidth - width;
            };
            font.drawInBatch(text, x, y, 0xFFFFFFFF, true, pose.last().pose(),
                    event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, event.getPackedLight());
        }
        pose.popPose();
    }

    private static float contourEdge(Entity player, float worldHeight) {
        float relative = worldHeight / player.getBbHeight();
        float scale = player.getBbWidth() / 0.6f;
        if (relative >= 0.76f) return 0.29f * scale; // outer hat layer
        if (relative >= 0.43f) return 0.56f * scale; // torso and sleeves
        return 0.28f * scale; // leg outer layers
    }

    static void halo(SocialPackets.HaloReply packet) {
        if (!packet.target().equals(haloTarget)) return;
        haloLines = packet.lines();
        haloReceivedAt = System.currentTimeMillis();
    }

    static void balance(SocialPackets.BalanceReply packet) {
        if (Minecraft.getInstance().screen instanceof TransferScreen screen) screen.accept(packet);
    }

    static void actions(SocialPackets.ActionsReply packet) {
        if (Minecraft.getInstance().screen instanceof SocialMenuScreen screen) screen.accept(packet);
    }

    static void transfer(SocialPackets.TransferReply packet) {
        if (Minecraft.getInstance().screen instanceof TransferScreen screen) screen.accept(packet);
    }

    static void tradeState(SocialPackets.TradeState packet) {
        if (Minecraft.getInstance().screen instanceof TradeScreen screen) screen.accept(packet);
    }
}
