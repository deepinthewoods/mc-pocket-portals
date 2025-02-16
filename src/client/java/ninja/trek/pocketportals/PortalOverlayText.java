package ninja.trek.pocketportals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class PortalOverlayText {
    private static String currentText = null;
    private static long lastUpdateTime = 0;
    private static final long DISPLAY_DURATION = 2000; // 2 seconds

    public static void setText(String text) {
        currentText = text;
        lastUpdateTime = System.currentTimeMillis();
    }

    public static void clearText() {
        currentText = null;
    }

    public static void render(DrawContext context) {
        if (currentText == null) return;
        // Clear text if it's been showing too long
        if (System.currentTimeMillis() - lastUpdateTime > DISPLAY_DURATION) {
            clearText();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // Split text into lines
        String[] lines = currentText.split("\n");

        // Calculate total height (adding a 2-pixel spacing between lines)
        int lineHeight = textRenderer.fontHeight + 2;
        int totalHeight = lines.length * lineHeight;

        // Get screen dimensions and define margins
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int sideMargin = 5;
        // Offset from bottom to clear the hotbar area; adjust this value as needed.
        int hotbarOffset = 30;

        // Set starting position above the hotbar.
        int baseX = sideMargin;
        int baseY = screenHeight - totalHeight - sideMargin - hotbarOffset;

        // Draw each line starting from the base position
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int y = baseY + (i * lineHeight);

            // Draw the line with white color (0xFFFFFF) and with a shadow.
            context.drawText(textRenderer, Text.literal(line), baseX, y, 0xFFFFFF, true);
        }
    }
}
