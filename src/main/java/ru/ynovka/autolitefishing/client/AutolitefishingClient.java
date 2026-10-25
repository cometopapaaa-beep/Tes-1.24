package ru.ynovka.autolitefishing.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import ru.ynovka.autolitefishing.TitleMessageEvents;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AutolitefishingClient implements ClientModInitializer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static boolean isUsingRP = false;
    private static long lastCallTime = 0;
    private static boolean wasRedActive = false;


    @Override
    public void onInitializeClient() {
        initAutoFish();
    }

    public void initAutoFish() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof FishingRodItem && player.fishHook != null) {
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    if (stack.getItem() instanceof FishingRodItem && player.fishHook == null) useRod();
                });
            }
            return ActionResult.PASS;
        });

        TitleMessageEvents.SET_TITLE.register((text, isSubTitle) -> {
            if (!isSubTitle && client.player != null) {
                String content = text.toString();
                String rawString = text.getString();
                long now = System.nanoTime();
                long deltaNanos = now - lastCallTime;
                double deltaMillis = deltaNanos / 1_000_000.0;
                lastCallTime = now;
                System.out.println(deltaMillis);

                Map<String, Integer> colors = countColorsInText(content);

                // Deteksi keberadaan warna merah pada title
                // Memeriksa format hex warna merah (misal FF0000, FF5555, e74c3c, d63031, dll.), §c / &c, atau atribut formatting=red
                boolean hasRedColor = content.contains("formatting=red") 
                        || rawString.contains("§c") 
                        || rawString.contains("&c")
                        || colors.keySet().stream().anyMatch(c -> c.startsWith("FF00") || c.startsWith("FF55") || c.startsWith("E7") || c.startsWith("D6") || c.startsWith("E74") || c.startsWith("FF2") || c.startsWith("FF3") || c.startsWith("FF4"));

                if (hasRedColor) {
                    wasRedActive = true;
                } else if (wasRedActive) {
                    // Ketika indikator warna merah sebelumnya aktif, lalu sekarang hilang/berubah -> tarik rod
                    wasRedActive = false;
                    useRod();
                    return;
                }

                if (deltaMillis > 1000) {
                    // Just threw a fishing rod
                    isUsingRP = colors.size() == 1;
                } else if (deltaMillis > 20) {
                    // Game is going
                    if (deltaMillis < getCurrentPing()) {
                        client.player.sendMessage(
                                Text.of("AutoFish can't predict when \"your ping\" > \"fish minigame speed\"."),
                                false
                        );
                    } else {
                        // Ping is OK. Following the game
                        if (isUsingRP) {
                            if (!content.contains("㋓")) useRod();
                        } else {
                            int hearts = countHeartsInText(content);
                            int heartsColorsCount = colors.values().stream().findFirst().orElse(0);

                            if (colors.size() == 1 && hearts == 0) useRod();
                            if (colors.size() == 3 && heartsColorsCount == hearts) useRod();
                        }
                    }
                }
            }
        });
    }

    private void useRod() {
        if (client.player == null || client.interactionManager == null) return;

        Hand hand = Hand.MAIN_HAND;
        ItemStack mainHandStack = client.player.getMainHandStack();

        if (!(mainHandStack.getItem() instanceof FishingRodItem)) return;

        ActionResult actionResult = client.interactionManager.interactItem(client.player, hand);
        if (actionResult.isAccepted()) {
            if (actionResult.isAccepted()) {
                ClientPlayerEntity player = client.player;
                if (player != null) { player.swingHand(hand); }
            }
            client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
        }
    }

    private Map<String, Integer> countColorsInText(String text) {
        String regex = "color=#([A-Fa-f0-9]{6})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        Map<String, Integer> colorCounts = new LinkedHashMap<>();

        while (matcher.find()) {
            String color = matcher.group(1).toUpperCase();
            colorCounts.put(color, colorCounts.getOrDefault(color, 0) + 1);
        }

        return colorCounts;
    }

    private Integer countHeartsInText(String text) {
        String regex = "literal\\{(.*?)}\\[style=\\{[^}]*!strikethrough[^}]*}]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        HashSet<String> hearts = new HashSet<>();
        while (matcher.find()) {
            hearts.add(matcher.group(1));
        }
        return hearts.size();
    }

    public static int getCurrentPing() {
        if (client.getNetworkHandler() != null && client.player != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) {return entry.getLatency(); } // Ping (ms)
        }
        return -1; // err
    }
}
