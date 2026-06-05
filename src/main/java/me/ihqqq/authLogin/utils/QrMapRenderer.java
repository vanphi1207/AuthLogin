package me.ihqqq.authLogin.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.*;

public class QrMapRenderer extends MapRenderer {
    private static final byte COLOR_BLACK = MapPalette.matchColor(Color.BLACK);
    private static final byte COLOR_WHITE = MapPalette.matchColor(Color.WHITE);

    private final String otpUri;
    private boolean rendered = false;

    public QrMapRenderer(String otpUri) {
        this.otpUri = otpUri;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;
        rendered = true;

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpUri, BarcodeFormat.QR_CODE, 128, 128);

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    canvas.setPixel(x, y, matrix.get(x, y) ? COLOR_BLACK : COLOR_WHITE);
                }
            }
        } catch (WriterException e) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    canvas.setPixel(x, y, COLOR_WHITE);
                }
            }
        }
    }
}
