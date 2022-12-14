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
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

// todo(nschultz): Watch file for changes and then send its content (setting), and/or watch entire directory for incoming files
// todo(nschultz): Ping button next to ipv4 field (isReachable())
// todo(nschultz): Check port button next to port field:
//                 On Windows : netstat -nao | find /I ":1234" -> If this returns non empty string, port is not free
//                 On Unix    : netstat -nao | grep -i ":1234" -> If this returns non empty string, port is not free
// todo(nschultz): Option to save communication/output to file
// todo(nschultz): Options to display time and date next to the messages
// todo(nschultz): Get your current ip
// todo(nschultz): Get your mac addresse
public final class MainWindow {

    // note(nschultz): This font gets packaged with the jdk
    // todo(nschultz): Event thread??????
    private static final Font defaultFont = new Font("Monospace", Font.PLAIN, 14);

    public JFrame frame;

    private String lastMessage = "";

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
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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

        final ServerConHandler serverConHandler = new ServerConHandler();
        final ClientConHandler clientConHandler = new ClientConHandler();

        final Lambdas.Nullary<Void> cleanup = () -> {
            // note(nschultz): We don't really need to do this cleanup, since the OS
            // will cleanup after the process terminates, I just want to be sure...
            if (serverConHandler.isOpen()) {
                serverConHandler.teardown();
            }

            if (clientConHandler.isConnected()) {
                clientConHandler.teardown();
            }

            Runtime.getRuntime().gc();
            Runtime.getRuntime().runFinalization();

            return (Void) null; // note(nschultz): unreachable
        };

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                cleanup.call();
            }
        });

        frame: {
            this.frame = new JFrame("TCP Client/Server Tester v0.1.0");
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.frame.setSize(800, 600);
            this.frame.setLocationRelativeTo(null);
        }

        // todo(nschultz): Refactor this mess (scope handling)
        final JMenuItem fileMenuClearClientItem = new JMenuItem("Clear client output");
        final JMenuItem fileMenuClearServerItem = new JMenuItem("Clear server output");
        menu_bar: {
            final JMenuBar menubar = new JMenuBar();
            final JMenu fileMenu = new JMenu("File");
            final JMenu helpMenu = new JMenu("Help");

            final JMenuItem fileMenuSettingsItem = new JMenuItem("Settings");
            fileMenuSettingsItem.addActionListener(e -> {
                new SettingsWindow(this).show();
            });
            final JMenuItem fileMenuExitItem = new JMenuItem("Exit");
            fileMenuExitItem.addActionListener(e -> {
                cleanup.call();
                System.exit(0);
            });
            final JMenuItem helpMenuCommonPortsItem = new JMenuItem("Common ports");
            helpMenuCommonPortsItem.addActionListener(e -> {
                final JDialog dialog = new JDialog();
                dialog.setTitle("Common ports");
                final JPanel panel = new JPanel(new BorderLayout());
                final DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Value"}, 0) {
                    @Override public boolean isCellEditable(final int row, final int col) {
                        return false;
                    }
                };
                model.addRow(new Object[]{"Echo", "7"});
                model.addRow(new Object[]{"Discard", "9"});
                model.addRow(new Object[]{"FTP (data port)", "20"});
                model.addRow(new Object[]{"FTP (control port)", "21"});
                model.addRow(new Object[]{"SSH", "22"});
                model.addRow(new Object[]{"Telnet", "23"});
                model.addRow(new Object[]{"SMTP (E-Mail)", "25"});
                model.addRow(new Object[]{"HTTP", "80"});
                model.addRow(new Object[]{"WS", "80"});
                model.addRow(new Object[]{"HTTPS", "443"});
                final JTable table = new JTable(model);
                table.getTableHeader().setReorderingAllowed(false);
                panel.add(new JScrollPane(table), BorderLayout.CENTER);
                dialog.add(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this.frame);
                dialog.setVisible(true);
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
            fileMenu.add(fileMenuClearClientItem);
            fileMenu.add(fileMenuClearServerItem);
            fileMenu.add(fileMenuSettingsItem);
            fileMenu.addSeparator();
            fileMenu.add(fileMenuExitItem);
            helpMenu.add(helpMenuCommonPortsItem);
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

            final JTextPane hexOutputArea = new JTextPane();
            hexOutputArea.setEditable(false);
            hexOutputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            clientConHandler.callback = new ClientConHandler.Callback() {
                @Override public void onConnectionEstablished() {
                    connectButton.setText("Disconnect");
                    statusLabel.setText("Status: online");
                    ipv4Field.setEditable(false);
                    portField.setEditable(false);

                    outputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(outputArea, "**CONNECTION ESTABLISHED**\n", Color.BLACK, true);

                    hexOutputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(hexOutputArea, "**CONNECTION ESTABLISHED**\n", Color.BLACK, true);

                    if (!Settings.msgOnConEst.isEmpty()) {
                        final String msgOnConEstMod = Settings.msgOnConEst.replaceAll("\\\\n", "\n");

                        clientConHandler.send(msgOnConEstMod);
                        appendToPane(outputArea, msgOnConEstMod, Color.BLACK, false);

                        for (final char c : msgOnConEstMod.toCharArray()) {
                            final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                            appendToPane(hexOutputArea, hex, Color.BLACK, false);

                            if (hex.strip().equals("0A")) { // note(nschultz): new line
                                appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                            }
                        }
                    }
                }
                @Override public void onIncomingData(final String data) {
                    appendToPane(outputArea, data, Color.BLUE, false);

                    for (final char c : data.toCharArray()) {
                        final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                        appendToPane(hexOutputArea, hex, Color.BLUE, false);

                        if (hex.strip().equals("0A")) { // note(nschultz): new line
                            appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                        }
                    }

                    // note(nschultz): Format has already been validated
                    if (!Settings.conditionalAnswer.isEmpty()) {
                        String ifMessage   = Settings.conditionalAnswer.split("@")[0];
                        String thenMessage = Settings.conditionalAnswer.split("@")[1];

                        ifMessage   = ifMessage.replaceAll("\\\\n", "\n");
                        thenMessage = thenMessage.replaceAll("\\\\n", "\n");

                        if (data.equals(ifMessage)) {
                            clientConHandler.send(thenMessage);

                            appendToPane(outputArea, thenMessage, Color.BLACK, false);

                            for (final char c : thenMessage.toCharArray()) {
                                final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                                appendToPane(hexOutputArea, hex, Color.BLACK, false);

                                if (hex.strip().equals("0A")) { // note(nschultz): new line
                                    appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                                }
                            }
                        }
                    }
                }
                @Override public void onConnectionFailure(final String reason) {
                    appendToPane(outputArea, String.format("**ERROR: %s**\n", reason), Color.BLACK, true);
                    appendToPane(hexOutputArea, String.format("**ERROR: %s**\n", reason), Color.BLACK, true);
                }
                @Override public void onConnectionTimeout() {
                    appendToPane(outputArea, "**CONNECTION ESTABLISHMENT TIMEOUT**\n", Color.BLACK, true);
                    appendToPane(hexOutputArea, "**CONNECTION ESTABLISHMENT TIMEOUT**\n", Color.BLACK, true);
                }
                @Override public void onConnectionReleased() {
                    connectButton.setText("Connect");
                    statusLabel.setText("Status: offline");
                    ipv4Field.setEditable(true);
                    portField.setEditable(true);
                    outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(outputArea, "**CONNECTION RELEASED**\n", Color.BLACK, true);
                    hexOutputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(hexOutputArea, "**CONNECTION RELEASED**\n", Color.BLACK, true);
                }
            };

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
            inputField.setDropTarget(new DropTarget() {
                @Override public synchronized void drop(final DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        final java.util.List<File> droppedFiles = (java.util.List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        final File firstFile = droppedFiles.get(0);
                        inputField.setText(new String(java.nio.file.Files.readAllBytes(firstFile.toPath()), java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        Main.logger.log(Level.SEVERE, "Failed to drag and drop file");
                    }
                }
            });
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
            inputField.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_UP) {
                        inputField.setText(MainWindow.this.lastMessage);
                    } else if (evt.getKeyCode() == KeyEvent.VK_F5) {
                        // todo(nschultz): This might be a bit hacky
                        // todo(nschultz): replace \n with actual new line character!
                        inputField.setText(Settings.macro);
                        inputField.getActionListeners()[0].actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    }
                }
            });
            inputField.addActionListener(e -> {
                if (clientConHandler.isConnected()) {
                    String input = inputField.getText();
                    this.lastMessage = input;
                    if (Settings.wrapInStxEtx) {
                        input = (char) 0x02 + input + (char) 0x03;
                    }
                    if (Settings.insertNewLine) {
                        input = input + (char) 0xA;
                    }

                    appendToPane(outputArea, input, Color.BLACK, false);

                    for (final char c : input.toCharArray()) {
                        final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                        appendToPane(hexOutputArea, hex, Color.BLACK, false);
                        if (hex.strip().equals("0A")) { // note(nschultz): new line
                            appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                        }
                    }

                    clientConHandler.send(input);
                }
                inputField.setText("");
            });

            final JTabbedPane viewTab = new JTabbedPane(JTabbedPane.BOTTOM);
            viewTab.addTab("String", new JScrollPane(outputArea));
            viewTab.addTab("Hex", new JScrollPane(hexOutputArea));

            clientPanel.add(viewTab, BorderLayout.CENTER);

            final JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
            inputPanel.add(inputField, BorderLayout.CENTER);
            // todo(nschultz): copy paster (make custom button)
            final JButton controlCharsButton = new JButton("ASCII");
            controlCharsButton.addActionListener(e -> {
                final JDialog dialog = new JDialog();
                dialog.setTitle("ASCII Table");
                final JPanel panel = new JPanel(new BorderLayout());
                final DefaultTableModel model = new DefaultTableModel(new String[] {"Chr", "Dec", "Hex"}, 0) {
                    @Override public boolean isCellEditable(final int row, final int col) {
                        return false;
                    }
                };
                for (int i = 0; i < 128; ++i) {
                    switch (i) {
                        case 0   -> model.addRow(new String[] {"<NUL>", String.valueOf(i), String.format("%02X", i)});
                        case 1   -> model.addRow(new String[] {"<SOH>", String.valueOf(i), String.format("%02X", i)});
                        case 2   -> model.addRow(new String[] {"<STX>", String.valueOf(i), String.format("%02X", i)});
                        case 3   -> model.addRow(new String[] {"<ETX>", String.valueOf(i), String.format("%02X", i)});
                        case 4   -> model.addRow(new String[] {"<EOT>", String.valueOf(i), String.format("%02X", i)});
                        case 5   -> model.addRow(new String[] {"<ENQ>", String.valueOf(i), String.format("%02X", i)});
                        case 6   -> model.addRow(new String[] {"<ACK>", String.valueOf(i), String.format("%02X", i)});
                        case 7   -> model.addRow(new String[] {"<BEL>", String.valueOf(i), String.format("%02X", i)});
                        case 8   -> model.addRow(new String[] {"<BS>",  String.valueOf(i), String.format("%02X", i)});
                        case 9   -> model.addRow(new String[] {"<TAB>", String.valueOf(i), String.format("%02X", i)});
                        case 10  -> model.addRow(new String[] {"<LF>",  String.valueOf(i), String.format("%02X", i)});
                        case 11  -> model.addRow(new String[] {"<VT>",  String.valueOf(i), String.format("%02X", i)});
                        case 12  -> model.addRow(new String[] {"<FF>",  String.valueOf(i), String.format("%02X", i)});
                        case 13  -> model.addRow(new String[] {"<CR>",  String.valueOf(i), String.format("%02X", i)});
                        case 14  -> model.addRow(new String[] {"<SO>",  String.valueOf(i), String.format("%02X", i)});
                        case 15  -> model.addRow(new String[] {"<SI>",  String.valueOf(i), String.format("%02X", i)});
                        case 16  -> model.addRow(new String[] {"<DLE>", String.valueOf(i), String.format("%02X", i)});
                        case 17  -> model.addRow(new String[] {"<DC1>", String.valueOf(i), String.format("%02X", i)});
                        case 18  -> model.addRow(new String[] {"<DC2>", String.valueOf(i), String.format("%02X", i)});
                        case 19  -> model.addRow(new String[] {"<DC3>", String.valueOf(i), String.format("%02X", i)});
                        case 20  -> model.addRow(new String[] {"<DC4>", String.valueOf(i), String.format("%02X", i)});
                        case 21  -> model.addRow(new String[] {"<NAK>", String.valueOf(i), String.format("%02X", i)});
                        case 22  -> model.addRow(new String[] {"<SYN>", String.valueOf(i), String.format("%02X", i)});
                        case 23  -> model.addRow(new String[] {"<ETB>", String.valueOf(i), String.format("%02X", i)});
                        case 24  -> model.addRow(new String[] {"<CAN>", String.valueOf(i), String.format("%02X", i)});
                        case 25  -> model.addRow(new String[] {"<EM>",  String.valueOf(i), String.format("%02X", i)});
                        case 26  -> model.addRow(new String[] {"<SUB>", String.valueOf(i), String.format("%02X", i)});
                        case 27  -> model.addRow(new String[] {"<Esc>", String.valueOf(i), String.format("%02X", i)});
                        case 28  -> model.addRow(new String[] {"<FS>",  String.valueOf(i), String.format("%02X", i)});
                        case 29  -> model.addRow(new String[] {"<GS>",  String.valueOf(i), String.format("%02X", i)});
                        case 30  -> model.addRow(new String[] {"<RS>",  String.valueOf(i), String.format("%02X", i)});
                        case 31  -> model.addRow(new String[] {"<US>",  String.valueOf(i), String.format("%02X", i)});
                        case 32  -> model.addRow(new String[] {"<SP>",  String.valueOf(i), String.format("%02X", i)});
                        case 127 -> model.addRow(new String[] {"<DEL>",  String.valueOf(i), String.format("%02X", i)});
                        default  -> model.addRow(new String[] {String.valueOf((char) i), String.valueOf(i), String.format("%02X", i)});
                    }
                }
                final JTable table = new JTable(model);
                table.getTableHeader().setReorderingAllowed(false);
                table.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(final MouseEvent evt) {
                        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
                            final int decVal = Integer.parseInt(((String) table.getValueAt(table.getSelectedRow(), 1)));
                            final char c = (char) decVal;
                            try {
                                inputField.getDocument().insertString(inputField.getCaretPosition(), String.valueOf(c), null);
                            } catch (final Exception ex) {
                                assert false;
                            }
                        }
                    }
                });
                panel.add(new JScrollPane(table), BorderLayout.CENTER);
                dialog.add(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this.frame);
                dialog.setVisible(true);
            });
            inputPanel.add(controlCharsButton, BorderLayout.EAST);
            clientPanel.add(inputPanel, BorderLayout.SOUTH);

            // todo(nschultz): We have to refactor this mess
            fileMenuClearClientItem.addActionListener(e -> {
                outputArea.setText("");
                hexOutputArea.setText("");
            });
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

            final JTextPane hexOutputArea = new JTextPane();
            hexOutputArea.setEditable(false);
            hexOutputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            serverConHandler.callback = new ServerConHandler.Callback() {
                @Override public void onOpen() {
                    openButton.setText("Close");
                    statusLabel.setText("Status: online");
                    portField.setEditable(false);
                    outputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(outputArea, "**SERVER OPEN**\n", Color.BLACK, true);
                    hexOutputArea.setBorder(BorderFactory.createLineBorder(new Color(20, 200, 20), 1));
                    appendToPane(hexOutputArea, "**SERVER OPEN**\n", Color.BLACK, true);
                }
                @Override public void onNewClient(final Socket client) {
                    appendToPane(outputArea, String.format("**NEW CLIENT: %s**\n", client.getInetAddress()), Color.BLACK, true);
                    appendToPane(hexOutputArea, String.format("**NEW CLIENT: %s**\n", client.getInetAddress()), Color.BLACK, true);

                    if (!Settings.msgOnConEst.isEmpty()) {
                        final String msgOnConEstMod = Settings.msgOnConEst.replaceAll("\\\\n", "\n");

                        serverConHandler.send(msgOnConEstMod);
                        appendToPane(outputArea, msgOnConEstMod, Color.BLACK, false);

                        for (final char c : msgOnConEstMod.toCharArray()) {
                            final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                            appendToPane(hexOutputArea, hex, Color.BLACK, false);

                            if (hex.strip().equals("0A")) { // note(nschultz): new line
                                appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                            }
                        }
                    }
                }
                @Override public void onClientLost(final Socket client) {
                    appendToPane(outputArea, String.format("**LOST CLIENT: %s**\n", client.getInetAddress()), Color.BLACK, true);
                    appendToPane(hexOutputArea, String.format("**LOST CLIENT: %s**\n", client.getInetAddress()), Color.BLACK, true);
                }
                @Override public void onIncomingData(final String data) {
                    appendToPane(outputArea, data, Color.BLUE, false);

                    for (final char c : data.toCharArray()) {
                        final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                        appendToPane(hexOutputArea, hex, Color.BLUE, false);
                        if (hex.strip().equals("0A")) { // note(nschultz): new line
                            appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                        }
                    }

                    // note(nschultz): Format has already been validated
                    // todo(nschultz): Perhaps we should make two separate settings for client and server
                    if (!Settings.conditionalAnswer.isEmpty()) {
                        String ifMessage   = Settings.conditionalAnswer.split("@")[0];
                        String thenMessage = Settings.conditionalAnswer.split("@")[1];

                        ifMessage   = ifMessage.replaceAll("\\\\n", "\n");
                        thenMessage = thenMessage.replaceAll("\\\\n", "\n");

                        if (data.equals(ifMessage)) {
                            serverConHandler.send(thenMessage);

                            appendToPane(outputArea, thenMessage, Color.BLACK, false);

                            for (final char c : thenMessage.toCharArray()) {
                                final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                                appendToPane(hexOutputArea, hex, Color.BLACK, false);

                                if (hex.strip().equals("0A")) { // note(nschultz): new line
                                    appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                                }
                            }
                        }
                    }
                }
                @Override public void onConnectionFailure(final String reason) {
                    appendToPane(outputArea, String.format("**ERROR: %s**\n", reason), Color.BLACK, true);
                    appendToPane(hexOutputArea, String.format("**ERROR: %s**\n", reason), Color.BLACK, true);
                }
                @Override public void onClose() {
                    openButton.setText("Open");
                    statusLabel.setText("Status: offline");
                    portField.setEditable(true);
                    outputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(outputArea, "**SERVER CLOSED**\n", Color.BLACK, true);
                    hexOutputArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                    appendToPane(hexOutputArea, "**SERVER CLOSED**\n", Color.BLACK, true);
                }
            };

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
            inputField.setDropTarget(new DropTarget() {
                @Override public synchronized void drop(final DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        final java.util.List<File> droppedFiles = (java.util.List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        final File firstFile = droppedFiles.get(0);
                        inputField.setText(new String(java.nio.file.Files.readAllBytes(firstFile.toPath()), java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        Main.logger.log(Level.SEVERE, "Failed to drag and drop file");
                    }
                }
            });
            inputField.addFocusListener(new FocusListener() {
                @Override public void focusGained(final FocusEvent evt) {
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
                }
                @Override public void focusLost(final FocusEvent evt) {
                    inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                }
            });
            inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            inputField.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(final KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_UP) {
                        inputField.setText(MainWindow.this.lastMessage);
                    } else if (evt.getKeyCode() == KeyEvent.VK_F5) {
                        // todo(nschultz): This might be a bit hacky
                        inputField.setText(Settings.macro);
                        inputField.getActionListeners()[0].actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    }
                }
            });
            inputField.addActionListener(e -> {
                if (serverConHandler.isOpen() && serverConHandler.hasClient()) {
                    String input = inputField.getText();
                    this.lastMessage = input;
                    if (Settings.wrapInStxEtx) {
                        input = (char) 0x02 + input + (char) 0x03;
                    }
                    if (Settings.insertNewLine) {
                        input = input + (char) 0xA;
                    }

                    appendToPane(outputArea, input, Color.BLACK, false);

                    for (final char c : input.toCharArray()) {
                        final String hex = String.format("%02X ", new BigInteger(1, new byte[]{(byte) c}));
                        appendToPane(hexOutputArea, hex, Color.BLACK, false);
                        if (hex.strip().equals("0A")) { // note(nschultz): new line
                            appendToPane(hexOutputArea, "\n", Color.BLACK, false);
                        }
                    }
                    serverConHandler.send(input);
                }
                inputField.setText("");
            });

            final JTabbedPane viewTab = new JTabbedPane(JTabbedPane.BOTTOM);
            viewTab.addTab("String", new JScrollPane(outputArea));
            viewTab.addTab("Hex", new JScrollPane(hexOutputArea));

            serverPanel.add(viewTab, BorderLayout.CENTER);
            final JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
            inputPanel.add(inputField, BorderLayout.CENTER);
            final JButton controlCharsButton = new JButton("ASCII");
            controlCharsButton.addActionListener(e -> {
                final JDialog dialog = new JDialog();
                dialog.setTitle("ASCII Table");
                final JPanel panel = new JPanel(new BorderLayout());
                final DefaultTableModel model = new DefaultTableModel(new String[] {"Chr", "Dec", "Hex"}, 0) {
                    @Override public boolean isCellEditable(final int row, final int col) {
                        return false;
                    }
                };
                for (int i = 0; i < 128; ++i) {
                    switch (i) {
                        case 0   -> model.addRow(new String[] {"<NUL>", String.valueOf(i), String.format("%02X", i)});
                        case 1   -> model.addRow(new String[] {"<SOH>", String.valueOf(i), String.format("%02X", i)});
                        case 2   -> model.addRow(new String[] {"<STX>", String.valueOf(i), String.format("%02X", i)});
                        case 3   -> model.addRow(new String[] {"<ETX>", String.valueOf(i), String.format("%02X", i)});
                        case 4   -> model.addRow(new String[] {"<EOT>", String.valueOf(i), String.format("%02X", i)});
                        case 5   -> model.addRow(new String[] {"<ENQ>", String.valueOf(i), String.format("%02X", i)});
                        case 6   -> model.addRow(new String[] {"<ACK>", String.valueOf(i), String.format("%02X", i)});
                        case 7   -> model.addRow(new String[] {"<BEL>", String.valueOf(i), String.format("%02X", i)});
                        case 8   -> model.addRow(new String[] {"<BS>",  String.valueOf(i), String.format("%02X", i)});
                        case 9   -> model.addRow(new String[] {"<TAB>", String.valueOf(i), String.format("%02X", i)});
                        case 10  -> model.addRow(new String[] {"<LF>",  String.valueOf(i), String.format("%02X", i)});
                        case 11  -> model.addRow(new String[] {"<VT>",  String.valueOf(i), String.format("%02X", i)});
                        case 12  -> model.addRow(new String[] {"<FF>",  String.valueOf(i), String.format("%02X", i)});
                        case 13  -> model.addRow(new String[] {"<CR>",  String.valueOf(i), String.format("%02X", i)});
                        case 14  -> model.addRow(new String[] {"<SO>",  String.valueOf(i), String.format("%02X", i)});
                        case 15  -> model.addRow(new String[] {"<SI>",  String.valueOf(i), String.format("%02X", i)});
                        case 16  -> model.addRow(new String[] {"<DLE>", String.valueOf(i), String.format("%02X", i)});
                        case 17  -> model.addRow(new String[] {"<DC1>", String.valueOf(i), String.format("%02X", i)});
                        case 18  -> model.addRow(new String[] {"<DC2>", String.valueOf(i), String.format("%02X", i)});
                        case 19  -> model.addRow(new String[] {"<DC3>", String.valueOf(i), String.format("%02X", i)});
                        case 20  -> model.addRow(new String[] {"<DC4>", String.valueOf(i), String.format("%02X", i)});
                        case 21  -> model.addRow(new String[] {"<NAK>", String.valueOf(i), String.format("%02X", i)});
                        case 22  -> model.addRow(new String[] {"<SYN>", String.valueOf(i), String.format("%02X", i)});
                        case 23  -> model.addRow(new String[] {"<ETB>", String.valueOf(i), String.format("%02X", i)});
                        case 24  -> model.addRow(new String[] {"<CAN>", String.valueOf(i), String.format("%02X", i)});
                        case 25  -> model.addRow(new String[] {"<EM>",  String.valueOf(i), String.format("%02X", i)});
                        case 26  -> model.addRow(new String[] {"<SUB>", String.valueOf(i), String.format("%02X", i)});
                        case 27  -> model.addRow(new String[] {"<Esc>", String.valueOf(i), String.format("%02X", i)});
                        case 28  -> model.addRow(new String[] {"<FS>",  String.valueOf(i), String.format("%02X", i)});
                        case 29  -> model.addRow(new String[] {"<GS>",  String.valueOf(i), String.format("%02X", i)});
                        case 30  -> model.addRow(new String[] {"<RS>",  String.valueOf(i), String.format("%02X", i)});
                        case 31  -> model.addRow(new String[] {"<US>",  String.valueOf(i), String.format("%02X", i)});
                        case 32  -> model.addRow(new String[] {"<SP>",  String.valueOf(i), String.format("%02X", i)});
                        case 127 -> model.addRow(new String[] {"<DEL>",  String.valueOf(i), String.format("%02X", i)});
                        default  -> model.addRow(new String[] {String.valueOf((char) i), String.valueOf(i), String.format("%02X", i)});
                    }
                }
                final JTable table = new JTable(model);
                table.getTableHeader().setReorderingAllowed(false);
                table.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(final MouseEvent evt) {
                        if (evt.getClickCount() == 2 && table.getSelectedRow() != -1) {
                            final int decVal = Integer.parseInt(((String) table.getValueAt(table.getSelectedRow(), 1)));
                            final char c = (char) decVal;
                            try {
                                inputField.getDocument().insertString(inputField.getCaretPosition(), String.valueOf(c), null);
                            } catch (final Exception ex) {
                                assert false;
                            }
                        }
                    }
                });
                panel.add(new JScrollPane(table), BorderLayout.CENTER);
                dialog.add(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this.frame);
                dialog.setVisible(true);
            });
            inputPanel.add(controlCharsButton, BorderLayout.EAST);
            serverPanel.add(inputPanel, BorderLayout.SOUTH);

            // todo(nschultz): We have to refactor this mess
            fileMenuClearServerItem.addActionListener(e -> {
                outputArea.setText("");
                hexOutputArea.setText("");
            });
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
                doc.insertString(doc.getLength(), string, attr);
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
                    doc.insertString(doc.getLength(), string, attr);
                } catch (final Exception ex) {
                    Main.logger.log(Level.SEVERE, ex.toString());
                }
            });
        }

    }
}
