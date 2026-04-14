package com.example.clientutils.mixin;

import com.example.clientutils.ClientUtilsMod;
import com.example.clientutils.feature.BookbanWriterUtil;
import com.example.clientutils.feature.BookClipboardUtil;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow
    private int currentPage;

    @Shadow
    private List<String> pages;

    @Shadow
    protected abstract void updatePage();

    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void clientutils$keyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (this.client == null) {
            return;
        }
        int keyCode = keyInput.key();

        if (ClientUtilsMod.CONFIG.allowBookClipboardImport
                && keyCode == GLFW.GLFW_KEY_V
                && hasModifier(keyInput, GLFW.GLFW_MOD_CONTROL)
                && hasModifier(keyInput, GLFW.GLFW_MOD_SHIFT)) {
            importFullBookFromClipboard();
            cir.setReturnValue(true);
            return;
        }

        if (ClientUtilsMod.CONFIG.allowFormattingCodes
                && keyCode == GLFW.GLFW_KEY_S
                && hasModifier(keyInput, GLFW.GLFW_MOD_CONTROL)
                && hasModifier(keyInput, GLFW.GLFW_MOD_ALT)) {
            String current = pages.get(currentPage);
            pages.set(currentPage, current + "\u00a7");
            updatePage();
            cir.setReturnValue(true);
            return;
        }

        if (ClientUtilsMod.CONFIG.autoBookbanWriterEnabled
                && keyCode == ClientUtilsMod.CONFIG.bookbanFillKey
                && hasModifier(keyInput, GLFW.GLFW_MOD_CONTROL)
                && hasModifier(keyInput, GLFW.GLFW_MOD_SHIFT)) {
            this.pages.clear();
            this.pages.addAll(BookbanWriterUtil.generateHeavyPages());
            this.currentPage = 0;
            updatePage();
            if (this.client.player != null) {
                this.client.player.sendMessage(Text.literal("[ClientUtils] Heavy book pages generated"), true);
            }
            cir.setReturnValue(true);
        }
    }

    private boolean hasModifier(KeyInput keyInput, int modifier) {
        return (keyInput.modifiers() & modifier) != 0;
    }

    private void importFullBookFromClipboard() {
        String clipboard = this.client.keyboard.getClipboard();
        List<String> parsed = BookClipboardUtil.parseClipboardToPages(clipboard);

        this.pages.clear();
        this.pages.addAll(parsed);
        this.currentPage = 0;
        updatePage();

        if (this.client.player != null) {
            this.client.player.sendMessage(Text.literal("[ClientUtils] Pasted " + this.pages.size() + " book pages"), true);
        }
    }
}
