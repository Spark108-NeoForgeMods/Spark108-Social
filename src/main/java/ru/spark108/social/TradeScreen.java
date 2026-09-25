package ru.spark108.social;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private Button confirmButton;
    private boolean selfConfirmed;
    private boolean otherConfirmed;
    private int secondsLeft;

    public TradeScreen(TradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 360;
        imageHeight = 235;
    }

    @Override protected void init() {
        super.init();
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.trade_confirm"),
                button -> PacketDistributor.sendToServer(
                        new SocialPackets.TradeDecision(menu.tradeId(), !selfConfirmed)))
                .bounds(leftPos + 22, topPos + 101, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.cancel"),
                button -> onClose()).bounds(leftPos + 277, topPos + 2, 74, 18).build());
        updateConfirmButton();
    }

    void accept(SocialPackets.TradeState state) {
        if (!menu.tradeId().equals(state.tradeId())) return;
        selfConfirmed = state.selfConfirmed();
        otherConfirmed = state.otherConfirmed();
        secondsLeft = state.secondsLeft();
        updateConfirmButton();
    }

    private void updateConfirmButton() {
        if (confirmButton != null) confirmButton.setMessage(Component.translatable(selfConfirmed
                ? "screen.spark108_social.trade_revoke" : "screen.spark108_social.trade_confirm"));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEA202830);
        graphics.fill(x, y, x + imageWidth, y + 2, 0xFF60D8EF);
        graphics.fill(x + 8, y + 23, x + 174, y + 125, 0xB9313E47);
        graphics.fill(x + 186, y + 23, x + 352, y + 125, 0xB9313E47);
        graphics.fill(x + 91, y + 148, x + 269, y + 232, 0xB9313E47);
        for (int index = 0; index < 9; index++) {
            int column = index % 3;
            int row = index / 3;
            slotBackground(graphics, x + 64 + column * 18, y + 35 + row * 18);
            slotBackground(graphics, x + 249 + column * 18, y + 35 + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotBackground(graphics, x + 98 + column * 18, y + 151 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotBackground(graphics, x + 98 + column * 18, y + 211);
        }
    }

    private static void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF0D1419);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF45545D);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 6, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.spark108_social.trade_you"),
                91, 25, 0xFFBDECDD);
        graphics.drawCenteredString(font, menu.otherName(), 269, 25, 0xFFBDECDD);
        graphics.drawCenteredString(font, Component.translatable(otherConfirmed
                        ? "screen.spark108_social.trade_other_confirmed"
                        : "screen.spark108_social.trade_other_waiting"),
                269, 108, otherConfirmed ? 0xFF68EA78 : 0xFFFF6B6B);
        if (secondsLeft > 0) {
            graphics.drawCenteredString(font, Component.translatable("screen.spark108_social.trade_countdown", secondsLeft),
                    imageWidth / 2, 130, 0xFFFFD35A);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.spark108_social.trade_hint"),
                    imageWidth / 2, 130, 0xFFC6D2D8);
        }
        graphics.drawString(font, playerInventoryTitle, 99, 140, 0xFFC6D2D8, false);
    }

    @Override public boolean isPauseScreen() { return false; }
}
