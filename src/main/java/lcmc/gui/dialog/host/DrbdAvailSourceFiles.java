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

import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.ComboInfo;
import lcmc.utilities.SSH;
import lcmc.utilities.WidgetListener;
import lcmc.gui.SpringUtilities;
import lcmc.gui.Widget;
import lcmc.gui.dialog.WizardDialog;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where available versions of drbd will be
 * determined.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
final class DrbdAvailSourceFiles extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Combo box with drbd tarballs. */
    private Widget drbdTarballCombo = null;
    /** Whether the listeners where added. */
    private boolean listenersAdded = false;

    /** Prepares a new <code>DrbdAvailSourceFiles</code> object. */
    DrbdAvailSourceFiles(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog and starts detecting the available drbd source
     * tarballs.
     */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        disableComponents(new JComponent[]{drbdTarballCombo});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(2000);
        availTarballs();
    }

    /** Finds abailable tarballs. */
    protected void availTarballs() {
        getHost().execCommand(
              "DrbdAvailVersionsSource",
              null, /* ProgresBar */
              new ExecCallback() {
                @Override
                public void done(final String ans) {
                    final String[] versions = ans.split("\\r?\\n");
                    if (versions.length == 0) {
                        allDone(null);
                        return;
                    }
                    final List<ComboInfo> items = new ArrayList<ComboInfo>();
                    for (final String versionString : versions) {
                        if (versionString != null
                            && versionString.length() > 16) {
                            final String version =
                                    versionString.substring(
                                                   9,
                                                   versionString.length() - 7);
                            items.add(new ComboInfo(version, versionString));
                        }
                    }
                    drbdTarballCombo.clear();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            drbdTarballCombo.reloadComboBox(
                                   items.get(0).toString(),
                                   items.toArray(new ComboInfo[items.size()]));
                            final ComboInfo selectedItem =
                                   (ComboInfo) drbdTarballCombo.getValue();
                            drbdTarballCombo.setEnabled(true);
                            allDone(selectedItem);
                        }
                    });
                }

                @Override
                public void doneError(final String ans,
                                                final int exitCode) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressBarDoneError();
                            printErrorAndRetry(Tools.getString(
                                   "Dialog.Host.DrbdAvailSourceFiles.NoBuilds"),
                                               ans,
                                               exitCode);
                        }
                    });
                }
              },
              null,   /* ConvertCmdCallback */
              false,  /* outputVisible */
              SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected void allDone(final ComboInfo versionInfo) {
        if (versionInfo != null) {
            answerPaneSetText("http://oss.linbit.com/drbd/"
                              + versionInfo.getStringValue());
            getHost().setDrbdVersionToInstall(versionInfo.toString());
            getHost().setDrbdVersionUrlStringToInstall(
                                            versionInfo.getStringValue());
        }
        // TODO: do something different if we did not get any versions
        drbdTarballCombo.setEnabled(true);
        nextDialogObject = new DrbdCommandInst(this, getHost());
        progressBarDone();
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            pressNextButton();
        }
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Description");
    }

    /** Returns the panel with combo boxes. */
    protected JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* build combo box */
        drbdTarballCombo = new Widget(null, /* selected value */
                                      null, /* items */
                                      null, /* units */
                                      Widget.Type.COMBOBOX,
                                      null, /* regexp */
                                      0,    /* width */
                                      null, /* abbrv */
                                      new AccessMode(
                                               ConfigData.AccessType.RO,
                                               false)); /* only adv mode*/

        //drbdTarballCombo.setEnabled(false);
        pane.add(drbdTarballCombo);

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Adds listeners to all combo boxes. */
    private void addListeners() {
        /* tarball combo box */
        drbdTarballCombo.addListeners(new WidgetListener() {
            @Override
            public void check(final Object value) {
                final ComboInfo item = (ComboInfo) drbdTarballCombo.getValue();
                    allDone(item);
            }
        });

    }

    /** Returns input pane with available drbd files. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getChoiceBoxes());
        pane.add(getProgressBarPane());
        pane.add(
            getAnswerPane(
               Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
