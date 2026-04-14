package com.example.clientutils.mixin;

import com.example.clientutils.ClientUtilsMod;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void clientutils$allowSectionChar(CharInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!ClientUtilsMod.CONFIG.allowFormattingCodes || input.codepoint() != '\u00a7') {
            return;
        }

        TextFieldWidget self = (TextFieldWidget) (Object) this;
        self.write(Character.toString((char) input.codepoint()));
        cir.setReturnValue(true);
    }
}
