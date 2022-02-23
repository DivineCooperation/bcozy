/**
 * ==================================================================
 *
 * This file is part of org.openbase.bco.bcozy.
 *
 * org.openbase.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.openbase.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.openbase.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy;

import javafx.application.Platform;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.bcozy.jp.JPFullscreenMode;
import org.openbase.bco.bcozy.jp.JPLanguage;
import org.openbase.bco.bcozy.view.LoadingPane;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.openbase.bco.bcozy.BCozy.APP_NAME;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCozyLauncher {

    /**
     * Application launcher logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BCozyLauncher.class);

    /**
     * Main Method starting JavaFX Environment.
     *
     * @param args Arguments from command line.
     */
    public static void main(final String... args) {
        LOGGER.info("Start " + APP_NAME + "...");

        //todo: this is a workaround for issue https://github.com/openbase/bco.bcozy/issues/99
        // to force fx to use gtk-2 which solves the touch handling issue. Please move it after issue has been solved.
        String[] args_extended = Arrays.copyOf(args, args.length+1);
        args_extended[args.length] = "-Djdk.gtk.version=2";

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPVerbose.class);
        JPService.registerProperty(JPFullscreenMode.class);
        JPService.registerProperty(JPLanguage.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPComPort.class);
        JPService.registerProperty(JPAuthentication.class);
        try {
            JPService.parseAndExitOnError(args_extended);
            Thread.setDefaultUncaughtExceptionHandler(BCozyLauncher::showError);
            BCozy.launch(BCozy.class, args_extended);
        } catch (IllegalStateException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            LOGGER.info(APP_NAME + " finished unexpected.");
        }
        LOGGER.info(APP_NAME + " finished.");
    }

    private static int errorCounter = 0;

    private static void showError(Thread t, Throwable ex) {
        errorCounter++;

        try {
            if (Platform.isFxApplicationThread()) {
                new FatalImplementationErrorException("Uncaught exception has occured!", "FxApplicationThread", ex);
            } else {
                new FatalImplementationErrorException("Uncaught exception has occured!", t.getName(), ex);
            }
        } catch (AssertionError exx) {
            // assertion error ignored because exception stack was already printed within the FatalImplementationErrorException.
        }

        // check if error counter is to high
        if (errorCounter == 100) {

            if (Platform.isFxApplicationThread()) {
                printShutdownErrorMessage();
            } else {
                Platform.runLater(() -> {
                    printShutdownErrorMessage();
                });
            }

            new Thread() {
                @Override
                public void run() {
                    try {
                        // wait until user feedback is given.
                        Thread.sleep(1000);
                    } catch (InterruptedException ex1) {
                        // just continue because exit is called anyway.
                    }
                    System.exit(100);
                }
            }.start();
            return;

        }
    }

    private static void printShutdownErrorMessage() {
        LOGGER.error("To many fatal errors occured! Exit bcozy...");
        try {
            LoadingPane.getInstance().error("To many fatal errors occured!\nApplication is shutting down ");
        } catch (NotAvailableException ex1) {
            // could not inform user about shutdown
        }
    }
}
