package ru.spark108.social;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

final class SocialMenuScreen extends Screen {
    private final UUID target;
    private final String name;
    private Button transferButton;
    private Button tradeButton;
    private boolean loaded;
    private boolean canTransfer;
    private boolean canTrade;

    SocialMenuScreen(UUID target, String name) {
        super(Component.translatable("screen.spark108_social.actions"));
        this.target = target;
        this.name = name;
    }

    @Override protected void init() {
        int x = Math.min(width - 190, width / 2 + 16);
        int y = height / 2 - 24;
        transferButton = addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.transfer_action"),
                button -> minecraft.setScreen(new TransferScreen(target, name)))
                .bounds(x + 8, y + 27, 170, 20).build());
        tradeButton = addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.trade_action"),
                button -> {
                    PacketDistributor.sendToServer(new SocialPackets.TradeOpenRequest(target));
                    minecraft.setScreen(null);
                }).bounds(x + 8, y + 51, 170, 20).build());
        updateButtons();
        PacketDistributor.sendToServer(new SocialPackets.ActionsRequest(target));
    }

    void accept(SocialPackets.ActionsReply reply) {
        if (!target.equals(reply.target())) return;
        if (reply.spectatorBlocked()) {
            minecraft.setScreen(null);
            return;
        }
        loaded = true;
        canTransfer = reply.transfer();
        canTrade = reply.trade();
        updateButtons();
    }

    private void updateButtons() {
        if (transferButton == null || tradeButton == null) return;
        transferButton.visible = canTransfer;
        tradeButton.visible = canTrade;
        tradeButton.setY(transferButton.getY() + (canTransfer ? 24 : 0));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        int x = Math.min(width - 190, width / 2 + 16);
        int y = height / 2 - 24;
        int actions = (canTransfer ? 1 : 0) + (canTrade ? 1 : 0);
        graphics.fill(x, y, x + 186, y + (actions == 0 ? 52 : 31 + actions * 24), 0xE0212630);
        graphics.fill(x, y, x + 186, y + 2, 0xFF60D8EF);
        graphics.drawString(font, name, x + 8, y + 8, 0xFFFFFFFF);
        if (actions == 0) graphics.drawString(font,
                Component.translatable(loaded ? "screen.spark108_social.no_actions" : "screen.spark108_social.loading"),
                x + 8, y + 29, 0xFFBFC7CC);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The blur is rendered before the panel in render().
    }

    @Override public boolean isPauseScreen() { return false; }
}
