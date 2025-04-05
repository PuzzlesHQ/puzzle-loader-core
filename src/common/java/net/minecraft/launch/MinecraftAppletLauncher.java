package net.minecraft.launch;

import com.github.puzzle.loader.util.Reflection;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class MinecraftAppletLauncher {

    public static Applet GAME_APPLET;
    public static Frame LAUNCHER_FRAME;

    public static void main(String[] args)  {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSpec<String> puzzleEdition = parser.accepts("puzzleEdition").withOptionalArg().ofType(String.class);
        final OptionSpec<File> gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        final OptionSpec<File> assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
        final OptionSpec<String> assetIndexOption = parser.accepts("assetIndex", "Assets index").withRequiredArg().ofType(String.class);
        final OptionSet options = parser.parse(args);

        MinecraftAssetDictionary.setup(gameDirOption.value(options).getAbsoluteFile(), assetIndexOption.value(options), assetsDirOption.value(options).getAbsoluteFile());
        MinecraftAssetDictionary.getAllResources();
        String clazzStr = puzzleEdition.value(options);
        try {
            Class<?> clazz = Class.forName(clazzStr.replaceFirst("\\/", "").replaceAll("\\/", ".").replace(".class", ""), false, MinecraftAppletLauncher.class.getClassLoader());
            Constructor<?> constructor = clazz.getConstructor();
            GAME_APPLET = (Applet) constructor.newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                String name = field.getType().getName();

                if (!name.contains("awt") && !name.contains("java") && !name.equals("long")) {
                    Field fileField = getWorkingDirField(name);
                    if (fileField != null) {
                        fileField.setAccessible(true);
                        fileField.set(null, MinecraftAssetDictionary.gameDir);
                        break;
                    }
                }
            }

            loadIconsOnFrames(assetsDirOption.value(options));

            start(GAME_APPLET, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String setTitle(String title) {
        if (LAUNCHER_FRAME != null)
            LAUNCHER_FRAME.setTitle(title);
        return title;
    }

    private static Field getWorkingDirField(String name) throws ClassNotFoundException {
        Class<?> clazz = MinecraftAppletLauncher.class.getClassLoader().loadClass(name);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().getName().equals("java.io.File")) {
                return field;
            }
        }

        return null;
    }

    private static ByteBuffer loadIcon(final File iconFile) throws IOException {
        final BufferedImage icon = ImageIO.read(iconFile);

        final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());

        final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
        for (int color : rgb) {
            buffer.putInt(color << 8 | ((color >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
    }

    public static void loadIconsOnFrames(File assetsDir) {
        try {
            // Load icon from disk
            final File smallIcon = MinecraftAssetDictionary.getAsset("icons/icon_16x16.png");
            final File bigIcon = MinecraftAssetDictionary.getAsset("icons/icon_32x32.png");
            Reflection.getMethod(Class.forName("org.lwjgl.opengl.Display"), "setIcon", ByteBuffer[].class)
                    .invoke(null, (Object) new ByteBuffer[]{
                            loadIcon(smallIcon),
                            loadIcon(bigIcon)
                    });
            Frame[] frames = Frame.getFrames();

            if (frames != null) {
                final List<Image> icons = Arrays.asList(ImageIO.read(smallIcon), ImageIO.read(bigIcon));

                for (Frame frame : frames) {
                    try {
                        frame.setIconImages(icons);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        System.out.println(throwable);
                    }
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | IOException | IllegalAccessException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    private static void start(Applet minecraft, String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSet options = parser.parse(args);
        OptionSpec<String> username = parser.accepts("username").withOptionalArg().ofType(String.class);
        OptionSpec<String> sessionId = parser.accepts("sessionId").withOptionalArg().ofType(String.class);

        final Map<String, String> params = new HashMap<>();

        params.put("username", username.value(options));
        params.put("sessionId", sessionId.value(options));

        LAUNCHER_FRAME = new Frame();
        LAUNCHER_FRAME.setTitle("Puzzle Craft");
        LAUNCHER_FRAME.setBackground(Color.BLACK);

        final JPanel panel = new JPanel();
        LAUNCHER_FRAME.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(854, 480));
        LAUNCHER_FRAME.add(panel, BorderLayout.CENTER);
        LAUNCHER_FRAME.pack();

        LAUNCHER_FRAME.setLocationRelativeTo(null);
        LAUNCHER_FRAME.setVisible(true);

        LAUNCHER_FRAME.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }
        });

        class LauncherFake extends Applet implements AppletStub {
            private static final long serialVersionUID = 1L;

            public void appletResize(int width, int height) {
                // Actually empty as well
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public URL getDocumentBase() {
                try {
                    return new URL("http://www.minecraft.net/game/");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public URL getCodeBase() {
                try {
                    return new URL("http://www.minecraft.net/game/");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public String getParameter(String paramName) {
                if (params.containsKey(paramName)) {
                    return params.get(paramName);
                }
                System.err.println("Client asked for parameter: " + paramName);
                return null;
            }
        }

        final LauncherFake fakeLauncher = new LauncherFake();
        minecraft.setStub(fakeLauncher);

        fakeLauncher.setLayout(new BorderLayout());
        fakeLauncher.add(minecraft, BorderLayout.CENTER);
        fakeLauncher.validate();

        LAUNCHER_FRAME.removeAll();
        LAUNCHER_FRAME.setLayout(new BorderLayout());
        LAUNCHER_FRAME.add(fakeLauncher, BorderLayout.CENTER);
        LAUNCHER_FRAME.validate();

        minecraft.init();
        minecraft.start();

        Runtime.getRuntime().addShutdownHook(new Thread(minecraft::stop));
    }

}
