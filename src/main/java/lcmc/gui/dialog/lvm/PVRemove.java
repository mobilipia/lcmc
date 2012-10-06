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

package lcmc.gui.dialog.lvm;

import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.BlockDevInfo;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.gui.Browser;

import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class implements PVRemove dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class PVRemove extends LV {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Block device info object. */
    private final MyButton removeButton = new MyButton("Remove PV");
    private final BlockDevInfo blockDevInfo;
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    /** Description. */
    private static final String PV_REMOVE_DESCRIPTION =
                                                    "Remove a physical volume.";
    /** Remove PV timeout. */
    private static final int REMOVE_TIMEOUT = 5000;
    /** Remove new PVRemove object. */
    public PVRemove(final BlockDevInfo blockDevInfo) {
        super(null);
        this.blockDevInfo = blockDevInfo;
    }

    /** Finishes the dialog and sets the information. */
    protected void finishDialog() {
        /* disable finish dialog */
    }

    /** Returns the title of the dialog. */
    protected String getDialogTitle() {
        return "Remove PV";
    }

    /** Returns the description of the dialog. */
    protected String getDescription() {
        return PV_REMOVE_DESCRIPTION;
    }

    /** Inits the dialog. */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(removeButton);
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        if (blockDevInfo.getBlockDevice().isPhysicalVolume()
            || blockDevInfo.getBlockDevice().isDrbdPhysicalVolume()) {
            SwingUtilities.invokeLater(new EnableRemoveRunnable(true));
        }
    }

    private class EnableRemoveRunnable implements Runnable {
        private final boolean enable;
        public EnableRemoveRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            final boolean e = enable;
            removeButton.setEnabled(e);
        }
    }


    /** Returns the input pane. */
    protected JComponent getInputPane() {
        removeButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        inputPane.add(new JLabel("Block Device:"));
        inputPane.add(new JLabel(blockDevInfo.getName()));
        removeButton.addActionListener(new RemoveActionListener());
        inputPane.add(removeButton);
        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel hostsPane = new JPanel(
                        new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        final Cluster cluster = blockDevInfo.getHost().getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Host h : hostCheckBoxes.keySet()) {
            hostCheckBoxes.get(h).addItemListener(
                                            new ItemChangeListener(true));
            final BlockDevInfo oBdi =
                blockDevInfo.getBrowser().getDrbdGraph().findBlockDevInfo(
                                                  h.getName(),
                                                  blockDevInfo.getName());
            if (blockDevInfo.getHost() == h) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(true);
            } else if (oBdi == null
                       || oBdi.getBlockDevice().isDrbd()
                       || !oBdi.getBlockDevice().isPhysicalVolume()
                    || oBdi.getBlockDevice().isVolumeGroupOnPhysicalVolume()) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(false);
            } else {
                hostCheckBoxes.get(h).setEnabled(true);
                hostCheckBoxes.get(h).setSelected(false);
            }
            hostsPane.add(hostCheckBoxes.get(h));
        }
        final javax.swing.JScrollPane sp = new javax.swing.JScrollPane(
                                                               hostsPane);
        sp.setPreferredSize(new java.awt.Dimension(0, 45));
        pane.add(sp);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 4, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        checkButtons();
        return pane;
    }

    /** Remove action listener. */
    private class RemoveActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Thread thread = new Thread(new RemoveRunnable());
            thread.start();
        }
    }

    private class RemoveRunnable implements Runnable {
        @Override
        public void run() {
            Tools.invokeAndWait(new EnableRemoveRunnable(false));
            disableComponents();
            getProgressBar().start(REMOVE_TIMEOUT
                                   * hostCheckBoxes.size());
            boolean oneFailed = false;
            for (final Host h : hostCheckBoxes.keySet()) {
                if (hostCheckBoxes.get(h).isSelected()) {
                    final BlockDevInfo oBdi =
                        blockDevInfo.getBrowser().getDrbdGraph()
                            .findBlockDevInfo(h.getName(),
                                              blockDevInfo.getName());
                    if (oBdi != null) {
                        final boolean ret = pvRemove(h, oBdi);
                        if (!ret) {
                            oneFailed = true;
                        }
                    }
                }
            }
            enableComponents();
            if (oneFailed) {
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h);
                }
                checkButtons();
                progressBarDoneError();
            } else {
                progressBarDone();
                disposeDialog();
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h);
                }
            }
        }
    }

    /** Remove PV. */
    private boolean pvRemove(final Host host,
                             final BlockDevInfo bdi) {
        final boolean ret = bdi.pvRemove(false);
        if (ret) {
            answerPaneAddText("Labels on physical volume "
                              + bdi.getName()
                              + " were successfully removed "
                              + " on " + host.getName() + ".");
        } else {
            answerPaneAddTextError("Removing labels on physical volume "
                                    + bdi.getName()
                                    + " on " + host.getName()
                                    + " failed.");
        }
        return ret;
    }

    /** Size combo box item listener. */
    private class ItemChangeListener implements ItemListener {
        /** Whether to check buttons on both select and deselect. */
        private final boolean onDeselect;
        /** Create ItemChangeListener object. */
        public ItemChangeListener(final boolean onDeselect) {
            super();
            this.onDeselect = onDeselect;
        }
        @Override
        public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED
                || onDeselect) {
                checkButtons();
            }
        }
    }
}
