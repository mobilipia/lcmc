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


package lcmc.gui.dialog.cluster;

import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where connection to every host will be checked
 * and established if there isn't one.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class Connect extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>Connect</code> object. */
    Connect(final WizardDialog previousDialog,
            final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /** Returns the next dialog which is ClusterDrbdConf. */
    @Override
    public WizardDialog nextDialog() {
        return new CommStack(getPreviousDialog(), getCluster());
    }

    /** Returns cluster dialog title. */
    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Connect.Title");
    }

    /** Returns description. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Connect.Description");
    }

    /** Checks hosts, if they are connected and if not reconnects them. */
    protected void checkHosts() {
        final StringBuilder text = new StringBuilder();
        boolean pending = false;
        boolean oneFailed = false;
        for (final Host host : getCluster().getHosts()) {
            String status;
            if (host.getSSH().isConnectionFailed()) {
                status = "failed.";
                oneFailed = true;
            } else if (host.isConnected()) {
                status = "connected.";
            } else {
                pending = true;
                status = "connecting...";
            }
            text.append(host.getName() + " " + status + "\n");
        }
        Tools.debug(this, "pending: " + pending + ", one failed: " + oneFailed);
        if (pending) {
             answerPaneSetText(text.toString());
        } else if (oneFailed) {
             printErrorAndRetry(text.toString());
        } else {
             answerPaneSetText(text.toString());
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException ex) {
                 Thread.currentThread().interrupt();
             }

             SwingUtilities.invokeLater(new Runnable() {
                 @Override
                 public void run() {
                    buttonClass(nextButton()).pressButton();
                 }
             });
        }
    }

    /** Connects all cluster hosts. */
    protected void connectHosts() {
        getCluster().connect(getDialogPanel(), true, 1);
        for (final Host host : getCluster().getHosts()) {
            host.waitOnLoading();
        }
        checkHosts();
    }

    /** Inits the dialog and connects the hosts. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                connectHosts();
            }
        });
        t.start();
    }

    /** Returns the connect hosts dialog content. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final StringBuilder text = new StringBuilder();
        for (final Host host : getCluster().getHosts()) {
            text.append(host.getName());
            text.append(" connecting...\n");
        }
        pane.add(getAnswerPane(text.toString()));

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Enable skip button. */
    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
