//
// Copyright (c) 2022 Niklas Schultz
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

import java.io.*;
import java.lang.management.*;
import java.nio.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

public final class Main {

    public static final boolean DEBUG_MODE = ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-ea");
    public static final Logger logger = allocateLogger();

    private static Logger allocateLogger() {
        LogManager.getLogManager().reset();
        final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Main.DEBUG_MODE ? Level.ALL : Level.SEVERE);
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(final LogRecord log) {
                return String.format("[%s] [%s] [%s.%s]: %s\n",
                                    log.getLevel().getLocalizedName(),
                                    new SimpleDateFormat("dd.MM.YYYY HH:mm:ss:SSS").format(new Date(log.getMillis())),
                                    log.getSourceClassName(),
                                    log.getSourceMethodName(),
                                    log.getMessage()
                );
            }
        });
        consoleHandler.setLevel(Main.DEBUG_MODE ? Level.ALL : Level.SEVERE);
        logger.addHandler(consoleHandler);

        return logger;
    }

    private static void loadAllClassesIntoMemory() {
        final String root = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        final ArrayList<String> files = new ArrayList<>();
        try (final Stream<Path> stream = Files.walk(Paths.get(root), Integer.MAX_VALUE)) {
            files.addAll(stream.map(String::valueOf).sorted().collect(Collectors.toList()));
        } catch (final IOException ex) {
            Main.logger.log(Level.SEVERE, "Failed to find all class files to load.", ex);
            return;
        }

        for (int i = 0, l = files.size(); i < l; ++i) {
            final String file = files.get(i);
            if (Files.isDirectory(Paths.get(file)) || !file.endsWith(".class")) {
                continue;
            }

            final String classDef = file.replace(root + File.separator, "").replaceAll("\\" + File.separator, "\\.").replace(".class", "");
            try {
                final Class<?> c = Class.forName(classDef);
                if (c != null) {
                    Main.logger.log(Level.INFO, String.format("Loaded '%s' into memory!", c.getName()));
                } else {
                    Main.logger.log(Level.SEVERE, "Failed to load all classes into memory beforehand!");
                    return;
                }
            } catch (final Exception ex) {
                Main.logger.log(Level.SEVERE, "Failed to load all classes into memory beforehand!", ex);
                return;
            }
        }
    }

    private static void initUncaughtExceptionHandler() {
        // note(nschultz): We only want to apply this exception handler in dev mode so
        // we do not waste CPU time checking whether the exception is an
        // instance of AssertionError, which can not happen anyway.
        if (Main.DEBUG_MODE) {
            Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
                if (ex instanceof AssertionError) {
                    System.err.println("--- ASSERTION FAILED ---");
                    System.err.println();
                    ex.printStackTrace(System.err);

                    // note(nschultz): Kill the JVM immediately.
                    // I have no clue why this is not the default behaviour
                    // when triggering assert statements.
                    // That just makes them almost completely pointless otherwise.
                    Runtime.getRuntime().halt(-1);
                }

                // note(nschultz): fallthrough to the default behaviour
                // (found here: ThreadGroup#uncaughtException)
                if (!(ex instanceof ThreadDeath)) {
                    System.err.print(
                        "Exception in thread \"" + t.getName() + "\" "
                    );
                    ex.printStackTrace(System.err);
                }
            });
        }
    }

    public static void main(final String[] args) {
        if (Main.DEBUG_MODE) {
            Main.logger.log(Level.INFO, "Running with assertions enabled!");
        }

        initUncaughtExceptionHandler();

        loadAllClassesIntoMemory();

        // note(nschultz): Let's try to collect some of the garbage we have made so far
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();

        // note(nschultz): Yield to other threads before we really go
        Thread.yield();

        new MainWindow().show();
    }
}
