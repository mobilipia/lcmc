/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011-2012, Rasto Levrinc
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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Host;
import lcmc.data.ClusterStatus;
import lcmc.data.PtestData;
import lcmc.utilities.Tools;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.DRBD;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.JColorChooser;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * This class provides menus for block device and host multi selection.
 */
public final class DrbdMultiSelectionInfo extends EditableInfo {
    /** All selected objects. */
    private final List<Info> selectedInfos;

    /** Prepares a new <code>DrbdMultiSelectionInfo</code> object. */
    public DrbdMultiSelectionInfo(final List<Info> selectedInfos,
                                  final Browser browser) {
        super("selection", browser);
        this.selectedInfos = selectedInfos;
    }

    /** @see EditableInfo#getMenuIcon() */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /** @see EditableInfo#getInfoType() */
    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** @see EditableInfo#getInfo() */
    @Override
    public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        s.append(Tools.getString("DrbdMultiSelectionInfo.Selection"));
        for (final Info si : selectedInfos) {
            s.append(si.toString());
            s.append("<br />");
        }
        return s.toString();
    }

    /** Create menu items for selected hosts. */
    private void createSelectedHostsPopup(
                                    final List<HostDrbdInfo> selectedHostInfos,
                                    final List<UpdatableItem> items) {
        /* load drbd */
        final MyMenuItem loadItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.LoadDrbd"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.LoadDrbd"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (hi.getHost().isConnected()) {
                            if (hi.getHost().isDrbdLoaded()) {
                                return "already loaded";
                            }
                        } else {
                            return Host.NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.load(hi.getHost(), CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost());
                    }
                }
            };
        items.add(loadItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
            new MyMenuItem(
                   Tools.getString("DrbdMultiSelectionInfo.AdjustAllDrbd"),
                   null,
                   Tools.getString("DrbdMultiSelectionInfo.AdjustAllDrbd"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isConnected()) {
                            return Host.NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.adjust(hi.getHost(), DRBD.ALL, null, CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost());
                    }
                }
            };
        items.add(adjustAllItem);
        final ClusterBrowser.DRBDMenuItemCallback adjustAllItemCallback =
               getBrowser().new DRBDMenuItemCallback(adjustAllItem,
                                                     getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.adjust(hi.getHost(), DRBD.ALL, null, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(adjustAllItem, adjustAllItemCallback);

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.UpAll"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.UpAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isDrbdStatus()) {
                            return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.up(hi.getHost(), DRBD.ALL, null, CRM.LIVE);
                    }
                }
            };
        items.add(upAllItem);
        final ClusterBrowser.DRBDMenuItemCallback upAllItemCallback =
             getBrowser().new DRBDMenuItemCallback(upAllItem,
                                                   getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.up(hi.getHost(), DRBD.ALL, null, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(upAllItem, upAllItemCallback);

        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(
                    Tools.getString("DrbdMultiSelectionInfo.ChangeHostColor"),
                    null,
                    "",
                    new AccessMode(ConfigData.AccessType.RO, false),
                    new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    final Host firstHost = selectedHostInfos.get(0).getHost();
                    final Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + selectedHostInfos
                                            + " color",
                                            firstHost.getPmColors()[0]);
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (newColor != null) {
                            hi.getHost().setSavedColor(newColor);
                        }
                    }
                }
            };
        items.add(changeHostColorItem);
    }

    /** Create menu items for selected block devices. */
    private void createSelectedBlockDevPopup(
                                 final List<BlockDevInfo> selectedBlockDevInfos,
                                 final List<UpdatableItem> items) {
        /* detach */
        final MyMenuItem detachMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Detach"),
                           BlockDevInfo.NO_HARDDISK_ICON_LARGE,
                           Tools.getString("DrbdMultiSelectionInfo.Detach"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneAttached = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.isDiskless(CRM.LIVE)) {
                            oneAttached = true;
                        }
                    }
                    return oneAttached;
                }

                @Override
                public String enablePredicate() {
                    boolean detachable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        detachable = true;
                    }
                    if (detachable) {
                        return null;
                    } else {
                        return "nothing do detach";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.isDiskless(CRM.LIVE)) {
                            bdi.detach(CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.DRBDMenuItemCallback detachItemCallback =
              getBrowser().new DRBDMenuItemCallback(detachMenu,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getConfigData().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && !bdi.isDiskless(CRM.LIVE)) {
                        bdi.detach(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(detachMenu, detachItemCallback);
        items.add(detachMenu);

        /* attach */
        final MyMenuItem attachMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Attach"),
                           BlockDevInfo.HARDDISK_DRBD_ICON_LARGE,
                           Tools.getString("DrbdMultiSelectionInfo.Attach"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneDetached = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isDiskless(CRM.LIVE)) {
                            oneDetached = true;
                        }
                    }
                    return oneDetached;
                }

                @Override
                public String enablePredicate() {
                    boolean attachable = true;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        attachable = true;
                    }
                    if (attachable) {
                        return null;
                    } else {
                        return "nothing to attach";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && bdi.isDiskless(CRM.LIVE)) {
                            bdi.attach(CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.DRBDMenuItemCallback attachItemCallback =
             getBrowser().new DRBDMenuItemCallback(attachMenu,
                                                   getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getConfigData().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && bdi.isDiskless(CRM.LIVE)) {
                        bdi.attach(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(attachMenu, attachItemCallback);
        items.add(attachMenu);

        /* connect */
        final MyMenuItem connectMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Connect"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Connect"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneDisconnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(CRM.LIVE)) {
                            oneDisconnected = true;
                        }
                    }
                    return oneDisconnected;
                }

                @Override
                public String enablePredicate() {
                    boolean connectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(CRM.LIVE)) {
                            continue;
                        }
                        connectable = true;
                    }
                    if (connectable) {
                        return null;
                    } else {
                        return "nothing to connect";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.isConnectedOrWF(CRM.LIVE)) {
                            bdi.connect(CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
              getBrowser().new DRBDMenuItemCallback(connectMenu,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getConfigData().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.isConnectedOrWF(CRM.LIVE)) {
                        bdi.connect(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);

        /* disconnect */
        final MyMenuItem disconnectMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Disconnect"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Disconnect"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneConnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(CRM.LIVE)) {
                            oneConnected = true;
                        }
                    }
                    return oneConnected;
                }

                @Override
                public String enablePredicate() {
                    boolean disconnectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()
                            && ((bdi.getBlockDevice().isPrimary()
                                 && bdi.getBlockDevice().isSyncTarget())
                                || (bdi.getOtherBlockDevInfo().getBlockDevice().
                                                                    isPrimary()
                                    && bdi.getBlockDevice().isSyncSource()))) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(CRM.LIVE)) {
                            continue;
                        }
                        disconnectable = true;
                    }
                    if (disconnectable) {
                        return null;
                    } else {
                        return "nothing to disconnect";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.isConnectedOrWF(CRM.LIVE)
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && (!bdi.getBlockDevice().isSyncing()
                                || (bdi.getBlockDevice().isPrimary()
                                    && bdi.getBlockDevice().isSyncSource())
                                   || (bdi.getOtherBlockDevInfo()
                                          .getBlockDevice().isPrimary()
                                       && bdi.getBlockDevice().isSyncTarget()))) {
                            bdi.disconnect(CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.DRBDMenuItemCallback disconnectItemCallback =
              getBrowser().new DRBDMenuItemCallback(disconnectMenu,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && bdi.isConnectedOrWF(CRM.LIVE)
                        && (Tools.getConfigData().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && (!bdi.getBlockDevice().isSyncing()
                            || (bdi.getBlockDevice().isPrimary()
                                && bdi.getBlockDevice().isSyncSource())
                               || (bdi.getOtherBlockDevInfo()
                                      .getBlockDevice().isPrimary()
                                   && bdi.getBlockDevice().isSyncTarget()))) {
                        bdi.disconnect(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(disconnectMenu, disconnectItemCallback);
        items.add(disconnectMenu);

        /* set primary */
        final MyMenuItem setPrimaryItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.SetPrimary"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.SetPrimary"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary()
                                 && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi =
                                                    bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.setPrimary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(setPrimaryItem);

        /* set secondary */
        final MyMenuItem setSecondaryItem =
            new MyMenuItem(
                        Tools.getString("DrbdMultiSelectionInfo.SetSecondary"),
                        null,
                        Tools.getString("DrbdMultiSelectionInfo.SetSecondary"),
                        new AccessMode(ConfigData.AccessType.OP, true),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean onePrimary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isPrimary()) {
                            onePrimary = true;
                        }
                    }
                    if (!onePrimary) {
                        return "nothing to demote";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isPrimary()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.setSecondary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(setSecondaryItem);

        /* force primary */
        final MyMenuItem forcePrimaryItem =
            new MyMenuItem(
                        Tools.getString("DrbdMultiSelectionInfo.ForcePrimary"),
                        null,
                        Tools.getString("DrbdMultiSelectionInfo.ForcePrimary"),
                        new AccessMode(ConfigData.AccessType.ADMIN, true),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary()
                                 && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi =
                                                    bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.forcePrimary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(forcePrimaryItem);

        /* invalidate */
        final MyMenuItem invalidateItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Invalidate"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Invalidate"),
                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean canInvalidate = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getDrbdVolumeInfo().isVerifying()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        canInvalidate = true;
                    }
                    if (canInvalidate) {
                        return null;
                    } else {
                        return "nothing to invalidate";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getDrbdVolumeInfo().isVerifying()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.invalidateBD(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(invalidateItem);

        /* resume */
        final MyMenuItem resumeSyncItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.ResumeSync"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.ResumeSync"),

                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean resumable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncTarget()
                            || bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        resumable = true;
                    }
                    if (!resumable) {
                        return "nothing to resume";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isSyncTarget()
                            && !bdi.getBlockDevice().isSyncSource()) {
                            bdi.resumeSync(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(resumeSyncItem);

        /* pause sync */
        final MyMenuItem pauseSyncItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.PauseSync"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.PauseSync"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean pausable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncTarget()
                            && !bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        pausable = true;
                    }
                    if (!pausable) {
                        return "nothing to pause";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (bdi.getBlockDevice().isSyncTarget()
                                || bdi.getBlockDevice().isSyncSource())) {
                            bdi.pauseSync(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(pauseSyncItem);

        /* resize */
        final MyMenuItem resizeItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Resize"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Resize"),
                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean resizable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        resizable = true;
                    }
                    if (resizable) {
                        return null;
                    } else {
                        return "nothing to resize";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && !bdi.getBlockDevice().isSyncing()) {
                            bdi.resizeDrbd(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(resizeItem);

        /* discard my data */
        final MyMenuItem discardDataItem =
            new MyMenuItem(
                         Tools.getString("DrbdMultiSelectionInfo.DiscardData"),
                         null,
                         Tools.getString("DrbdMultiSelectionInfo.DiscardData"),
                         new AccessMode(ConfigData.AccessType.ADMIN, true),
                         new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean discardable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getConfigData().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isPrimary()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        discardable = true;
                    }
                    if (discardable) {
                        return null;
                    } else {
                        return "nothing to discard";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && (Tools.getConfigData().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isPrimary()) {
                            bdi.discardData(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(discardDataItem);
    }

    /** @see EditableInfo#createPopup() */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final List<BlockDevInfo> selectedBlockDevInfos =
                                                 new ArrayList<BlockDevInfo>();
        final List<HostDrbdInfo> selectedHostInfos =
                                                new ArrayList<HostDrbdInfo>();
        for (final Info i : selectedInfos) {
            if (i instanceof BlockDevInfo) {
                selectedBlockDevInfos.add((BlockDevInfo) i);
            } else if (i instanceof HostDrbdInfo) {
                selectedHostInfos.add((HostDrbdInfo) i);
            }
        }
        if (!selectedHostInfos.isEmpty()) {
            createSelectedHostsPopup(selectedHostInfos, items);
        }
        if (!selectedBlockDevInfos.isEmpty()) {
            createSelectedBlockDevPopup(selectedBlockDevInfos, items);
        }
        return items;
    }

    /** @see EditableInfo#getBrowser() */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** @see EditableInfo#getGraphicalView() */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /** @see EditableInfo#isEnabledOnlyInAdvancedMode() */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /** @see EditableInfo#getAccessType() */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getSection()
     */
    @Override
    protected String getSection(final String param) {
        return null;
    }

    /** @see EditableInfo#isRequired() */
    @Override
    protected boolean isRequired(final String param) {
        return false;
    }

    /** @see EditableInfo#isAdvanced() */
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** @see EditableInfo#isEnabled() */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** @see EditableInfo#isInteger() */
    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    /** @see EditableInfo#isLabel() */
    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    /** @see EditableInfo#isTimeType() */
    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    /** @see EditableInfo#isCheckBox() */
    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    /** @see EditableInfo#getParamType() */
    @Override
    protected String getParamType(final String param) {
        return null;
    }


    /** @see EditableInfo#getParametersFromXML() */
    @Override
    public String[] getParametersFromXML() {
        return null;
    }

    /**
     * @see EditableInfo#getParamPossibleChoices()
     */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        return null;
    }

    /** @see EditableInfo#checkParam() */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        return true;
    }

    /** @see EditableInfo#getParamDefault() */
    @Override
    public String getParamDefault(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamPreferred()
     */
    @Override
    protected String getParamPreferred(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamShortDesc()
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamLongDesc()
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return null;
    }

    /** @see EditableInfo#getInfoPanel() */
    @Override
    public JComponent getInfoPanel() {
        final boolean abExisted = getApplyButton() != null;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                if (Tools.versionBeforePacemaker(dcHost)) {
                    return false;
                }
                return true;
            }
            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getHeartbeatGraph().stopTestAnimation(
                                                             getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            @Override
            public void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                getApplyButton().setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                getApplyButton().setToolTipBackground(Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getHeartbeatGraph().startTestAnimation(
                                                               getApplyButton(),
                                                               startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus cs = getBrowser().getClusterStatus();
                cs.setPtestData(null);
                apply(dcHost, CRM.TESTONLY);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                getApplyButton().setToolTipText(ptestData.getToolTip());
                cs.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), CRM.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final AbstractButton serviceMenu = getActionsButton();
        buttonPanel.add(serviceMenu, BorderLayout.EAST);

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        final String[] params = getParametersFromXML();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        mainPanel.add(super.getInfoPanel());
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanelDone();
        return newPanel;
    }

    /**
     * Apply the changes to the drbd parameters.
     * not implemented
     */
    void apply(final Host dcHost, final boolean testOnly) {
    }
}
