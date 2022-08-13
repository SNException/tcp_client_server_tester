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
import java.awt.event.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public final class MainWindow {

    // note(nschultz): This font gets packaged with the jdk
    private static final Font defaultFont = new Font("Monospace", Font.PLAIN, 14);

    private JFrame frame;

    public MainWindow() {
        assert !EventQueue.isDispatchThread();

        EventQueue.invokeLater(() -> {
            init();
        });
    }

    private void init() {
        assert EventQueue.isDispatchThread();

        skin: {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (final Exception ex) {
                Main.logger.log(Level.SEVERE, "Failed to set system look and feel.");
            }
        }

        fonts: {
            UIManager.put("Button.font", defaultFont);
            UIManager.put("ToggleButton.font", defaultFont);
            UIManager.put("RadioButton.font", defaultFont);
            UIManager.put("CheckBox.font", defaultFont);
            UIManager.put("ColorChooser.font", defaultFont);
            UIManager.put("ComboBox.font", defaultFont);
            UIManager.put("Label.font", defaultFont);
            UIManager.put("List.font", defaultFont);
            UIManager.put("MenuBar.font", defaultFont);
            UIManager.put("MenuItem.font", defaultFont);
            UIManager.put("RadioButtonMenuItem.font", defaultFont);
            UIManager.put("CheckBoxMenuItem.font", defaultFont);
            UIManager.put("Menu.font", defaultFont);
            UIManager.put("PopupMenu.font", defaultFont);
            UIManager.put("OptionPane.font", defaultFont);
            UIManager.put("Panel.font", defaultFont);
            UIManager.put("ProgressBar.font", defaultFont);
            UIManager.put("ScrollPane.font", defaultFont);
            UIManager.put("Viewport.font", defaultFont);
            UIManager.put("TabbedPane.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
            UIManager.put("TableHeader.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("PasswordField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("TextPane.font", defaultFont);
            UIManager.put("EditorPane.font", defaultFont);
            UIManager.put("TitledBorder.font", defaultFont);
            UIManager.put("ToolBar.font", defaultFont);
            UIManager.put("ToolTip.font", defaultFont);
            UIManager.put("Tree.font", defaultFont);
        }

        frame: {
            this.frame = new JFrame("TCP Client/Server Tester v0.1.0");
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.setSize(800, 600);
            this.frame.setLocation(350, 450); // todo(nschultz): temporary
            // this.frame.setLocationRelativeTo(null);
        }

        menu_bar: {
            final JMenuBar menubar = new JMenuBar();
            final JMenu fileMenu = new JMenu("File");
            final JMenu helpMenu = new JMenu("Help");
            final JMenuItem fileMenuExitItem = new JMenuItem("Exit");
            fileMenuExitItem.addActionListener(e -> {
                // todo(nschultz): shutdown possible connections? Not really needed since the OS does that anyway but...

                Runtime.getRuntime().gc();
                Runtime.getRuntime().runFinalization();
                System.exit(0);
            });
            final JMenuItem helpMenuAboutItem = new JMenuItem("About");
            helpMenuAboutItem.addActionListener(e -> {
                final JDialog dialog = new JDialog();
                dialog.setTitle("About");
                dialog.setSize(800, 500);
                dialog.setLocationRelativeTo(this.frame);
                final JPanel panel = new JPanel(new BorderLayout());
                final JTextArea area = new JTextArea();
                area.setLineWrap(true);
                area.append("""
                            TCP Client/Server Tester
                            ===============

                            Simple tool to both test tcp client and server communications.

                            License
                            =====
                            Copyright (c) 2022 Niklas Schultz

                            Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
                            documentation files (the "Software"), to deal in the Software without restriction,
                            including without limitation the rights to use, copy, modify, merge, publish, distribute,
                            sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
                            subject to the following conditions:

                            The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

                            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
                            TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
                            IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
                            WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
                            SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
                            """);
                area.setEditable(false);
                area.setCaretPosition(0);
                panel.add(new JScrollPane(area), BorderLayout.CENTER);
                dialog.add(panel);
                dialog.setVisible(true);
            });
            fileMenu.add(fileMenuExitItem);
            helpMenu.add(helpMenuAboutItem);
            menubar.add(fileMenu);
            menubar.add(helpMenu);
            this.frame.setJMenuBar(menubar);
        }

        final JTabbedPane tabPane = new JTabbedPane();
        final JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        root.add(tabPane, BorderLayout.CENTER);
        final JPanel clientPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel serverPanel = new JPanel(new BorderLayout(4, 4));
        tabPane.addTab("Client", clientPanel);
        tabPane.addTab("Server", serverPanel);
        tabPane.addTab("Info", new JPanel()); // todo(nschultz): Parse ipconfig output

        client_tab: {
            final JButton connectButton = new JButton("Connect");

            // note(nschultz): 'must be effect final' *sigh*
            final AtomicBoolean ipv4Valid = new AtomicBoolean(true);
            final AtomicBoolean portValid = new AtomicBoolean(true);

            final JLabel ipv4Label = new JLabel("IPv4: ");
            final JTextField ipv4Field = new JTextField("127.0.0.1");
            ipv4Field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            ipv4Field.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void changedUpdate(final DocumentEvent evt) {
                    if (checkIpv4Input(ipv4Field.getText())) {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        ipv4Valid.set(true);;
                    } else {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        ipv4Valid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
                @Override public void removeUpdate(final DocumentEvent evt)  {
                    if (checkIpv4Input(ipv4Field.getText())) {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        ipv4Valid.set(true);;
                    } else {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        ipv4Valid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
                @Override public void insertUpdate(final DocumentEvent evt)  {
                    if (checkIpv4Input(ipv4Field.getText())) {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        ipv4Valid.set(true);;
                    } else {
                        ipv4Field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        ipv4Valid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
            });
            final JLabel portLabel = new JLabel("Port: ");
            final JTextField portField = new JTextField("1234");
            portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            portField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void changedUpdate(final DocumentEvent evt) {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        portValid.set(true);;
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        portValid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
                @Override public void removeUpdate(final DocumentEvent evt)  {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        portValid.set(true);;
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        portValid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
                @Override public void insertUpdate(final DocumentEvent evt)  {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        portValid.set(true);;
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        portValid.set(false);;
                    }

                    if (ipv4Valid.get() && portValid.get()) {
                        connectButton.setEnabled(true);
                    } else {
                        connectButton.setEnabled(false);
                    }
                }
            });
            final JLabel statusLabel = new JLabel("Status: offline");
            statusLabel.setBackground(Color.RED);

            final JTextPane outputArea = new JTextPane();
            outputArea.setEditable(false);
            outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            final ClientConHandler clientConHandler = new ClientConHandler(new ClientConHandler.Callback() {
                @Override public void onConnectionEstablished() {
                    connectButton.setText("Disconnect");
                    statusLabel.setText("Status: online");
                    ipv4Field.setEditable(false);
                    portField.setEditable(false);
                    outputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(outputArea, "**CONNECTION ESTABLISHED**", Color.BLACK, true);
                }
                @Override public void onIncomingData(final String data) {
                    appendToPane(outputArea, data, Color.BLUE, false);
                }
                @Override public void onConnectionFailure(final String reason) {
                    appendToPane(outputArea, String.format("**ERROR: %s**", reason), Color.BLACK, true);
                }
                @Override public void onConnectionTimeout() {
                    appendToPane(outputArea, "**CONNECTION ESTABLISHMENT TIMEOUT**", Color.BLACK, true);
                }
                @Override public void onConnectionReleased() {
                    connectButton.setText("Connect");
                    statusLabel.setText("Status: offline");
                    ipv4Field.setEditable(true);
                    portField.setEditable(true);
                    outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(outputArea, "**CONNECTION RELEASED**", Color.BLACK, true);
                }
            });

            connectButton.addActionListener(e -> {
                if (clientConHandler.isConnected()) {
                    clientConHandler.teardown();
                    return;
                }

                clientConHandler.start(ipv4Field.getText(), Integer.parseInt(portField.getText()));
            });

            final JPanel headerPanel = new JPanel(new GridLayout(3, 3, 4, 4));
            headerPanel.add(ipv4Label);
            headerPanel.add(ipv4Field);
            headerPanel.add(portLabel);
            headerPanel.add(portField);
            headerPanel.add(statusLabel);
            headerPanel.add(connectButton);
            clientPanel.add(headerPanel, BorderLayout.NORTH);

            final JTextField inputField = new JTextField();
            inputField.addFocusListener(new FocusListener() {
                @Override public void focusGained(final FocusEvent evt) {
                    // todo(nschultz): lerp border
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                }
                @Override public void focusLost(final FocusEvent evt) {
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                }
            });
            inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            inputField.addActionListener(e -> {
                if (clientConHandler.isConnected()) {
                    final String input = inputField.getText();
                    appendToPane(outputArea, input, Color.BLACK, false);
                    clientConHandler.send(input.getBytes());
                }
                inputField.setText("");
            });
            clientPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
            clientPanel.add(inputField, BorderLayout.SOUTH);
        }

        server_tab: {
            final JButton openButton = new JButton("Open");

            final JLabel portLabel = new JLabel("Port: ");
            final JTextField portField = new JTextField("1234");
            portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            portField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void changedUpdate(final DocumentEvent evt) {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        openButton.setEnabled(true);
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        openButton.setEnabled(false);
                    }
                }
                @Override public void removeUpdate(final DocumentEvent evt)  {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        openButton.setEnabled(true);
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        openButton.setEnabled(false);
                    }
                }
                @Override public void insertUpdate(final DocumentEvent evt)  {
                    if (checkPortInput(portField.getText())) {
                        portField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        openButton.setEnabled(true);
                    } else {
                        portField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        openButton.setEnabled(false);
                    }
                }
            });
            final JLabel statusLabel = new JLabel("Status: offline");
            statusLabel.setBackground(Color.RED);

            final JTextPane outputArea = new JTextPane();
            outputArea.setEditable(false);
            outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            final ServerConHandler serverConHandler = new ServerConHandler(new ServerConHandler.Callback() {
                @Override public void onOpen() {
                    openButton.setText("Close");
                    statusLabel.setText("Status: online");
                    portField.setEditable(false);
                    outputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(outputArea, "**SERVER OPEN**", Color.BLACK, true);
                }
                @Override public void onNewClient(final Socket client) {
                    appendToPane(outputArea, String.format("**NEW CLIENT: %s**", client.getInetAddress()), Color.BLACK, true);
                }
                @Override public void onClientLost(final Socket client) {
                    appendToPane(outputArea, String.format("**LOST CLIENT: %s**", client.getInetAddress()), Color.BLACK, true);
                }
                @Override public void onIncomingData(final String data) {
                    appendToPane(outputArea, data, Color.BLUE, false);
                }
                @Override public void onConnectionFailure(final String reason) {
                    appendToPane(outputArea, String.format("**ERROR: %s**", reason), Color.BLACK, true);
                }
                @Override public void onClose() {
                    openButton.setText("Open");
                    statusLabel.setText("Status: offline");
                    portField.setEditable(true);
                    outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(outputArea, "**SERVER CLOSED**", Color.BLACK, true);
                }
            });

            openButton.addActionListener(e -> {
                if (serverConHandler.isOpen()) {
                    serverConHandler.teardown();
                    return;
                }

                serverConHandler.start(Integer.parseInt(portField.getText()));
            });

            final JPanel headerPanel = new JPanel(new GridLayout(2, 2, 4, 4));
            headerPanel.add(portLabel);
            headerPanel.add(portField);
            headerPanel.add(statusLabel);
            headerPanel.add(openButton);
            serverPanel.add(headerPanel, BorderLayout.NORTH);

            final JTextField inputField = new JTextField();
            inputField.addFocusListener(new FocusListener() {
                @Override public void focusGained(final FocusEvent evt) {
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                }
                @Override public void focusLost(final FocusEvent evt) {
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                }
            });
            inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            inputField.addActionListener(e -> {
                if (serverConHandler.isOpen() && serverConHandler.hasClient()) {
                    final String input = inputField.getText();
                    appendToPane(outputArea, input, Color.BLACK, false);
                    serverConHandler.send(input.getBytes());
                }
                inputField.setText("");
            });
            serverPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
            serverPanel.add(inputField, BorderLayout.SOUTH);
        }

        // note(nschultz): finally add all the content to our frame
        this.frame.setContentPane(root);
    }

    public void show() {
        assert !EventQueue.isDispatchThread();

        EventQueue.invokeLater(() -> {
            this.frame.setVisible(true);
        });
    }

    private static boolean checkIpv4Input(final String text) {
        assert text != null;

        if (text.isBlank()) return false;

        final String[] octets = text.split("\\.");
        if (octets == null)     return false;
        if (octets.length != 4) return false;

        for (final String octetString : octets) {
            try {
                final int octet = Integer.parseInt(octetString);
                if (octet < 0 || octet > 255) {
                    return false;
                }
            } catch (final NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkPortInput(final String text) {
        assert text != null;

        if (text.isBlank()) return false;

        try  {
            final int port = Integer.parseInt(text);
            if (port <= 0 || port > 65535) {
                return false;
            }
        } catch (final NumberFormatException ex) {
            return false;
        }
        return true;
    }

    private static void appendToPane(final JTextPane pane, final String string, final Color color, final boolean bold) {
        assert string != null;
        assert pane   != null;
        assert color  != null;

        if (EventQueue.isDispatchThread()) {
            try {
                final SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, color);
                StyleConstants.setBold(attr, bold);

                final StyledDocument doc = pane.getStyledDocument();
                doc.insertString(doc.getLength(), string + "\n", attr);
            } catch (final Exception ex) {
                Main.logger.log(Level.SEVERE, ex.toString());
            }
        } else {
            EventQueue.invokeLater(() -> {
                try {
                    final SimpleAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setForeground(attr, color);
                    StyleConstants.setBold(attr, bold);

                    final StyledDocument doc = pane.getStyledDocument();
                    doc.insertString(doc.getLength(), string + "\n", attr);
                } catch (final Exception ex) {
                    Main.logger.log(Level.SEVERE, ex.toString());
                }
            });
        }
    }
}
