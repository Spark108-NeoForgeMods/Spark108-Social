package ru.spark108.social;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

final class SocialMenuScreen extends Screen {
    private final UUID target;
    private final String name;
    private record MenuAction(Component title, Runnable execute) {}
    private final List<MenuAction> actions = new ArrayList<>();
    private int scroll;
    private boolean loaded;

    SocialMenuScreen(UUID target, String name) {
        super(Component.translatable("screen.spark108_social.actions"));
        this.target = target;
        this.name = name;
    }

    @Override protected void init() {
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
        actions.clear();
        if (reply.transfer()) actions.add(new MenuAction(Component.translatable("screen.spark108_social.transfer_action"),
                () -> minecraft.setScreen(new TransferScreen(target, name))));
        if (reply.trade()) actions.add(new MenuAction(Component.translatable("screen.spark108_social.trade_action"), () -> {
            PacketDistributor.sendToServer(new SocialPackets.TradeOpenRequest(target));
            minecraft.setScreen(null);
        }));
        for (SocialPackets.MenuButton button : reply.buttons()) actions.add(new MenuAction(button.title(), () -> {
            minecraft.setScreen(null);
            PacketDistributor.sendToServer(new SocialPackets.MenuActionRequest(target, button.id()));
        }));
        updateButtons();
    }

    private int capacity() { return Math.max(1, (height - 60) / 24); }
    private int visibleCount() { return Math.min(actions.size(), capacity()); }
    private int panelHeight() { return actions.isEmpty() ? 52 : 31 + visibleCount() * 24; }
    private int panelX() { return Math.max(4, Math.min(width - 190, width / 2 + 16)); }
    private int panelY() { return Math.max(4, Math.min(height / 2 - 24, height - panelHeight() - 4)); }

    private void updateButtons() {
        clearWidgets();
        scroll = Math.max(0, Math.min(scroll, actions.size() - visibleCount()));
        for (int row = 0; row < visibleCount(); row++) {
            MenuAction action = actions.get(scroll + row);
            addRenderableWidget(Button.builder(action.title(), button -> action.execute().run())
                    .bounds(panelX() + 8, panelY() + 27 + row * 24, 170, 20).build());
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (actions.size() <= capacity() || mouseX < panelX() || mouseX > panelX() + 186
                || mouseY < panelY() || mouseY > panelY() + panelHeight())
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scroll += vertical > 0 ? -1 : vertical < 0 ? 1 : 0;
        updateButtons();
        return true;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        int x = panelX();
        int y = panelY();
        graphics.fill(x, y, x + 186, y + panelHeight(), 0xE0212630);
        graphics.fill(x, y, x + 186, y + 2, 0xFF60D8EF);
        graphics.drawString(font, name, x + 8, y + 8, 0xFFFFFFFF);
        if (actions.size() > capacity()) graphics.drawString(font,
                (scroll > 0 ? "↑" : "") + (scroll + visibleCount() < actions.size() ? "↓" : ""),
                x + 164, y + 8, 0xFFBFC7CC);
        if (actions.isEmpty()) graphics.drawString(font,
                Component.translatable(loaded ? "screen.spark108_social.no_actions" : "screen.spark108_social.loading"),
                x + 8, y + 29, 0xFFBFC7CC);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The blur is rendered before the panel in render().
    }

    @Override public boolean isPauseScreen() { return false; }
}
