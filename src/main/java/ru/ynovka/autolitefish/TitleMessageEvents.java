package ru.ynovka.autolitefishing;

import net.minecraft.text.Text;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;


public class TitleMessageEvents {
    public static final Event<TitleSetEvent> SET_TITLE = EventFactory.createArrayBacked(TitleSetEvent.class, handlers -> (text, subtitle) -> {
        for (TitleSetEvent handler : handlers) {
            handler.onTitleSet(text, subtitle);
        }
    });

    @FunctionalInterface
    public interface TitleSetEvent {
        void onTitleSet(Text text, boolean isSubTitle);
    }
}