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
import javax.swing.*;
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
            model.addRow(new Object[]{"Wrap STX-ETX",    Settings.wrapInStxEtx});
            model.addRow(new Object[]{"Insert new line", Settings.insertNewLine});
        }

        final JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        final JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            final boolean stxetx  = (boolean) model.getValueAt(0, 1);
            final boolean newline = (boolean) model.getValueAt(1, 1);

            Settings.wrapInStxEtx  = stxetx;
            Settings.insertNewLine = newline;

            this.frame.dispose();
            /*for (int x = 0; x < table.getColumnCount(); ++x) {
                for (int y = 0; y < table.getRowCount(); ++y) {
                    System.err.println(model.getValueAt(x, y));
                }
            }*/
        });
        // saveButton.setPreferredSize(new Dimension(100, 20));
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

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return switch (columnIndex) {
                case 0  -> String.class;
                case 1  -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return column == 1;
        }
    }
}

