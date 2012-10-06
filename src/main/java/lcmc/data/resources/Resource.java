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


package lcmc.data.resources;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * This class holds resource data.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the resource. */
    private String name = null;
    /** Map from parameter name to the saved value. */
    private final Map<String, String> savedValue =
                                                new HashMap<String, String>();
    /** Map from parameter to its possible choices for pulldown menus. */
    private final Map<String, Object[]> possibleChoicesMap =
                                                new HashMap<String, Object[]>();
    /** Map from parameter name to its default value. */
    private final Map<String, String> defaultValueMap =
                                                new HashMap<String, String>();
    /** Map from parameter name to its preferred value. */
    private final Map<String, String> preferredValueMap =
                                                new HashMap<String, String>();

    /** Whether the resource is newly allocated. */
    private boolean newResource = false;
    /**
     * Prepares a new <code>Resource</code> object.
     *
     * If called whith this constructor the name must be set later.
     */
    public Resource() {
        /* name must be set later. */
        //TODO:
    }

    /**
     * Prepares a new <code>Resource</code> object.
     *
     * @param name
     *          name of this resource.
     */
    public Resource(final String name) {
        this.name = name;
    }

    /** Sets value for paramter. */
    public final void setValue(final String param, final String value) {
        savedValue.put(param, value);
    }

    /** Returns value for a parameter. */
    public String getValue(final String param) {
        return savedValue.get(param);
    }

    /** Gets name of this resource. */
    public final String getName() {
        return name;
    }

    /** Sets name of this resource. */
    public final void setName(final String name) {
        this.name = name;
    }

    /** Gets name of this resource. */
    @Override
    public String toString() {
        return name;
    }

    ///**
    // * Not implemented at all
    // */
    //public final String[] getDependentResources() {
    //    return null;
    //}

    /**
     * Creates default values list and returns them as a List object.
     * It uses the selected value as first item in the list if it exists.
     * It removes this value if it is in the values array. If there is selected
     * value it also removes empty string from values array.
     *
     * @param param
     *          parameter for which the default values are created.
     *
     * @param values
     *          values that should be in the list.
     *
     * @return
     *          list of default values.
     */
    protected List<Object> getPossibleChoices(final String param,
                                              final Object[] values) {
        final List<Object> possibleChoices = new ArrayList<Object>();
        if (values == null) {
            return possibleChoices;
        }
        for (int i = 0; i < values.length; i++) {
            possibleChoices.add(values[i]);
        }
        return possibleChoices;
    }

    /** Sets possible choices for parameter combo box. */
    public final void setPossibleChoices(final String param,
                                         final Object[] possibleChoices) {
        possibleChoicesMap.remove(param);
        possibleChoicesMap.put(param, possibleChoices);
    }

    /** Returns possible choices for parameter combo box. */
    public Object[] getPossibleChoices(final String param) {
        final List<Object> possibleChoices =
                             getPossibleChoices(param,
                                                possibleChoicesMap.get(param));
        return possibleChoices.toArray(new Object[possibleChoices.size()]);
    }

    /** Sets default value for the parameter. */
    public final void setDefaultValue(final String param,
                               final String defaultValue) {
        defaultValueMap.put(param, defaultValue);
    }

    /** Returns default value for the parameter. */
    public final String getDefaultValue(final String param) {
        return defaultValueMap.get(param);
    }

    /** Sets preferred value for the parameter. */
    final void setPreferredValue(final String param,
                                        final String preferredValue) {
        preferredValueMap.put(param, preferredValue);
    }

    /** Returns the preferred value for the parameter. */
    public final String getPreferredValue(final String param) {
        return preferredValueMap.get(param);
    }

    /** Sets whether the service is newly allocated. */
    public final void setNew(final boolean newResource) {
        this.newResource = newResource;
    }

    /** Returns whether the service is newly allocated. */
    public final boolean isNew() {
        return newResource;
    }
}
