package net.minecraft.launch;

import com.github.puzzle.loader.launch.Piece;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MinecraftAppletLauncher {

    public static void main(String[] args)  {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSet options = parser.parse(args);
        OptionSpec<String> puzzleEdition = parser.accepts("puzzleEdition").withOptionalArg().ofType(String.class);

        String clazzStr = puzzleEdition.value(options);
        try {
            Class<?> clazz = Piece.classLoader.findClass(clazzStr);
            Constructor<?> constructor = clazz.getConstructor();
            Applet minecraft = (Applet) constructor.newInstance();

            start(minecraft, args);
        } catch (Exception ignore) {}
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

        final Frame launcherFrame = new Frame();
        launcherFrame.setTitle("Puzzle Craft");
        launcherFrame.setBackground(Color.BLACK);

        final JPanel panel = new JPanel();
        launcherFrame.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(854, 480));
        launcherFrame.add(panel, BorderLayout.CENTER);
        launcherFrame.pack();

        launcherFrame.setLocationRelativeTo(null);
        launcherFrame.setVisible(true);

        launcherFrame.addWindowListener(new WindowAdapter() {
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

        launcherFrame.removeAll();
        launcherFrame.setLayout(new BorderLayout());
        launcherFrame.add(fakeLauncher, BorderLayout.CENTER);
        launcherFrame.validate();

        minecraft.init();
        minecraft.start();

        Runtime.getRuntime().addShutdownHook(new Thread(minecraft::stop));
    }

}
