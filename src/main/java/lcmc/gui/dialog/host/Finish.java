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

package lcmc.gui.dialog.host;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.data.Host;
import lcmc.AddClusterDialog;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;

/**
 * Host finish dialog with buttons to configure next host or configure the
 * clsuter.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class Finish extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Add another host button. */
    private MyButton addAnotherHostButton;
    /** Configure cluster button. */
    private MyButton confClusterButton;
    /** Host icon for add another host button. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                            Tools.getDefault("Dialog.Host.Finish.HostIcon"));
    /** Cluster icon for define cluster button. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                            Tools.getDefault("Dialog.Host.Finish.ClusterIcon"));
    /** Dimensions of the buttons. */
    private static final Dimension BUTTON_DIMENSION = new Dimension(300, 100);
    /** Save checkbox. */
    private final JCheckBox saveCB = new JCheckBox(
                                    Tools.getString("Dialog.Host.Finish.Save"),
                                    true);
    /** Next dialog. */
    private WizardDialog nextDialog = null;
    /**
     * Prepares a new <code>Finish</code> object.
     */
    Finish(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Returns next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialog;
    }

    /** Finishes the dialog, and saves the host. */
    @Override
    protected void finishDialog() {
        if (saveCB.isSelected()) {
            final String saveFile = Tools.getConfigData().getSaveFile();
            Tools.save(saveFile, false);
        }
    }

    /** Inits the dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton()),
                                               buttonClass(finishButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents(new JComponent[]{buttonClass(nextButton())});
        if (Tools.getConfigData().danglingHostsCount() < 2) {
            makeDefaultAndRequestFocusLater(addAnotherHostButton);
        } else {
            makeDefaultAndRequestFocusLater(confClusterButton);
        }
        Tools.getConfigData().removeAutoHost();
        if (Tools.getConfigData().getAutoHosts().isEmpty()) {
            if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        confClusterButton.pressButton();
                    }
                });
            }
        } else {
            Tools.sleep(1000);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    addAnotherHostButton.pressButton();
                }
            });
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.Finish.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Finish.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.Finish.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Finish.Description");
    }

    /**
     * Returns input pane with two big buttons: configure a cluster or add
     * another host.
     */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        /* host wizard button */
        addAnotherHostButton = new MyButton(
                    Tools.getString("Dialog.Host.Finish.AddAnotherHostButton"),
                    HOST_ICON);
        addAnotherHostButton.setPreferredSize(BUTTON_DIMENSION);
        final DialogHost thisClass = this;
        addAnotherHostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Host newHost = new Host();
                        newHost.getSSH().setPasswords(
                                 getHost().getSSH().getLastDSAKey(),
                                 getHost().getSSH().getLastRSAKey(),
                                 getHost().getSSH().getLastPassword());
                        nextDialog = new NewHost(thisClass, newHost);
                        Tools.getGUIData().allHostsUpdate();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                addAnotherHostButton.setEnabled(false);
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    }
                });
                t.start();
            }
        });
        /* cluster wizard button */
        confClusterButton = new MyButton(
                Tools.getString("Dialog.Host.Finish.ConfigureClusterButton"),
                CLUSTER_ICON);
        confClusterButton.setPreferredSize(BUTTON_DIMENSION);
        confClusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                confClusterButton.setEnabled(false);
                                buttonClass(finishButton()).pressButton();
                            }
                        });
                        final AddClusterDialog c = new AddClusterDialog();
                        c.showDialogs();
                    }
                });
                t.start();
            }
        });
        pane.add(addAnotherHostButton);
        if (Tools.getConfigData().danglingHostsCount() < 1) {
            confClusterButton.setEnabled(false);
        }
        pane.add(confClusterButton);
        /* Save checkbox */
        saveCB.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        pane.add(saveCB);
        return pane;
    }
}
