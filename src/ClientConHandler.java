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

public final class ClientConHandler implements Runnable {

    public Callback callback; // note(nschultz): Gets set after ctor, if not then default callback will be used

    private Socket clientSocket = null;
    private String ipv4;
    private int port;

    public ClientConHandler()  {
        // note(nschultz): To avoid potential NPE
        this(new Callback() {
            @Override public void onConnectionEstablished() {}
            @Override public void onIncomingData(final String data) {}
            @Override public void onConnectionFailure(final String reason) {}
            @Override public void onConnectionTimeout() {}
            @Override public void onConnectionReleased() {}
        });
    }

    public ClientConHandler(final Callback callback)  {
        assert callback != null;

        this.callback = callback;
    }

    @Override
    public void run() {
        try_open: {
            this.clientSocket = new Socket();
            try {
                final int timeoutMillis = 4000; // todo(nschultz): Dynamic
                this.clientSocket.connect(new InetSocketAddress(ipv4, this.port), timeoutMillis); // note(nschultz): Blocks until error, timeout or connection establishment
                // this.clientSocket.setSoTimeout(); // todo(nschultz): Later, if we have the option for a heartbeat connection

                // note(nschultz): We do not store the return values because
                // we will call them for each send we are doing. If they then fail at some point
                // we know the connection has been lost.
                this.clientSocket.getInputStream();
                this.clientSocket.getOutputStream();

                this.callback.onConnectionEstablished(); // todo(nschultz): pass time it took?
            } catch (final IOException ex) {
                if (ex instanceof SocketTimeoutException) {
                    Main.logger.log(Level.INFO, String.format("Failed to establish connection to '%s:%s' due to timeout", ipv4, this.port));
                    this.callback.onConnectionTimeout();
                    if (isConnected()) teardown(); // todo(nschultz): Perhaps we connected but were unable to obtain the streams? Is that even possible?
                    return; // note(nschultz): Prevent going into the read loop
                } else {
                    Main.logger.log(Level.INFO, String.format("Failed to establish connection to '%s:%s'", ipv4, this.port));
                    this.callback.onConnectionFailure(ex.getMessage());
                    if (isConnected()) teardown(); // todo(nschultz): Perhaps we connected but were unable to obtain the streams? Is that even possible?
                    return; // note(nschultz): Prevent going into the read loop
                }
            }
        }

        read_loop: {
            for (;;) {
                try {
                    final InputStream in = this.clientSocket.getInputStream();
                    final byte[] buf = new byte[Settings.bufSize];
                    final int readBytes = in.read(buf);
                    if (readBytes == -1) {
                        Main.logger.log(Level.INFO, String.format("Connection has been closed from '%s:%s'", ipv4, this.port));
                        teardown();
                        return; // note(nschultz): User has to call 'start()' again
                    } else {
                        this.callback.onIncomingData(new String(buf, 0, readBytes, StandardCharsets.UTF_8)); // todo(nschultz): encoding
                        continue;
                    }
                } catch (final IOException ex) {
                    Main.logger.log(Level.INFO, String.format("Failed to read data to '%s:%s'", ipv4, this.port));
                    this.callback.onConnectionFailure(ex.getMessage());
                    teardown();
                    return; // note(nschultz): User has to call 'start()' again
                }

            }
        }
    }

    public void start(final String ipv4, final int port) {
        assert ipv4 != null;
        assert port >= 1 && port <= 65535;
        assert !isConnected();

        this.ipv4 = ipv4;
        this.port = port;

        final Thread thread = new Thread(this);
        thread.setName("ClientConHandlerThread");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isConnected() {
        if (this.clientSocket != null) {
            try {
                this.clientSocket.getInputStream();
                this.clientSocket.getOutputStream();
                return true;
            } catch (final IOException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void send(final String data) {
        assert isConnected();

        try {
            final OutputStream out = this.clientSocket.getOutputStream();
            out.write(data.getBytes(StandardCharsets.UTF_8)); // todo(nschultz): encoding
            out.flush();
        } catch (final IOException ex) {
            Main.logger.log(Level.INFO, String.format("Failed to write data to '%s:%s'", this.clientSocket.getInetAddress(), this.clientSocket.getPort()));
            this.callback.onConnectionFailure(ex.getMessage());
            teardown();
        }
    }

    public void teardown() {
        if (!isConnected()) {
            this.clientSocket = null;
            return;
        }

        try {
            this.clientSocket.close();
            this.clientSocket = null;
            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();
            this.callback.onConnectionReleased();
        } catch (final IOException ex) {
            Main.logger.log(Level.SEVERE, "Failed to cleanup client connection!");
            // todo(nschultz): panic?
        }
    }

    public interface Callback {

        public void onConnectionEstablished();
        public void onIncomingData(final String data);
        public void onConnectionFailure(final String reason);
        public void onConnectionTimeout();
        public void onConnectionReleased();
    }
}
