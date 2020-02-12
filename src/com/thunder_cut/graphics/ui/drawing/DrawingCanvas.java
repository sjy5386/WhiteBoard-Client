/*
 * DrawingCanvas.java
 * Author : susemeeee
 * Created Date : 2020-01-17
 */
package com.thunder_cut.graphics.ui.drawing;

import com.thunder_cut.graphics.controller.MouseData;
import com.thunder_cut.graphics.controller.MouseStatus;
import com.thunder_cut.graphics.controller.Resizer;
import com.thunder_cut.netio.ConnectionModule;
import com.thunder_cut.netio.DataType;
import com.thunder_cut.netio.DataWrapper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DrawingCanvas {
    private Canvas canvas;
    private CanvasPixelInfo canvasPixelInfo;
    private BiConsumer<MouseData, CanvasPixelInfo> mouseHandler;
    private Consumer<MouseStatus> workDataRecorder;
    private Resizer resizer = new Resizer();

    public DrawingCanvas() {
        canvas = new Canvas();
        canvas.setIgnoreRepaint(true);
        canvas.setBackground(Color.WHITE);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                workDataRecorder.accept(MouseStatus.PRESSED);
                mouseHandler.accept(new MouseData(MouseStatus.PRESSED, e.getX(), e.getY()), canvasPixelInfo);
                drawCanvas();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseHandler.accept(new MouseData(MouseStatus.RELEASED, e.getX(), e.getY()), canvasPixelInfo);
                workDataRecorder.accept(MouseStatus.RELEASED);
                ConnectionModule.getInstance().send(new DataWrapper(getByteBuffer(canvasPixelInfo.toBufferedImage()), DataType.IMG));
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseHandler.accept(new MouseData(MouseStatus.DRAGGED, e.getX(), e.getY()), canvasPixelInfo);
                drawCanvas();
            }
        });
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                //Here you can check the changing size
                try {
                    resizer.resize(canvas.getWidth(),canvas.getHeight(),canvasPixelInfo);
                }
                catch (NullPointerException e1) { }

                drawCanvas();
            }
        });
    }

    public void addMouseHandler(BiConsumer<MouseData, CanvasPixelInfo> mouseHandler) {
        this.mouseHandler = mouseHandler;
    }

    public void addWorkDataRecorder(Consumer<MouseStatus> workDataRecorder) {
        this.workDataRecorder = workDataRecorder;
    }

    public void createPixelInfo() {
        canvasPixelInfo = new CanvasPixelInfo(canvas.getWidth(), canvas.getHeight(), Color.WHITE);
    }

    public void drawCanvas() {
        BufferStrategy canvasBuffer = canvas.getBufferStrategy();

        if (canvasBuffer == null) {
            canvas.createBufferStrategy(2);
            return;
        }

        BufferedImage image = canvasPixelInfo.toBufferedImage();

        Graphics2D g = (Graphics2D) canvasBuffer.getDrawGraphics();
        g.drawImage(image, 0, 0, canvas);
        g.dispose();
        canvasBuffer.show();
    }

    private ByteBuffer getByteBuffer(BufferedImage image) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ByteBuffer.wrap(bos.toByteArray());
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public CanvasPixelInfo getCanvasPixelInfo() {
        return canvasPixelInfo;
    }

    public void notifyFrameMoved(){
        drawCanvas();
    }
}
