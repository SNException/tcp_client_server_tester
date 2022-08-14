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
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public final class SettingsWindow {

    private final MainWindow owner;
    private JFrame frame;

    public SettingsWindow(final MainWindow owner) {
        assert EventQueue.isDispatchThread();

        this.owner = owner;

        init();
    }

    private void init() {
        this.frame = new JFrame("TCP Client/Server Tester v0.1.0 - Settings");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // this.frame.setSize(700, 500);

        final JPanel root = new JPanel(new BorderLayout());
        final SettingsTableModel model = new SettingsTableModel();

        insert_settings: {
            model.addRow(new Object[]{"Wrap STX-ETX",        String.valueOf(Settings.wrapInStxEtx)});
            model.addRow(new Object[]{"Insert new line",     String.valueOf(Settings.insertNewLine)});
            model.addRow(new Object[]{"Reading buffer size", String.valueOf(Settings.bufSize)});
        }

        final JTable table = new JTable(model);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.getTableHeader().setReorderingAllowed(false);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        final JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            // note(nschultz): Already validated
            final boolean stxetx  = Boolean.parseBoolean((String) model.getValueAt(0, 1));
            final boolean newline = Boolean.parseBoolean((String) model.getValueAt(1, 1));
            final int     bufSize = Integer.parseInt((String) model.getValueAt(2, 1));

            Settings.wrapInStxEtx  = stxetx;
            Settings.insertNewLine = newline;
            Settings.bufSize       = bufSize;

            this.frame.dispose();
        });

        root.add(saveButton, BorderLayout.SOUTH);

        this.frame.setContentPane(root);
        this.frame.pack();
        this.frame.setLocationRelativeTo(this.owner.frame);
    }

    public void show() {
        this.frame.setVisible(true);
    }

    @SuppressWarnings("serial")
    private final class SettingsTableModel extends DefaultTableModel {

        public SettingsTableModel() {
            super(new String[] {"Name", "Value"}, 0);
        }

        // note(nschultz): I would love to do that. However, I don't think this works
        // if I want to have different types of components in the same column.
        // So for example I would like some rows to have a checkbox, and others
        // a combobox. But that is not possible with that solution. Since I can only generate one type of component
        // for all rows in a column.
        // Therefore I will have to use textfields for all of them, which is not ideal, because we are now
        // prone to bad user input. *Sigh*
        /*@Override
        public Class<?> getColumnClass(final int columnIndex) {
            return switch (columnIndex) {
                case 0  -> String.class;
                case 1  -> Boolean.class;
                default -> String.class;
            };
        }*/

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return column == 1;
        }

        @Override
        public void setValueAt(final Object value, final int row, final int column) {
            super.setValueAt(value, row, column);

            // note(nschultz): validation
            final String stxetx = ((String) super.getValueAt(0, 1)).strip();
            if (stxetx.equalsIgnoreCase("true") || stxetx.equalsIgnoreCase("false")) {
                // note(nschultz): we good
            } else {
                super.setValueAt("false", 0, 1);
            }

            final String newline = ((String) super.getValueAt(1, 1)).strip();
            if (newline.equalsIgnoreCase("true") || newline.equalsIgnoreCase("false")) {
                // note(nschultz): we good
            } else {
                super.setValueAt("false", 1, 1);
            }

            final String bufSize = ((String) super.getValueAt(2, 1)).strip();
            try {
                final int bufSizeInt = Integer.parseInt(bufSize);
                if (bufSizeInt <= 0) {
                    super.setValueAt("1", 2, 1);
                } else {
                    // note(nschultz): we good
                }
            } catch (final NumberFormatException ex) {
                // note(nschultz): Not a number
                super.setValueAt("4096", 2, 1);
            }
        }
    }
}

