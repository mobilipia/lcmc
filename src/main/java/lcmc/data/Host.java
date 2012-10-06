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

package lcmc.data;

import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.ConnectionCallback;
import lcmc.utilities.NewOutputCallback;
import lcmc.utilities.RoboTest;
import lcmc.gui.ProgressBar;
import lcmc.gui.TerminalPanel;
import lcmc.gui.SSHGui;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.ResourceGraph;
import lcmc.data.resources.NetInterface;
import lcmc.data.resources.BlockDevice;
import lcmc.configs.DistResource;
import lcmc.Exceptions;
import java.awt.geom.Point2D;

import java.awt.Color;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * This class holds host data and implementation of host related methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Host {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the host. */
    private String name;
    /** Hostname as entered by the user. Could be ip, hostname with or without
     * the domain name. */
    private String hostnameEntered = Tools.getDefault("SSH.Host");
    /** Ip of the host. */
    private String ip;
    /** Ips in the combo in Dialog.Host.Configuration. */
    private final Map<Integer, String[]> ips = new HashMap<Integer, String[]>();
    /** Hostname of the host. */
    private String hostname = "unknown";
    /** Username, root most of the times. */
    private String username = null;
    /** Detected kernel name. */
    private String detectedKernelName = "";
    /** Detected distribution. */
    private String detectedDist = "";
    /** Detected distribution version. */
    private String detectedDistVersion = "";
    /** Detected kernel version. */
    private String detectedKernelVersion = "";
    /** Detected kernel architecture. */
    private String detectedArch = "";
    /** Kernel name (could be different than detected). */
    private String kernelName = "";
    /** Distribution (could be different than detected). */
    private String dist = "";
    /** Distribution version (could be different than detected). */
    private String distVersion = "";
    /** Distribution version string (could be different than detected). */
    private String distVersionString = "";
    /** Kernel version (could be different than detected). */
    private String kernelVersion = "";
    /** Kernel architecture (could be different than detected). */
    private String arch = "";
    /** Available drbd versions. */
    private List<String> availableDrbdVersions = null;
    /** Drbd version, that will be installed. */
    private String drbdVersionToInstall = null;
    /** Drbd version of the source tarball, that will be installed . */
    private String drbdVersionUrlStringToInstall = null;
    /** Build, which will be installed. */
    private String drbdBuildToInstall = null;
    /** Drbd packages, that will be installed. */
    private String drbdPackagesToInstall = null;
    /** Cluster data object. */
    private Cluster cluster = null;
    /** Drbd version of drbdadm tool. */
    private String drbdVersion = null;
    /** Drbd version of drbd module. */
    private String drbdModuleVersion = null;
    /** Map of network interfaces of this host. */
    private Map<String, NetInterface> netInterfaces =
                                     new LinkedHashMap<String, NetInterface>();
    /** Available file systems. */
    private Set<String> fileSystems = new TreeSet<String>();
    /** Available crypto modules. */
    private Set<String> cryptoModules = new TreeSet<String>();
    /** Available qemu keymaps. */
    private Set<String> qemuKeymaps = new TreeSet<String>();
    /** Available libvirt cpu models. */
    private Set<String> cpuMapModels = new TreeSet<String>();
    /** Available libvirt cpu vendors. */
    private Set<String> cpuMapVendors = new TreeSet<String>();
    /** Mount points that exist in /mnt dir. */
    private Set<String> mountPoints = new TreeSet<String>();
    /** List of block devices of this host. */
    private Map<String, BlockDevice> blockDevices =
                                      new LinkedHashMap<String, BlockDevice>();
    /** List of drbd block devices of this host. */
    private Map<String, BlockDevice> drbdBlockDevices =
                                      new LinkedHashMap<String, BlockDevice>();
    /** Options for GUI drop down lists. */
    private Map<String, List<String>> guiOptions =
                                          new HashMap<String, List<String>>();
    /** Color of this host in graphs. */
    private Color defaultColor;
    /** Color of this host in graphs. */
    private Color savedColor;
    /** Thread where connection status command is running. */
    private ExecCommandThread connectionStatusThread = null;
    /** Thread where drbd status command is running. */
    private ExecCommandThread drbdStatusThread = null;
    /** Thread where hb status command is running. */
    private ExecCommandThread clStatusThread = null;
    /** Thread where server status command is running. */
    private ExecCommandThread serverStatusThread = null;
    /** List of positions of the services.
     *  Question is this: the saved positions can be different on different
     *  hosts, but only one can be used in the hb graph.
     *  Only one will be used and by next save the problem solves itself.
     */
    private final Map<String, Point2D> servicePositions =
                                            new HashMap<String, Point2D>();
    /** Pacemaker version. */
    private String pacemakerVersion = null;
    /** Openais version. */
    private String openaisVersion = null;
    /** Whether the comm layer is stopping. */
    private boolean commLayerStopping = false;
    /** Whether the comm layer is starting. */
    private boolean commLayerStarting = false;
    /** Whether the pcmk is starting. */
    private boolean pcmkStarting = false;
    /** Is "on" if corosync is in rc. */
    private boolean csIsRc = false;
    /** Is "on" if openais is in rc. */
    private boolean aisIsRc = false;
    /** Is "on" if heartbeat has an init script. */
    private boolean heartbeatInit = false;
    /** Is "on" if corosync has an init script. */
    private boolean csInit = false;
    /** Is "on" if openais has an init script. */
    private boolean aisInit = false;
    /** Is "on" if corosync is running. */
    private boolean csRunning = false;
    /** Is "on" if openais is running. */
    private boolean aisRunning = false;
    /** Is "on" if corosync/openais config exists. */
    private boolean csAisConf = false;
    /** Is "on" if heartbeat is in rc. */
    private boolean heartbeatIsRc = false;
    /** Is "on" if heartbeat is running. */
    private boolean heartbeatRunning = false;
    /** Is "on" if heartbeat config exists. */
    private boolean heartbeatConf = false;
    /** Is "on" if pacemaker is in rc. */
    private boolean pcmkIsRc = false;
    /** Is "on" if pacemaker is running. */
    private boolean pcmkRunning = false;
    /** Is "on" if pacemaker has an init script. */
    private boolean pcmkInit = false;
    /** Pacemaker service version. From version 1, use pacamker init script. */
    private int pcmkServiceVersion = -1;
    /** Is "on" if drbd module is loaded. */
    private boolean drbdLoaded = false;
    /** Corosync version. */
    private String corosyncVersion = null;
    /** Heartbeat version. */
    private String heartbeatVersion = null;
    /** Whether drbd will be upgraded. */
    private boolean drbdWillBeUpgraded = false;
    /** Whether drbd was newly installed. */
    private boolean drbdWasInstalled = false;
    /** Whether heartbeat status is ok. */
    private boolean clStatus = false;
    /** Whether drbd status is ok. */
    private boolean drbdStatus = false;

    /** SSH object of the connection to this host. */
    private final SSH ssh = new SSH();
    /** Terminal panel of this host. */
    private TerminalPanel terminalPanel = null;
    /** SSH port. */
    private String sshPort = null;
    /** Whether sudo should be used. */
    private Boolean useSudo = null;
    /** Sudo password. */
    private String sudoPassword = "";
    /** Browser panel (the one with menus and all the logic) of this host. */
    private HostBrowser browser;
    /** A gate that is used to synchronize the loading sequence. */
    private CountDownLatch isLoadingGate;
    /** A gate that waits for server status. */
    private final CountDownLatch serverStatusLatch = new CountDownLatch(1);
    /** List of gui elements that are to be enabled if the host is connected.*/
    private final List<JComponent> enableOnConnectList =
                                                   new ArrayList<JComponent>();
    /** Corosync/Openais/pacemaker installation method index. */
    private String pmInstallMethod;
    /** Heartbeat/pacemaker installation method index. */
    private String hbPmInstallMethod;
    /** Drbd installation method index. */
    private String drbdInstallMethod;
    /** Heartbeat lib path. */
    private String hbLibPath = null;
    /** MD5 checksum of VM Info from server. */
    private String vmInfoMD5 = null;
    /** Index of this host in its cluster. */
    private int index = 0;
    /** Whether the last connection check was positive. */
    private volatile boolean lastConnected = false;
    /** Whether corosync or heartbeat is running. */
    private Boolean corosyncHeartbeatRunning = null;
    /** Libvirt version. */
    private String libvirtVersion = null;
    /** String that is displayed as a tool tip for disabled menu item. */
    public static final String NOT_CONNECTED_STRING =
                                                   "not connected to the host";
    /** Block device with number pattern. */
    public static final Pattern BDP = Pattern.compile("(\\D+)\\d+");
    /** DRBD bd pattern. */
    public static final Pattern DRBDP = Pattern.compile(".*\\/drbd\\d+$");
    /** Physical volumes on this host. */
    private List<BlockDevice> physicalVolumes = new ArrayList<BlockDevice>();
    /** Volume group information on this host. */
    private Map<String, Long> volumeGroups = new LinkedHashMap<String, Long>();
    /** Volume group with all lvs in it. */
    private Map<String, Set<String>> volumeGroupsLVS =
                                            new HashMap<String, Set<String>>();
    /** Whether this cluster should be saved. */
    private boolean savable = true;
    /** Ping is set every 10s. */
    private volatile AtomicBoolean ping = new AtomicBoolean(true);
    /** Global drbd status lock. */
    private final Lock mDRBDStatusLock = new ReentrantLock();
    /** Update VMS lock. */
    private final Lock mUpdateVMSlock = new ReentrantLock();
    /** Time stamp lock */
    private final Lock mInfoTimestampLock = new ReentrantLock();
    /** Time stamp hash. */
    private final Map<String, Double> infoTimestamp =
                                                new HashMap<String, Double>();
    /** Timeout after which the connection is considered to be dead. */
    private final int PING_TIMEOUT           = 40000;
    private final int DRBD_EVENTS_TIMEOUT    = 40000;
    private final int CLUSTER_EVENTS_TIMEOUT = 40000;
    private final int HW_INFO_TIMEOUT        = 40000;

    /** Choices for gui drop down menus. */
    public static final String VM_FILESYSTEM_SOURCE_DIR_LXC =
                                                "vm.filesystem.source.dir.lxc";

    /** Root user name. */
    public static final String ROOT_USER = "root";
    /** Default SSH port. */
    public static final String DEFAULT_SSH_PORT = "22";
    /**
     * Prepares a new <code>Host</code> object. Initializes host browser and
     * host's resources.
     */
    public Host() {
        if (Tools.getConfigData().getHosts().size() == 1) {
            hostnameEntered = Tools.getDefault("SSH.SecHost");
        }
        browser = new HostBrowser(this);
        browser.initHostResources();
        mountPoints.add("/mnt/");
    }

    /** Prepares a new <code>Host</code> object. */
    public Host(final String ip) {
    this();
    this.ip = ip;
    }

    /** Returns borwser object for this host. */
    public HostBrowser getBrowser() {
        return browser;
    }

    /**
     * Sets cluster in which this host is in. Set null,
     * if it is removed from the cluster. One host can be
     * only in one cluster.
     */
    public void setCluster(final Cluster cluster) {
        this.cluster = cluster;
    }

    /** Returns the cluster data object. */
    public Cluster getCluster() {
        return cluster;
    }

    /** Returns color objects of this host for drbd graph. */
    public Color[] getDrbdColors() {
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        Color col;
        Color secColor;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        if (isConnected()) {
            if (isDrbdStatus() && isDrbdLoaded()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }


    /** Returns color objects of this host. */
    public Color[] getPmColors() {
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        Color col;
        Color secColor;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        if (isConnected()) {
            if (isClStatus()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }

    /** Sets color of the host. */
    void setColor(final Color defaultColor) {
        this.defaultColor = defaultColor;
        if (savedColor == null) {
            savedColor = defaultColor;
        }
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /** Sets color of the host, when it was saved. */
    public void setSavedColor(final Color savedColor) {
        this.savedColor = savedColor;
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /** Sets if hb status failed or not. */
    public void setClStatus(final boolean clStatus) {
        this.clStatus = clStatus;
    }

    /** Sets if drbd status failed or not. */
    public void setDrbdStatus(final boolean drbdStatus) {
        this.drbdStatus = drbdStatus;
        if (!drbdStatus) {
            for (BlockDevice b : getBlockDevices()) {
                b.resetDrbd();
            }
        }
    }

    /** Returns whether cluster status is available. */
    public boolean isClStatus() {
        return clStatus && isConnected();
    }

    /** Returns whether drbd status is available. */
    public boolean isDrbdStatus() {
        return drbdStatus;
    }

    /** Returns true when this host is in a cluster. */
    public boolean isInCluster() {
        return cluster != null;
    }

    /**
     * Returns true when this host is in a cluster and is different than the
     * specified cluster.
     */
    public boolean isInCluster(final Cluster otherCluster) {
        return cluster != null && !cluster.equals(otherCluster);
    }

    /**
     * Sets hostname as entered by user, this can be also ip. If
     * hostnameEntered changed, it reinitilizes the name.
     */
    public void setHostnameEntered(final String hostnameEntered) {
        if (hostnameEntered != null
            && !hostnameEntered.equals(this.hostnameEntered)) {
            /* back button and hostname changed */
            setName(null);
            setIp(null);
            setHostname(null);
        }
        this.hostnameEntered = hostnameEntered;
    }

    /** Sets hostname of the host. */
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets user name for the host. This username is used to connect
     * to the host. The default is "root". If username changed disconnect
     * the old connection.
     */
    public void setUsername(final String username) {
        if (!username.equals(this.username)) {
            ssh.disconnect();
        }
        this.username = username;
    }

    /**
     * Sets ip. If ip has changed, disconnect the
     * old connection.
     */
    public void setIp(final String ip) {
        if (ip != null) {
            if (!ip.equals(this.ip)) {
                ssh.disconnect();
            }
        } else if (this.ip != null) {
            ssh.disconnect();
        }
        this.ip = ip;
    }

    /** Sets ips. */
    public void setIps(final int hop, final String[] ipsForHop) {
        ips.put(hop, ipsForHop);
    }

    /** Returns net interfaces. */
    public NetInterface[] getNetInterfaces() {
        return netInterfaces.values().toArray(
                                    new NetInterface[netInterfaces.size()]);
    }

    /** Get net interfaces that are bridges. */
    public List<String> getBridges() {
        final List<String> bridges = new ArrayList<String>();
        for (final NetInterface ni : netInterfaces.values()) {
            if (ni.isBridge()) {
                bridges.add(ni.getName());
            }
        }
        return bridges;
    }

    /** Returns blockDevices. */
    public BlockDevice[] getBlockDevices() {
        return blockDevices.values().toArray(
                                    new BlockDevice[blockDevices.size()]);
    }

    /**
     * Returns blockDevices as array list of device names. Removes the
     * ones that are in the drbd and are already used in CRM.
     */
    List<String> getBlockDevicesNames() {
        final List<String> blockDevicesNames = new ArrayList<String>();
        for (final String bdName : blockDevices.keySet()) {
            final BlockDevice bd = blockDevices.get(bdName);
            if (!bd.isDrbd() && !bd.isUsedByCRM()) {
                blockDevicesNames.add(bdName);
            }
        }
        return blockDevicesNames;
    }

    /**
     * Returns blockDevices as array list of device names.
     *
     * @param otherBlockDevices
     *          list of block devices with which the intersection with
     *          block devices of this host is made.
     *
     */
    List<String> getBlockDevicesNamesIntersection(
                                        final List<String> otherBlockDevices) {
        final List<String> blockDevicesIntersection = new ArrayList<String>();
        if (otherBlockDevices == null) {
            return getBlockDevicesNames();
        }
        for (final String otherBd : otherBlockDevices) {
            final BlockDevice bd = blockDevices.get(otherBd);
            if (bd != null && !bd.isDrbd()) {
                blockDevicesIntersection.add(otherBd);
            }
        }
        return blockDevicesIntersection;
    }

    /** Returns network ips as array list. */
    Map<String, String> getNetworkIps() {
        final Map<String, String> networkIps =
                                         new LinkedHashMap<String, String>();
        for (final NetInterface ni : netInterfaces.values()) {
            final String netIp = ni.getNetworkIp();
            networkIps.put(netIp, ni.getNetmask());
        }
        return networkIps;
    }

    /** Returns list of networks that exist on all hosts. */
    public Map<String, String> getNetworksIntersection(
                                   final Map<String, String> otherNetworkIps) {
        if (otherNetworkIps == null) {
            return getNetworkIps();
        }
        final Map<String, String> networksIntersection =
                                           new LinkedHashMap<String, String>();
        for (final NetInterface ni : netInterfaces.values()) {
            final String networkIp = ni.getNetworkIp();
            if (otherNetworkIps.containsKey(networkIp)
                && !networksIntersection.containsKey(networkIp)) {
                networksIntersection.put(networkIp, ni.getNetmask());
            }
        }
        return networksIntersection;
    }

    /** Returns ips that belong the the network. */
    List<String> getIpsFromNetwork(final String netIp) {
        final List<String> networkIps = new ArrayList<String>();
        for (final NetInterface ni : netInterfaces.values()) {
            if (netIp.equals(ni.getNetworkIp())) {
                networkIps.add(ni.getIp());
            }
        }
        return networkIps;
    }

    /** Returns BlockDevice object identified with device name. */
    BlockDevice getBlockDevice(final String device) {
        return blockDevices.get(device);
    }

    /** Removes file system from the list of file systems. */
    void removeFileSystems() {
        fileSystems.clear();
    }

    /** Returns available file systems. */
    String[] getFileSystems() {
        return fileSystems.toArray(new String [fileSystems.size()]);
    }

    /** Returns available file systems devices as a list of strings. */
    Set<String> getFileSystemsList() {
        return fileSystems;
    }

    /** Returns available crypto modules as a list of strings. */
    Set<String> getCryptoModules() {
        return cryptoModules;
    }

    /** Returns available qemu keymaps as a list of strings. */
    public Set<String> getQemuKeymaps() {
        return qemuKeymaps;
    }

    /** Returns available libvirt's cpu map models. */
    public Set<String> getCPUMapModels() {
        return cpuMapModels;
    }

    /** Returns available libvirt's cpu map vendors. */
    public Set<String> getCPUMapVendors() {
        return cpuMapVendors;
    }

    /** Returns mount points as a list of strings. */
    Set<String> getMountPointsList() {
        return mountPoints;
    }

    /** Returns ips of this host. */
    public String[] getIps(final int hop) {
        return ips.get(hop);
    }

    /** Sets available drbd versions. */
    public void setAvailableDrbdVersions(final String[] versions) {
        availableDrbdVersions = new ArrayList<String>(Arrays.asList(versions));
    }

    /** Retruns available drbd versions as array of strings. */
    public String[] getAvailableDrbdVersions() {
        if (availableDrbdVersions == null) {
            return null;
        }
        return availableDrbdVersions.toArray(
                                    new String [availableDrbdVersions.size()]);
    }

    /** Returns whether there is a drbd upgrade available. */
    public boolean isDrbdUpgradeAvailable(final String versionString) {
        if (availableDrbdVersions == null || versionString == null) {
            return false;
        }
        final String version = versionString.split(" ")[0];
        for (final String v : availableDrbdVersions) {
            try {
                if (Tools.compareVersions(v, version) > 0) {
                    return true;
                }
            } catch (Exceptions.IllegalVersionException e) {
                Tools.appWarning(e.getMessage(), e);
            }
        }
        return false;
    }

    /** Returns installed drbd version. */
    public String getDrbdVersion() {
        return drbdVersion;
    }

    /** Returns installed drbd module version. */
    public String getDrbdModuleVersion() {
        return drbdModuleVersion;
    }

    /**
     * Sets drbdVersionToInstall. This version is one that is to be installed.
     * If drbd is already installed.
     */
    public void setDrbdVersionToInstall(final String drbdVersionToInstall) {
        this.drbdVersionToInstall = drbdVersionToInstall;
    }

    /**
     * Sets the drbd version in the form that is in the source tarball on
     * linbit website, like so: "8.3/drbd-8.3.1.tar.gz".
     */
    public void setDrbdVersionUrlStringToInstall(
                                  final String drbdVersionUrlStringToInstall) {
        this.drbdVersionUrlStringToInstall = drbdVersionUrlStringToInstall;
    }

    /**
     * Gets drbdVersionToInstall. This version is one that is to be installed.
     * If drbd is already installed.
     */
    public String getDrbdVersionToInstall() {
        return drbdVersionToInstall;
    }

    /**
     * Gets drbd version of the source tarball in the form as it is on
     * the linbit website: "8.3/drbd-8.3.1.tar.gz".
     */
    public String getDrbdVersionUrlStringToInstall() {
        return drbdVersionUrlStringToInstall;
    }

    /**
     * Sets drbdBuildToInstall. This build is the one that is to be installed.
     */
    public void setDrbdBuildToInstall(final String drbdBuildToInstall) {
        this.drbdBuildToInstall = drbdBuildToInstall;
    }

    /** Returns the drbd build to be installed. */
    public String getDrbdBuildToInstall() {
        return drbdBuildToInstall;
    }

    /** Sets drbd packages to install. */
    public void setDrbdPackagesToInstall(final String drbdPackagesToInstall) {
        this.drbdPackagesToInstall = drbdPackagesToInstall;
    }

    /**
     * Sets distribution info for this host from array of strings.
     * Array consists of: kernel name, kernel version, arch, os, version
     * and distribution.
     */
    @SuppressWarnings("fallthrough")
    void setDistInfo(final String[] info) {
        if (info == null) {
            return;
        }
        /* no breaks in the switch statement are intentional */
        String lsbVersion = null;
        String lsbDist = null;
        switch (info.length) {
            case 9:
                lsbVersion = info[8]; // TODO: not used
            case 8:
                lsbDist = info[7];
            case 7:
                lsbVersion = info[6]; // TODO: not used
            case 6:
                lsbDist = info[5];
            case 5:
                if (lsbDist == null || "linux".equals(lsbDist)) {
                    detectedDist = info[4];
                } else {
                    detectedDist = lsbDist;
                }
            case 4:
                if (lsbVersion == null) {
                    detectedDistVersion = info[3];
                } else {
                    detectedDistVersion = info[3] + "/" + lsbVersion;
                }
            case 3:
                detectedKernelVersion = info[2];
            case 2:
                detectedArch = info[1];
            case 1:
                detectedKernelName = info[0];
            case 0:
                break;
            default:
                Tools.appError("Error.SetDistInfo.CannotSetInfo",
                               java.util.Arrays.asList(info).toString());
                break;
        }
        dist = detectedDist;
        distVersion = detectedDistVersion;
        initDistInfo();
        Tools.debug(this, "kernel name: " + detectedKernelName, 1);
        Tools.debug(this, "kernel version: " + detectedKernelVersion, 1);
        Tools.debug(this, "arch: " + detectedArch, 1);
        Tools.debug(this, "dist version: " + detectedDistVersion, 1);
        Tools.debug(this, "dist: " + detectedDist, 1);
    }

    /** Initializes dist info. Must be called after setDistInfo. */
    void initDistInfo() {
        if (!"Linux".equals(detectedKernelName)) {
            Tools.appWarning("detected kernel not linux: "
                             + detectedKernelName);
        }
        setKernelName("Linux");

        if (!dist.equals(detectedDist)) {
            Tools.appError("dist: " + dist + " does not match " + detectedDist);
        }
        distVersionString = Tools.getDistVersionString(dist, distVersion);
        distVersion = Tools.getDistString("distributiondir",
                                          detectedDist,
                                          distVersionString,
                                          null);
        setKernelVersion(Tools.getKernelDownloadDir(detectedKernelVersion,
                                                    getDist(),
                                                    distVersionString,
                                                    null));
        setArch(Tools.getDistString("arch:" + detectedArch,
                                    getDist(),
                                    distVersionString,
                                    null));
    }

    /** Returns the detected info to show. */
    public String getDetectedInfo() {
        return detectedDist + " " + detectedDistVersion;
    }

    /**
     * Gets distribution name from distribution version. E.g. suse from sles9.
     * This is used only when the distribution is selected in the pulldown menu,
     * not when it is detected.
     * The conversion rules for distributions are defined in DistResource.java,
     * with 'dist:' prefix.
     * TODO: remove it?
     */
    String getDistFromDistVersion(final String dV) {
        /* remove numbers */
        if ("No Match".equals(dV)) {
            return null;
        }
        Tools.debug(this, "getDistVersionString dist:"
                          + dV.replaceFirst("\\d.*", ""), 0);
        return Tools.getDistString("dist:" + dV.replaceFirst("\\d.*", ""),
                                   "",
                                   "",
                                   null);
    }


    /** Sets distribution name. */
    void setDist(final String dist) {
        this.dist = dist;
    }

    /** Sets distribution version. */
    void setDistVersion(final String distVersion) {
        this.distVersion = distVersion;
        distVersionString = Tools.getDistVersionString(dist, distVersion);
        dist = getDistFromDistVersion(distVersion);
    }

    /** Sets arch, e.g. "i386". */
    public void setArch(final String arch) {
        this.arch = arch;
    }

    /** Sets kernel name, e.g. "linux". */
    void setKernelName(final String kernelName) {
        this.kernelName = kernelName;
    }

    /** Sets kernel version. */
    void setKernelVersion(final String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    /** Gets kernel name. Normaly "Linux" for this application. */
    String getKernelName() {
        return kernelName;
    }

    /**
     * Gets kernel version. Usually some version,
     * like: "2.6.13.2ws-k7-up-lowmem".
     */
    public String getKernelVersion() {
        return kernelVersion;
    }

    /** Returns the detected kernel version. */
    public String getDetectedKernelVersion() {
        return detectedKernelVersion;
    }

    /** Gets architecture like i686. */
    public String getArch() {
        return arch;
    }

    /** Returns heartbeat lib path. */
    public String getHeartbeatLibPath() {
        if (hbLibPath != null) {
            return hbLibPath;
        }
        if ("".equals(arch)) {
            Tools.appWarning(
                        "getHeartbeatLibPath() called to soon: unknown arch");
        } else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            return "/usr/lib64/heartbeat";
        }
        return "/usr/lib/heartbeat";
    }

    /** Returns lxc lib path. */
    public String getLxcLibPath() {
        return getDistString("libvirt.lxc.libpath");
    }

    /** Returns xen lib path. */
    public String getXenLibPath() {
        return getDistString("libvirt.xen.libpath");
    }

    /** Gets distribution, e.g., debian. */
    public String getDist() {
        return dist;
    }

    /** Gets distribution version. */
    public String getDistVersion() {
        return distVersion;
    }

    /** Gets distribution version string. */
    public String getDistVersionString() {
        return distVersionString;
    }

    /** Disconnects this host. */
    public void disconnect() {
        if (ssh.isConnected()) {
            ssh.forceDisconnect();
        }
        setVMInfoMD5(null);
    }

    /**
     * Converts command string to real command for a distribution, specifying
     * the convert command callback.
     */
    public String getDistCommand(final String commandString,
                                 final ConvertCmdCallback convertCmdCallback) {
        return Tools.getDistCommand(commandString,
                                    dist,
                                    distVersionString,
                                    arch,
                                    convertCmdCallback,
                                    false); /* in bash */
    }

    /** Converts a string that is specific to the distribution distribution. */
    public String getDistString(final String commandString) {
        return Tools.getDistString(commandString,
                                   dist,
                                   distVersionString,
                                   arch);
    }

    /**
     *  Gets list of strings that are specific to the distribution
     *  distribution.
     */
    public List<String> getDistStrings(final String commandString) {
        return Tools.getDistStrings(commandString,
                                    dist,
                                    distVersionString,
                                    arch);
    }


    /**
     * Converts command string to real command for a distribution, specifying
     * what-with-what hash.
     */
    public String getDistCommand(final String commandString,
                                 final Map<String, String> replaceHash) {
        return Tools.getDistCommand(
                    commandString,
                    dist,
                    distVersionString,
                    arch,
                    new ConvertCmdCallback() {
                        @Override
                        public String convert(String command) {
                            for (final String tag : replaceHash.keySet()) {
                                if (tag != null && command.indexOf(tag) > -1) {
                                    final String s = replaceHash.get(tag);
                                    if (s != null) {
                                        command = command.replaceAll(tag, s);
                                    }
                                }
                            }
                            return command;
                        }
                    },
                    false); /* in bash */
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public ExecCommandThread execCommand(
                               final String commandString,
                               final ExecCallback execCallback,
                               final ConvertCmdCallback convertCmdCallback,
                               final boolean outputVisible,
                               final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback,
                                                    false), /* in bash */
                               execCallback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command. Command is not converted for different distributions
     * and is executed in a new thread, after command is finished callback.done
     * function will be called. In case of error, callback.doneError is called.
     */
    public ExecCommandThread execCommandRaw(final String command,
                                            final ExecCallback callback,
                                            final boolean outputVisible,
                                            final boolean commandVisible,
                                            final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(command,
                               callback,
                               outputVisible,
                               commandVisible,
                               commandTimeout);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished callback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public ExecCommandThread execCommand(
                               final String commandString,
                               final ProgressBar progressBar,
                               final ExecCallback callback,
                               final ConvertCmdCallback convertCmdCallback,
                               final boolean outputVisible,
                               final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback,
                                                    false), /* in bash */
                               progressBar,
                               callback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command with bash -c. Command is executed in a new thread,
     * after command * is finished callback.done function will be called.
     * In case of error, callback.doneError is called.
     */
    public ExecCommandThread execCommandInBash(
                               final String commandString,
                               final ProgressBar progressBar,
                               final ExecCallback callback,
                               final ConvertCmdCallback convertCmdCallback,
                               final boolean outputVisible,
                               final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback,
                                                    true), /* in bash */
                               progressBar,
                               callback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command. Command is not converted for different distributions
     * and is executed in a new thread, after command is finished callback.done
     * function will be called. In case of error, callback.doneError is called.
     */
    public ExecCommandThread execCommandRaw(
                                   final String command,
                                   final ProgressBar progressBar,
                                   final ExecCallback execCallback,
                                   final boolean outputVisible,
                                   final boolean commandVisible,
                                   final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(command,
                               progressBar,
                               execCallback,
                               outputVisible,
                               commandVisible,
                               commandTimeout);
    }

    /**
     * Executes get status command which runs in the background and updates the
     * block device object. The command is 'drbdsetup /dev/drbdX events'
     * The session is stored, so that in can be stopped with 'stop' button.
     */
    public void execDrbdStatusCommand(final ExecCallback execCallback,
                                      final NewOutputCallback outputCallback) {
        if (drbdStatusThread == null) {
            drbdStatusThread = ssh.execCommand(
                                Tools.getDistCommand(
                                                "DRBD.getDrbdStatus",
                                                dist,
                                                distVersionString,
                                                arch,
                                                null, /* ConvertCmdCallback */
                                                false), /* in bash */
                                    execCallback,
                                    outputCallback,
                                    false,
                                    false,
                                    DRBD_EVENTS_TIMEOUT);
        } else {
            Tools.appWarning("trying to start started drbd status");
        }
    }

    /** Stops server (hw) status background process. */
    public void stopServerStatus() {
        if (serverStatusThread == null) {
            Tools.appWarning("trying to stop stopped server status");
            return;
        }
        serverStatusThread.cancel();
        serverStatusThread = null;
    }

    /** Stops drbd status background process. */
    public void stopDrbdStatus() {
        if (drbdStatusThread == null) {
            Tools.appWarning("trying to stop stopped drbd status");
            return;
        }
        drbdStatusThread.cancel();
        drbdStatusThread = null;
    }

    /** Waits till the drbd status command finishes. */
    public void waitOnDrbdStatus() {
        if (drbdStatusThread != null) {
            try {
                /* it probably hangs after this timeout, so it will be
                 * killed. */
                drbdStatusThread.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopDrbdStatus();
        }
    }

    /** Executes an hb status command. */
    public void execClStatusCommand(final ExecCallback execCallback,
                                    final NewOutputCallback outputCallback) {
        if (clStatusThread == null) {
            clStatusThread = ssh.execCommand(
                            Tools.getDistCommand("Heartbeat.getClStatus",
                                                 dist,
                                                 distVersionString,
                                                 arch,
                                                 null, /* ConvertCmdCallback */
                                                 false), /* in bash */
                                execCallback,
                                outputCallback,
                                false,
                                false,
                                CLUSTER_EVENTS_TIMEOUT);
        } else {
            Tools.appWarning("trying to start started hb status");
        }
    }

    /** Waits while the hb status thread finishes. */
    public void waitOnClStatus() {
        try {
            clStatusThread.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clStatusThread = null;
    }

    /** Stops hb status background process. */
    public void stopClStatus() {
        if (clStatusThread == null) {
            Tools.appWarning("trying to stop stopped hb status");
            return;
        }
        clStatusThread.cancel();
    }

    /** Gets ip. There can be more ips, delimited with "," */
    public String getIp() {
        return ip;
    }

    /** Returns the ip for the hop. */
    public String getIp(final int hop) {
        if (ip == null) {
            return null;
        }
        final String[] ips = ip.split(",");
        if (ips.length < hop + 1) {
            return null;
        }
        return ips[hop];
    }

    /** Return first hop ip. */
    public String getFirstIp() {
        if (ip == null) {
            return null;
        }
        final String[] ips = ip.split(",");
        return ips[0];
    }
    /** Returns username. */
    public String getUsername() {
        return username;
    }

    /** Returns first username in a hop. */
    public String getFirstUsername() {
        final String[] usernames = username.split(",");
        return usernames[0];
    }

    /** Gets hostname as entered by user. */
    public String getHostnameEntered() {
        return hostnameEntered;
    }

    /** Returns sudo prefix. */
    String getSudoPrefix(final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            if (sudoTest) {
                return "sudo -E -n ";
            } else {
                return "sudo -E -p '"
                       + SSH.SUDO_PROMPT + "' ";
            }
        } else {
            return "";
        }
    }
    /** Returns command exclosed in sh -c "". */
    public String getSudoCommand(final String command,
                                 final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            final String sudoPrefix = getSudoPrefix(sudoTest);
            return command.replaceAll(DistResource.SUDO, sudoPrefix);
        } else {
            return command.replaceAll(DistResource.SUDO, " "); /* must be " " */
        }
    }

    /**
     * Returns command with all the sshs that will be hopped.
     *
     * ssh -A   -tt -l root x.x.x.x "ssh -A   -tt -l root x.x.x.x \"ssh
     * -A   -tt -l root x.x.x.x \\\"ls\\\"\""
     */
    public String getHoppedCommand(final String command) {
        final int hops = Tools.charCount(ip, ',') + 1;
        final String[] usernames = username.split(",");
        final String[] ips = ip.split(",");
        final StringBuilder s = new StringBuilder(200);
        if (hops > 1) {
            String sshAgentPid = "";
            String sshAgentSock = "";
            final Map<String, String> variables = System.getenv();
            for (final String name : variables.keySet()) {
                final String value = variables.get(name);
                if ("SSH_AGENT_PID".equals(name)) {
                    sshAgentPid = value;
                } else if ("SSH_AUTH_SOCK".equals(name)) {
                    sshAgentSock = value;
                }
            }

            s.append("SSH_AGENT_PID=");
            s.append(sshAgentPid);
            s.append(" SSH_AUTH_SOCK=");
            s.append(sshAgentSock);
            s.append(' ');
        }
        for (int i = 1; i < hops; i++) {
            s.append("ssh -q -A -tt -o 'StrictHostKeyChecking no' "
                     + "-o 'ForwardAgent yes' -l ");
            if (i < usernames.length) {
                s.append(usernames[i]);
            } else {
                s.append(ROOT_USER);
            }
            s.append(' ');
            s.append(ips[i]);
            s.append(' ');
            s.append(Tools.escapeQuotes("\"", i - 1));
        }

        s.append(Tools.escapeQuotes(command, hops - 1));

        for (int i = hops - 1; i > 0; i--) {
            s.append(Tools.escapeQuotes("\"", i - 1));
        }
        return s.toString();
    }

    /** Returns hostname of this host. */
    public String getHostname() {
        return hostname;
    }

    /** Returns the host name. */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Gets name, that is shown in the tab. Name is either host name, if it is
     * set or ip.
     */
    public String getName() {
        if (name == null) {
            String nodeName;
            if (hostname != null) {
                final int i = hostname.indexOf(',');
                if (i > 0) {
                    nodeName = hostname.substring(i + 1);
                } else {
                    nodeName = hostname;
                }
            } else if (hostnameEntered != null) {
                final int i = hostnameEntered.indexOf(',');
                if (i > 0) {
                    nodeName = hostnameEntered.substring(i + 1);
                } else {
                    nodeName = hostnameEntered;
                }
            } else {
                return ip;
            }

            //final int index = nodeName.indexOf('.');
            //if (index > 0 && !Tools.checkIp(nodeName)) {
            //    return nodeName.substring(0, index);
            //} else {
            //    return nodeName;
            //}
            return nodeName;
        } else {
            return name;
        }
    }

    /** Sets name of the host as it will be identified. */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets string with user and hostname as used in prompt or ssh like
     * rasto@linbit.at.
     */
    public String getUserAtHost() {
        return username + "@" + getHostname();
    }

    /** Gets SSH object. */
    public SSH getSSH() {
        return ssh;
    }


    /**
     * Sets terminal panel object. This is the panel where the commands and
     * their results are shown for every host.
     */
    public void setTerminalPanel(final TerminalPanel terminalPanel) {
        this.terminalPanel = terminalPanel;
    }

    /** Gets terminal panel object. */
    public TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     */
    public void connect(final SSHGui sshGui,
                        final ConnectionCallback callback) {
        ssh.connect(sshGui, callback, this);
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     *
     * @param sshGui
     *          ssh gui dialog
     *
     * @param progressBar
     *          progress bar that is used to show progress through connecting
     *
     * @param callback
     *          callback class that implements ConnectionCallback interface
     */
    public void connect(final SSHGui sshGui,
                        final ProgressBar progressBar,
                        final ConnectionCallback callback) {
        Tools.debug(this, "host connect: " + sshGui, 1);
        ssh.connect(sshGui, progressBar, callback, this);
    }

    /**
     * Register a component that will be enabled if the host connected and
     * disabled if disconnected.
     */
    public void registerEnableOnConnect(final JComponent c) {
        if (!enableOnConnectList.contains(c)) {
            enableOnConnectList.add(c);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                c.setEnabled(isConnected());
            }
        });
    }

    /**
     * Is called after the host is connected or disconnected and
     * enables/disables the conponents that are registered to be enabled on
     * connect.
     */
    public void setConnected() {
        final boolean con = isConnected();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent c : enableOnConnectList) {
                    c.setEnabled(con);
                }
            }
        });
        if (lastConnected != con) {
            lastConnected = con;
            if (con) {
               Tools.info(getName() + ": connection established");
            } else {
               Tools.info(getName() + ": connection lost");
            }
            final ClusterBrowser cb = getBrowser().getClusterBrowser();
            if (cb != null) {
                cb.getHeartbeatGraph().repaint();
                cb.getDrbdGraph().repaint();
            }
        }
    }

    /** Make an ssh connection to the host. */
    void connect(SSHGui sshGui,
                 final boolean progressIndicator,
                 final int index) {
        if (!isConnected()) {
            final String hostName = getName();
            if (progressIndicator) {
                Tools.startProgressIndicator(
                                hostName,
                                Tools.getString("Dialog.Host.SSH.Connecting")
                                + " (" + index + ")");
            }
            if (sshGui == null) {
                sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                    this,
                                    null);
            }

            connect(sshGui,
                    new ConnectionCallback() {
                        @Override
                        public void done(final int flag) {
                            setConnected();
                            getSSH().execCommandAndWait(":", /* activate sudo */
                                    false,
                                    false,
                                    10000);
                            getSSH().installGuiHelper();
                            getAllInfo();
                            if (progressIndicator) {
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ")");
                            }
                        }

                        @Override
                        public void doneError(final String errorText) {
                            setLoadingError();
                            setConnected();
                            if (progressIndicator) {
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ")");
                                Tools.progressIndicatorFailed(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ")");
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ")");
                            }
                        }
                    });
        }
    }

    /** Gets and stores info about the host. */
    void getAllInfo() {
        final Thread t = execCommand("GetHostAllInfo",
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 setLoadingError();
                             }
                         },
                         null, /* ConvertCmdCallback */
                         false,
                         HW_INFO_TIMEOUT);
        try {
            t.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Gets and stores hardware info about the host. */
    public void getHWInfo(final CategoryInfo[] infosToUpdate,
                          final ResourceGraph[] graphs) {
        final Thread t = execCommand("GetHostHWInfo",
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 for (final CategoryInfo ci
                                                     : infosToUpdate) {
                                     ci.updateTable(CategoryInfo.MAIN_TABLE);
                                 }
                                 for (final ResourceGraph g : graphs) {
                                     if (g != null) {
                                         g.repaint();
                                     }
                                 }
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 setLoadingError();
                                 getSSH().forceReconnect();
                             }
                         },
                         null, /* ConvertCmdCallback */
                         false,
                         HW_INFO_TIMEOUT);
        try {
            t.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getOutput(final String type, final StringBuffer buffer) {
        final String infoStart = "--" + type + "-info-start--";
        final String infoEnd = "--" + type + "-info-end--";
        final int infoStartLength = infoStart.length();
        final int infoEndLength = infoEnd.length();
        final int s = buffer.indexOf(infoStart);
        final int s2 = buffer.indexOf("\r\n", s);
        final int e = buffer.indexOf(infoEnd, s);
        String out = null;
        if (s > -1 && s < s2 && s2 <= e) {
            Double timestamp = null;
            final String ts = buffer.substring(s + infoStartLength, s2);
            try {
                timestamp = Double.parseDouble(ts);
            }  catch (final NumberFormatException nfe) {
                Tools.debug(this, "could not parse: " + ts + " " + nfe);
            }
            mInfoTimestampLock.lock();
            if (!infoTimestamp.containsKey(type)
                || (timestamp != null && timestamp >= infoTimestamp.get(type))) {
                infoTimestamp.put(type, timestamp);
                mInfoTimestampLock.unlock();
                out = buffer.substring(s2 + 2, e);
            } else {
                mInfoTimestampLock.unlock();
            }
            buffer.delete(0, e + infoEndLength + 2);
        }
        return out;
    }

    public void startPing() {
        final Thread t = ssh.execCommand(
                                Tools.getDistCommand(
                                                "PingCommand",
                                                dist,
                                                distVersionString,
                                                arch,
                                                null, /* ConvertCmdCallback */
                                                true), /* in bash */
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                             }
                         },
                         new NewOutputCallback() {
                             @Override
                             public void output(final String output) {
                                 ping.set(true);
                             }
                         },
                         false,
                         false,
                         PING_TIMEOUT);
        try {
            t.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Gets and stores hardware info about the host. */
    public void startHWInfoDaemon(final CategoryInfo[] infosToUpdate,
                                  final ResourceGraph[] graphs) {
        final Host host = this;
        serverStatusThread = ssh.execCommand(
                                Tools.getDistCommand(
                                                "HostHWInfoDaemon",
                                                dist,
                                                distVersionString,
                                                arch,
                                                null, /* ConvertCmdCallback */
                                                false), /* in bash */
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 for (final CategoryInfo ci
                                                     : infosToUpdate) {
                                     ci.updateTable(CategoryInfo.MAIN_TABLE);
                                 }
                                 for (final ResourceGraph g : graphs) {
                                     if (g != null) {
                                         g.repaint();
                                     }
                                 }
                                 if (host.isServerStatusLatch()) {
                                     final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 if (host.isServerStatusLatch()) {
                                     final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingError();
                             }
                         },
                         new NewOutputCallback() {
                             private StringBuffer outputBuffer =
                                                        new StringBuffer(300);
                             @Override
                             public void output(final String output) {
                                 outputBuffer.append(output);
                                 final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
                                 int i = 0;
                                 String hw, vm, drbdConfig;
                                 String hwUpdate = null;
                                 String vmUpdate = null;
                                 String drbdUpdate = null;
                                 do {
                                     i++;
                                     hw = getOutput("hw", outputBuffer); 
                                     if (hw != null) {
                                         hwUpdate = hw;
                                     }
                                     vmStatusLock();
                                     vm = getOutput("vm", outputBuffer); 
                                     if (vm != null) {
                                         vmUpdate = vm;
                                     }
                                     vmStatusUnlock();
                                     drbdStatusLock();
                                     drbdConfig = getOutput("drbd",
                                                            outputBuffer); 
                                     if (drbdConfig != null) {
                                         drbdUpdate = drbdConfig;
                                     }
                                     drbdStatusUnlock();
                                 } while (hw != null
                                          || vm != null
                                          || drbdConfig != null);

                                 Tools.chomp(outputBuffer);
                                 if (hwUpdate != null) {
                                     parseHostInfo(hwUpdate);
                                     for (final ResourceGraph g : graphs) {
                                         if (g != null) {
                                             g.repaint();
                                         }
                                     }
                                 }
                                 if (vmUpdate != null) {
                                     final VMSXML newVMSXML =
                                                        new VMSXML(host);
                                     if (newVMSXML.update(vmUpdate)) {
                                         cb.vmsXMLPut(host, newVMSXML);
                                         cb.updateVMS();
                                     }
                                 }
                                 if (drbdUpdate != null) {
                                     final DrbdXML dxml =
                                           new DrbdXML(cluster.getHostsArray(),
                                                       cb.getDrbdParameters());
                                     dxml.update(drbdUpdate);
                                     cb.setDrbdXML(dxml);
                                     cb.getDrbdGraph().getDrbdInfo().setParameters();
                                     cb.updateDrbdResources();
                                 }
                                 if (drbdUpdate != null
                                     || vmUpdate != null) {
                                     cb.updateHWInfo(host);
                                 }
                                 if (drbdUpdate != null) {
                                     cb.updateServerStatus(host);
                                 }
                                 if (isServerStatusLatch()) {
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }
                         },
                         false,
                         false,
                         HW_INFO_TIMEOUT);
        try {
            serverStatusThread.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Starts connection status. */
    public void startConnectionStatus() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (ping.get()) {
                       Tools.debug(this, "connection ok on " + getName(),
                                   2);
                       setConnected();
                       ping.set(false);
                    } else {
                       Tools.debug(this, "connection lost on "
                                         + getName(),
                                   2);
                       getSSH().forceReconnect();
                       setConnected();
                    }
                    Tools.sleep(PING_TIMEOUT);
                    if (getBrowser().getClusterBrowser().isCancelServerStatus()) {
                        break;
                    }
                }
            }
        });
        thread.start();
    }

    /** Returns whether host ssh connection was established. */
    public boolean isConnected() {
        if (ssh == null) {
            return false;
        }
        return ssh.isConnected();
    }

    /**
     * replaces variables in command. For output to the user set hidePassword
     * to true, so that variables like passwords are not shown to the user.
     * This functions should not be used to really sensitive passwords, since
     * it is not secure.
     *
     * following variables are defined:
     *
     * \@USER\@           user for download area
     * \@PASSWORD\@       password for download area
     * \@KERNELVERSION\@  version of the kernel
     * \@DRBDVERSION\@    version of drbd, that will be installed
     * \@DISTRIBUTION\@   distribution for which the drbd will be installed.
     *
     * @param command
     *          command in which the variables will be replaced
     *
     * @param hidePassword
     *          if set to true all vars will be replaced except of sensitive
     *          ones.
     *
     * @return command with replaced variables
     */
    public String replaceVars(String command,
                              final boolean hidePassword) {
        if (command.indexOf("@USER@") > -1) {
            command = command.replaceAll(
                                    "@USER@",
                                    Tools.getConfigData().getDownloadUser());
        }
        if (command.indexOf("@PASSWORD@") > -1) {
            if (hidePassword) {
                command = command.replaceAll("@PASSWORD@", "*****");
            } else {
                command = command.replaceAll(
                                   "@PASSWORD@",
                                   Tools.getConfigData().getDownloadPassword());
            }
        }
        String supportDir = "support";
        if (Tools.getConfigData().isStagingDrbd()) {
            supportDir = "support/staging";
        }
        final String drbdDir = "drbd";
        if (kernelVersion != null
            && command.indexOf("@KERNELVERSIONDIR@") > -1) {
            command = command.replaceAll("@KERNELVERSIONDIR@", kernelVersion);
        }
        if (drbdVersionToInstall != null
            && command.indexOf("@DRBDVERSION@") > -1) {
            command = command.replaceAll("@DRBDVERSION@", drbdVersionToInstall);
        }
        if (distVersion != null
            && command.indexOf("@DISTRIBUTION@") > -1) {
            command = command.replaceAll("@DISTRIBUTION@", distVersion);
        }
        if (arch != null
            && command.indexOf("@ARCH@") > -1) {
            command = command.replaceAll("@ARCH@", arch);
        }
        if (drbdBuildToInstall != null
            && command.indexOf("@BUILD@") > -1) {
            command = command.replaceAll("@BUILD@", drbdBuildToInstall);
        }
        if (drbdPackagesToInstall != null
            && command.indexOf("@DRBDPACKAGES@") > -1) {
            command = command.replaceAll("@DRBDPACKAGES@",
                                         drbdPackagesToInstall);
        }
        if (command.indexOf("@SUPPORTDIR@") > -1) {
            command = command.replaceAll("@SUPPORTDIR@", supportDir);
        }
        if (command.indexOf("@DRBDDIR@") > -1) {
            command = command.replaceAll("@DRBDDIR@", drbdDir);
        }
        if (command.indexOf("@GUI-HELPER@") > -1) {
            command = command.replaceAll("@GUI-HELPER@",
                                         "/usr/local/bin/lcmc-gui-helper-"
                                         + Tools.getRelease());
        }
        return command;
    }

    /**
     * Replaces variables in command.
     *
     * Following variables are defined:
     *
     * \@USER\@           user for download area
     * \@PASSWORD\@       password for download area
     * \@KERNELVERSION\@  version of the kernel
     * \@DRBDVERSION\@    version of drbd, that will be installed
     * \@DISTRIBUTION\@   distribution for which the drbd will be installed.
     *
     * @param command
     *          command in which the variables will be replaced
     *
     * @return command with replaced variables
     */
    public String replaceVars(final String command) {
        return replaceVars(command, false);
    }

    /** Parses the host info. */
    public void parseHostInfo(final String ans) {
        Tools.debug(this, "updating host info: " + getName(), 1);
        final String[] lines = ans.split("\\r?\\n");
        String type = "";
        final List<String> versionLines = new ArrayList<String>();
        final Map<String, BlockDevice> newBlockDevices =
                                     new LinkedHashMap<String, BlockDevice>();
        final Map<String, BlockDevice> newDrbdBlockDevices =
                                     new LinkedHashMap<String, BlockDevice>();
        final Map<String, NetInterface> newNetInterfaces =
                                     new LinkedHashMap<String, NetInterface>();
        final Map<String, Long> newVolumeGroups =
                                     new LinkedHashMap<String, Long>();
        final Map<String, Set<String>> newVolumeGroupsLVS =
                                     new HashMap<String, Set<String>>();
        final List<BlockDevice> newPhysicalVolumes =
                                                 new ArrayList<BlockDevice>();
        final Set<String> newFileSystems = new TreeSet<String>();
        final Set<String> newCryptoModules = new TreeSet<String>();
        final Set<String> newQemuKeymaps = new TreeSet<String>();
        final Set<String> newCpuMapModels = new TreeSet<String>();
        final Set<String> newCpuMapVendors = new TreeSet<String>();
        final Set<String> newMountPoints = new TreeSet<String>();

        final Map<String, List<String>> newGuiOptions =
                                          new HashMap<String, List<String>>();

        boolean netInfo = false;
        boolean diskInfo = false;
        boolean vgInfo = false;
        boolean filesystemsInfo = false;
        boolean cryptoInfo = false;
        boolean qemuKeymapsInfo = false;
        boolean cpuMapModelInfo = false;
        boolean cpuMapVerndorInfo = false;
        boolean mountPointsInfo = false;
        boolean guiInfo = false;
        boolean installationInfo = false;
        boolean guiOptionsInfo = false;
        boolean versionInfo = false;

        newMountPoints.add("/mnt/");
        String guiOptionName = null;

        for (String line : lines) {
            if (line.indexOf("ERROR:") == 0) {
                break;
            } else if (line.indexOf("WARNING:") == 0) {
                continue;
            }
            if ("net-info".equals(line)
                || "disk-info".equals(line)
                || "vg-info".equals(line)
                || "filesystems-info".equals(line)
                || "crypto-info".equals(line)
                || "qemu-keymaps-info".equals(line)
                || "cpu-map-model-info".equals(line)
                || "cpu-map-vendor-info".equals(line)
                || "mount-points-info".equals(line)
                || "gui-info".equals(line)
                || "installation-info".equals(line)
                || "gui-options-info".equals(line)
                || "version-info".equals(line)) {
                type = line;
                continue;
            }
            if ("net-info".equals(type)) {
                NetInterface netInterface = new NetInterface(line);
                if (netInterfaces.containsKey(netInterface.getName())) {
                    netInterface = netInterfaces.get(netInterface.getName());
                }
                newNetInterfaces.put(netInterface.getName(), netInterface);
                netInfo = true;
            } else if ("disk-info".equals(type)) {
                BlockDevice blockDevice = new BlockDevice(line);
                final String name = blockDevice.getName();
                if (name != null) {
                    final Matcher drbdM = DRBDP.matcher(name);
                    if (drbdM.matches()) {
                        if (drbdBlockDevices.containsKey(name)) {
                            /* get the existing block device object,
                               forget the new one. */
                            blockDevice = drbdBlockDevices.get(name);
                            blockDevice.update(line);
                        }
                        newDrbdBlockDevices.put(name, blockDevice);
                    } else {
                        if (blockDevices.containsKey(name)) {
                            /* get the existing block device object,
                               forget the new one. */
                            blockDevice = blockDevices.get(name);
                            blockDevice.update(line);
                        }
                        newBlockDevices.put(name, blockDevice);
                        if (blockDevice.getVolumeGroup() == null
                            && name.length() > 5 && name.indexOf('/', 5) < 0) {
                            final Matcher m = BDP.matcher(name);
                            if (m.matches()) {
                                newBlockDevices.remove(m.group(1));
                            }
                        }
                    }
                }
                final String vg = blockDevice.getVolumeGroup();
                if (vg != null) {
                    Set<String> logicalVolumes = newVolumeGroupsLVS.get(vg);
                    if (logicalVolumes == null) {
                        logicalVolumes = new HashSet<String>();
                        newVolumeGroupsLVS.put(vg, logicalVolumes);
                    }
                    final String lv = blockDevice.getLogicalVolume();
                    if (lv != null) {
                        logicalVolumes.add(lv);
                    }
                }
                if (blockDevice.isPhysicalVolume()) {
                    newPhysicalVolumes.add(blockDevice);
                }
                diskInfo = true;
            } else if ("vg-info".equals(type)) {
                final String[] vgi = line.split("\\s+");
                if (vgi.length == 2) {
                    newVolumeGroups.put(vgi[0], Long.parseLong(vgi[1]));
                } else {
                    Tools.appWarning("could not parse volume info: " + line);
                }
                vgInfo = true;
            } else if ("filesystems-info".equals(type)) {
                newFileSystems.add(line);
                filesystemsInfo = true;
            } else if ("crypto-info".equals(type)) {
                newCryptoModules.add(line);
                cryptoInfo = true;
            } else if ("qemu-keymaps-info".equals(type)) {
                newQemuKeymaps.add(line);
                qemuKeymapsInfo = true;
            } else if ("cpu-map-model-info".equals(type)) {
                newCpuMapModels.add(line);
                cpuMapModelInfo = true;
            } else if ("cpu-map-vendor-info".equals(type)) {
                newCpuMapVendors.add(line);
                cpuMapVerndorInfo = true;
            } else if ("mount-points-info".equals(type)) {
                newMountPoints.add(line);
                mountPointsInfo = true;
            } else if ("gui-info".equals(type)) {
                parseGuiInfo(line);
                guiInfo = true;
            } else if ("installation-info".equals(type)) {
                parseInstallationInfo(line);
                installationInfo = true;
            } else if ("gui-options-info".equals(type)) {
                guiOptionName = parseGuiOptionsInfo(line,
                                                    guiOptionName,
                                                    newGuiOptions);
                guiOptionsInfo = true;
            } else if ("version-info".equals(type)) {
                versionLines.add(line);
                versionInfo = true;
            }
        }

        if (netInfo) {
            netInterfaces = newNetInterfaces;
        }

        if (diskInfo) {
            blockDevices = newBlockDevices;
            drbdBlockDevices = newDrbdBlockDevices;
            physicalVolumes = newPhysicalVolumes;
            volumeGroupsLVS = newVolumeGroupsLVS;
        }

        if (vgInfo) {
            volumeGroups = newVolumeGroups;
        }

        if (filesystemsInfo) {
            fileSystems = newFileSystems;
        }

        if (cryptoInfo) {
            cryptoModules = newCryptoModules;
        }

        if (qemuKeymapsInfo) {
            qemuKeymaps = newQemuKeymaps;
        }

        if (cpuMapModelInfo) {
            cpuMapModels = newCpuMapModels;
        }

        if (cpuMapVerndorInfo) {
            cpuMapVendors = newCpuMapVendors;
        }

        if (mountPointsInfo) {
            mountPoints = newMountPoints;
        }

        if (versionInfo) {
            setDistInfo(versionLines.toArray(new String[versionLines.size()]));
        }

        if (guiOptionsInfo) {
            guiOptions = newGuiOptions;
        }

        getBrowser().updateHWResources(getNetInterfaces(),
                                       getBlockDevices(),
                                       getFileSystems());
    }

    /** Parses the gui info, with drbd and heartbeat graph positions. */
    private void parseGuiInfo(final String line) {
        final String[] tokens = line.split(";");
        String id = null;
        String x = null;
        String y = null;
        for (String token : tokens) {
            final String[] r = token.split("=");
            if (r.length == 2) {
                if (r[0].equals("hb") || r[0].equals("dr")) {
                    id = token;
                } else if (r[0].equals("x")) {
                    x = r[1];
                } else if (r[0].equals("y")) {
                    y = r[1];
                }
            }
        }
        if (id != null && x != null && y != null) {
            servicePositions.put(id, new Point2D.Double(
                                                  new Double(x).doubleValue(),
                                                  new Double(y).doubleValue()));
        }
    }
    /** Parses the gui options info. */
    public String parseGuiOptionsInfo(
                                    final String line,
                                    final String guiOptionName,
                                    final Map<String, List<String>> goptions) {
        if (line.length() > 2 && line.substring(0, 2).equals("o:")) {
            final String name = line.substring(2);
            goptions.put(name, new ArrayList<String>());
            return name;
        }
        if (guiOptionName != null) {
            final List<String> options = goptions.get(guiOptionName);
            if (options != null) {
                options.add(line);
            }
        }
        return guiOptionName;
    }

    /** Parses the installation info. */
    public void parseInstallationInfo(final String line) {
        final String[] tokens = line.split(":|\\s+");
        if (tokens.length < 2) {
            return;
        }
        if ("pm".equals(tokens[0])) {
            if (tokens.length == 2) {
                pacemakerVersion = tokens[1].trim();
            } else {
                pacemakerVersion = null;
            }
        } else if ("cs".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncVersion = tokens[1].trim();
            } else {
                corosyncVersion = null;
            }
        } else if ("ais".equals(tokens[0])) {
            if (tokens.length == 2) {
                openaisVersion = tokens[1].trim();
            } else {
                openaisVersion = null;
            }
        } else if ("ais-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                aisIsRc = "on".equals(tokens[1].trim());
            } else {
                aisIsRc = false;
            }
        } else if ("cs-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                csIsRc = "on".equals(tokens[1].trim());
            } else {
                csIsRc = false;
            }
        } else if ("cs-ais-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                csAisConf = "on".equals(tokens[1].trim());
            } else {
                csAisConf = false;
            }
        } else if ("cs-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                csRunning = "on".equals(tokens[1].trim());
            } else {
                csRunning = false;
            }
        } else if ("ais-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                aisRunning = "on".equals(tokens[1].trim());
                commLayerStarting = false;
                pcmkStarting = false;
            } else {
                aisRunning = false;
            }
        } else if ("cs-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                csInit = "on".equals(tokens[1].trim());
            } else {
                csInit = false;
            }
        } else if ("ais-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                aisInit = "on".equals(tokens[1].trim());
            } else {
                aisInit = false;
            }
        } else if ("hb".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatVersion = tokens[1].trim();
            } else {
                heartbeatVersion = null;
            }
        } else if ("hb-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatInit = "on".equals(tokens[1].trim());
            } else {
                heartbeatInit = false;
            }
        } else if ("hb-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatIsRc = "on".equals(tokens[1].trim());
            } else {
                heartbeatIsRc = false;
            }
        } else if ("hb-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatConf = "on".equals(tokens[1].trim());
            } else {
                heartbeatConf = false;
            }
        } else if ("hb-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatRunning = "on".equals(tokens[1].trim());
            } else {
                heartbeatRunning = false;
            }
        } else if ("pcmk-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkIsRc = "on".equals(tokens[1].trim());
            } else {
                pcmkIsRc = false;
            }
        } else if ("pcmk-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkRunning = "on".equals(tokens[1].trim());
            } else {
                pcmkRunning = false;
            }
        } else if ("pcmk-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkInit = "on".equals(tokens[1].trim());
            } else {
                pcmkInit = false;
            }
        } else if ("pcmk-svc-ver".equals(tokens[0])) {
            if (tokens.length == 2) {
                try {
                    pcmkServiceVersion = Integer.parseInt(tokens[1].trim());
                } catch (java.lang.NumberFormatException e) {
                    pcmkServiceVersion = -1;
                }
            }
        } else if ("drbd-loaded".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdLoaded = "on".equals(tokens[1].trim());
            } else {
                drbdLoaded = false;
            }
        } else if ("hb-lib-path".equals(tokens[0])) {
            if (tokens.length == 2) {
                hbLibPath = tokens[1].trim();
            } else {
                hbLibPath = null;
            }
        } else if ("hn".equals(tokens[0])) {
            if (tokens.length == 2) {
                hostname = tokens[1].trim();
            } else {
                hostname = null;
            }
            setName(hostname);
        } else if ("drbd".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdVersion = tokens[1].trim();
            } else {
                drbdVersion = null;
            }
        } else if ("drbd-mod".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdModuleVersion = tokens[1].trim();
            } else {
                drbdModuleVersion = null;
            }
        }
        corosyncHeartbeatRunning = heartbeatRunning || csRunning || aisRunning;
        if (commLayerStarting
            && (csRunning || aisRunning || heartbeatRunning)) {
            commLayerStarting = false;
        }
        if (pcmkStarting && pcmkRunning) {
            pcmkStarting = false;
        }
        if (commLayerStopping
            && !csRunning
            && !aisRunning
            && !heartbeatRunning) {
            commLayerStopping = false;
        }
    }

    /** Returns the graph position of id. */
    public Point2D getGraphPosition(final String id) {
        return servicePositions.get(id);
    }

    /** Resets the graph positions. */
    public void resetGraphPosition(final String id) {
        servicePositions.remove(id);
    }

    /** Saves the positions in the graphs. */
    public void saveGraphPositions(final Map<String, Point2D> positions) {
        final StringBuilder lines = new StringBuilder();
        for (final String id : positions.keySet()) {
            final Point2D p = positions.get(id);
            double x = p.getX();
            if (x < 0) {
                x = 0;
            }
            double y = p.getY();
            if (y < 0) {
                y = 0;
            }
            lines.append(id + ";x=" + x + ";y=" + y + "\n");
        }
        getSSH().createConfig(lines.toString(),
                              "drbdgui.cf",
                              "/var/lib/heartbeat/",
                              "0600",
                              false,
                              null,
                              null);
    }

    /** Sets the heartbeat version. */
    public void setHeartbeatVersion(final String heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /** Sets the corosync version. */
    public void setCorosyncVersion(final String corosyncVersion) {
        this.corosyncVersion = corosyncVersion;
    }

    /** Sets the pacemaker version. */
    public void setPacemakerVersion(final String pacemakerVersion) {
        this.pacemakerVersion = pacemakerVersion;
    }

    /** Sets the openais version. */
    public void setOpenaisVersion(final String openaisVersion) {
        this.openaisVersion = openaisVersion;
    }

    /** Returns the pacemaker version. */
    public String getPacemakerVersion() {
        return pacemakerVersion;
    }

    /** Returns the corosync version. */
    public String getCorosyncVersion() {
        return corosyncVersion;
    }

    /** Returns whether corosync is installed. */
    public boolean isCorosync() {
        return corosyncVersion != null;
    }

    /** Returns whether openais is a wrapper. */
    public boolean isOpenaisWrapper() {
        return "wrapper".equals(openaisVersion);
    }


    /** Returns the openais version. */
    public String getOpenaisVersion() {
        return openaisVersion;
    }

    /** Returns the heartbeat version. */
    public String getHeartbeatVersion() {
        return heartbeatVersion;
    }

    /** Sets that drbd will be upgraded. */
    public void setDrbdWillBeUpgraded(final boolean drbdWillBeUpgraded) {
        this.drbdWillBeUpgraded = drbdWillBeUpgraded;
    }

    /** Sets that drbd was installed. */
    public void setDrbdWasInstalled(final boolean drbdWasInstalled) {
        this.drbdWasInstalled = drbdWasInstalled;
    }

    /**
     * Returns true if drbd will be upgraded and drbd was installed.
     * TODO: ???
     */
    public boolean isDrbdUpgraded() {
        return drbdWillBeUpgraded && drbdWasInstalled;
    }

    /**
     * Sets the 'is loading' latch, so that something can wait while the load
     * sequence is running.
     */
    public void setIsLoading() {
        isLoadingGate = new CountDownLatch(1);
    }

    /** Waits on the 'is loading' latch. */
    public void waitOnLoading() {
        if (isLoadingGate == null) {
            return;
        }
        try {
            isLoadingGate.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * When loading is done, this latch is opened and whatever is waiting on it
     * is notified.
     */
    void setLoadingDone() {
        isLoadingGate.countDown();
    }

    /**
     * When loading is done but with an error. Currently it is the same as
     * setLoadingDone().
     */
    void setLoadingError() {
        isLoadingGate.countDown();
    }

    /** Waits for the server status latch. */
    public void waitForServerStatusLatch() {
        try {
            serverStatusLatch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** The latch is set when the server status is run for the first time. */
    public void serverStatusLatchDone() {
        serverStatusLatch.countDown();
    }

    /** Returns true if latch is set. */
    public boolean isServerStatusLatch() {
        return serverStatusLatch.getCount() == 1;
    }

    /** Returns ssh port. */
    public String getSSHPort() {
        return sshPort;
    }

    /** Returns ssh port as integer. */
    public int getSSHPortInt() {
        return Integer.valueOf(sshPort);
    }

    /** Sets ssh port. */
    public void setSSHPort(final String sshPort) {
        if (sshPort != null && !sshPort.equals(this.sshPort)) {
            ssh.disconnect();
        }
        this.sshPort = sshPort;
    }

    /** Returns sudo password. */
    public String getSudoPassword() {
        return sudoPassword;
    }

    /** Sets sudo password. */
    public void setSudoPassword(final String sudoPassword) {
        this.sudoPassword = sudoPassword;
    }

    /** Returns whether sudo is used. */
    public Boolean isUseSudo() {
        return useSudo;
    }

    /** Sets whether sudo should be used. */
    public void setUseSudo(final Boolean useSudo) {
        this.useSudo = useSudo;
    }

    /** Sets openais/pacemaker installation method index. */
    public void setPmInstallMethod(final String pmInstallMethod) {
        this.pmInstallMethod = pmInstallMethod;
    }

    /** Returns openais/pacemaker installation method. */
    public String getPmInstallMethod() {
        return pmInstallMethod;
    }

    /** Sets heartbeat/pacemaker installation method index. */
    public void setHbPmInstallMethod(final String hbPmInstallMethod) {
        this.hbPmInstallMethod = hbPmInstallMethod;
    }

    /** Returns heartbeat/pacemaker installation method. */
    public String getHbPmInstallMethod() {
        return hbPmInstallMethod;
    }

    /** Sets drbd installation method index. */
    public void setDrbdInstallMethod(final String drbdInstallMethod) {
        this.drbdInstallMethod = drbdInstallMethod;
    }

    /** Returns drbd installation method. */
    public String getDrbdInstallMethod() {
        return drbdInstallMethod;
    }

    /** Returns whether Corosync is rc script. */
    public boolean isCsRc() {
       return csIsRc;
    }

    /** Returns whether Openais is rc script. */
    public boolean isAisRc() {
       return aisIsRc;
    }

    /** Returns whether Pacemaker is rc script. */
    public boolean isPcmkRc() {
       return pcmkIsRc;
    }

    /** Returns whether Heartbeat has an init script. */
    public boolean isHeartbeatInit() {
       return heartbeatInit;
    }

    /** Returns whether Corosync has an init script. */
    public boolean isCsInit() {
       return csInit;
    }

    /** Returns whether Openais has an init script. */
    public boolean isAisInit() {
       return aisInit;
    }

    /** Returns whether Pacemaker has an init script. */
    public boolean isPcmkInit() {
       return pcmkInit;
    }


    /** Returns whether Corosync is running script. */
    public boolean isCsRunning() {
       return csRunning;
    }

    /** Returns whether Pacemakerd is running. */
    public boolean isPcmkRunning() {
       return pcmkRunning;
    }

    /** Returns whether Openais is running script. */
    public boolean isAisRunning() {
       return aisRunning;
    }

    /** Returns whether Corosync/Openais config exists. */
    public boolean isCsAisConf() {
       return csAisConf;
    }

    /** Returns whether Heartbeat is rc script. */
    public boolean isHeartbeatRc() {
       return heartbeatIsRc;
    }

    /** Returns whether Heartbeat is running script. */
    public boolean isHeartbeatRunning() {
       return heartbeatRunning;
    }

    /** Returns whether Heartbeat config exists. */
    public boolean isHeartbeatConf() {
       return heartbeatConf;
    }

    /** Returns whether drbd module is loaded. */
    public boolean isDrbdLoaded() {
       return drbdLoaded;
    }

    /** Returns MD5 checksum of VM Info from server. */
    String getVMInfoMD5() {
        return vmInfoMD5;
    }

    /** Sets MD5 checksum of VM Info from server. */
    void setVMInfoMD5(final String vmInfoMD5) {
        this.vmInfoMD5 = vmInfoMD5;
    }

    /** Sets index of this host in cluster. */
    void setIndex(final int index) {
        this.index = index;
    }

    /** Returns index of this host in cluster. */
    int getIndex() {
        return index;
    }

    /** This is part of testsuite. */
    boolean checkTest(final String checkCommand,
                      final String test,
                      final double index,
                      final String name,
                      final int maxHosts) {
        Tools.sleep(1500);
        final StringBuilder command = new StringBuilder(50);
        command.append(DistResource.SUDO + replaceVars("@GUI-HELPER@"));
        command.append(' ');
        command.append(checkCommand);
        command.append(' ');
        command.append(test);
        command.append(' ');
        final String indexString =
                            Double.toString(index).replaceFirst("\\.0+$", "");
        command.append(indexString);
        if (name != null) {
            command.append(' ');
            command.append(name);
        }
        int h = 1;
        for (final Host host : getCluster().getHosts()) {
            Tools.debug(this, "host" + h + " = " + host.getName(), 1);
            command.append(' ');
            command.append(host.getName());
            if (maxHosts > 0 && h >= maxHosts) {
                break;
            }
            h++;
        }
        command.append(" 2>&1");
        int i = 0;
        SSH.SSHOutput out = null;
        while (i < 5) {
            out = getSSH().execCommandAndWait(command.toString(),
                                              false,
                                              false,
                                              60000);
            /* 10 - no such file */
            if (out.getExitCode() == 0 || out.getExitCode() == 10) {
                break;
            }
            i++;
            RoboTest.sleepNoFactor(i * 2000);
        }
        String nameS = " " + name;
        if (name == null) {
            nameS = "";
        }
        if (i > 0) {
            RoboTest.info(getName() + " "
                           + test + " " + index + nameS + " tries: " + (i + 1));
        }
        RoboTest.info(getName() + " "
                       + test + " " + index + nameS + " " + out.getOutput());
        return out.getExitCode() == 0;
    }

    /** This is part of testsuite, it checks Pacemaker. */
    public boolean checkPCMKTest(final String test, final double index) {
        return checkTest("gui-test", test, index, null, 0);
    }

    /** This is part of testsuite, it checks DRBD. */
    public boolean checkDRBDTest(final String test, final double index) {
        final StringBuilder testName = new StringBuilder(20);
        if (Tools.getConfigData().getBigDRBDConf()) {
            testName.append("big-");
        }
        if (!hasVolumes()) {
            testName.append("novolumes-");
        }
        testName.append(test);
        return checkTest("gui-drbd-test", testName.toString(), index, null, 2);
    }

    /** This is part of testsuite, it checks VMs. */
    public boolean checkVMTest(final String test,
                               final double index,
                               final String name) {
        return checkTest("gui-vm-test", test, index, name, 0);
    }

    /** Returns color of this host. Null if it is default color. */
    String getColor() {
        if (savedColor == null || defaultColor == savedColor) {
            return null;
        }
        return Integer.toString(savedColor.getRGB());
    }

    /** Sets color of this host. Don't if it is default color. */
    public void setSavedColor(final String colorString) {
        try {
            savedColor = new Color(Integer.parseInt(colorString));
        } catch (java.lang.NumberFormatException e) {
            Tools.appWarning("could not parse: " + colorString);
            /* ignore it */
        }
    }

    /** Returns how much is free space in a volume group. */
    public long getFreeInVolumeGroup(final String volumeGroup) {
        final Long f = volumeGroups.get(volumeGroup);
        if (f == null) {
            return 0;
        }
        return f;
    }

    /** Returns all volume groups. */
    public Set<String> getVolumeGroupNames() {
        return volumeGroups.keySet();
    }

    /** Returns if corosync or heartbeat is running, null for unknown. */
    public Boolean getCorosyncHeartbeatRunning() {
        return corosyncHeartbeatRunning;
    }

    /** Sets corosyncHeartbeatRunning. */
    public void setCorosyncHeartbeatRunning(
                                     final Boolean corosyncHeartbeatRunning) {
        this.corosyncHeartbeatRunning = corosyncHeartbeatRunning;
    }

    /** Returns true if comm layer is stopping. */
    public boolean isCommLayerStopping() {
        return commLayerStopping;
    }

    /** Sets whether the comm layer is stopping. */
    public void setCommLayerStopping(final boolean commLayerStopping) {
        this.commLayerStopping = commLayerStopping;
    }

    /** Returns true if comm layer is starting. */
    public boolean isCommLayerStarting() {
        return commLayerStarting;
    }

    /** Sets whether the comm layer is starting. */
    public void setCommLayerStarting(final boolean commLayerStarting) {
        this.commLayerStarting = commLayerStarting;
    }

    /** Returns true if pcmk is starting. */
    public boolean isPcmkStarting() {
        return pcmkStarting;
    }

    /** Sets whether the pcmk is starting. */
    public void setPcmkStarting(final boolean pcmkStarting) {
        this.pcmkStarting = pcmkStarting;
    }

    /** Returns whether pacemaker is started by corosync. */
    public boolean isPcmkStartedByCorosync() {
        if (pcmkServiceVersion == 0) {
            return true;
        }
        return false;
    }

    /** Sets libvirt version. */
    void setLibvirtVersion(final String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }

    /** Returns libvirt version. */
    String getLibvirtVersion() {
        return libvirtVersion;
    }

    /** Returns logical volumes from volume group. */
    public Set<String> getLogicalVolumesFromVolumeGroup(final String vg) {
        return volumeGroupsLVS.get(vg);
    }

    /** Returns all logical volumes. */
    public Set<String> getAllLogicalVolumes() {
        final Set<String> allLVS = new LinkedHashSet<String>();
        for (final String vg : volumeGroups.keySet()) {
            final Set<String> lvs = volumeGroupsLVS.get(vg);
            if (lvs != null) {
                allLVS.addAll(lvs);
            }
        }
        return allLVS;
    }

    /** Returns whether DRBD has volume feature. */
    public boolean hasVolumes() {
        try {
            return Tools.compareVersions(getDrbdVersion(), "8.4") >= 0;
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }
        return true;
    }

    /** Returns physical volumes. */
    public List<BlockDevice> getPhysicalVolumes() {
        return physicalVolumes;
    }

    /** Set whether this host should be saved. */
    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    /** Return whether this host should be saved. */
    public boolean isSavable() {
        return savable;
    }

    /** Return block device object of the drbd device. */
    public BlockDevice getDrbdBlockDevice(final String device) {
        return drbdBlockDevices.get(device);
    }

    /** Return DRBD block device objects. */
    public Collection<BlockDevice> getDrbdBlockDevices() {
        return drbdBlockDevices.values();
    }

    /** Return list of block devices that have the specified VG. */
    public List<BlockDevice> getPhysicalVolumes(final String vg) {
        final List<BlockDevice> bds = new ArrayList<BlockDevice>();
        if (vg == null) {
            return bds;
        }
        for (final BlockDevice b : physicalVolumes) {
            if (vg.equals(b.getVolumeGroupOnPhysicalVolume())) {
                bds.add(b);
            }
        }
        return bds;
    }

    /** drbdStatusTryLock global lock. */
    public boolean drbdStatusTryLock() {
        return mDRBDStatusLock.tryLock();
    }

    /** drbdStatusLock global lock. */
    public void drbdStatusLock() {
        mDRBDStatusLock.lock();
    }

    /** drbdStatusLock global unlock. */
    public void drbdStatusUnlock() {
        mDRBDStatusLock.unlock();
    }

    /** vmStatusLock global lock. */
    public void vmStatusLock() {
        mUpdateVMSlock.lock();
    }

    /** vmStatusLock try global lock. */
    public boolean vmStatusTryLock() {
        return mUpdateVMSlock.tryLock();
    }

    /** vmStatusLock global unlock. */
    public void vmStatusUnlock() {
        mUpdateVMSlock.unlock();
    }

    /** Return whether the user is root */
    public boolean isRoot() {
        return ROOT_USER.equals(username);
    }

    /** Return options for GUI elements. */
    public List<String> getGuiOptions(final String name) {
        final List<String> opts = guiOptions.get(name);
        if (opts == null) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(guiOptions.get(name));
    }
}

