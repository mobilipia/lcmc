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
import lcmc.data.Host;
import lcmc.data.resources.Service;
import lcmc.data.ClusterStatus;
import lcmc.data.CRMXML;
import lcmc.data.ConfigData;
import lcmc.utilities.CRM;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;

/**
 * Object that holds a colocation constraint information.
 */
final class HbColocationInfo extends EditableInfo
                             implements HbConstraintInterface {
    /** Resource 1 in colocation constraint. */
    private ServiceInfo serviceInfoRsc;
    /** Resource 2 in colocation constraint. */
    private ServiceInfo serviceInfoWithRsc;
    /** Connection that keeps this constraint. */
    private final HbConnectionInfo connectionInfo;

    /** Prepares a new <code>HbColocationInfo</code> object. */
    HbColocationInfo(final HbConnectionInfo connectionInfo,
                     final ServiceInfo serviceInfoRsc,
                     final ServiceInfo serviceInfoWithRsc,
                     final Browser browser) {
        super("Colocation", browser);
        setResource(new Service("Colocation"));
        this.connectionInfo = connectionInfo;
        this.serviceInfoRsc = serviceInfoRsc;
        this.serviceInfoWithRsc = serviceInfoWithRsc;
    }

    /** Sets service info of the resource. */
    void setServiceInfoRsc(final ServiceInfo serviceInfoRsc) {
        this.serviceInfoRsc = serviceInfoRsc;
    }

    /** Sets service info of the with-resource. */
    void setServiceInfoWithRsc(final ServiceInfo serviceInfoWithRsc) {
        this.serviceInfoWithRsc = serviceInfoWithRsc;
    }

    /** Returns browser object of this info. */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }


    /** Sets the colocation's parameters. */
    void setParameters() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String colId = getService().getHeartbeatId();
        final Map<String, String> resourceNode = new HashMap<String, String>();

        if (serviceInfoRsc == null
            || serviceInfoWithRsc == null) {
            /* rsc set placeholder */
            final CRMXML.ColocationData colocationData =
                            clStatus.getColocationData(colId);
            final String score = colocationData.getScore();
            resourceNode.put(CRMXML.SCORE_STRING, score);
        } else if (serviceInfoRsc.isConstraintPH()
                   || serviceInfoWithRsc.isConstraintPH()) {
            /* rsc set edge */
            ConstraintPHInfo cphi;
            CRMXML.RscSet rscSet;
            if (serviceInfoRsc.isConstraintPH()) {
                cphi = (ConstraintPHInfo) serviceInfoRsc;
                rscSet = cphi.getRscSetConnectionDataCol().getRscSet1();
            } else {
                cphi = (ConstraintPHInfo) serviceInfoWithRsc;
                rscSet = cphi.getRscSetConnectionDataCol().getRscSet2();
            }
            resourceNode.put("sequential", rscSet.getSequential());
            resourceNode.put("role", rscSet.getColocationRole());
        } else {
            final CRMXML.ColocationData colocationData =
                            clStatus.getColocationData(colId);
            if (colocationData != null) {
                final String rsc = colocationData.getRsc();
                final String withRsc = colocationData.getWithRsc();
                final String score = colocationData.getScore();
                final String rscRole = colocationData.getRscRole();
                final String withRscRole = colocationData.getWithRscRole();

                resourceNode.put(CRMXML.SCORE_STRING, score);
                resourceNode.put("rsc-role", rscRole);
                resourceNode.put("with-rsc-role", withRscRole);
            }
        }


        final String[] params = getParametersFromXML();
        if (params != null) {
            for (String param : params) {
                String value = resourceNode.get(param);
                if (value == null) {
                    value = getParamDefault(param);
                }
                if ("".equals(value)) {
                    value = null;
                }
                final String oldValue = getParamSaved(param);
                if ((value == null && value != oldValue)
                    || (value != null && !value.equals(oldValue))) {
                    getResource().setValue(param, value);
                    final Widget wi = getWidget(param, null);
                    if (wi != null) {
                        wi.setValue(value);
                    }
                }
            }
        }
    }

    /** Returns that this is order constraint. */
    @Override
    public boolean isOrder() {
        return false;
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {

        final String text =
                    getBrowser().getCRMXML().getColocationParamLongDesc(param);
        if (serviceInfoRsc != null && serviceInfoWithRsc != null) {
            return text.replaceAll("@RSC@",
                                   Matcher.quoteReplacement(
                                            serviceInfoRsc.toString()))
                       .replaceAll("@WITH-RSC@",
                                   Matcher.quoteReplacement(
                                            serviceInfoWithRsc.toString()));
        }
        return text;
    }

    /** Returns short description of the parameter, that is used as * label. */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getColocationParamShortDesc(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        return getBrowser().getCRMXML().checkColocationParam(param, newValue);
    }

    /** Returns default for this parameter. */
    @Override
    protected String getParamDefault(final String param) {
        return getBrowser().getCRMXML().getColocationParamDefault(param);
    }

    /** Returns preferred value for this parameter. */
    @Override
    protected String getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getColocationParamPreferred(param);
    }

    /** Returns lsit of all parameters as an array. */
    @Override
    public String[] getParametersFromXML() {
        if (serviceInfoRsc == null
            || serviceInfoWithRsc == null) {
            /* rsc set colocation */
            return getBrowser().getCRMXML().getRscSetColocationParameters();
        } else if (serviceInfoRsc.isConstraintPH()
                   || serviceInfoWithRsc.isConstraintPH()) {
            /* when rsc set edges are clicked */
            return getBrowser().getCRMXML().getRscSetColConnectionParameters();
        } else {
            return getBrowser().getCRMXML().getColocationParameters();
        }
    }

    /** Returns when at least one resource in rsc set can be promoted. */
    private boolean isRscSetMaster() {
        ConstraintPHInfo cphi;
        CRMXML.RscSet rscSet;
        if (serviceInfoRsc.isConstraintPH()) {
            cphi = (ConstraintPHInfo) serviceInfoRsc;
            rscSet = cphi.getRscSetConnectionDataCol().getRscSet1();
        } else {
            cphi = (ConstraintPHInfo) serviceInfoWithRsc;
            rscSet = cphi.getRscSetConnectionDataCol().getRscSet2();
        }
        if (rscSet == null) {
            return false;
        }
        return getBrowser().isOneMaster(rscSet.getRscIds());
    }


    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        if ("role".equals(param)) {
            return getBrowser().getCRMXML().getColocationParamPossibleChoices(
                                param,
                                isRscSetMaster());
        } else if ("with-rsc-role".equals(param)) {
            return getBrowser().getCRMXML().getColocationParamPossibleChoices(
                                param,
                                serviceInfoWithRsc.getService().isMaster());
        } else if ("rsc-role".equals(param)) {
            return getBrowser().getCRMXML().getColocationParamPossibleChoices(
                                param,
                                serviceInfoRsc.getService().isMaster());
        } else {
            return getBrowser().getCRMXML().getColocationParamPossibleChoices(
                                param,
                                false);
        }
    }

    /** Returns parameter type, boolean etc. */
    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCRMXML().getColocationParamType(param);
    }

    /** Returns section to which the global belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getCRMXML().getColocationSection(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isColocationBoolean(param);
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isColocationTimeType(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isColocationInteger(param);
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCRMXML().isColocationLabel(param);
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isColocationRequired(param);
    }

    /** Returns attributes of this colocation. */
    protected Map<String, String> getAttributes() {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = true;
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (!value.equals(getParamSaved(param))) {
                changed = true;
            }
            attrs.put(param, value);
        }
        return attrs;
    }

    /** Applies changes to the colocation parameters. */
    @Override
    public void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = true;
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (!value.equals(getParamSaved(param))) {
                changed = true;
            }
            attrs.put(param, value);
        }
        if (changed) {
            final String colId = getService().getHeartbeatId();
            if (serviceInfoRsc == null || serviceInfoWithRsc == null) {
                /* placeholder */
                final PcmkRscSetsInfo prsi = (PcmkRscSetsInfo) connectionInfo;
                CRM.setRscSet(dcHost,
                              colId,
                              false,
                              null,
                              false,
                              prsi.getAllAttributes(dcHost,
                                                    null,
                                                    null,
                                                    true,
                                                    testOnly),
                              null,
                              attrs,
                              testOnly);
            } else if (serviceInfoRsc.isConstraintPH()
                       || serviceInfoWithRsc.isConstraintPH()) {
                /* edge */
                ConstraintPHInfo cphi;
                CRMXML.RscSet rscSet;
                if (serviceInfoRsc.isConstraintPH()) {
                    cphi = (ConstraintPHInfo) serviceInfoRsc;
                    rscSet = cphi.getRscSetConnectionDataCol().getRscSet1();
                } else {
                    cphi = (ConstraintPHInfo) serviceInfoWithRsc;
                    rscSet = cphi.getRscSetConnectionDataCol().getRscSet2();
                }
                final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();

                CRM.setRscSet(dcHost,
                              colId,
                              false,
                              null,
                              false,
                              prsi.getAllAttributes(dcHost,
                                                    rscSet,
                                                    attrs,
                                                    true,
                                                    testOnly),
                              null,
                              prsi.getColocationAttributes(colId),
                              testOnly);
            } else {
                CRM.addColocation(dcHost,
                                  colId,
                                  serviceInfoRsc.getHeartbeatId(testOnly),
                                  serviceInfoWithRsc.getHeartbeatId(testOnly),
                                  attrs,
                                  testOnly);
            }
        }
        if (!testOnly) {
            storeComboBoxValues(params);
        }
    }

    /** Returns service that belongs to this info object. */
    @Override
    public Service getService() {
        return (Service) getResource();
    }

    /** Returns name of the rsc1 attribute. */
    @Override
    public String getRsc1Name() {
        return "rsc";
    }

    /** Returns name of the rsc2 attribute. */
    @Override
    public String getRsc2Name() {
        return "with-rsc";
    }

    /** Resource 1 in colocation constraint. */
    @Override
    public String getRsc1() {
        return serviceInfoRsc.toString();
    }

    /** Resource 2 in colocation constraint. */
    @Override
    public String getRsc2() {
        return serviceInfoWithRsc.toString();
    }

    /** Resource 1 object in colocation constraint. */
    @Override
    public ServiceInfo getRscInfo1() {
        return serviceInfoRsc;
    }

    /** Resource 2 object in colocation constraint. */
    @Override
    public ServiceInfo getRscInfo2() {
        return serviceInfoWithRsc;
    }

    /** Returns the score of this colocation. */
    int getScore() {
        //final String rsc = serviceInfoRsc.getService().getHeartbeatId();
        //final String withRsc =
        //                  serviceInfoWithRsc.getService().getHeartbeatId();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String colId = getService().getHeartbeatId();
        final CRMXML.ColocationData data = clStatus.getColocationData(colId);
        if (data == null) {
            return 0;
        }
        final String score = data.getScore();
        if (score == null) {
            return 0;
        } else if (CRMXML.INFINITY_STRING.equals(score)
                   || CRMXML.PLUS_INFINITY_STRING.equals(score)) {
            return 1000000;
        } else if (CRMXML.MINUS_INFINITY_STRING.equals(score)) {
            return -1000000;
        }
        return Integer.parseInt(score);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return true;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Returns access type of this parameter. */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
         return false;
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        return checkResourceFieldsCorrect(param, params, false);
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params,
                                              final boolean fromUp) {
        if (fromUp) {
            return super.checkResourceFieldsCorrect(param, params);
        } else {
            return connectionInfo.checkResourceFieldsCorrect(param, null);
        }
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    boolean checkResourceFieldsChanged(final String param,
                                       final String[] params) {
        return checkResourceFieldsChanged(param, params, false);
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params,
                                              final boolean fromUp) {
        if (fromUp) {
            return super.checkResourceFieldsChanged(param, params);
        } else {
            return connectionInfo.checkResourceFieldsChanged(param, null);
        }
    }
}
