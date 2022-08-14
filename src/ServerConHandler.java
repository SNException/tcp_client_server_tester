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

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.logging.*;

public final class ServerConHandler implements Runnable {

    public Callback callback; // note(nschultz): Gets set after ctor, if not then default callback will be used

    private ServerSocket serverSocket = null;
    private Socket theClient          = null;
    private int port;

    public ServerConHandler()  {
        // note(nschultz): To avoid potential NPE
        this(new Callback() {
            public void onOpen() {}
            public void onNewClient(final Socket client) {}
            public void onClientLost(final Socket client) {}
            public void onIncomingData(final String data) {}
            public void onConnectionFailure(final String reason) {}
            public void onClose() {}
        });
    }

    public ServerConHandler(final Callback callback)  {
        assert callback != null;

        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            final int maxCons = 1;
            this.serverSocket = new ServerSocket(this.port, maxCons);
            this.callback.onOpen();

            waitForClient();
            if (this.theClient == null) {
                return;
            }

        } catch (final IOException ex) {
            Main.logger.log(Level.INFO, String.format("Failed to open server on port '%s'", this.port));
            this.callback.onConnectionFailure(ex.getMessage());
            return; // note(nschultz): User has to call 'start()' again
        }

        read_loop: {
            while (isOpen() && hasClient()) {
                try {
                    final InputStream in = this.theClient.getInputStream();
                    final byte[] buf = new byte[Settings.bufSize];
                    final int readBytes = in.read(buf);
                    if (readBytes == -1) {
                        Main.logger.log(Level.INFO, String.format("Connection has been closed from '%s:%s'", theClient.getInetAddress(), theClient.getPort()));
                        this.callback.onClientLost(this.theClient);
                        closeClient();
                        waitForClient();
                        if (this.theClient == null) {
                            // note(nschultz): User has to call 'start()' again
                            return;
                        } else {
                            continue;
                        }
                    } else {
                        this.callback.onIncomingData(new String(buf, 0, readBytes, StandardCharsets.UTF_8)); // todo(nschultz): encoding
                        continue;
                    }
                } catch (final IOException ex) {
                    Main.logger.log(Level.INFO, "Failed to read data from client");
                    this.callback.onConnectionFailure(ex.getMessage());
                    teardown();
                    return; // note(nschultz): User has to call 'start()' again
                }
            }
        }
    }

    private void waitForClient() {
        assert isOpen();
        assert !hasClient();

        try {
            this.theClient = this.serverSocket.accept();
            // note(nschultz): We do not store the return values because
            // we will call them for each send we are doing. If they then fail at some point
            // we know the connection has been lost.
            this.theClient.getInputStream();
            this.theClient.getOutputStream();

            this.callback.onNewClient(this.theClient);

        } catch (final IOException ex) {
            Main.logger.log(Level.INFO, String.format("Failed to wait for client on port '%s'", this.port));
            this.callback.onConnectionFailure(ex.getMessage());
        }
    }

    public void start(final int port) {
        assert port >= 1 && port <= 65535;

        this.port = port;

        final Thread thread = new Thread(this);
        thread.setName("ServerConHandlerThread");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isOpen() {
        // todo(nschultz): Check if port is in use
        return this.serverSocket != null;
    }

    public boolean hasClient() {
        if (this.theClient != null) {
            try {
                this.theClient.getInputStream();
                this.theClient.getOutputStream();
                return true;
            } catch (final IOException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void send(final String data) {
        assert isOpen() && hasClient();

        try {
            final OutputStream out = this.theClient.getOutputStream();
            out.write(data.getBytes(StandardCharsets.UTF_8)); // todo(nschultz): encoding
            out.flush();
        } catch (final IOException ex) {
            Main.logger.log(Level.INFO, String.format("Failed to write data to '%s:%s'", this.theClient.getInetAddress(), this.theClient.getPort()));
            this.callback.onConnectionFailure(ex.getMessage());
            teardown();
        }
    }

    private void closeClient() {
        try {
            this.theClient.close();
            this.theClient = null;
        } catch (final IOException ex) {
            Main.logger.log(Level.SEVERE, "Failed to cleanup client connection from server site!");
            // todo(nschultz): panic?
        }
    }

    public void teardown() {
        if (!isOpen()) return;

        try {
            this.serverSocket.close();
            this.serverSocket = null;
            if (hasClient()) {
                closeClient();
            }
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            this.callback.onClose();
        } catch (final IOException ex) {
            Main.logger.log(Level.SEVERE, "Failed to cleanup server connection!");
            // todo(nschultz): panic?
        }
    }

    public interface Callback {

        public void onOpen();
        public void onNewClient(final Socket client);
        public void onClientLost(final Socket client);
        public void onIncomingData(final String data);
        public void onConnectionFailure(final String reason);
        public void onClose();
    }
}

