package ru.ynovka.autolitefishing.mixin;

import ru.ynovka.autolitefishing.TitleMessageEvents;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InGameHud.class)
public class TitleEventMixin {
    @Inject(method = "setTitle(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void setTitleEvent(Text subtitle, CallbackInfo ci) {
        TitleMessageEvents.SET_TITLE.invoker().onTitleSet(subtitle, false);
    }
}