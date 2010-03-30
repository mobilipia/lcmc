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
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.data.VMSXML;
import drbd.data.VMSXML.DiskData;
import drbd.data.VMSXML.InterfaceData;
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.data.ConfigData;
import drbd.data.LinuxFile;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.VIRSH;
import drbd.utilities.Unit;
import drbd.utilities.MyButton;
import drbd.utilities.SSH;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds info about Virtual Disks
 */
public class VMSDiskInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** VMS virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                                 Tools.getDefault("BackIcon"));
    /** Parameters. */
    private static final String[] PARAMETERS = {DiskData.TYPE,
                                                DiskData.DEVICE,
                                                DiskData.TARGET_DEVICE,
                                                DiskData.SOURCE_FILE,
                                                DiskData.SOURCE_DEVICE,
                                                DiskData.TARGET_BUS,
                                                DiskData.READONLY};
    /** Section map. */
    private static final Map<String, String> SECTION_MAP =
                                                 new HashMap<String, String>();
    /** Default units. */
    private static final Map<String, String> DEFAULT_UNIT =
                                                new HashMap<String, String>();
    /** If it has units. */
    private static final Map<String, Boolean> HAS_UNIT =
                                                new HashMap<String, Boolean>();
    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    static {
        FIELD_TYPES.put(DiskData.SOURCE_FILE,
                        GuiComboBox.Type.TEXTFIELDWITHBUTTON);
    }
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(DiskData.TYPE, "Type");
        SHORTNAME_MAP.put(DiskData.DEVICE, "Device");
        SHORTNAME_MAP.put(DiskData.TARGET_DEVICE, "Target Device");
        SHORTNAME_MAP.put(DiskData.SOURCE_FILE, "Source File");
        SHORTNAME_MAP.put(DiskData.SOURCE_DEVICE, "Source Device");
        SHORTNAME_MAP.put(DiskData.TARGET_BUS, "Target Bus");
        SHORTNAME_MAP.put(DiskData.READONLY, "Readonly");
    }

    /** Default name. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                               new HashMap<String, Object[]>();
    /** Default location for libvirt images. */
    private static final String LIBVIRT_IMAGE_LOCATION =
                                                    "/var/lib/libvirt/images/";
    private static final Pattern STAT_PATTERN = Pattern.compile(
       "(.).{9}\\s+(\\d+)\\s+"
       + "(\\d+)\\s+"
       + "(\\d+) (.*)$");
    /**
     * Creates the VMSDiskInfo object.
     */
    public VMSDiskInfo(final String name, final Browser browser,
                       final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser);
        setResource(new Resource(name));
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns info panel.
     */
    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = applyButton != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JTable headerTable = getTable(vmsVirtualDomainInfo.HEADER_TABLE);
        if (headerTable != null) {
            mainPanel.add(headerTable.getTableHeader());
            mainPanel.add(headerTable);
        }
        mainPanel.add(getTablePanel("Disk", vmsVirtualDomainInfo.DISK_TABLE));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel(
                                        new FlowLayout(FlowLayout.LEFT, 0, 20));
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(false);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        final JPanel extraButtonPanel =
                           new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        extraButtonPanel.setBackground(Browser.STATUS_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        final MyButton overviewButton = new MyButton("VM Host Overview",
                                                     BACK_ICON);
        overviewButton.setPreferredSize(new Dimension(150, 50));
        overviewButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                vmsVirtualDomainInfo.selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null);
        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        applyButton.setEnabled(checkResourceFields(null, params));
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Returns list of menu items for VM.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        return items;
    }

    /**
     * Returns service icon in the menu.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /**
     * Returns long description of the specified parameter.
     */
    protected final String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /**
     * Returns short description of the specified parameter.
     */
    protected final String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /**
     * Returns preferred value for specified parameter.
     */
    protected final String getParamPreferred(final String param) {
        return null;
    }

    /**
     * Returns default value for specified parameter.
     */
    protected final String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /**
     * Returns parameters.
     */
    public final String[] getParametersFromXML() {
        return PARAMETERS;
    }

    /** Returns possible choices for drop down lists. */
    protected final Object[] getParamPossibleChoices(final String param) {
        if (VMSXML.VM_PARAM_BOOT.equals(param)) {
            return POSSIBLE_VALUES.get(param);
        }
        return null;
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected final String getSection(final String param) {
        final String sm = SECTION_MAP.get(param);
        if (sm == null) {
            return "Disk Options";
        }
        return sm;
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return true;
    }

    /**
     * Returns true if the specified parameter is integer.
     */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /**
     * Returns whether parameter is checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /**
     * Returns the type of the parameter.
     */
    protected final String getParamType(final String param) {
        return "undef"; // TODO:
    }

    /**
     * Returns type of the field.
     */
    protected final GuiComboBox.Type getFieldType(final String param) {
        //return FIELD_TYPES.get(param);
        return GuiComboBox.Type.LABELFIELD;
    }

    /**
     * Applies the changes.
     */
    public final void apply(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                applyButton.setEnabled(false);
            }
        });
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            if (!Tools.areEqual(getParamSaved(param), value)) {
                parameters.put(param, value);
                getResource().setValue(param, value);
            }
        }
        //VIRSH.setParameters(getBrowser().getClusterHosts(),
        //                    domainName(),
        //                    parameters);
        checkResourceFields(null, params);
    }

    /**
     * Returns whether this parameter has a unit prefix.
     */
    protected final boolean hasUnitPrefix(final String param) {
        return HAS_UNIT.containsKey(param) && HAS_UNIT.get(param);
    }

    /**
     * Returns units.
     */
    protected final Unit[] getUnits() {
        return new Unit[]{
                   //new Unit("", "", "KiByte", "KiBytes"), /* default unit */
                   new Unit("K", "K", "KiByte", "KiBytes"),
                   new Unit("M", "M", "MiByte", "MiBytes"),
                   new Unit("G",  "G",  "GiByte",      "GiBytes"),
                   new Unit("T",  "T",  "TiByte",      "TiBytes")
       };
    }

    /**
     * Returns the default unit for the parameter.
     */
    protected final String getDefaultUnit(final String param) {
        return DEFAULT_UNIT.get(param);
    }

    /**
     * Returns columns for the table.
     */
    protected final String[] getColumnNames(final String tableName) {
        return vmsVirtualDomainInfo.getColumnNames(tableName);
    }

    /**
     * Returns data for the table.
     */
    protected final Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getMainTableData();
        } else if (VMSVirtualDomainInfo.DISK_TABLE.equals(tableName)) {
            return new Object[][]{vmsVirtualDomainInfo.getDiskDataRow(
                                            getName(),
                                            null,
                                            vmsVirtualDomainInfo.getDisks(),
                                            true)};
        //} else if (INTERFACES_TABLE.equals(tableName)) {
        }
        return new Object[][]{};
    }

    /** Execute when row in the table was clicked. */
    protected final void rowClicked(final String tableName, final String key) {
        vmsVirtualDomainInfo.selectMyself();
    }

    /**
     * Retrurns color for some rows.
     */
    protected final Color getTableRowColor(final String tableName,
                                           final String key) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableRowColor(tableName, key);
        }
        return Browser.PANEL_BACKGROUND;
    }

    /**
     * Alignment for the specified column.
     */
    protected final int getTableColumnAlignment(final String tableName,
                                                final int column) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableColumnAlignment(tableName,
                                                                column);
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    protected final Info getTableInfo(final String tableName,
                                      final String key) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo;
        }
        return null;
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        return false;
    }
    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /**
     * Returns true if the value of the parameter is ok.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Updates parameters. */
    public final void updateParameters() {
        final Map<String, DiskData> disks = vmsVirtualDomainInfo.getDisks();
        if (disks != null) {
            final DiskData diskData = disks.get(getName());
            if (diskData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h : getBrowser().getClusterHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                               diskData.getValue(param);
                            if (savedValue != null) {
                                value = savedValue;
                            }
                        }
                    }
                    if (!Tools.areEqual(value, oldValue)) {
                        getResource().setValue(param, value);
                        if (cb != null) {
                            /* only if it is not changed by user. */
                            cb.setValue(value);
                        }
                    }
                }
            }
        }
        updateTable(VMSVirtualDomainInfo.HEADER_TABLE);
        updateTable(VMSVirtualDomainInfo.DISK_TABLE);
    }

    private Host getFirstConnectedHost() {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && h.isConnected()) {
                return h;
            }
        }
        return null;
    }

    /** Starts file chooser. */
    private void startFileChooser(final GuiComboBox paramCb) {
        final Host host = getFirstConnectedHost();
        if (host == null) {
            Tools.appError("Connection to host lost.");
            return;
        }
        final JFileChooser fc = new JFileChooser();
        fc.setFileSystemView(new FileSystemView() {
            public final File createNewFolder(final File containingDir) {
                System.out.println("create new folder");
                return null;
            }
            public final File[] getFiles(final File dir,
                                         final boolean useFileHiding) {
                System.out.println("get files: " + dir);
                final SSH.SSHOutput out =
                        Tools.execCommandProgressIndicator(
                                      host,
                                      "stat -c \"%A %a %Y %s %n\" "
                                      + dir.getAbsolutePath()
                                      + "/* 2>/dev/null || true",
                                      null,
                                      true,
                                      "executing...");
                final List<File> files = new ArrayList<File>();
                if (out.getExitCode() == 0) {
                    for (final String line : out.getOutput().split("\r\n")) {
                        final Matcher m = STAT_PATTERN.matcher(line);
                        if (m.matches()) {
                            //System.out.println("type: " + m.group(1));
                            //System.out.println("perm: " + m.group(2));
                            //System.out.println("time: " + m.group(3));
                            //System.out.println("size: " + m.group(4));
                            //System.out.println("name: " + m.group(5));
                            final String type = m.group(1);
                            final long lastModified =
                                                    Long.parseLong(m.group(3));
                            final long size = Long.parseLong(m.group(4));
                            final String filename = m.group(5);
                            files.add(new LinuxFile(filename,
                                                    type,
                                                    lastModified,
                                                    size));
                        } else {
                            Tools.appWarning("could not match: " + line);
                        }
                    }
                }
                return files.toArray(new File[files.size()]);

                //return new File[]{new File(dir.getAbsolutePath() + "/boot"),
                //                  new File(dir.getAbsolutePath() + "/a"),
                //                  new File("a"),
                //                  new File("b") {
                //                      public boolean isDirectory() {
                //                          return true;
                //                      }
                //                  }};
            }
            //public final Boolean isTraversable(final File f) {
            //    System.out.println("is traversable: " + super.isTraversable(f));
            //    return true;
            //}
        });
        fc.setSelectedFile(new File(LIBVIRT_IMAGE_LOCATION));
        //fc.addPropertyChangeListener(new PropertyChangeListener() {
        //    public void propertyChange(final PropertyChangeEvent e) {
        //        boolean update = false;
        //        final String prop = e.getPropertyName();
        //        System.out.println("fc prop: " + prop
        //                           + " - " + e.getNewValue());
        //        //If the directory changed, don't show an image.
        //        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
        //            //file = null;
        //            //update = true;

        //        //If a file became selected, find out which one.
        //        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
        //            //file = (File) e.getNewValue();
        //            //update = true;
        //        }

        //        ////Update the preview accordingly.
        //        //if (update) {
        //        //    thumbnail = null;
        //        //    if (isShowing()) {
        //        //        loadImage();
        //        //        repaint();
        //        //    }
        //        //}
        //    }
        //});
        final int ret = fc.showOpenDialog(
                           Tools.getGUIData().getMainFrame());
        if (ret == JFileChooser.APPROVE_OPTION) {
            final String name = fc.getSelectedFile().getAbsolutePath();
            paramCb.setValue(name);
        }
    }

    /** Returns combo box for parameter. */
    protected GuiComboBox getParamComboBox(final String param,
                                           final String prefix,
                                           final int width) {
        //if (DiskData.SOURCE_FILE.equals(param)) {
        //    /* get networks */
        //    final String sourceFile = getParamSaved(DiskData.SOURCE_FILE);
        //    final String regexp = "[^/]$";
        //    final MyButton fileChooserBtn = new MyButton("Browse...");
        //    final GuiComboBox paramCb = new GuiComboBox(sourceFile,
        //                              null,
        //                              null, /* units */
        //                              GuiComboBox.Type.TEXTFIELDWITHBUTTON,
        //                              regexp,
        //                              width,
        //                              null, /* abbrv */
        //                              getAccessType(param),
        //                              fileChooserBtn);

        //    fileChooserBtn.addActionListener(new ActionListener() {
        //        public void actionPerformed(final ActionEvent e) {
        //            final Thread t = new Thread(new Runnable() {
        //                public void run() {
        //                    startFileChooser(paramCb);
        //                }
        //            });
        //            t.start();
        //        }
        //    });
        //    paramComboBoxAdd(param, prefix, paramCb);
        //    return paramCb;
        //} else {
            return super.getParamComboBox(param, prefix, width);
        //}
    }
}