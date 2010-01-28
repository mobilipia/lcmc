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

package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.data.Host;
import drbd.data.CRMXML;
import drbd.data.ClusterStatus;
import drbd.data.resources.Service;
import drbd.utilities.CRM;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Object that holds an order constraint information.
 */
public class HbOrderInfo extends EditableInfo
                         implements HbConstraintInterface {
    /** Parent resource in order constraint. */
    private final ServiceInfo serviceInfoParent;
    /** Child resource in order constraint. */
    private final ServiceInfo serviceInfoChild;
    /** Connection that keeps this constraint. */
    private final HbConnectionInfo connectionInfo;

    /**
     * Prepares a new <code>HbOrderInfo</code> object.
     */
    public HbOrderInfo(final HbConnectionInfo connectionInfo,
                       final ServiceInfo serviceInfoParent,
                       final ServiceInfo serviceInfoChild,
                       final Browser browser) {
        super("Order", browser);
        setResource(new Service("Order"));
        this.connectionInfo = connectionInfo;
        this.serviceInfoParent = serviceInfoParent;
        this.serviceInfoChild = serviceInfoChild;
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }


    /**
     * Sets the order's parameters.
     */
    public final void setParameters() {
        final String rscParent =
                        serviceInfoParent.getService().getHeartbeatId();
        final String rscChild = serviceInfoChild.getService().getHeartbeatId();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String score = clStatus.getOrderScore(rscParent, rscChild);
        final String symmetrical = clStatus.getOrderSymmetrical(rscParent,
                                                                rscChild);
        final String firstAction = clStatus.getOrderFirstAction(rscParent,
                                                                rscChild);
        final String thenAction = clStatus.getOrderThenAction(rscParent,
                                                              rscChild);
        final String ordId = clStatus.getOrderId(rscParent,
                                                 rscChild);
        final Map<String, String> resourceNode = new HashMap<String, String>();
        resourceNode.put(CRMXML.SCORE_STRING, score);
        resourceNode.put("symmetrical", symmetrical);
        resourceNode.put("first-action", firstAction);
        resourceNode.put("then-action", thenAction);

        final String[] params = getBrowser().getCRMXML().getOrderParameters();
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
                }
            }
        }
    }
    /**
     * Returns that this is order constraint.
     */
    public final boolean isOrder() {
        return true;
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    protected final String getParamLongDesc(final String param) {
        final String text =
                        getBrowser().getCRMXML().getOrderParamLongDesc(param);
        return text.replaceAll("@FIRST-RSC@", serviceInfoParent.toString())
                   .replaceAll("@THEN-RSC@", serviceInfoChild.toString());
    }

    /**
     * Returns short description of the parameter, that is used as * label.
     */
    protected final String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getOrderParamShortDesc(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        return getBrowser().getCRMXML().checkOrderParam(param, newValue);
    }

    /**
     * Returns default for this parameter.
     */
    protected final String getParamDefault(final String param) {
        return getBrowser().getCRMXML().getOrderParamDefault(param);
    }

    /**
     * Returns preferred value for this parameter.
     */
    protected final String getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getOrderParamPreferred(param);
    }

    /**
     * Returns lsit of all parameters as an array.
     */
    public final String[] getParametersFromXML() {
        return getBrowser().getCRMXML().getOrderParameters();
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        if ("first-action".equals(param)) {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(
                                param,
                                serviceInfoParent.getService().isMaster());
        } else if ("then-action".equals(param)) {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(
                                param,
                                serviceInfoChild.getService().isMaster());
        } else {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(param,
                                                                         false);
        }
    }

    /**
     * Returns parameter type, boolean etc.
     */
    protected final String getParamType(final String param) {
        return getBrowser().getCRMXML().getOrderParamType(param);
    }

    /**
     * Returns section to which the global belongs.
     */
    protected final String getSection(final String param) {
        return getBrowser().getCRMXML().getOrderSection(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isOrderBoolean(param);
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isOrderTimeType(param);
    }

    /**
     * Returns true if the specified parameter is integer.
     */
    protected final boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isOrderInteger(param);
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isOrderRequired(param);
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        return connectionInfo.checkResourceFields(param, null);
    }

    /**
     * Applies changes to the order parameters.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = false;
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (!value.equals(getParamSaved(param))) {
                changed = true;
            }
            if (!value.equals(getParamDefault(param))) {
                attrs.put(param, value);
            }
        }
        if (changed) {
            CRM.addOrder(dcHost,
                         getService().getHeartbeatId(),
                         serviceInfoParent.getHeartbeatId(testOnly),
                         serviceInfoChild.getHeartbeatId(testOnly),
                         attrs,
                         testOnly);
        }
        if (!testOnly) {
            storeComboBoxValues(params);
            checkResourceFields(null, params);
        }
    }

    /**
     * Returns service that belongs to this info object.
     */
    public final Service getService() {
        return (Service) getResource();
    }

    /**
     * Get parent resource in order constraint.
     */
    public final String getRsc1() {
        return serviceInfoParent.toString();
    }

    /**
     * Get child resource in order constraint.
     */
    public final String getRsc2() {
        return serviceInfoChild.toString();
    }
}
