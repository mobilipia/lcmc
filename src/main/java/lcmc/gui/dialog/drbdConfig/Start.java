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

import lcmc.utilities.Tools;
import lcmc.gui.resources.DrbdInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.StringInfo;
import lcmc.gui.resources.DrbdResourceInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.resources.BlockDevInfo;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.SpringUtilities;
import lcmc.gui.Widget;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An implementation of a dialog where user start to configure the DRBD.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Start extends WizardDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** DRBD resource pulldown menu. */
    private Widget drbdResourceWi;
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;
    /** DRBD info object. */
    private final DrbdInfo drbdInfo;
    /** The first block device info object. */
    private final BlockDevInfo blockDevInfo1;
    /** The second block device info object. */
    private final BlockDevInfo blockDevInfo2;
    /** DRBD resource info object. */
    private DrbdResourceInfo drbdResourceInfo;

    /** Prepares a new <code>Start</code> object. */
    public Start(final WizardDialog previousDialog,
                 final DrbdInfo drbdInfo,
                 final BlockDevInfo blockDevInfo1,
                 final BlockDevInfo blockDevInfo2) {
        super(previousDialog);
        this.drbdInfo = drbdInfo;
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        boolean newResource = false;
        final Info i = (Info) drbdResourceWi.getValue();
        if (i.getStringValue() == null) {
            final List<BlockDevInfo> bdis =
                    new ArrayList<BlockDevInfo>(Arrays.asList(blockDevInfo1,
                                                              blockDevInfo2));
            drbdResourceInfo = drbdInfo.getNewDrbdResource(
                               DrbdVolumeInfo.getHostsFromBlockDevices(bdis));
            drbdInfo.addDrbdResource(drbdResourceInfo);
            newResource = true;
        } else {
            drbdResourceInfo = (DrbdResourceInfo) i;
        }
        final DrbdVolumeInfo dvi = drbdInfo.getNewDrbdVolume(
                                drbdResourceInfo,
                                new ArrayList<BlockDevInfo>(Arrays.asList(
                                                              blockDevInfo1,
                                                              blockDevInfo2)));
        drbdResourceInfo.addDrbdVolume(dvi);
        drbdInfo.addDrbdVolume(dvi);
        if (newResource) {
            return new Resource(this, dvi);
        } else {
            return new Volume(this, dvi);
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.DrbdConfig.Start.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Start.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.DrbdConfig.Start.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Start.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pressNextButton();
                }
            });
        }
    }

    /** Returns input pane where user can configure a drbd resource. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* Drbd Resource */
        final JLabel drbdResourceLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.Start.DrbdResource"));
        final String newDrbdResource =
                    Tools.getString("Dialog.DrbdConfig.Start.NewDrbdResource");
        final List<Info> choices = new ArrayList<Info>();
        choices.add(new StringInfo(newDrbdResource, null, null));
        for (final DrbdResourceInfo dri : drbdInfo.getDrbdResources()) {
            choices.add(dri);
        }
        drbdResourceWi = new Widget(null,
                                    choices.toArray(
                                               new Info[choices.size()]),
                                    null, /* units */
                                    Widget.Type.COMBOBOX,
                                    null, /* regexp */
                                    COMBOBOX_WIDTH,
                                    null, /* abbrv */
                                    new AccessMode(
                                             ConfigData.AccessType.RO,
                                             false)); /* only adv. mode */
        inputPane.add(drbdResourceLabel);
        inputPane.add(drbdResourceWi);
        SpringUtilities.makeCompactGrid(inputPane, 1, 2,  // rows, cols
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
}
