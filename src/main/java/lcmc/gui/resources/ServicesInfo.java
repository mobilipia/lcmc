/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
import lcmc.gui.Widget;
import lcmc.gui.HeartbeatGraph;
import lcmc.gui.dialog.ClusterLogs;
import lcmc.data.Host;
import lcmc.data.ResourceAgent;
import lcmc.data.ClusterStatus;
import lcmc.data.resources.Resource;
import lcmc.data.PtestData;
import lcmc.data.CRMXML;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Unit;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.CRM;
import lcmc.utilities.Tools;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.Exceptions;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JDialog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class holds info data for services view and global heartbeat
 * config.
 */
public final class ServicesInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** No clone parameter. */
    public static final CloneInfo NO_CLONE = null;

    /** Prepares a new <code>ServicesInfo</code> object. */
    public ServicesInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Sets info panel. */
    void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /** Returns icon for services menu item. */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /** Returns names of all global parameters. */
    @Override
    public String[] getParametersFromXML() {
        final CRMXML crmxml = getBrowser().getCRMXML();
        if (crmxml == null) {
            return null;
        }
        return crmxml.getGlobalParameters();
    }

    /**
     * Returns long description of the global parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamLongDesc(param);
    }

    /**
     * Returns short description of the global parameter, that is used as
     * label.
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamShortDesc(param);
    }

    /** Returns default for this global parameter. */
    @Override
    protected String getParamDefault(final String param) {
        return getBrowser().getCRMXML().getGlobalParamDefault(param);
    }

    /** Returns preferred value for this global parameter. */
    @Override
    protected String getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPreferred(param);
    }

    /** Returns possible choices for pulldown menus if applicable. */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPossibleChoices(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        return getBrowser().getCRMXML().checkGlobalParam(param, newValue);
    }

    /** Returns whether the global parameter is of the integer type. */
    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isGlobalInteger(param);
    }
    /** Returns whether the global parameter is of the label type. */
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCRMXML().isGlobalLabel(param);
    }

    /** Returns whether the global parameter is of the time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isGlobalTimeType(param);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        return getBrowser().getCRMXML().isGlobalAdvanced(param);
    }

    /** Returns access type of this parameter. */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return getBrowser().getCRMXML().getGlobalAccessType(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /** Returns whether the global parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isGlobalRequired(param);
    }

    /**
     * Returns whether the global parameter is of boolean type and
     * requires a checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isGlobalBoolean(param);
    }

    /** Returns type of the global parameter. */
    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCRMXML().getGlobalParamType(param);
    }

    /** Returns section to which the global parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getCRMXML().getGlobalSection(param);
    }

    /** Applies changes that user has entered. */
    void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        if (!testOnly) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText(null);
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();

        /* update pacemaker */
        final Map<String, String> args = new HashMap<String, String>();
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (value.equals(getParamDefault(param))) {
                continue;
            }

            if ("".equals(value)) {
                continue;
            }
            args.put(param, value);
        }
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final String[] rdiParams = rdi.getParametersFromXML();
        final Map<String, String> rdiMetaArgs =
                                           new LinkedHashMap<String, String>();
        for (final String param : rdiParams) {
            final String value = rdi.getComboBoxValue(param);
            if (value.equals(rdi.getParamDefault(param))) {
                    continue;
            }
            if (!"".equals(value)) {
                rdiMetaArgs.put(param, value);
            }
        }
        final String rscDefaultsId =
                    getBrowser().getClusterStatus().getRscDefaultsId(testOnly);
        CRM.setGlobalParameters(dcHost,
                                args,
                                rdiMetaArgs,
                                rscDefaultsId,
                                testOnly);
        if (!testOnly) {
            storeComboBoxValues(params);
            rdi.storeComboBoxValues(rdiParams);
        }
        for (ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (si.checkResourceFieldsCorrect(null,
                                              si.getParametersFromXML(),
                                              true,
                                              false,
                                              false)
                && si.checkResourceFieldsChanged(null,
                                                 si.getParametersFromXML(),
                                                 true,
                                                 false,
                                                 false)) {
                si.apply(dcHost, testOnly);
            }
        }
        if (!testOnly) {
            setApplyButtons(null, params);
        }
    }

    /** Sets heartbeat global parameters after they were obtained. */
    public void setGlobalConfig(final ClusterStatus clStatus) {
        final String[] params = getParametersFromXML();
        for (String param : params) {
            final String value = clStatus.getGlobalParam(param);
            final String oldValue = getParamSaved(param);
            if (value != null && !value.equals(oldValue)) {
                getResource().setValue(param, value);
                final Widget wi = getWidget(param, null);
                if (wi != null) {
                    wi.setValue(value);
                }
            }
        }
        if (infoPanel == null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getInfoPanel();
                }
            });
        }
    }

    /**
     * Check if this connection is filesystem with drbd ra and if so, set it.
     */
    private void setFilesystemWithDrbd(final ServiceInfo siP,
                                       final ServiceInfo si) {
        if (siP.getResourceAgent().isLinbitDrbd()) {
            /* linbit::drbd -> Filesystem */
            ((FilesystemInfo) si).setLinbitDrbdInfo((LinbitDrbdInfo) siP);
        } else {
            /* drbddisk -> Filesystem */
            ((FilesystemInfo) si).setDrbddiskInfo((DrbddiskInfo) siP);
        }
    }

    /** Sets clone info object. */
    private CloneInfo setCreateCloneInfo(final String cloneId,
                                         final ClusterStatus clStatus,
                                         final boolean testOnly) {
        CloneInfo newCi = null;
        newCi = (CloneInfo) getBrowser().getServiceInfoFromCRMId(cloneId);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (newCi == null) {
            final Point2D p = null;
            newCi =
               (CloneInfo) addServicePanel(
                                        getBrowser().getCRMXML().getHbClone(),
                                        p,
                                        false,
                                        cloneId,
                                        null,
                                        testOnly);
            getBrowser().addToHeartbeatIdList(newCi);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
            if (!testOnly) {
                newCi.setUpdated(false);
                hg.repaint();
            }
        }
        newCi.getService().setNew(false);
        return newCi;
    }

    /** Sets group info object. */
    private GroupInfo setCreateGroupInfo(final String group,
                                         final CloneInfo newCi,
                                         final ClusterStatus clStatus,
                                         final boolean testOnly) {
        GroupInfo newGi = null;
        newGi = (GroupInfo) getBrowser().getServiceInfoFromCRMId(group);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (newGi == null) {
            final Point2D p = null;
            newGi =
              (GroupInfo) addServicePanel(
                                     getBrowser().getCRMXML().getHbGroup(),
                                     p,
                                     false,
                                     group,
                                     newCi,
                                     testOnly);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode =
                                    clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (!testOnly) {
                newGi.setUpdated(false);
                hg.repaint();
            }
        }
        newGi.getService().setNew(false);
        return newGi;
    }

    /** Sets or create all resources. */
    private void setGroupResources(
                               final Set<String> allGroupsAndClones,
                               final String grpOrCloneId,
                               final GroupInfo newGi,
                               final CloneInfo newCi,
                               final List<ServiceInfo> serviceIsPresent,
                               final List<ServiceInfo> groupServiceIsPresent,
                               final ClusterStatus clStatus,
                               final boolean testOnly) {
        final Map<ServiceInfo, Map<String, String>> setParametersHash =
                           new HashMap<ServiceInfo, Map<String, String>>();
        if (newCi != null) {
            setParametersHash.put(
                            newCi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        } else if (newGi != null) {
            setParametersHash.put(
                            newGi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        boolean newService = false;
        int pos = 0;
        final List<String> gs = clStatus.getGroupResources(grpOrCloneId,
                                                           testOnly);
        if (gs == null) {
            return;
        }
        for (final String hbId : gs) {
            if (clStatus.isOrphaned(hbId) && Tools.getConfigData().isNoLRM()) {
                continue;
            }
            ServiceInfo newSi;
            if (allGroupsAndClones.contains(hbId)) {
                if (newGi != null) {
                    Tools.appWarning("group in group not implemented");
                    continue;
                }
                /* clone group */
                final GroupInfo gi = setCreateGroupInfo(hbId,
                                                        newCi,
                                                        clStatus,
                                                        testOnly);
                setGroupResources(allGroupsAndClones,
                                  hbId,
                                  gi,
                                  null,
                                  serviceIsPresent,
                                  groupServiceIsPresent,
                                  clStatus,
                                  testOnly);
                newSi = (ServiceInfo) gi;
            } else {
                final ResourceAgent newRA = clStatus.getResourceType(hbId);
                if (newRA == null) {
                    /* This is bad. There is a service but we do not have
                     * the heartbeat script of this service or the we look
                     * in the wrong places.
                     */
                    Tools.appWarning(hbId + ": could not find resource agent");
                }
                /* continue of creating/updating of the
                 * service in the gui.
                 */
                newSi = getBrowser().getServiceInfoFromCRMId(hbId);
                final Map<String, String> resourceNode =
                                             clStatus.getParamValuePairs(hbId);
                if (newSi == null) {
                    newService = true;
                    // TODO: get rid of the service name? (everywhere)
                    String serviceName;
                    if (newRA == null) {
                        serviceName = hbId;
                    } else {
                        serviceName = newRA.getName();
                    }
                    if (newRA != null && newRA.isFilesystem()) {
                        newSi = new FilesystemInfo(serviceName,
                                                   newRA,
                                                   hbId,
                                                   resourceNode,
                                                   getBrowser());
                    } else if (newRA != null && newRA.isLinbitDrbd()) {
                        newSi = new LinbitDrbdInfo(serviceName,
                                                   newRA,
                                                   hbId,
                                                   resourceNode,
                                                   getBrowser());
                    } else if (newRA != null && newRA.isDrbddisk()) {
                        newSi = new DrbddiskInfo(serviceName,
                                                 newRA,
                                                 hbId,
                                                 resourceNode,
                                                 getBrowser());
                    } else if (newRA != null && newRA.isIPaddr()) {
                        newSi = new IPaddrInfo(serviceName,
                                               newRA,
                                               hbId,
                                               resourceNode,
                                               getBrowser());
                    } else if (newRA != null && newRA.isVirtualDomain()) {
                        newSi = new VirtualDomainInfo(serviceName,
                                                      newRA,
                                                      hbId,
                                                      resourceNode,
                                                      getBrowser());
                    } else {
                        newSi = new ServiceInfo(serviceName,
                                                newRA,
                                                hbId,
                                                resourceNode,
                                                getBrowser());
                    }
                    newSi.getService().setHeartbeatId(hbId);
                    getBrowser().addToHeartbeatIdList(newSi);
                    final Point2D p = null;
                    if (newGi != null) {
                        newGi.addGroupServicePanel(newSi, false);
                    } else if (newCi != null) {
                        newCi.addCloneServicePanel(newSi);
                    } else {
                        addServicePanel(newSi, p, false, false, testOnly);
                    }
                } else {
                    getBrowser().addNameToServiceInfoHash(newSi);
                    setParametersHash.put(newSi, resourceNode);
                }
                newSi.getService().setNew(false);
                serviceIsPresent.add(newSi);
                if (newGi != null || newCi != null) {
                    groupServiceIsPresent.add(newSi);
                }
            }
            final DefaultMutableTreeNode n = newSi.getNode();
            if (n != null) {
                final int p = pos;
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        final DefaultMutableTreeNode parent =
                                        (DefaultMutableTreeNode) n.getParent();
                        if (parent != null) {
                            final int i = parent.getIndex(n);
                            if (i > p) {
                                parent.remove(n);
                                parent.insert(n, p);
                                getBrowser().reload(parent, false);
                            }
                        }
                    }
                });
                pos++;
            }
        }

        for (final ServiceInfo newSi : setParametersHash.keySet()) {
            newSi.setParameters(setParametersHash.get(newSi));
            if (!testOnly) {
                newSi.setUpdated(false);
            }
        }
        if (newService) {
            getBrowser().reload(getBrowser().getServicesNode(), false);
        }
        hg.repaint();
    }

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public void setAllResources(final ClusterStatus clStatus,
                                final boolean testOnly) {
        if (clStatus == null) {
            return;
        }
        final Set<String> allGroupsAndClones = clStatus.getAllGroups();
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final List<ServiceInfo> groupServiceIsPresent =
                                                  new ArrayList<ServiceInfo>();
        final List<ServiceInfo> serviceIsPresent = new ArrayList<ServiceInfo>();
        for (final String groupOrClone : allGroupsAndClones) {
            CloneInfo newCi = null;
            GroupInfo newGi = null;
            if (clStatus.isClone(groupOrClone)) {
                /* clone */
                newCi = setCreateCloneInfo(groupOrClone, clStatus, testOnly);
                serviceIsPresent.add(newCi);
            } else if (!"none".equals(groupOrClone)) {
                /* group */
                final GroupInfo gi =
                         (GroupInfo) getBrowser().getServiceInfoFromCRMId(
                                                                 groupOrClone);
                if (gi != null && gi.getCloneInfo() != null) {
                    /* cloned group is already done */
                    groupServiceIsPresent.add(gi);
                    continue;
                }
                newGi = setCreateGroupInfo(groupOrClone,
                                           newCi,
                                           clStatus,
                                           testOnly);
                serviceIsPresent.add(newGi);
            }
            setGroupResources(allGroupsAndClones,
                              groupOrClone,
                              newGi,
                              newCi,
                              serviceIsPresent,
                              groupServiceIsPresent,
                              clStatus,
                              testOnly);
        }

        hg.clearKeepColocationList();
        hg.clearKeepOrderList();
        /* resource sets */
        final List<CRMXML.RscSetConnectionData> rscSetConnections =
                                       clStatus.getRscSetConnections();
        if (rscSetConnections != null) {
            final Map<CRMXML.RscSetConnectionData, ConstraintPHInfo>
             rdataToCphi =
                         new LinkedHashMap<CRMXML.RscSetConnectionData,
                                           ConstraintPHInfo>();
            getBrowser().lockNameToServiceInfo();
            final Map<String, ServiceInfo> idToInfoHash =
                 getBrowser().getNameToServiceInfoHash(
                                                ConstraintPHInfo.NAME);
            final List<ConstraintPHInfo> preNewCphis =
                                    new ArrayList<ConstraintPHInfo>();
            if (idToInfoHash != null) {
                for (final String id : idToInfoHash.keySet()) {
                    final ConstraintPHInfo cphi =
                               (ConstraintPHInfo) idToInfoHash.get(id);
                    final CRMXML.RscSetConnectionData rdataOrd =
                                    cphi.getRscSetConnectionDataOrd();
                    final CRMXML.RscSetConnectionData rdataCol =
                                    cphi.getRscSetConnectionDataCol();
                    if (cphi.getService().isNew()) {
                        preNewCphis.add(cphi);
                    }
                    if (rdataOrd != null && !rdataOrd.isEmpty()) {
                        rdataToCphi.put(rdataOrd, cphi);
                    }
                    if (rdataCol != null && !rdataCol.isEmpty()) {
                        rdataToCphi.put(rdataCol, cphi);
                    }
                }
            }
            getBrowser().unlockNameToServiceInfo();
            final List<ConstraintPHInfo> newCphis =
                                    new ArrayList<ConstraintPHInfo>();
            for (final CRMXML.RscSetConnectionData rdata
                                                : rscSetConnections) {
                ConstraintPHInfo cphi = null;
                PcmkRscSetsInfo prsi = null;

                for (final CRMXML.RscSetConnectionData ordata
                                              : rdataToCphi.keySet()) {
                    if (ordata == rdata) {
                        continue;
                    }
                    if (rdata.equals(ordata)
                        || rdata.equalsReversed(ordata)) {
                        cphi = rdataToCphi.get(ordata);
                        cphi.setRscSetConnectionData(rdata);
                        break;
                    }
                }
                if (cphi == null) {
                    for (final CRMXML.RscSetConnectionData ordata
                                              : rdataToCphi.keySet()) {
                        if (ordata == rdata) {
                            cphi = rdataToCphi.get(ordata);
                            break;
                        }
                        if (rdataToCphi.get(ordata).sameConstraintId(
                                                              rdata)) {
                            /* use the same rsc set info object */
                            prsi = rdataToCphi.get(
                                         ordata).getPcmkRscSetsInfo();
                        }
                        if (rdataToCphi.get(
                                           ordata).getService().isNew()
                            || (rdata.samePlaceholder(ordata)
                                && rdataToCphi.get(
                                             ordata).sameConstraintId(
                                                             rdata))) {
                            cphi = rdataToCphi.get(ordata);
                            cphi.setRscSetConnectionData(rdata);
                            prsi = cphi.getPcmkRscSetsInfo();
                            if (prsi != null) {
                                if (rdata.isColocation()) {
                                    prsi.addColocation(
                                               rdata.getConstraintId(),
                                               cphi);
                                } else {
                                    prsi.addOrder(
                                               rdata.getConstraintId(),
                                               cphi);
                                }
                            }
                            break;
                        }
                    }
                }
                if (cphi == null && !preNewCphis.isEmpty()) {
                    /* placeholder */
                    cphi = preNewCphis.remove(0);
                    rdataToCphi.put(rdata, cphi);
                    cphi.setRscSetConnectionData(rdata);
                }
                if (cphi == null) {
                    cphi = new ConstraintPHInfo(
                                            getBrowser(),
                                            rdata,
                                            ConstraintPHInfo.Preference.AND);
                    if (prsi == null) {
                        prsi = new PcmkRscSetsInfo(getBrowser());
                    }
                    if (rdata.isColocation()) {
                        prsi.addColocation(rdata.getConstraintId(),
                                           cphi);
                    } else {
                        prsi.addOrder(rdata.getConstraintId(), cphi);
                    }
                    cphi.setPcmkRscSetsInfo(prsi);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    newCphis.add(cphi); /* have to add it later,
                                           so that ids are correct. */
                    rdataToCphi.put(rdata, cphi);
                }
                serviceIsPresent.add(cphi);

                final CRMXML.RscSet rscSet1 = rdata.getRscSet1();
                final CRMXML.RscSet rscSet2 = rdata.getRscSet2();
                if (rdata.isColocation()) {
                    /* colocation */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addColocation(rdata.getConstraintId(),
                                             cphi,
                                             si);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addColocation(rdata.getConstraintId(),
                                             si,
                                             cphi);
                        }
                    }
                } else {
                    /* order */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addOrder(rdata.getConstraintId(),
                                        si,
                                        cphi);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addOrder(rdata.getConstraintId(),
                                        cphi,
                                        si);
                        }
                    }
                }
                if (!testOnly && cphi != null) {
                    cphi.setUpdated(false);
                    cphi.getService().setNew(false);
                }
            }

            for (final ConstraintPHInfo cphi : newCphis) {
                hg.addConstraintPlaceholder(cphi,
                                            null, /* pos */
                                            false);
            }
        }

        /* colocations */
        final Map<String, List<CRMXML.ColocationData>> colocationMap =
                                        clStatus.getColocationRscMap();
        for (final String rscId : colocationMap.keySet()) {
            final List<CRMXML.ColocationData> withs =
                                              colocationMap.get(rscId);
            for (final CRMXML.ColocationData data : withs) {
                final String withRscId = data.getWithRsc();
                final ServiceInfo withSi =
                      getBrowser().getServiceInfoFromCRMId(withRscId);
                final ServiceInfo siP =
                           getBrowser().getServiceInfoFromCRMId(rscId);
                hg.addColocation(data.getId(), siP, withSi);
            }
        }

        /* orders */
        final Map<String, List<CRMXML.OrderData>> orderMap =
                                              clStatus.getOrderRscMap();
        for (final String rscFirstId : orderMap.keySet()) {
            for (final CRMXML.OrderData data
                                         : orderMap.get(rscFirstId)) {
                final String rscThenId = data.getRscThen();
                final ServiceInfo si =
                        getBrowser().getServiceInfoFromCRMId(rscThenId);
                if (si != null) { /* not yet complete */
                    final ServiceInfo siP =
                      getBrowser().getServiceInfoFromCRMId(rscFirstId);
                    if (siP != null && siP.getResourceAgent() != null) {
                        /* dangling orders and colocations */
                        if ((siP.getResourceAgent().isDrbddisk()
                             || siP.getResourceAgent().isLinbitDrbd())
                            && si.getName().equals("Filesystem")) {
                            final List<CRMXML.ColocationData> cds =
                               clStatus.getColocationDatas(rscFirstId);
                            if (cds != null) {
                                for (final CRMXML.ColocationData cd
                                                               : cds) {
                                    if (cd.getWithRsc().equals(
                                                         rscThenId)) {
                                        setFilesystemWithDrbd(siP, si);
                                    }
                                }
                            }
                        }
                        hg.addOrder(data.getId(), siP, si);
                    }
                }
            }
        }

        final Enumeration e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                      (DefaultMutableTreeNode) e.nextElement();
            final ServiceInfo g = (ServiceInfo) n.getUserObject();
            if (g.getResourceAgent().isGroup()
                || g.getResourceAgent().isClone()) {
                final Enumeration ge = g.getNode().children();
                while (ge.hasMoreElements()) {
                    final DefaultMutableTreeNode gn =
                             (DefaultMutableTreeNode) ge.nextElement();
                    final ServiceInfo s =
                                     (ServiceInfo) gn.getUserObject();
                    if (!groupServiceIsPresent.contains(s)
                        && !s.getService().isNew()) {
                        /* remove the group service from the menu
                           that does not exist anymore. */
                        s.removeInfo();
                    } else if (!testOnly) {
                        s.updateMenus(null);
                    }
                }
            }
            g.updateMenus(null);
        }
        hg.setServiceIsPresentList(serviceIsPresent);
        /** Set placeholders to "new", if they have no connections. */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hg.killRemovedEdges();
                final Map<String, ServiceInfo> idToInfoHash =
                   getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
                if (idToInfoHash != null) {
                    for (final String id : idToInfoHash.keySet()) {
                        final ConstraintPHInfo cphi =
                                       (ConstraintPHInfo) idToInfoHash.get(id);
                        if (!cphi.getService().isNew() && cphi.isEmpty()) {
                            cphi.getService().setNew(true);
                        }
                    }
                }
                hg.killRemovedVertices();
                hg.scale();
            }
        });
    }

    /** Clears the info panel cache, forcing it to reload. */
    @Override
    boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns type of the info text. text/plain or text/html. */
    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns info for info panel, that hb status failed or null, in which
     * case the getInfoPanel() function will show.
     */
    @Override
    String getInfo() {
        if (getBrowser().clStatusFailed()) {
            return Tools.getString("ClusterBrowser.ClStatusFailed");
        }
        return null;
    }

    /** Creates rsc_defaults panel. */
    private void addRscDefaultsPanel(final JPanel optionsPanel,
                                     final int leftWidth,
                                     final int rightWidth) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.widgetClear();
        final String[] params = rdi.getParametersFromXML();
        rdi.addParams(optionsPanel,
                      params,
                      leftWidth,
                      rightWidth,
                      null);
    }

    /** Returns editable info panel for global crm config. */
    @Override
    public JComponent getInfoPanel() {
        /* if don't have hb status we don't have all the info we need here.
         * TODO: OR we need to get hb status only once
         */
        if (getBrowser().clStatusFailed()) {
            return super.getInfoPanel();
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (infoPanel != null) {
            hg.pickBackground();
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        if (getBrowser().getCRMXML() == null) {
            return newPanel;
        }
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
                hg.stopTestAnimation(getApplyButton());
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
                hg.startTestAnimation(getApplyButton(), startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus clStatus = getBrowser().getClusterStatus();
                clStatus.setPtestData(null);
                apply(dcHost, true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                getApplyButton().setToolTipText(ptestData.getToolTip());
                clStatus.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initCommitButton(buttonCallback);
        getBrowser().getRscDefaultsInfo().setApplyButton(getApplyButton());
        getBrowser().getRscDefaultsInfo().setRevertButton(getRevertButton());
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);

        newPanel.add(buttonPanel);

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        addRscDefaultsPanel(
                      optionsPanel,
                      Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"));
        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), false);
                                getBrowser().clStatusUnlock();
                            }
                        }
                    );
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

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setApplyButtons(null, params);
            }
        });

        mainPanel.add(optionsPanel);

        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));

        hg.pickBackground();
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Returns heartbeat graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /**
     * Adds service to the list of services.
     * TODO: are they both used?
     */
    ServiceInfo addServicePanel(final ResourceAgent newRA,
                                final Point2D pos,
                                final boolean reloadNode,
                                final String heartbeatId,
                                final CloneInfo newCi,
                                final boolean testOnly) {
        ServiceInfo newServiceInfo;
        final String name = newRA.getName();
        if (newRA.isFilesystem()) {
            newServiceInfo = new FilesystemInfo(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = new LinbitDrbdInfo(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = new DrbddiskInfo(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = new IPaddrInfo(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = new VirtualDomainInfo(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            newServiceInfo = new GroupInfo(newRA, getBrowser());
        } else if (newRA.isClone()) {
            final boolean master =
                         getBrowser().getClusterStatus().isMaster(heartbeatId);
            String cloneName;
            if (master) {
                cloneName = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = ConfigData.PM_CLONE_SET_NAME;
            }
            newServiceInfo = new CloneInfo(newRA,
                                           cloneName,
                                           master,
                                           getBrowser());
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        if (heartbeatId != null) {
            newServiceInfo.getService().setHeartbeatId(heartbeatId);
            getBrowser().addToHeartbeatIdList(newServiceInfo);
        }
        if (newCi == null) {
            addServicePanel(newServiceInfo,
                            pos,
                            reloadNode,
                            true,
                            testOnly);
        }
        return newServiceInfo;
    }

    /**
     * Adds new service to the specified position. If position is null, it
     * will be computed later. reloadNode specifies if the node in
     * the menu should be reloaded and get uptodate.
     */
    void addServicePanel(final ServiceInfo newServiceInfo,
                         final Point2D pos,
                         final boolean reloadNode,
                         final boolean interactive,
                         final boolean testOnly) {
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (!hg.addResource(newServiceInfo,
                            null,
                            pos,
                            false, /* colocation only */
                            false, /* order only */
                            testOnly)) {
            getBrowser().addNameToServiceInfoHash(newServiceInfo);
            final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
            newServiceInfo.setNode(newServiceNode);
            getBrowser().getServicesNode().add(newServiceNode);
            if (interactive) {
                if (newServiceInfo.getResourceAgent().isProbablyMasterSlave()) {
                    /* only if it was added manually. */
                    newServiceInfo.changeType(
                                        ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                } else if (
                        newServiceInfo.getResourceAgent().isProbablyClone()) {
                    newServiceInfo.changeType(
                                        ServiceInfo.CLONE_TYPE_STRING);
                }
            }
            if (reloadNode) {
                /* show it */
                getBrowser().reload(getBrowser().getServicesNode(), false);
                getBrowser().reload(newServiceNode, true);
            }
            getBrowser().reloadAllComboBoxes(newServiceInfo);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hg.scale();
                }
            });
        }
        hg.reloadServiceMenus();
    }

    /** Returns 'add service' list for graph popup menu. */
    List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;

        /* add group */
        final MyMenuItem addGroupMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"),
                           null,
                           null,
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          false),
                           new AccessMode(ConfigData.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    addServicePanel(getBrowser().getCRMXML().getHbGroup(),
                                    getPos(),
                                    true,
                                    null,
                                    null,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
        items.add((UpdatableItem) addGroupMenuItem);
        final ServicesInfo thisClass = this;

        /* add service */
        final MyMenu addServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddService"),
                        new AccessMode(ConfigData.AccessType.OP,
                                       false),
                        new AccessMode(ConfigData.AccessType.OP,
                                       false)) {
            private static final long serialVersionUID = 1L;
            private final Lock mUpdateLock = new ReentrantLock();

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void update() {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mUpdateLock.tryLock()) {
                            try {
                                updateThread();
                            } finally {
                                mUpdateLock.unlock();
                            }
                        }
                    }
                });
                t.start();
            }
            private void updateThread() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setEnabled(false);
                    }
                });
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                    }
                });
                Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                final ResourceAgent fsService = crmXML.getResourceAgent(
                                        "Filesystem",
                                        ResourceAgent.HEARTBEAT_PROVIDER,
                                        ResourceAgent.OCF_CLASS);
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it,
                                                       if it is not */
                    final ResourceAgent linbitDrbdService =
                                                   crmXML.getHbLinbitDrbd();
                    final MyMenuItem ldMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                     null,
                     null,
                     new AccessMode(ConfigData.AccessType.ADMIN,
                                    false),
                     new AccessMode(ConfigData.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            hidePopup();
                            if (!getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(false);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent()) {
                        ldMenuItem.setEnabled(false);
                    }
                    ldMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            add(ldMenuItem);
                        }
                    });
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ResourceAgent.HEARTBEAT_PROVIDER,
                                         ResourceAgent.OCF_CLASS);
                if (ipService != null) { /* just skip it, if it is not*/
                    final MyMenuItem ipMenuItem =
                         new MyMenuItem(ipService.getMenuName(),
                                        null,
                                        null,
                                        new AccessMode(
                                                  ConfigData.AccessType.ADMIN,
                                                  false),
                                        new AccessMode(ConfigData.AccessType.OP,
                                                       false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            hidePopup();
                            addServicePanel(ipService,
                                            getPos(),
                                            true,
                                            null,
                                            null,
                                            testOnly);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    ipMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            add(ipMenuItem);
                        }
                    });
                }
                if (crmXML.isDrbddiskPresent()
                    && (getBrowser().isDrbddiskPreferred()
                        || getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent())) {
                    final ResourceAgent drbddiskService =
                                                crmXML.getHbDrbddisk();
                    final MyMenuItem ddMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                     null,
                     null,
                     new AccessMode(ConfigData.AccessType.ADMIN,
                                    false),
                     new AccessMode(ConfigData.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            hidePopup();
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(true);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().isOneLinbitDrbd()
                        || !crmXML.isDrbddiskPresent()) {
                        ddMenuItem.setEnabled(false);
                    }
                    ddMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            add(ddMenuItem);
                        }
                    });
                }
                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final List<ResourceAgent> services = getAddServiceList(cl);
                    if (services.size() == 0) {
                        /* no services, don't show */
                        continue;
                    }
                    boolean mode = !AccessMode.ADVANCED;
                    if (ResourceAgent.UPSTART_CLASS.equals(cl)
                        || ResourceAgent.SYSTEMD_CLASS.equals(cl)) {
                        mode = AccessMode.ADVANCED;
                    }
                    if (ResourceAgent.LSB_CLASS.equals(cl)
                        && !getAddServiceList(
                                    ResourceAgent.SERVICE_CLASS).isEmpty()) {
                        mode = AccessMode.ADVANCED;
                    }
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.getClassMenu(cl),
                                       new AccessMode(
                                                   ConfigData.AccessType.ADMIN,
                                                   mode),
                                       new AccessMode(ConfigData.AccessType.OP,
                                                      mode));
                    MyListModel dlm = new MyListModel();
                    for (final ResourceAgent ra : services) {
                        final MyMenuItem mmi =
                                new MyMenuItem(ra.getMenuName(),
                                               null,
                                               null,
                                               new AccessMode(
                                                    ConfigData.AccessType.ADMIN,
                                                    false),
                                               new AccessMode(
                                                    ConfigData.AccessType.OP,
                                                    false)) {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void action() {
                                hidePopup();
                                for (final JDialog otherP : popups) {
                                    otherP.dispose();
                                }
                                if (ra.isLinbitDrbd()
                                    &&
                                     !getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                } else if (ra.isHbDrbd()
                                    &&
                                     !getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                addServicePanel(ra,
                                                getPos(),
                                                true,
                                                null,
                                                null,
                                                testOnly);
                                getBrowser().getHeartbeatGraph().repaint();
                            }
                        };
                        mmi.setPos(pos);
                        dlm.addElement(mmi);
                    }
                    final boolean ret = Tools.getScrollingMenu(
                                        ClusterBrowser.getClassMenu(cl),
                                        null, /* options */
                                        classItem,
                                        dlm,
                                        new MyList(dlm, getBackground()),
                                        thisClass,
                                        popups,
                                        null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            add(classItem);
                        }
                    });
                }
                super.update();
            }
        };
        items.add((UpdatableItem) addServiceMenuItem);

        /* add constraint placeholder (and) */
        final MyMenuItem addConstraintPlaceholderAnd =
            new MyMenuItem(Tools.getString(
                                 "ServicesInfo.AddConstraintPlaceholderAnd"),
                           null,
                           Tools.getString(
                            "ServicesInfo.AddConstraintPlaceholderAnd.ToolTip"),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          false),
                           new AccessMode(ConfigData.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
                    final ConstraintPHInfo cphi =
                         new ConstraintPHInfo(getBrowser(),
                                              null,
                                              ConstraintPHInfo.Preference.AND);
                    cphi.getService().setNew(true);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    hg.addConstraintPlaceholder(cphi, getPos(), testOnly);
                    final PcmkRscSetsInfo prsi =
                                      new PcmkRscSetsInfo(getBrowser(), cphi);
                    cphi.setPcmkRscSetsInfo(prsi);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hg.scale();
                        }
                    });
                }
            };
        items.add((UpdatableItem) addConstraintPlaceholderAnd);

        /* add constraint placeholder (or) */
        final MyMenuItem addConstraintPlaceholderOr =
            new MyMenuItem(Tools.getString(
                                 "ServicesInfo.AddConstraintPlaceholderOr"),
                           null,
                           Tools.getString(
                            "ServicesInfo.AddConstraintPlaceholderOr.ToolTip"),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          false),
                           new AccessMode(ConfigData.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final String pmV =
                                getBrowser().getDCHost().getPacemakerVersion();
                    try {
                        //TODO: get this from constraints-.rng files
                        if (pmV == null
                            || Tools.compareVersions(pmV, "1.1.7") <= 0) {
                            return HbOrderInfo.NOT_AVAIL_FOR_PCMK_VERSION;
                        }
                    } catch (Exceptions.IllegalVersionException e) {
                        Tools.appWarning("unkonwn version: " + pmV);
                        /* enable it, if version check doesn't work */
                    }
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
                    final ConstraintPHInfo cphi =
                         new ConstraintPHInfo(getBrowser(),
                                              null,
                                              ConstraintPHInfo.Preference.OR);
                    cphi.getService().setNew(true);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    hg.addConstraintPlaceholder(cphi, getPos(), testOnly);
                    final PcmkRscSetsInfo prsi =
                                      new PcmkRscSetsInfo(getBrowser(), cphi);
                    cphi.setPcmkRscSetsInfo(prsi);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hg.scale();
                        }
                    });
                }
            };
        items.add((UpdatableItem) addConstraintPlaceholderOr);

        /* stop all services. */
        final MyMenuItem stopAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.StopAllServices"),
                ServiceInfo.STOP_ICON,
                new AccessMode(ConfigData.AccessType.ADMIN, true),
                new AccessMode(ConfigData.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (!si.isStopped(false) && !si.getService().isOrphaned()) {
                        return null;
                    }
                }
                return "all services are stopped";
            }

            @Override
            public void action() {
                hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null
                        && !si.isStopped(false)
                        && !si.getService().isOrphaned()
                        && !si.getService().isNew()) {
                        si.stopResource(dcHost, false);
                    }
                }
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback stopAllItemCallback =
                   getBrowser().new ClMenuItemCallback(stopAllMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null
                        && !si.isConstraintPH()
                        && !si.isStopped(true)
                        && !si.getService().isOrphaned()
                        && !si.getService().isNew()) {
                        si.stopResource(thisDCHost, true); /* test only */
                    }
                }
            }
        };
        addMouseOverListener(stopAllMenuItem, stopAllItemCallback);
        items.add((UpdatableItem) stopAllMenuItem);

        /* unmigrate all services. */
        final MyMenuItem unmigrateAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.UnmigrateAllServices"),
                ServiceInfo.UNMIGRATE_ICON,
                new AccessMode(ConfigData.AccessType.OP, false),
                new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return enablePredicate() == null;
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (ServiceInfo si
                                 : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        return null;
                    }
                }
                return "nothing to unmigrate";
            }

            @Override
            public void action() {
                hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        si.unmigrateResource(dcHost, false);
                    }
                }
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback unmigrateAllItemCallback =
               getBrowser().new ClMenuItemCallback(unmigrateAllMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        si.unmigrateResource(dcHost, true); /* test only */
                    }
                }
            }
        };
        addMouseOverListener(unmigrateAllMenuItem, unmigrateAllItemCallback);
        items.add((UpdatableItem) unmigrateAllMenuItem);

        /* remove all services. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.RemoveAllServices"),
                ClusterBrowser.REMOVE_ICON,
                new AccessMode(ConfigData.AccessType.ADMIN, true),
                new AccessMode(ConfigData.AccessType.ADMIN, true)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null) {
                        if (si.isRunning(false)) {
                            return "there are running services";
                        }
                    }
                }
                return null;
            }

            @Override
            public void action() {
                hidePopup();
                if (Tools.confirmDialog(
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Title"),
                     Tools.getString(
                     "ClusterBrowser.confirmRemoveAllServices.Description"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Yes"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.No"))) {
                    final Thread t = new Thread() {
                        public void run() {
                            final Host dcHost = getBrowser().getDCHost();
                            List<ServiceInfo> services =
                                    getBrowser().getExistingServiceList(null);
                            for (ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra =
                                                        si.getResourceAgent();
                                    if (ra != null && !ra.isClone()) {
                                        si.getService().setRemoved(true);
                                    }
                                }
                            }
                            CRM.erase(dcHost, testOnly);
                            for (ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra =
                                                        si.getResourceAgent();
                                    if (si.getService().isNew()) {
                                        si.removeMyself(testOnly);
                                    } else if (ra != null && !ra.isClone()) {
                                        si.cleanupResource(dcHost, false);
                                    }
                                }
                            }
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    t.start();
                }
            }
        };
        final ClusterBrowser.ClMenuItemCallback removeItemCallback =
               getBrowser().new ClMenuItemCallback(removeMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                CRM.erase(dcHost, true); /* test only */
            }
        };
        addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);

        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO,
                                          false),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    ClusterLogs l = new ClusterLogs(getBrowser().getCluster());
                    l.showDialog();
                }
            };
        items.add((UpdatableItem) viewLogsItem);
        return items;
    }

    /** Returns units. */
    @Override
    protected Unit[] getUnits() {
        return new Unit[]{
            new Unit("", "s", "Second", "Seconds"), /* default unit */
            new Unit("ms",  "ms", "Millisecond", "Milliseconds"),
            new Unit("us",  "us", "Microsecond", "Microseconds"),
            new Unit("s",   "s",  "Second",      "Seconds"),
            new Unit("min", "m",  "Minute",      "Minutes"),
            new Unit("h",   "h",  "Hour",        "Hours")
        };
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        boolean ret = true;
        if (!rdi.checkResourceFieldsCorrect(param,
                                            rdi.getParametersFromXML(),
                                            true)) {
            ret = false;
        }
        if (!super.checkResourceFieldsCorrect(param, params)) {
            ret = false;
        }
        for (ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (!si.checkResourceFieldsCorrect(null,
                                              si.getParametersFromXML(),
                                              true,
                                              false,
                                              false)) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        boolean changed = false;
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        if (super.checkResourceFieldsChanged(param, params)) {
            changed = true;
        }
        if (rdi.checkResourceFieldsChanged(param,
                                           rdi.getParametersFromXML(),
                                           true)) {
            changed = true;
        }
        for (ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (si.checkResourceFieldsChanged(null,
                                              si.getParametersFromXML(),
                                              true,
                                              false,
                                              false)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.revert();
        for (ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (si.checkResourceFieldsChanged(null,
                                              si.getParametersFromXML(),
                                              true,
                                              false,
                                              false)) {
                si.revert();
            }
        }
        //TODO: should remove new resources and constraints
    }

    /**
     * Copy/paste field from one field to another.
     */
    private void copyPasteField(final Widget oldWi, final Widget newWi) {
        if (newWi == null || oldWi == null) {
            return;
        }
        final String oldValue = oldWi.getStringValue();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if ("".equals(oldValue)) {
                    newWi.setValueNoListeners(null);
                } else {
                    newWi.setValueNoListeners(oldValue);
                }
            }
        });
    }

    private void copyPasteFields(final ServiceInfo oldSi,
                                 final ServiceInfo newSi) {
        /* parameters */
        for (final String param : oldSi.getParametersFromXML()) {
            if (ServiceInfo.GUI_ID.equals(param)
                || ServiceInfo.PCMK_ID.equals(param)) {
                if (getBrowser().isCRMId(oldSi.getService().getHeartbeatId())) {
                    continue;
                }
            }
            copyPasteField(oldSi.getWidget(param, null),
                           newSi.getWidget(param, null));
        }

        /* operations */
        copyPasteField(oldSi.getSameAsOperationsWi(),
                       newSi.getSameAsOperationsWi());

        for (final String op : oldSi.getResourceAgent().getOperationNames()) {
            for (final String param
                          : getBrowser().getCRMOperationParams(op)) {
                copyPasteField(oldSi.getOperationsComboBox(op, param),
                               newSi.getOperationsComboBox(op, param));
            }
        }

        /* locations */
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            copyPasteField(oldSi.getScoreComboBoxHash().get(hi),
                           newSi.getScoreComboBoxHash().get(hi));
        }
        /* ping */
        copyPasteField(oldSi.getPingComboBox(),
                       newSi.getPingComboBox());
    }

    public void pasteServices(final List<Info> oldInfos) {
        if (oldInfos.size() == 0) {
            return;
        }
        final String cn = getBrowser().getCluster().getName();
        Tools.startProgressIndicator(cn, "paste");
        final ClusterBrowser otherBrowser =
                                (ClusterBrowser) oldInfos.get(0).getBrowser();
        getBrowser().getClusterViewPanel().setDisabledDuringLoad(true);
        otherBrowser.getClusterViewPanel().setDisabledDuringLoad(true);
        for (Info oldI : oldInfos) {
            CloneInfo oci = null;
            if (oldI instanceof CloneInfo) {
                oci = (CloneInfo) oldI;
                oldI = oci.getContainedService();
            }
            final CloneInfo oldCi = oci;
            if (oldI instanceof ServiceInfo) {
                final ServiceInfo oldSi = (ServiceInfo) oldI;
                final ServiceInfo newSi =
                    addServicePanel(oldSi.getResourceAgent(),
                                    null, /* pos */
                                    true,
                                    null, /* clone id */
                                    null,
                                    CRM.LIVE);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!(newSi instanceof CloneInfo)) {
                            oldSi.getInfoPanel();
                            newSi.getInfoPanel();
                            oldSi.waitForInfoPanel();
                            newSi.waitForInfoPanel();
                        }
                        if (oldCi != null) {
                            final CloneInfo oci = oldCi;
                            final String v =
                                    newSi.getTypeRadioGroup().getStringValue();
                            if (oci.getService().isMaster()) {
                                if (!ServiceInfo.MASTER_SLAVE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(
                                         ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                                }
                            } else {
                                if (!ServiceInfo.CLONE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(
                                                ServiceInfo.CLONE_TYPE_STRING);
                                }
                            }
                        }
                        copyPasteFields(oldSi, newSi);
                    }
                });

                /* clone parameters */
                final CloneInfo newCi = newSi.getCloneInfo();
                if (newCi != null) {
                    final CloneInfo oldCi0 = oldCi;
                    for (final String param : oldCi0.getParametersFromXML()) {
                        if (ServiceInfo.GUI_ID.equals(param)
                            || ServiceInfo.PCMK_ID.equals(param)) {
                            if (getBrowser().isCRMId(
                                    oldCi0.getService().getHeartbeatId())) {
                                continue;
                            }
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                copyPasteField(oldCi0.getWidget(param, null),
                                               newCi.getWidget(param, null));
                            }
                        });
                    }
                }
                if (oldI instanceof GroupInfo) {
                    final GroupInfo oldGi = (GroupInfo) oldI;
                    final GroupInfo newGi = (GroupInfo) newSi;

                    final Enumeration e = oldGi.getNode().children();
                    while (e.hasMoreElements()) {
                        final DefaultMutableTreeNode n =
                                      (DefaultMutableTreeNode) e.nextElement();
                        final ServiceInfo oldChild =
                                            (ServiceInfo) n.getUserObject();
                        oldChild.getInfoPanel();
                        oldChild.waitForInfoPanel();
                        final ServiceInfo newChild =
                                              newGi.addGroupServicePanel(
                                                    oldChild.getResourceAgent(),
                                                    false);
                        newChild.getInfoPanel();
                        newChild.waitForInfoPanel();
                        Tools.waitForSwing();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                copyPasteFields(oldChild, newChild);
                            }
                        });
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getBrowser().reload(newGi.getNode(), false);
                        }
                    });
                }
            }
        }
        Tools.stopProgressIndicator(cn, "paste");
        otherBrowser.getClusterViewPanel().setDisabledDuringLoad(false);
        getBrowser().getClusterViewPanel().setDisabledDuringLoad(false);
    }
}
