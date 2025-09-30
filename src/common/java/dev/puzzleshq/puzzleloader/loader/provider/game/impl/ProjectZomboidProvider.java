package dev.puzzleshq.puzzleloader.loader.provider.game.impl;

import com.github.villadora.semver.Version;
import dev.puzzleshq.mod.info.ModInfoBuilder;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.launch.PieceClassLoader;
import dev.puzzleshq.puzzleloader.loader.mod.ModContainer;
import dev.puzzleshq.puzzleloader.loader.provider.game.IGameProvider;
import dev.puzzleshq.puzzleloader.loader.util.EnvType;
import dev.puzzleshq.puzzleloader.loader.util.ModFinder;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import org.hjson.JsonObject;
import org.objectweb.asm.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProjectZomboidProvider implements IGameProvider {
    private Version cachedVersion = null;

    public ProjectZomboidProvider() {
        Piece.provider = this;
    }

    @Override
    public String getId() {
        return "project-zomboid";
    }

    @Override
    public String getName() {
        return "Project Zomboid";
    }

    @Override
    public Version getGameVersion() {
        if (cachedVersion == null) {
            fetchGameVersion();
        }
        return cachedVersion;
    }

    @Override
    public String getRawVersion() {
        if (cachedVersion == null) {
            fetchGameVersion();
        }
        return cachedVersion.toString();
    }

    @Override
    public String getEntrypoint() {
        String launcher = "zombie/network/GameServer.class";
        if (Piece.getSide() == EnvType.SERVER) {
            try {
                RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
                return launcher.replaceAll("/", ".").replace(".class", "");
            } catch (Exception ignore) {
                throw new RuntimeException("Project Zomboid Server Main does not exist.");
            }
        }
        try {
            launcher = "zombie/gameStates/MainScreenState.class";
            RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
        } catch (Exception e) {
            throw new RuntimeException("Project Zomboid Client Main does not exist.");
        }

        return launcher.replaceAll("/", ".").replace(".class", "");
    }

    @Override
    public Collection<String> getArgs() {
        return args;
    }

    List<String> args;

    @Override
    public void initArgs(String[] args) {
        this.args = new ArrayList<>(Arrays.asList(args));
    }

    @Override
    public void inject(PieceClassLoader classLoader) {}

    @Override
    public void addBuiltinMods() {
        ModInfoBuilder projectZomboidModInfo = new ModInfoBuilder();
        {
            projectZomboidModInfo.setDisplayName(getName());
            projectZomboidModInfo.setId(getId());
            projectZomboidModInfo.setDescription("The base game.");
            projectZomboidModInfo.addAuthor("The Indie Stone");
            projectZomboidModInfo.setVersion(getRawVersion());
            projectZomboidModInfo.addMeta("icon", JsonObject.valueOf("pack.png"));
            ModFinder.addModWithContainer(new ModContainer(projectZomboidModInfo.build()));
        }

    }

    @Override
    public String getDefaultNamespace() {
        return "zomboid";
    }

    @Override
    public boolean isValid() {
        try {
            String launcher = "zombie/network/GameServer.class";
            if (Piece.getSide() == EnvType.SERVER) {
                try {
                    RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
                    return true;
                } catch (Exception ignore) {
                    throw new RuntimeException("Project Zomboid Server Main does not exist.");
                }
            }
            try {
                launcher = "zombie/gameStates/MainScreenState.class";
                RawAssetLoader.getLowLevelClassPathAssetErrors(launcher, false).dispose();
            } catch (Exception e) {
                throw new RuntimeException("Project Zomboid Client Main does not exist.");
            }
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean isBinaryPatchable() {
        return false;
    }

    private void fetchGameVersion() {
        try {
            RawAssetLoader.RawFileHandle core = RawAssetLoader.getLowLevelClassPathAssetErrors("zombie/core/Core.class", false);
            assert core != null;
            ByteArrayInputStream is = new ByteArrayInputStream(core.getBytes());
            ClassReader cr = new ClassReader(is);
            VersionVisitor finder = new VersionVisitor();
            cr.accept(finder, 0);
            cachedVersion = finder.detectedVersion;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class VersionVisitor extends ClassVisitor {
        public Version detectedVersion;
        private Integer build = null;

        public VersionVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if ("buildVersion".equals(name) && value instanceof Integer) {
                build = (Integer) value;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("<clinit>".equals(name)) {
                return new StaticInitVisitor();
            }
            return null;
        }

        private class StaticInitVisitor extends MethodVisitor {
            Object major = null, minor = null, suffix = null;
            private final List<Object> stack = new ArrayList<>();
            private boolean nextIsGameVersion = false;

            public StaticInitVisitor() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visitLdcInsn(Object value) {
                stack.add(value);
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                stack.add(operand);
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                    int val = opcode - Opcodes.ICONST_0;
                    stack.add(val);
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (
                        opcode == Opcodes.INVOKESPECIAL &&
                                owner.equals("zombie/core/GameVersion") &&
                                name.equals("<init>")
                ) {
                    Type[] args = Type.getArgumentTypes(desc);
                    if (args.length == 3 &&
                            args[0].getClassName().equals("int") &&
                            args[1].getClassName().equals("int") &&
                            args[2].getClassName().equals("java.lang.String")) {

                        if (stack.size() >= 3) {
                            int size = stack.size();
                            major = stack.get(size - 3);
                            minor = stack.get(size - 2);
                            suffix = stack.get(size - 1);
                            nextIsGameVersion = true;
                        }
                    }
                } else {
                    nextIsGameVersion = false;
                }
                stack.clear();
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (
                        opcode == Opcodes.PUTSTATIC &&
                                owner.equals("zombie/core/Core") &&
                                name.equals("gameVersion") &&
                                nextIsGameVersion
                ) {
                    if (major instanceof Integer && minor instanceof Integer && build != null && suffix instanceof String) {
                        if (((String) suffix).isEmpty()) {
                            detectedVersion = Version.valueOf(major + "." + minor + "." + build);
                        } else {
                            detectedVersion = Version.valueOf(major + "." + minor + "." + build + "-" + suffix);
                        }
                    }

                }
                nextIsGameVersion = false;
                stack.clear();
            }
        }
    }
}
