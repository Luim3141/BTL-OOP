package library.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IconProvider {
    private static final String BASE_PATH = "/library/icons/";
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private IconProvider() {
    }

    public static ImageView icon(String name, double size) {
        ImageView view = new ImageView(image(name));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    public static Image image(String name) {
        return CACHE.computeIfAbsent(name, IconProvider::loadImage);
    }

    private static Image loadImage(String name) {
        String resourceName = BASE_PATH + name + ".png";
        InputStream stream = IconProvider.class.getResourceAsStream(resourceName);
        if (stream == null) {
            return placeholder();
        }
        return new Image(stream);
    }

    private static Image placeholder() {
        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(24, 24);
        javafx.scene.image.PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                boolean border = x == 0 || y == 0 || x == 23 || y == 23;
                writer.setArgb(x, y, border ? 0xFF888888 : 0x00FFFFFF);
            }
        }
        return image;
    }
}
