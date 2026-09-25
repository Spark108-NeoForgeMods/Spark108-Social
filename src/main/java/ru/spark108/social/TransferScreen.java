package ru.spark108.social;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigDecimal;
import java.util.UUID;

final class TransferScreen extends Screen {
    private final UUID target;
    private final String name;
    private EditBox amount;
    private Button transfer;
    private Component balance = Component.translatable("screen.spark108_social.loading");
    private Component status = Component.empty();
    private boolean balanceLoaded;
    private boolean pending;
    private boolean completed;
    private String currency = "";

    TransferScreen(UUID target, String name) {
        super(Component.translatable("screen.spark108_social.transfer_title"));
        this.target = target;
        this.name = name;
    }

    @Override protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 65;
        amount = addRenderableWidget(new EditBox(font, x + 12, y + 65, 196, 20,
                Component.translatable("screen.spark108_social.amount")));
        amount.setMaxLength(32);
        amount.setFilter(value -> value.matches("[0-9.,]*"));
        amount.setResponder(value -> updateButton());
        setInitialFocus(amount);
        addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.cancel"),
                button -> onClose()).bounds(x + 12, y + 98, 94, 20).build());
        transfer = addRenderableWidget(Button.builder(Component.translatable("screen.spark108_social.transfer"),
                button -> sendTransfer()).bounds(x + 114, y + 98, 94, 20).build());
        updateButton();
        PacketDistributor.sendToServer(new SocialPackets.BalanceRequest(target));
    }

    private void sendTransfer() {
        if (!balanceLoaded || pending || completed) return;
        String value = amount.getValue().replace(',', '.');
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.spark108_social.invalid_amount");
            return;
        }
        pending = true;
        status = Component.translatable("screen.spark108_social.sending");
        updateButton();
        PacketDistributor.sendToServer(new SocialPackets.TransferRequest(target, value));
    }

    void accept(SocialPackets.BalanceReply reply) {
        if (!target.equals(reply.target())) return;
        balanceLoaded = reply.success();
        if (reply.success()) {
            currency = reply.currency();
            balance = Component.literal(money(reply.balance()) + " " + reply.currency());
            status = Component.empty();
        } else {
            balance = Component.translatable("screen.spark108_social.unavailable");
            status = Component.translatable(reply.message());
        }
        updateButton();
    }

    void accept(SocialPackets.TransferReply reply) {
        if (!target.equals(reply.target()) || !pending) return;
        pending = false;
        completed = reply.success();
        if (reply.success()) {
            balance = Component.literal(money(reply.balance()) + " " + currency);
        }
        status = Component.translatable(reply.message());
        updateButton();
    }

    private void updateButton() {
        if (transfer != null) transfer.active = balanceLoaded && !pending && !completed
                && amount != null && !amount.getValue().isBlank();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        int x = width / 2 - 110;
        int y = height / 2 - 65;
        graphics.fill(x, y, x + 220, y + 146, 0xE0212630);
        graphics.fill(x, y, x + 220, y + 2, 0xFF60D8EF);
        graphics.drawCenteredString(font, title, width / 2, y + 9, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.spark108_social.recipient", name), x + 12, y + 28, 0xFFDDE8EE);
        graphics.drawString(font, Component.translatable("screen.spark108_social.balance", balance), x + 12, y + 44, 0xFFA8E6B0);
        graphics.drawString(font, Component.translatable("screen.spark108_social.amount"), x + 12, y + 55, 0xFFBFC7CC);
        graphics.drawCenteredString(font, status, width / 2, y + 126, 0xFFFFCB72);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The blur is rendered before the panel in render().
    }

    @Override public boolean isPauseScreen() { return false; }

    private static String money(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
