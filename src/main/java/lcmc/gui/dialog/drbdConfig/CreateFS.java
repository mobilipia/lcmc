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


package lcmc.gui.dialog.drbdConfig;

import lcmc.Exceptions;
import lcmc.utilities.Tools;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.BlockDevInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.resources.StringInfo;
import lcmc.gui.Widget;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CreateFS extends DrbdConfig {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Pull down menu with hosts (or no host). */
    private Widget hostW;
    /** Pull down menu with file systems. */
    private Widget filesystemW;
    /** Whether to skip the initial full sync. */
    private Widget skipSyncW;
    /** Whether to skip the initial full sync label. */
    private JLabel skipSyncLabel;
    /** Make file system button. */
    private final MyButton makeFsButton = new MyButton(
                Tools.getString("Dialog.DrbdConfig.CreateFS.CreateFsButton"));
    /** No host string. (none) */
    private static final String NO_HOST_STRING =
                    Tools.getString("Dialog.DrbdConfig.CreateFS.NoHostString");
    /** No file system (use existing data). */
    private static final String NO_FILESYSTEM_STRING =
                Tools.getString("Dialog.DrbdConfig.CreateFS.SelectFilesystem");
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;
    /** Skip sync false. */
    private static final String SKIP_SYNC_FALSE = "false";
    /** Skip sync true. */
    private static final String SKIP_SYNC_TRUE = "true";

    /** Prepares a new <code>CreateFS</code> object. */
    CreateFS(final WizardDialog previousDialog,
             final DrbdVolumeInfo drbdVolumeInfo) {
        super(previousDialog, drbdVolumeInfo);
    }

    /**
     * Finishes the dialog. If primary bd was choosen it is forced to be a
     * primary.
     */
    @Override
    protected void finishDialog() {
        final BlockDevInfo bdiPri = getPrimaryBD();
        if (bdiPri != null) {
            final boolean testOnly = false;
            if (SKIP_SYNC_TRUE.equals(skipSyncW.getStringValue())) {
                bdiPri.skipInitialFullSync(testOnly);
            }
            bdiPri.forcePrimary(testOnly);
        }
    }

    /** Returns the primary block device. */
    protected BlockDevInfo getPrimaryBD() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        final String h = hostW.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi1;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi2;
        } else {
            return null;
        }
    }

    /** Returns the secondary block device. */
    protected BlockDevInfo getSecondaryBD() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        final String h = hostW.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi2;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi1;
        } else {
            Tools.appError("unknown host: " + h);
            return null;
        }
    }

    /** Creates the file system. */
    protected void createFilesystem() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getProgressBar().start(1);
                answerPaneSetText(
                        Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS"));
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        buttonClass(finishButton()).setEnabled(false);
                        makeFsButton.setEnabled(false);
                    }
                });
                BlockDevInfo bdiPri = getPrimaryBD();
                BlockDevInfo bdiSec = getSecondaryBD();
                final boolean testOnly = false;
                if (SKIP_SYNC_TRUE.equals(skipSyncW.getStringValue())) {
                    bdiPri.skipInitialFullSync(testOnly);
                }
                bdiPri.forcePrimary(testOnly);
                final String fs = filesystemW.getStringValue();
                bdiPri.makeFilesystem(fs, testOnly);
                if (bdiPri.getDrbdVolumeInfo() != null) {
                    /* could be canceled */
                    getDrbdVolumeInfo().setCreatedFs(fs);
                    bdiPri.setSecondary(testOnly);
                    hostW.setValue(NO_HOST_STRING);
                    filesystemW.setValue(NO_FILESYSTEM_STRING);
                    answerPaneSetText(
                     Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS.Done"));
                }
                progressBarDone();
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    /** Returns the next dialog, null in this dialog. */
    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns title of the dialog.
     * It is defined in TextResources as "Dialog.DrbdConfig.CreateFS.Title"
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Title");
    }

    /**
     * Returns description of the dialog.
     * It is defined in TextResources as
     * "Dialog.DrbdConfig.CreateFS.Description"
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        makeFsButton.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    makeFsButton.pressButton();
                }
            });
        }
    }

    /**
     * Enables and disables the make fs and finish buttons depending on what
     * was chosen by user.
     */
    protected void checkButtons() {
        final boolean noHost = hostW.getStringValue().equals(NO_HOST_STRING);
        final boolean noFileSystem = filesystemW.getStringValue().equals(
                                                        NO_FILESYSTEM_STRING);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (noHost) {
                    skipSyncW.setEnabled(false);
                    skipSyncLabel.setEnabled(false);
                    skipSyncW.setValue(SKIP_SYNC_FALSE);
                } else {
                    if (skipSyncAvailable()) {
                        skipSyncW.setEnabled(true);
                        skipSyncLabel.setEnabled(true);
                    }
                }
            }
        });
        if (noFileSystem) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(true);
                    makeFsButton.setEnabled(false);
                    skipSyncW.setValue(SKIP_SYNC_FALSE);
                }
            });
        } else if (noHost) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                }
            });
            makeFsButton.setEnabled(false);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                    makeFsButton.setEnabled(true);
                    if (skipSyncAvailable()) {
                        skipSyncW.setValue(SKIP_SYNC_TRUE);
                        skipSyncW.setEnabled(true);
                    }
                }
            });
        }
    }

    /**
     * Returns input pane, where file system can be created on the selected
     * host.
     */
    @Override
    protected JComponent getInputPane() {
        makeFsButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* host */
        final String[] hostNames = new String[3];
        hostNames[0] = NO_HOST_STRING;
        int i = 1;
        for (final Host host : getDrbdVolumeInfo().getHosts()) {
            hostNames[i] = host.getName();
            i++;
        }
        final JLabel hostLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.ChooseHost"));
        String defaultHost = NO_HOST_STRING;
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            defaultHost = hostNames[1];
        }
        hostW = new Widget(defaultHost,
                           hostNames,
                           null, /* units */
                           Widget.Type.COMBOBOX,
                           null, /* regexp */
                           COMBOBOX_WIDTH,
                           null, /* abbrv */
                           new AccessMode(ConfigData.AccessType.RO,
                                          false)); /* only adv. mode */
        hostW.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    checkButtons();
                                }
                            });
        inputPane.add(hostLabel);
        inputPane.add(hostW);
        inputPane.add(new JLabel(""));

        /* Filesystem */
        final JLabel filesystemLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.Filesystem"));
        String defaultValue = NO_FILESYSTEM_STRING;
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            defaultValue = "ext3";
        }
        final StringInfo[] filesystems =
            getDrbdVolumeInfo().getDrbdResourceInfo().getCommonFileSystems(
                                                                defaultValue);

        filesystemW = new Widget(defaultValue,
                                 filesystems,
                                 null, /* units */
                                 Widget.Type.COMBOBOX,
                                 null, /* regexp */
                                 COMBOBOX_WIDTH,
                                 null, /* abbrv */
                                 new AccessMode(ConfigData.AccessType.RO,
                                                false)); /* only adv. */
        inputPane.add(filesystemLabel);
        inputPane.add(filesystemW);
        filesystemW.addListeners(new WidgetListener() {
                            @Override
                            public void check(final Object value) {
                                if (NO_HOST_STRING.equals(
                                                hostW.getStringValue())
                                    && !NO_FILESYSTEM_STRING.equals(
                                            filesystemW.getStringValue())) {
                                    hostW.setValue(hostNames[1]);
                                } else {
                                    checkButtons();
                                }
                            }
                        });

        makeFsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                createFilesystem();
            }
        });
        inputPane.add(makeFsButton);
        /* skip initial full sync */
        skipSyncLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.SkipSync"));
        skipSyncLabel.setEnabled(false);
        skipSyncW = new Widget(SKIP_SYNC_FALSE,
                               new String[]{SKIP_SYNC_TRUE,
                                            SKIP_SYNC_FALSE},
                               null, /* units */
                               Widget.Type.CHECKBOX,
                               null, /* regexp */
                               COMBOBOX_WIDTH,
                               null, /* abbrv */
                               new AccessMode(ConfigData.AccessType.RO,
                                              false)); /* only adv. */
        skipSyncW.setEnabled(false);
        skipSyncW.setBackgroundColor(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(skipSyncLabel);
        inputPane.add(skipSyncW);
        inputPane.add(new JLabel(""));

        SpringUtilities.makeCompactGrid(inputPane, 3, 3,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        pane.add(inputPane);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }

    /** Returns whether skip sync is available. */
    private boolean skipSyncAvailable() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        try {
            return Tools.compareVersions(
                                bdi1.getHost().getDrbdVersion(), "8.3.2") >= 0
                   && Tools.compareVersions(
                                bdi2.getHost().getDrbdVersion(), "8.3.2") >= 0;
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
            return false;
        }
    }
}
