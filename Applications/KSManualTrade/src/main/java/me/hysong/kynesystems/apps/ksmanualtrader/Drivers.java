package me.hysong.kynesystems.apps.ksmanualtrader;

import me.hysong.apis.kstrader.v1.driver.TraderDriverManifestV1;
import me.hysong.apis.kstrader.v1.strategy.TraderStrategyManifestV1;
import me.hysong.kynesystems.apps.ksmanualtrader.Application;
import me.hysong.kynesystems.common.foundation.SystemLogs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Drivers {

    private static DriverLoader classLoader = new DriverLoader(new URL[]{}, Application.class.getClassLoader());
    private static ArrayList<String> jarsLoaded = new ArrayList<>();

    public final static HashMap<String, Class<?>> drivers = new HashMap<>();
    public final static HashMap<String, TraderDriverManifestV1> driversInstantiated = new HashMap<>(); // Key:
    public final static HashMap<String, Class<?>> strategies = new HashMap<>();
    public final static HashMap<String, TraderStrategyManifestV1> strategiesInstantiated = new HashMap<>();

    public static class DriverLoader extends URLClassLoader {
        public DriverLoader(URL[] initial, ClassLoader parent) {
            super(initial, parent);
        }
        public void addJar(URL jarUrl) {
            super.addURL(jarUrl);
        }
    }

    public static void coreComponent(String pathToJar) throws Exception{
        if (jarsLoaded.contains(pathToJar)) {
            SystemLogs.log("WARNING", "Jar file " + pathToJar + " is not loaded because it is already loaded.");
            return;
        }
        File jarFile = new File(pathToJar);
        URL jarUrl = jarFile.toURI().toURL();

        if (classLoader == null) {
            classLoader = new DriverLoader(new URL[]{jarUrl}, Application.class.getClassLoader());
        } else {
            classLoader.addJar(jarUrl);
        }

        jarsLoaded.add(pathToJar);
        SystemLogs.log("INFO", "Loaded JAR: " + jarFile.getName());
    }

    public static Class<?> getClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    public static void loadJarsIn(File directory) throws Exception {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jar")) {
                        coreComponent(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        loadJarsIn(file);
                    }
                }
            }
        }
    }

    public static final class DriverIntrospection {

        private DriverIntrospection() {}

        /**
         * Find every concrete class loaded by {@code loader} that implements {@code interfaceType}.
         *
         * @param interfaceType the interface to match (must indeed be an interface)
         * @return map keyed by fully‑qualified class name → {@code Class<?>}
         * @throws IOException  in case a JAR cannot be opened
         */
        public static Map<String, Class<?>> findImplementations(Class<?> interfaceType) throws IOException {

            if (!interfaceType.isInterface()) {
                throw new IllegalArgumentException(interfaceType.getName() + " is not an interface");
            }
            return scan(Drivers.classLoader, candidate ->
                    interfaceType.isAssignableFrom(candidate)
                            && !candidate.isInterface()
                            && !Modifier.isAbstract(candidate.getModifiers()));
        }

        /**
         * Find every concrete subclass of {@code superClass} loaded by {@code loader}.
         *
         * @param superClass the parent class to match (must not be an interface)
         * @return map keyed by fully‑qualified class name → {@code Class<?>}
         * @throws IOException in case a JAR cannot be opened
         */
        public static Map<String, Class<?>> findSubclasses(Class<?> superClass) throws IOException {

            if (superClass.isInterface()) {
                throw new IllegalArgumentException(superClass.getName() + " is an interface;"
                        + " use findImplementations instead");
            }
            return scan(Drivers.classLoader, candidate ->
                    superClass.isAssignableFrom(candidate)
                            && !candidate.equals(superClass)
                            && !Modifier.isAbstract(candidate.getModifiers()));
        }

        /* ---------- internal plumbing ---------- */

        @FunctionalInterface
        private interface ClassPredicate {
            boolean test(Class<?> c);
        }

        private static Map<String, Class<?>> scan(URLClassLoader loader,
                                                  ClassPredicate accept) throws IOException {

            Map<String, Class<?>> hits = new HashMap<>();

            for (URL url : loader.getURLs()) {
                // Narrow scope: only file‑based JAR URLs
                if (!"file".equalsIgnoreCase(url.getProtocol()) || !url.getPath().endsWith(".jar")) {
                    continue;
                }

                // Resolve symlinks / spaces correctly
                try (JarFile jar = new JarFile(Paths.get(url.toURI()).toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                            continue;
                        }

                        String className = entry.getName()
                                .replace('/', '.')
                                .substring(0, entry.getName().length() - 6); // strip ".class"
                        try {
                            Class<?> cls = loader.loadClass(className);
                            if (accept.test(cls)) {
                                hits.put(cls.getName(), cls);
                            }
                        } catch (Throwable ignore) {
                            // Class failed to load – missing deps, VerifyError, etc. Ignore and continue.
                        }
                    }
                } catch (Exception e) {
                    // Bubble up as IOException so the caller can decide whether to abort or continue
                    throw new IOException("Failed to inspect JAR " + url, e);
                }
            }
            return hits;
        }
    }
}


