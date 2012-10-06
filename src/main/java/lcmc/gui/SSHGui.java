/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package lcmc.gui;

import lcmc.data.Host;
import lcmc.utilities.Tools;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JApplet;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import java.awt.Container;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of dialogs that are needed for establishing of a ssh
 * connection.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class SSHGui {
    /** Root pane on which the dialogs are comming to. */
    private final Container rootPane;
    /** Host data object. */
    private final Host host;
    /** Progress bar. */
    private final ProgressBar progressBar;
    /** Default length of fields. */
    private static final int DEFAULT_FIELD_LENGTH = 20;

    /** Prepares a new <code>SSHGui</code> object. */
    public SSHGui(final Container rootPane,
                  final Host host,
                  final ProgressBar progressBar) {
        this.rootPane = rootPane;
        this.host = host;
        this.progressBar = progressBar;
    }

    /** Displays Confirm Dialog whith Yes, No, Cancel options. */
    public int getConfirmDialogChoice(final String message) {
        Tools.debug(this, "get confirm dialog");
        return JOptionPane.showConfirmDialog(rootPane, message);
    }

    /** Checks if choice is yes option. */
    public boolean isConfirmDialogYes(final int choice) {
        return choice == JOptionPane.YES_OPTION;
    }

    /** Checks if choice is cancel option. */
    public boolean isConfirmDialogCancel(final int choice) {
        return choice == JOptionPane.CANCEL_OPTION;
    }

    /** Creates dialog with some text or password field to enter by user. */
    public String enterSomethingDialog(final String title,
                                       final String[] content,
                                       final String underText,
                                       final String defaultValue,
                                       final boolean isPassword) {
        EnterSomethingDialog esd;
        if (rootPane instanceof JDialog) {
            esd = new EnterSomethingDialog((JDialog) rootPane, title,
                    content, underText, defaultValue, isPassword);
        } else if (rootPane instanceof JApplet) {
            esd = new EnterSomethingDialog((JApplet) rootPane, title,
                    content, underText, defaultValue, isPassword);
        } else {
            esd = new EnterSomethingDialog((Frame) rootPane, title,
                    content, underText, defaultValue, isPassword);
        }

        esd.setVisible(true);

        return esd.answer;
    }

    /**
     * This dialog displays a number of text lines and a text field.
     * The text field can either be plain text or a password field.
     */
    private class EnterSomethingDialog extends JDialog {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;

        /** Answer field. */
        private JTextField answerField;
        /** Password field. */
        private JPasswordField passwordField;
        /** Whether there is password field. */
        private boolean isPassword;
        /** User answer. */
        private String answer;

        /** Prepares a new <code>EnterSomethingDialog</code> object. */
        EnterSomethingDialog(final Container parent,
                             final String title,
                             final String content,
                             final String underText,
                             final boolean isPasswordA) {
            this((JDialog) parent,
                 title,
                 new String[] {content},
                 underText,
                 null,
                 isPasswordA);
        }

        /** Prepares a new <code>EnterSomethingDialog</code> object. */
        EnterSomethingDialog(final JDialog parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super(parent, title, true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        /** Prepares a new <code>EnterSomethingDialog</code> object. */
        EnterSomethingDialog(final Frame parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super(parent, title, true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        /** Prepares a new <code>EnterSomethingDialog</code> object. */
        EnterSomethingDialog(final JApplet parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super((Frame) SwingUtilities.getAncestorOfClass(Frame.class,
                                                            parent),
                  title,
                  true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        /** Init. */
        private void init(final String[] content,
                          final String underText,
                          final String defaultValue,
                          final boolean isPassword) {
            this.isPassword = isPassword;
            final List<String> strippedContent = new ArrayList<String>();
            for (final String s : content) {
                if (s != null && !s.equals("")) {
                    /* strip some html */
                    strippedContent.add(s.replaceAll("\\<.*?\\>", "")
                                         .replaceAll("&nbsp;", " "));
                }
            }
            host.getTerminalPanel().addCommandOutput(
                  strippedContent.toArray(new String[strippedContent.size()]));

            if (progressBar != null) {
                progressBar.hold();
            }

            final JPanel pan = new JPanel();
            pan.setBorder(new LineBorder(
                   Tools.getDefaultColor("ConfigDialog.Background.Light"), 5));
            pan.setBackground(java.awt.Color.WHITE);
            pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
            if (host != null && host.getName() != null) {
                pan.add(new JLabel("host: " + host.getName()));
            }

            for (final String el : content) {
                if ((el == null) || (el.equals(""))) {
                    continue;
                }
                final JLabel contentLabel = new JLabel(el);
                pan.add(contentLabel);
            }

            answerField = new JTextField(DEFAULT_FIELD_LENGTH);
            passwordField = new JPasswordField(DEFAULT_FIELD_LENGTH);

            if (isPassword) {
                if (defaultValue != null) {
                    passwordField.setText(defaultValue);
                    passwordField.selectAll();
                }
                pan.add(passwordField);
            } else {
                if (defaultValue != null) {
                    answerField.setText(defaultValue);
                    answerField.selectAll();
                }
                pan.add(answerField);
            }

            final KeyAdapter kl = new KeyAdapter() {
                @Override
                public void keyTyped(final KeyEvent e) {
                    if (e.getKeyChar() == '\n') {
                        finish();
                    }
                }
            };

            answerField.addKeyListener(kl);
            passwordField.addKeyListener(kl);

            getContentPane().add(BorderLayout.CENTER, pan);
            if (underText != null) {
                final JLabel l = new JLabel(underText);
                final Font font = l.getFont();
                final String name = font.getFontName();
                final int style = Font.ITALIC;
                final int size = font.getSize();
                l.setFont(new Font(name, style, size - 3));
                l.setForeground(java.awt.Color.GRAY);
                pan.add(l);
            }
            setResizable(false);
            pack();
        }

        /** Finish. */
        private void finish() {
            if (isPassword) {
                answer = new String(passwordField.getPassword());
            } else {
                answer = answerField.getText();
            }

            if (progressBar != null) {
                progressBar.cont();
            }
            host.getTerminalPanel().addCommandOutput("\n");
            dispose();
        }
    }
}
