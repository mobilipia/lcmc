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
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.ProgressBar;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.utilities.SSH.ExecCommandThread;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;

/**
 * An implementation of a dialog where hardware information is collected.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class Devices extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>Devices</code> object. */
    Devices(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Checks the answer and makes it visible to the user. */
    void checkAnswer(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            progressBarDoneError();
            answerPaneSetTextError(Tools.getString(
                                            "Dialog.Host.Devices.CheckError"));
            enableComponents();
            buttonClass(nextButton()).requestFocus();
        } else {
            getHost().parseHostInfo(ans);
            progressBarDone();
            answerPaneSetText(ans);
            enableComponents();
            buttonClass(nextButton()).requestFocus();
        }
        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Inits the dialog and starts the info collecting thread. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    getProgressBar().start(6000);
                    final ExecCommandThread t = getHost().execCommand(
                             "installGuiHelper",
                             (ProgressBar) null, //getProgressBar(),
                             new ExecCallback() {
                                 @Override
                                 public void done(final String ans) {
                                     getAllInfo();
                                 }
                                 @Override
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     /* in case of error, the next command will
                                        find out, so it's not checked here. Gui
                                        Helper can be installed anyway. */
                                     getAllInfo();
                                 }
                             },
                             null,  /* ConvertCmdCallback */
                             false, /* outputVisible */
                             SSH.DEFAULT_COMMAND_TIMEOUT);
                    setCommandThread(t);
                }
            });
        thread.start();
    }

    /** Returns info for input pane. */
    protected void getAllInfo() {
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        final ExecCommandThread t = getHost().execCommand("GetHostAllInfo",
                         (ProgressBar) null, //getProgressBar(),
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 checkAnswer(ans);
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 printErrorAndRetry(Tools.getString(
                                            "Dialog.Host.Devices.CheckError"),
                                                    ans,
                                                    exitCode);
                             }
                         },
                         null, /* ConvertCmdCallback */
                         true, /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        setCommandThread(t);
    }

    /** Returns the next dialog object. */
    @Override
    public WizardDialog nextDialog() {
        return new DistDetection(this, getHost());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.Devices.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Devices.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.Devices.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Devices.Description");
    }

    /** Returns pane where collected info is displayed. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                            Tools.getString("Dialog.Host.Devices.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }
}
