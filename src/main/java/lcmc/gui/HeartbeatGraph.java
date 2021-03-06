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

package lcmc.gui;

import lcmc.utilities.Tools;
import lcmc.utilities.MyMenuItem;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.ServiceInfo;
import lcmc.gui.resources.GroupInfo;
import lcmc.gui.resources.HbConnectionInfo;
import lcmc.gui.resources.HostInfo;
import lcmc.gui.resources.ConstraintPHInfo;
import lcmc.gui.resources.PcmkMultiSelectionInfo;
import lcmc.data.Subtext;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;

import edu.uci.ics.jung.graph.util.Pair;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.RoundRectangle2D;
import java.awt.GradientPaint;
import java.awt.BasicStroke;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import edu.uci.ics.jung.visualization.Layer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class creates graph and provides methods to add new nodes, edges,
 * remove or modify them.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class HeartbeatGraph extends ResourceGraph {
    /** List with edges that are order constraints. */
    private final Set<Edge> edgeIsOrderList = new HashSet<Edge>();
    /** List with edges that should be kept as order constraints. */
    private final Set<Edge> keepEdgeIsOrderList = new HashSet<Edge>();
    /** List with edges that are colocation constraints. */
    private final Set<Edge> edgeIsColocationList = new HashSet<Edge>();
    /** List with edges that should be kept as colocation constraints. */
    private final Set<Edge> keepEdgeIsColocationList = new HashSet<Edge>();
    /** Vertex is present lock. */
    private final Lock mVertexIsPresentListLock = new ReentrantLock();
    /** List with vertices that are present. */
    private Set<Vertex> vertexIsPresentList = new HashSet<Vertex>();
    /** Map from vertex to 'Add service' menu. */
    private final Map<Vertex, JMenu> vertexToAddServiceMap =
                                                new HashMap<Vertex, JMenu>();
    /** Map from vertex to 'Add existing service' menu. */
    private final Map<Vertex, JMenu> vertexToAddExistingServiceMap =
                                                new HashMap<Vertex, JMenu>();
    /** Map from edge to the hb connection info of this constraint. */
    private final Map<Edge, HbConnectionInfo> edgeToHbconnectionMap =
                                new LinkedHashMap<Edge, HbConnectionInfo>();
    /** Map from hb connection info to the edge. */
    private final Map<HbConnectionInfo, Edge> hbconnectionToEdgeMap =
                                new LinkedHashMap<HbConnectionInfo, Edge>();
    /** Pcmk connection lock. */
    private final ReadWriteLock mHbConnectionLock =
                                                  new ReentrantReadWriteLock();
    /** Pcmk connection read lock. */
    private final Lock mHbConnectionReadLock = mHbConnectionLock.readLock();
    /** Pcmk connection write lock. */
    private final Lock mHbConnectionWriteLock = mHbConnectionLock.writeLock();
    /** Map from the vertex to the host. */
    private final Map<Vertex, HostInfo> vertexToHostMap =
                                         new LinkedHashMap<Vertex, HostInfo>();
    /** Map from the host to the vertex. */
    private final Map<Info, Vertex> hostToVertexMap =
                                         new LinkedHashMap<Info, Vertex>();
    /** Map from the vertex to the constraint placeholder. */
    private final Map<Vertex, ConstraintPHInfo> vertexToConstraintPHMap =
                                 new HashMap<Vertex, ConstraintPHInfo>();
    /** Map from the host to the vertex. */
    private final Map<Info, Vertex> constraintPHToVertexMap =
                                         new HashMap<Info, Vertex>();

    /** The first X position of the host. */
    private int hostDefaultXPos = 10;
    /** Interval beetween two animation frames. */
    private Info multiSelectionInfo = null;
    /** X position of a new block device. */
    private static final int BD_X_POS = 15;
    /** Y position of the host. */
    private static final int HOST_Y_POS = 40;
    /** Vertical step in pixels by which the hosts are drawn in the graph. */
    private static final int HOST_STEP_X = 230;
    /** Minimum vertical position. */
    private static final int MIN_Y_POS = 20;
    /** Maximum horizontal position. */
    private static final int MAX_X_POS = 2600;
    /** Maximum vertical position. */
    private static final int MAX_Y_POS = 2600;
    /** Height of the vertices. */
    private static final int VERTEX_HEIGHT = 50;
    /** Host standby icon. */
    private static final ImageIcon HOST_STANDBY_ICON =
     Tools.createImageIcon(Tools.getDefault("HeartbeatGraph.HostStandbyIcon"));
    /** Icon that indicates a running service. */
    private static final ImageIcon SERVICE_RUNNING_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceRunningIcon"));
    /** Icon that indicates a running that failed. */
    private static final ImageIcon SERVICE_RUNNING_FAILED_ICON =
                                Tools.createImageIcon(Tools.getDefault(
                                   "HeartbeatGraph.ServiceRunningFailedIcon"));
    /** Icon that indicates a started service (but not running). */
    private static final ImageIcon SERVICE_STARTED_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceStartedIcon"));
    /** Icon that indicates a stopping service (but not stopped). */
    private static final ImageIcon SERVICE_STOPPING_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceStoppingIcon"));
    /** Icon that indicates a not running service. */
    private static final ImageIcon SERVICE_STOPPED_ICON =
                                Tools.createImageIcon(Tools.getDefault(
                                        "HeartbeatGraph.ServiceStoppedIcon"));
    /** Icon that indicates a not running service that failed. */
    private static final ImageIcon SERVICE_STOPPED_FAILED_ICON =
                                Tools.createImageIcon(Tools.getDefault(
                                   "HeartbeatGraph.ServiceStoppedFailedIcon"));
    /** Icon that indicates an unmanaged service. */
    private static final ImageIcon SERVICE_UNMANAGED_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceUnmanagedIcon"));
    /** Icon that indicates a migrated service. */
    private static final ImageIcon SERVICE_MIGRATED_ICON =
                                     Tools.createImageIcon(Tools.getDefault(
                                       "HeartbeatGraph.ServiceMigratedIcon"));
    /** Prepares a new <code>HeartbeatGraph</code> object. */
    HeartbeatGraph(final ClusterBrowser clusterBrowser) {
        super(clusterBrowser);
    }

    /** Inits the graph. */
    @Override
    protected void initGraph() {
        super.initGraph(new DirectedSparseGraph<Vertex, Edge>());
    }

    /**
     * Returns true if vertex v is one of the ancestors or the same as
     * vertex p. The vertex and edge lists must be locked when called.
     */
    private boolean isAncestor(final Vertex v,
                               final Vertex p,
                               final List<Vertex> list) {
        if (p.equals(v)) {
            return true;
        }
        for (final Vertex pre : getGraph().getPredecessors(p)) {
            if (list.contains(pre)) {
                return false;
            }
            list.add(pre);
            if (isAncestor(v, pre, list)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if service exists already in the parent ancestor line
     * or parent already has this service as a child.
     */
    boolean existsInThePath(final ServiceInfo si,
                            final ServiceInfo parent) {
        if (parent == null) {
            return true;
        }
        if (si.equals(parent)) {
            return true;
        }
        lockGraph();
        final Vertex v = getVertex(si);
        if (v == null) {
            unlockGraph();
            return false;
        }
        final Vertex pv = getVertex(parent);
        if (pv == null) {
            unlockGraph();
            return false;
        }
        if (getGraph().isSuccessor(pv, v)
            || isAncestor(v, pv, new ArrayList<Vertex>())) {
            unlockGraph();
            return true;
        }
        unlockGraph();
        return false;
    }

    /**
     * Returns heartbeat ids of all services with colocation with this service.
     */
    String[] getColocationNeighbours(final ServiceInfo si) {
        final List<String> parentsHbIds = new ArrayList<String>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getPredecessors(v)) {
                final ServiceInfo psi = (ServiceInfo) getInfo(pV);
                if (psi == null) {
                    continue;
                }
                parentsHbIds.add(psi.getService().getHeartbeatId());
            }
            unlockGraph();
        }

        return parentsHbIds.toArray(new String [parentsHbIds.size()]);
    }

    /** Returns heartbeat ids from this service info's parents. */
    public Set<ServiceInfo> getParents(final ServiceInfo si) {
        final Set<ServiceInfo> parents = new TreeSet<ServiceInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getPredecessors(v)) {
                final ServiceInfo psi = (ServiceInfo) getInfo(pV);
                if (psi != null) {
                    parents.add(psi);
                }
            }
            unlockGraph();
        }
        return parents;
    }

    /** Returns children of the service. */
    public Set<ServiceInfo> getChildren(final ServiceInfo si) {
        final Set<ServiceInfo> children = new TreeSet<ServiceInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getSuccessors(v)) {
                final ServiceInfo psi = (ServiceInfo) getInfo(pV);
                if (psi != null) {
                    children.add(psi);
                }
            }
            unlockGraph();
        }
        return children;
    }

    /** Returns children and parents of the service. */
    public Set<ServiceInfo> getChildrenAndParents(final ServiceInfo si) {
        final Set<ServiceInfo> chAndP = new TreeSet<ServiceInfo>();
        chAndP.addAll(getChildren(si));
        chAndP.addAll(getParents(si));
        return chAndP;
    }

    /** Returns all connections from this service. */
    public HbConnectionInfo[] getHbConnections(final ServiceInfo si) {
        final List<HbConnectionInfo> infos = new ArrayList<HbConnectionInfo>();
        final Vertex v = getVertex(si);
        if (v != null) {
            lockGraph();
            for (final Vertex pV : getGraph().getPredecessors(v)) {
                Edge edge = getGraph().findEdge(pV, v);

                if (edge == null) {
                    edge = getGraph().findEdge(v, pV);
                }
                if (edge != null) {
                    mHbConnectionReadLock.lock();
                    final HbConnectionInfo hbci =
                                              edgeToHbconnectionMap.get(edge);
                    mHbConnectionReadLock.unlock();
                    if (hbci != null) {
                        infos.add(hbci);
                    }
                }
            }
            for (final Vertex sV : getGraph().getSuccessors(v)) {
                Edge edge = getGraph().findEdge(v, sV);

                if (edge == null) {
                    edge = getGraph().findEdge(sV, v);
                }
                if (edge != null) {
                    mHbConnectionReadLock.lock();
                    final HbConnectionInfo hbci =
                                              edgeToHbconnectionMap.get(edge);
                    mHbConnectionReadLock.unlock();
                    if (hbci != null) {
                        infos.add(hbci);
                    }
                }
            }
            unlockGraph();
        }
        return infos.toArray(new HbConnectionInfo[infos.size()]);
    }

    /** Returns id that is used for saving of the vertex positions to a file. */
    @Override
    protected String getId(final Info i) {
        final String id = i.getId();
        if (id == null) {
            return null;
        }
        return "hb=" + i.getId();
    }

    /**
     * Exchanges object in vertex, e.g., wenn ra changes to/from m/s resource.
     */
    public void exchangeObjectInTheVertex(final ServiceInfo newSI,
                                          final ServiceInfo oldSI) {
        final Vertex v = getVertex(oldSI);
        removeVertex(oldSI);
        putInfoToVertex(newSI, v);
        putVertexToInfo(v, (Info) newSI);
        somethingChanged();
    }

    /**
     * adds resource/service to the graph. Adds also edge from the parent.
     * If vertex exists add only the edge. If parent is null add only vertex
     * without edge. Return true if vertex existed in the graph before.
     */
    public boolean addResource(final ServiceInfo serviceInfo,
                               final ServiceInfo parent,
                               final Point2D pos,
                               final boolean colocation,
                               final boolean order,
                               final boolean testOnly) {
        boolean vertexExists = true;
        Vertex v = getVertex(serviceInfo);
        if (v == null) {
            v = new Vertex();
            if (pos == null) {
                final Point2D newPos = getSavedPosition(serviceInfo);
                if (newPos == null) {
                    final Point2D max = getLastPosition();
                    final float maxYPos = (float) max.getY();
                    getVertexLocations().put(v,
                                             new Point2D.Float(BD_X_POS,
                                                               maxYPos + 40));
                    putVertexLocations();
                } else {
                    getVertexLocations().put(v, newPos);
                    putVertexLocations();
                }
            } else {
                final Point2D p = getVisualizationViewer()
                                    .getRenderContext()
                                        .getMultiLayerTransformer()
                                            .inverseTransform(Layer.VIEW, pos);
                getVertexLocations().put(v, posWithScrollbar(p));
                putVertexLocations();
            }

            lockGraph();
            getGraph().addVertex(v);
            unlockGraph();
            somethingChanged();
            putInfoToVertex(serviceInfo, v);
            putVertexToInfo(v, (Info) serviceInfo);
            vertexExists = false;
        } else if (testOnly) {
            addTestEdge(getVertex(parent), getVertex(serviceInfo));
        }

        if (parent != null && !testOnly) {
            if (colocation) {
                addColocation(null, serviceInfo, parent);
            }
            if (order) {
                addOrder(null, parent, serviceInfo);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        });
        return vertexExists;
    }

    /** Adds order constraint from parent to the service. */
    public void addOrder(final String ordId,
                         final ServiceInfo parent,
                         final ServiceInfo serviceInfo) {
        if (parent == null || serviceInfo == null) {
            return;
        }
        Vertex vP = getVertex(parent);
        Vertex v = getVertex(serviceInfo);
        if (vP == v) {
            return;
        }
        if (parent.isConstraintPH() || serviceInfo.isConstraintPH()) {
            /* if it is sequential rsc set change it to show to the resource
             * instead on placeholder. */

            final ConstraintPHInfo cphi;
            if (parent.isConstraintPH()) {
                cphi = (ConstraintPHInfo) parent;
                final ServiceInfo si = cphi.prevInSequence(serviceInfo, false);
                if (si != null) {
                    vP = getVertex(si);
                }
            } else {
                cphi = (ConstraintPHInfo) serviceInfo;
                final ServiceInfo si = cphi.nextInSequence(parent, false);
                if (si != null) {
                    v = getVertex(si);
                }
            }
        }
        if (v == null || vP == null) {
            return;
        }
        final Vertex vP0 = vP;
        final Vertex v0 = v;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mHbConnectionWriteLock.lock();
                Edge edge = null;
                try {
                    lockGraph();
                    edge = getGraph().findEdge(vP0, v0);
                    if (edge == null) {
                        edge = getGraph().findEdge(v0, vP0);
                        unlockGraph();
                        if (edge != null) {
                            edge.reverse();
                            if (edgeIsOrderList.contains(edge)) {
                                edge.setWrongColocation(true);
                            } else {
                                edge.setWrongColocation(false);
                            }
                        }
                    } else {
                        if (!edgeIsOrderList.contains(edge)) {
                            edge.setWrongColocation(false);
                        }
                        unlockGraph();
                    }
                } catch (final Exception e) {
                    unlockGraph();
                }
                HbConnectionInfo hbci;
                if (edge == null) {
                    hbci = getClusterBrowser().getNewHbConnectionInfo();
                    edge = new Edge(vP0, v0);
                    lockGraph();
                    getGraph().addEdge(edge, vP0, v0);
                    unlockGraph();
                    edgeToHbconnectionMap.put(edge, hbci);
                    hbconnectionToEdgeMap.put(hbci, edge);
                } else {
                    hbci = edgeToHbconnectionMap.get(edge);
                }
                mHbConnectionWriteLock.unlock();
                if (hbci != null) {
                    hbci.addOrder(ordId, parent, serviceInfo);
                    edgeIsOrderList.add(edge);
                    keepEdgeIsOrderList.add(edge);
                }
            }
        });
    }

    /** Reverse the edge. */
    void reverse(final HbConnectionInfo hbConnectionInfo) {
        mHbConnectionReadLock.lock();
        final Edge e = hbconnectionToEdgeMap.get(hbConnectionInfo);
        mHbConnectionReadLock.unlock();
        if (e != null && edgeIsColocationList.contains(e)) {
            e.reverse();
        }
    }

    /** Returns whether these two services are colocated. */
    public boolean isColocation(final ServiceInfo parent,
                                final ServiceInfo serviceInfo) {
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (vP == null || v == null) {
            return false;
        }

        lockGraph();
        Edge edge = getGraph().findEdge(vP, v);
        if (edge == null) {
            edge = getGraph().findEdge(v, vP);
        }
        unlockGraph();
        if (edge != null && edgeIsColocationList.contains(edge)) {
            return true;
        }
        return false;
    }

    /** Returns whether these two services are ordered. */
    public boolean isOrder(final ServiceInfo parent,
                           final ServiceInfo serviceInfo) {
        final Vertex vP = getVertex(parent);
        final Vertex v = getVertex(serviceInfo);
        if (vP == null || v == null) {
            return false;
        }

        lockGraph();
        Edge edge = getGraph().findEdge(vP, v);
        if (edge == null) {
            edge = getGraph().findEdge(v, vP);
        }
        unlockGraph();
        if (edge != null && edgeIsOrderList.contains(edge)) {
            return true;
        }
        return false;
    }

    /** Adds colocation constraint from parent to the service. */
    public void addColocation(final String colId,
                              final ServiceInfo rsc,
                              final ServiceInfo withRsc) {
        if (rsc == null || withRsc == null) {
            return;
        }
        Vertex vRsc = getVertex(rsc);
        Vertex vWithRsc = getVertex(withRsc);
        if (vRsc == vWithRsc) {
            return;
        }
        if (rsc.isConstraintPH() || withRsc.isConstraintPH()) {
            /* if it is sequential rsc set change it to show to the resource
             * instead on placeholder. */

            final ConstraintPHInfo cphi;
            if (rsc.isConstraintPH()) {
                cphi = (ConstraintPHInfo) rsc;
                final ServiceInfo si = cphi.nextInSequence(withRsc,
                                                           true);
                if (si != null) {
                    vRsc = getVertex(si);
                }
            } else {
                cphi = (ConstraintPHInfo) withRsc;
                final ServiceInfo si = cphi.prevInSequence(rsc,
                                                           true);
                if (si != null) {
                    vWithRsc = getVertex(si);
                }
            }
        }
        if (vWithRsc == null || vRsc == null) {
            return;
        }
        final Vertex vWithRsc0 = vWithRsc;
        final Vertex vRsc0 = vRsc;
        final ServiceInfo withRsc0 = withRsc;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mHbConnectionWriteLock.lock();
                Edge edge = null;
                try {
                    lockGraph();
                    edge = getGraph().findEdge(vWithRsc0, vRsc0);
                    if (edge == null) {
                        edge = getGraph().findEdge(vRsc0, vWithRsc0);
                        if (edge != null) {
                            edge.setWrongColocation(true);
                        }
                    } else {
                        edge.setWrongColocation(false);
                    }
                    unlockGraph();
                } catch (final Exception e) {
                    /* ignore */
                }
                HbConnectionInfo hbci;
                if (edge == null) {
                    hbci = getClusterBrowser().getNewHbConnectionInfo();
                    edge = new Edge(vWithRsc0, vRsc0);
                    lockGraph();
                    getGraph().addEdge(edge, vWithRsc0, vRsc0);
                    unlockGraph();
                    edgeToHbconnectionMap.put(edge, hbci);
                    hbconnectionToEdgeMap.put(hbci, edge);
                } else {
                    hbci = edgeToHbconnectionMap.get(edge);
                }
                mHbConnectionWriteLock.unlock();
                if (hbci != null) {
                    hbci.addColocation(colId, rsc, withRsc0);
                    edgeIsColocationList.add(edge);
                    keepEdgeIsColocationList.add(edge);
                }
            }
        });
    }

    /** Removes items from order list. */
    public void clearKeepOrderList() {
        lockGraph();
        for (final Edge edge : getGraph().getEdges()) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
            mHbConnectionReadLock.unlock();
            if (hbci != null && !hbci.isNew()) {
                /* don't remove the new ones. */
                keepEdgeIsOrderList.remove(edge);
            }
        }
        unlockGraph();
    }

    /** Removes items from colocation list. */
    public void clearKeepColocationList() {
        lockGraph();
        for (final Edge edge : getGraph().getEdges()) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
            mHbConnectionReadLock.unlock();
            if (hbci != null && !hbci.isNew()) {
                /* don't remove the new ones. */
                keepEdgeIsColocationList.remove(edge);
            }
        }
        unlockGraph();
    }

    /** Returns label for service vertex. */
    @Override
    protected String getMainText(final Vertex v, final boolean testOnly) {
        String str;
        if (vertexToHostMap.containsKey(v)) {
            str = vertexToHostMap.get(v).toString();
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            str = vertexToConstraintPHMap.get(v).getMainTextForGraph();
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si == null) {
                return "";
            }
            final Set<Vertex> vipl = getVertexIsPresentList();
            putVertexIsPresentList();
            if (si.getService().isRemoved()) {
                str = Tools.getString("HeartbeatGraph.Removing");
            } else if (vipl.contains(v)) {
                str = si.getMainTextForGraph();
            } else {
                if (si.getService().isNew()) {
                    str = si.getMainTextForGraph();
                } else {
                    str = Tools.getString("HeartbeatGraph.Unconfigured");
                }
            }
        }
        return str;
    }

    /** Returns shape of the service vertex. */
    @Override
    protected Shape getVertexShape(final Vertex v,
                                   final VertexShapeFactory<Vertex> factory) {
        if (vertexToHostMap.containsKey(v)) {
            return factory.getRectangle(v);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return factory.getEllipse(v);
        } else {
            final RoundRectangle2D r = factory.getRoundRectangle(v);

            return new RoundRectangle2D.Double(r.getX(),
                                        r.getY(),
                                        r.getWidth(),
                                        r.getHeight(),
                                        20,
                                        20);
        }
    }

    /** Picks info. */
    @Override
    public void pickInfo(final Info i) {
        final GroupInfo groupInfo = ((ServiceInfo) i).getGroupInfo();
        if (groupInfo == null) {
            super.pickInfo(i);
        } else {
            /* group is picked in the graph, if group service was selected. */
            final Vertex v = getVertex(groupInfo);
            final PickedState<Edge> psEdge =
              getVisualizationViewer().getRenderContext().getPickedEdgeState();
            final PickedState<Vertex> psVertex =
             getVisualizationViewer().getRenderContext().getPickedVertexState();
            psEdge.clear();
            psVertex.clear();
            psVertex.pick(v, true);
        }
    }

    /** Handles right click on the service vertex and creates popup menu. */
    @Override
    protected JPopupMenu handlePopupVertex(final Vertex v,
                                           final List<Vertex> pickedV,
                                           final Point2D p) {
        if (pickedV.size() > 1) {
            final Info msi = multiSelectionInfo;
            if (msi != null) {
                return msi.getPopup();
            }
        }
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            if (hi == null) {
                return null;
            }
            return hi.getPopup(p);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            final ConstraintPHInfo cphi = vertexToConstraintPHMap.get(v);
            if (cphi == null) {
                return null;
            }
            return cphi.getPopup(p);
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si == null) {
                return null;
            }
            return si.getPopup(p);
        }
    }

    /**
     * Recreates the add existing service popup, since it can change in
     * very unlikely event.
     */
    private void reloadAddExistingServicePopup(final JMenu addServiceMenuItem,
                                               final Vertex v) {
        if (addServiceMenuItem == null) {
            return;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        addServiceMenuItem.removeAll();
        if (si == null) {
            return;
        }
        boolean separatorAdded = false;
        final boolean tOnly = isTestOnly();
        for (final ServiceInfo asi
                            : getClusterBrowser().getExistingServiceList(si)) {
            final MyMenuItem mmi = new MyMenuItem(
                          asi.toString(),
                          null,
                          null,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;
                @Override
                public void action() {
                    si.addServicePanel(asi,
                                       null,
                                       false, /* TODO: colocation only */
                                       false, /* order only */
                                       true,
                                       getClusterBrowser().getDCHost(),
                                       tOnly);
                    repaint();
                }
            };
            if (asi.getStringValue().equals("Filesystem")
                || asi.getStringValue().equals("IPaddr2")) {

                mmi.setSpecialFont();
                addServiceMenuItem.add(mmi);
            } else {
                if (!separatorAdded) {
                    addServiceMenuItem.addSeparator();
                    separatorAdded = true;
                }
                addServiceMenuItem.add(mmi);
            }
        }
    }

    /** Handles right click on the edge and creates popup menu. */
    @Override
    protected JPopupMenu handlePopupEdge(final Edge edge) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
        mHbConnectionReadLock.unlock();
        if (hbci == null) {
            return null;
        }
        return hbci.getPopup();
    }

    /** Handles right click on the background and creates popup menu. */
    @Override
    protected JPopupMenu handlePopupBackground(final Point2D pos) {
        return getClusterBrowser().getServicesInfo().getPopup(pos);
    }

    /**
     * After service vertex v was released in position pos, set its location
     * there, so that it doesn't jump back. If position is outside of the view,
     * bring the vertex back in the view.
     */
    @Override
    protected void vertexReleased(final Vertex v, final Point2D pos) {
        double x = pos.getX();
        double y = pos.getY();
        final double minPos = (getVertexWidth(v)
                               - getDefaultVertexWidth(v)) / 2;
        x = x < minPos ? minPos : x;
        x = x > MAX_X_POS ? MAX_X_POS : x;
        y = y < MIN_Y_POS ? MIN_Y_POS : y;
        y = y > MAX_Y_POS ? MAX_Y_POS : y;
        pos.setLocation(x, y);
        getVertexLocations().put(v, pos);
        putVertexLocations();
        getLayout().setLocation(v, pos);
    }

    /**
     * Selects Info in the view if a service vertex was pressed. If more
     * vertices were selected, it does not do anything.
     */
    @Override
    protected void oneVertexPressed(final Vertex v) {
        pickVertex(v);
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            if (hi != null) {
                Tools.getGUIData().setTerminalPanel(
                                              hi.getHost().getTerminalPanel());
                getClusterBrowser().setRightComponentInView(hi);
            }
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            final ConstraintPHInfo cphi = vertexToConstraintPHMap.get(v);
            if (cphi != null) {
                getClusterBrowser().setRightComponentInView(cphi);
            }
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si != null) {
                si.selectMyself();
            }
        }
    }

    /**
     * Is called, when background of the graph is clicked. It deselects
     * selected node.
     */
    @Override
    protected void backgroundClicked() {
        getClusterBrowser().getServicesInfo().selectMyself();
    }

    /** Returns tool tip when mouse is over a service vertex. */
    @Override
    String getVertexToolTip(final Vertex v) {
        final boolean tOnly = isTestOnly();

        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getToolTipForGraph(tOnly);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getToolTipForGraph(tOnly);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getToolTipText(tOnly);
    }

    /** Returns the tool tip for the edge. */
    @Override
    String getEdgeToolTip(final Edge edge) {
        // TODO: move this to the clusterbrowser
        final Pair<Vertex> p = getGraph().getEndpoints(edge);
        final boolean edgeIsOrder = edgeIsOrderList.contains(edge);
        final boolean edgeIsColocation = edgeIsColocationList.contains(edge);

        final Vertex v = p.getSecond();
        final Vertex parent = p.getFirst();
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        final ServiceInfo siP = (ServiceInfo) getInfo(parent);
        if (siP == null) {
            return null;
        }
        final StringBuilder s = new StringBuilder(100);
        s.append(siP.toString());
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(edge);
        mHbConnectionReadLock.unlock();
        if (edgeIsOrder && hbci != null && !hbci.isOrdScoreNull(null, null)) {
            s.append(" is started before ");
        } else {
            s.append(" and ");
        }
        s.append(si.toString());
        if (!edgeIsOrder) {
            s.append(" are located");
        }
        if (edgeIsColocation) {
            final HbConnectionInfo.ColScoreType colSType =
                                        hbci.getColocationScoreType(null, null);
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                s.append(" not on the same host");
            } else {
                s.append(" on the same host");
            }
        } else {
            s.append(" not necessarily on the same host");
        }

        return s.toString();
    }

    /**
     * Returns color of the vertex, depending if the service is configured,
     * removed or color of the host.
     */
    @Override
    protected Color getVertexFillColor(final Vertex v) {
        final boolean tOnly = isTestOnly();
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getHost().getPmColors()[0];
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            if (vertexToConstraintPHMap.get(v).getService().isNew()) {
                return Tools.getDefaultColor(
                                       "HeartbeatGraph.FillPaintUnconfigured");
            } else {
                // TODO fillpaint.placeholder
                return Tools.getDefaultColor(
                                        "HeartbeatGraph.FillPaintPlaceHolder");
            }
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        final Set<Vertex> vipl = getVertexIsPresentList();
        putVertexIsPresentList();
        if (getClusterBrowser().allHostsDown()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintUnknown");
        } else if (si.getService().isOrphaned()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintUnknown");
        } else if (si.isFailed(tOnly)) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintFailed");
        } else if (!si.isRunning(tOnly)) {
            return ClusterBrowser.FILL_PAINT_STOPPED;
        } else if (getClusterBrowser().clStatusFailed()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintUnknown");
        } else if (vipl.contains(v) || tOnly) {
            final List<Color> colors = si.getHostColors(tOnly);
            if (colors.size() >= 1) {
                return colors.get(0);
            } else {
                return Color.WHITE; /* more colors */
            }
        } else if (!si.getService().isNew()) {
            return Tools.getDefaultColor("HeartbeatGraph.FillPaintRemoved");
        } else {
            return Tools.getDefaultColor(
                                      "HeartbeatGraph.FillPaintUnconfigured");
        }
    }

    /** Returns label that describes the edge. */
    @Override
    protected String getLabelForEdgeStringer(final Edge e) {
        if (isTestEdge(e)) {
            return "ptest...";
        }
        final boolean edgeIsOrder = edgeIsOrderList.contains(e);
        final boolean edgeIsColocation =
                                    edgeIsColocationList.contains(e);
        final Pair<Vertex> p = getGraph().getEndpoints(e);
        if (p == null) {
            return "";
        }
        final ServiceInfo s1 = (ServiceInfo) getInfo(p.getSecond());
        if (s1 == null) {
            return "";
        }
        final ServiceInfo s2 = (ServiceInfo) getInfo(p.getFirst());
        if (s2 == null) {
            return "";
        }
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        mHbConnectionReadLock.unlock();
        if (hbci == null) {
            return "";
        }
        String colDesc = null;
        String ordDesc = null;
        String desc = null;
        String leftArrow = "\u2190 "; /* <- */
        String rightArrow = " \u2192"; /* -> */
        final HbConnectionInfo.ColScoreType colSType =
                                    hbci.getColocationScoreType(null, null);
        if (edgeIsColocation) {
            if (hbci.isColocationTwoDirections()) {
                leftArrow = "\u21AE "; /* </> */
                rightArrow = "\u21AE "; /* </> */
            } else if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
            } else if (colSType == HbConnectionInfo.ColScoreType.IS_NULL) {
                leftArrow = "\u21E0 "; /* < - - */
                rightArrow = " \u21E2"; /* - - > */
            } else if (colSType == HbConnectionInfo.ColScoreType.MIXED) {
                leftArrow = "\u219A "; /* </- */
                rightArrow = " \u219B"; /* -/> */
            }
        }
        String colScore = "";
        String ordScore = "";
        if (edgeIsColocation
            && colSType == HbConnectionInfo.ColScoreType.IS_NULL) {
            colScore = "0";
        }
        if (edgeIsOrder
            && hbci != null
            && hbci.isOrdScoreNull(null, null)) {
                ordScore = "0";
        }

        if (edgeIsOrder && edgeIsColocation) {
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                colDesc = "repelled";
            } else {
                colDesc = "col";
            }
            ordDesc = "ord";
        } else if (edgeIsOrder) {
            ordDesc = "ordered";
        } else if (edgeIsColocation) {
            if (colSType == HbConnectionInfo.ColScoreType.NEGATIVE
                || colSType == HbConnectionInfo.ColScoreType.MINUS_INFINITY) {
                colDesc = "repelled";
            } else {
                colDesc = "colocated";
            }
        } else if (s1.getService().isNew() || s2.getService().isNew()) {
            desc = Tools.getString("HeartbeatGraph.Unconfigured");
        } else {
            desc = Tools.getString("HeartbeatGraph.Removing");
        }
        if (desc != null) {
            return desc;
        }
        final Vertex v1 = p.getSecond();
        final Vertex v2 = p.getFirst();
        double s1X = 0;
        double s2X = 0;
        final Point2D loc1 = getLayout().transform(v1);
        if (loc1 != null) {
            s1X = loc1.getX();
        }
        final Point2D loc2 = getLayout().transform(v2);
        if (loc2 != null) {
            s2X = loc2.getX();
        }
        final StringBuilder sb = new StringBuilder(15);
        final boolean upsideDown = s1X < s2X;
        final boolean left = hbci.isWithRsc(s2) && !e.isWrongColocation();
        if (edgeIsColocation) {
            if ((left && !upsideDown) || (!left && upsideDown)) {
                sb.append(leftArrow); /* <- */
                sb.append(colScore);
                sb.append(' ');
                sb.append(colDesc);
                if (edgeIsOrder) {
                    sb.append('/');
                    sb.append(ordDesc);
                    sb.append(' ');
                    sb.append(ordScore);
                }
            } else if ((!left && !upsideDown) || (left && upsideDown)) {
                if (edgeIsOrder) {
                    sb.append(ordScore);
                    sb.append(' ');
                    sb.append(ordDesc);
                    sb.append('/');
                }
                sb.append(colDesc);
                sb.append(' ');
                sb.append(colScore);
                sb.append(rightArrow); /* -> */
            }
        } else if (edgeIsOrder) {
            if (left && !upsideDown) {
                sb.append(ordDesc);
                sb.append(' ');
                sb.append(ordScore);
            } else {
                sb.append(ordScore);
                sb.append(' ');
                sb.append(ordDesc);
            }
        }
        return sb.toString();
    }

    /** Returns paint of the edge. */
    @Override
    protected Paint getEdgeDrawPaint(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        mHbConnectionReadLock.unlock();
        if (hbci != null && hbci.isNew()) {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintNew");
        } else if (edgeIsOrderList.contains(e)
                   && edgeIsColocationList.contains(e)) {
            return super.getEdgeDrawPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintBrighter");
        } else {
            return Tools.getDefaultColor("ResourceGraph.EdgeDrawPaintRemoved");
        }
    }

    /** Returns paint of the picked edge. */
    @Override
    protected Paint getEdgePickedPaint(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        mHbConnectionReadLock.unlock();
        if (hbci != null && hbci.isNew()) {
            return Tools.getDefaultColor("ResourceGraph.EdgePickedPaintNew");
        } else if (edgeIsOrderList.contains(e)
                   && edgeIsColocationList.contains(e)) {
            return super.getEdgePickedPaint(e);
        } else if (edgeIsOrderList.contains(e)
                   || edgeIsColocationList.contains(e)) {
            return Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintBrighter");
        } else {
            return Tools.getDefaultColor(
                                    "ResourceGraph.EdgePickedPaintRemoved");
        }
    }

    /**
     * Remove edges that were marked as not present, meaning there are no
     * colocation nor order constraints.
     */
    public void killRemovedEdges() {
        lockGraph();
        final List<Edge> edges =
                            new ArrayList<Edge>(getGraph().getEdges().size());
        for (final Edge e : getGraph().getEdges()) {
            edges.add(e);
        }
        unlockGraph();

        final boolean tOnly = isTestOnly();
        for (int i = 0; i < edges.size(); i++) {
            final Edge e = edges.get(i);
            final Pair<Vertex> p = getGraph().getEndpoints(e);
            final ServiceInfo s1 =
                            (ServiceInfo) getInfo(p.getSecond());
            if (s1 == null) {
                continue;
            }
            final ServiceInfo s2 =
                            (ServiceInfo) getInfo(p.getFirst());
            if (s2 == null) {
                continue;
            }
            if (!keepEdgeIsOrderList.contains(e)) {
                edgeIsOrderList.remove(e);
            }
            if (!keepEdgeIsColocationList.contains(e)) {
                edgeIsColocationList.remove(e);
            }
            if (s1.getService().isNew() && !s1.getService().isRemoved()
                || (s2.getService().isNew() && !s2.getService().isRemoved())) {
                continue;
            }
            if (!keepEdgeIsOrderList.contains(e)) {
                mHbConnectionReadLock.lock();
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                mHbConnectionReadLock.unlock();
                if (hbci != null) {
                    hbci.removeOrders();
                }
            }

            if (!keepEdgeIsColocationList.contains(e)) {
                mHbConnectionReadLock.lock();
                final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
                mHbConnectionReadLock.unlock();
                if (hbci != null) {
                    hbci.removeColocations();
                }
            }
            if (!keepEdgeIsOrderList.contains(e)
                && !keepEdgeIsColocationList.contains(e)) {
                removeEdge(e, false);
            }
        }
    }

    /** Removes edge if it is not in the list of constraints. */
    private void removeEdge(final Edge e, final boolean testOnly) {
        if (e == null) {
            return;
        }
        if (!edgeIsOrderList.contains(e)
            && !edgeIsColocationList.contains(e)) {
            e.reset();
            lockGraph();
            getGraph().removeEdge(e);
            unlockGraph();
            mHbConnectionWriteLock.lock();
            try {
                mHbConnectionWriteLock.lock();
                final HbConnectionInfo hbci =
                                        edgeToHbconnectionMap.get(e);
                edgeToHbconnectionMap.remove(e);
                mHbConnectionWriteLock.unlock();
                if (hbci != null) {
                    hbconnectionToEdgeMap.remove(hbci);
                    hbci.removeMyself(testOnly);
                }
            } finally {
                mHbConnectionWriteLock.unlock();
            }
        }
    }

    /** Remove vertices that were marked as not present. */
    public void killRemovedVertices() {
        /* Make copy first. */
        final List<Vertex> vertices = new ArrayList<Vertex>();
        lockGraph();
        for (final Vertex vo : getGraph().getVertices()) {
            vertices.add(vo);
        }
        unlockGraph();
        for (final Vertex v : vertices) {
            if (vertexToHostMap.containsKey(v)) {
                continue;
            }
            lockGraph();
            if (!getGraph().getInEdges(v).isEmpty()
                || !getGraph().getOutEdges(v).isEmpty()) {
                unlockGraph();
                continue;
            }
            unlockGraph();
            final Set<Vertex> vipl = getVertexIsPresentList();
            putVertexIsPresentList();
            if (vertexToConstraintPHMap.containsKey(v)) {
                final ConstraintPHInfo cphi = (ConstraintPHInfo) getInfo(v);
                if (cphi == null) {
                    continue;
                }
                if (!vipl.contains(v)) {
                    cphi.setUpdated(false);
                    if (!getClusterBrowser().clStatusFailed()
                        && cphi.getService().isRemoved()) {
                        getVertexLocations().put(v, null);
                        putVertexLocations();
                        lockGraph();
                        getGraph().removeVertex(v);
                        removeInfo(v);
                        removeVertex(cphi);
                        unlockGraph();
                        cphi.removeInfo();
                        getVertexToMenus().remove(v);
                        vertexToConstraintPHMap.remove(v);
                        constraintPHToVertexMap.remove(cphi);
                        somethingChanged();
                    } else {
                        cphi.getService().setNew(true);
                    }
                }
                cphi.resetRscSetConnectionData();
            } else {
                if (!vipl.contains(v)) {
                    final ServiceInfo si = (ServiceInfo) getInfo(v);
                    if (si != null
                        && !si.getService().isNew()
                        && !getClusterBrowser().clStatusFailed()) {

                        si.setUpdated(false);
                        getVertexLocations().put(v, null);
                        putVertexLocations();
                        lockGraph();
                        getGraph().removeVertex(v);
                        removeInfo(v);
                        removeVertex(si);
                        unlockGraph();
                        si.removeInfo();
                        //TODO: unregister removePopup(v);
                        getVertexToMenus().remove(v);
                        vertexToAddServiceMap.remove(v);
                        vertexToAddExistingServiceMap.remove(v);
                        //TODO: positions are still there
                        somethingChanged();
                    }
                }
            }
        }
    }

    /** Mark vertex and its edges to be removed later. */
    @Override
    protected void removeInfo(final Info i) {
        final Vertex v = getVertex(i);
        /* remove edges */

        lockGraph();
        for (final Edge e : getGraph().getInEdges(v)) {
            edgeIsOrderList.remove(e);
            edgeIsColocationList.remove(e);
            keepEdgeIsOrderList.remove(e);
            keepEdgeIsColocationList.remove(e);
        }

        for (final Edge e : getGraph().getOutEdges(v)) {
            edgeIsOrderList.remove(e);
            edgeIsColocationList.remove(e);
            keepEdgeIsOrderList.remove(e);
            keepEdgeIsColocationList.remove(e);
        }
        unlockGraph();

        /* remove vertex */
        getVertexIsPresentList().remove(v);
        putVertexIsPresentList();
        ((ServiceInfo) i).getService().setNew(false);
        resetSavedPosition(i);
    }

    /**
     * Returns vertex location list, it acquires mVertexIsPresentListLock and
     * must be followed by putVertexIsPresentList.
     */
    Set<Vertex> getVertexIsPresentList() {
        mVertexIsPresentListLock.lock();
        return vertexIsPresentList;
    }

    /** Releases mVertexIsPresentListLock. */
    void putVertexIsPresentList() {
        mVertexIsPresentListLock.unlock();
    }

    /** Set vertex-is-present list. */
    public void setServiceIsPresentList(final List<ServiceInfo> sis) {
        final Set<Vertex> vipl = new HashSet<Vertex>();
        for (final ServiceInfo si : sis) {
            final Vertex v = getVertex(si);
            if (v == null) {
                /* e.g. group vertices */
                continue;
            }
            vipl.add(v);
        }
        mVertexIsPresentListLock.lock();
        vertexIsPresentList = vipl;
        mVertexIsPresentListLock.unlock();
    }

    ///** Set vertex as present. */
    //public final void setVertexIsPresent(final ServiceInfo si) {
    //    final Vertex v = getVertex(si);
    //    if (v == null) {
    //        //Tools.Toolsrror("no vertex associated with service info");
    //        /* group vertices */
    //        return;
    //    }
    //    getVertexIsPresentList().add(v);
    //    putVertexIsPresentList();
    //}

    /** Returns an icon for the vertex. */
    @Override
    protected List<ImageIcon> getIconsForVertex(final Vertex v,
                                                final boolean testOnly) {
        final List<ImageIcon> icons = new ArrayList<ImageIcon>();
        final HostInfo hi = vertexToHostMap.get(v);
        if (hi != null) {
            if (hi.getHost().isClStatus()) {
                icons.add(HostBrowser.HOST_ON_ICON_LARGE);
            } else {
                icons.add(HostBrowser.HOST_ICON_LARGE);
            }
            if (hi.isStandby(testOnly)) {
                icons.add(HOST_STANDBY_ICON);
            }
            return icons;
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return null;
        }

        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        if (si.isFailed(testOnly)) {
            if (si.isRunning(testOnly)) {
                icons.add(SERVICE_RUNNING_FAILED_ICON);
            } else {
                icons.add(SERVICE_STOPPED_FAILED_ICON);
            }
        } else if (si.isStopped(testOnly)
                   || getClusterBrowser().allHostsDown()) {
            if (si.isRunning(testOnly)) {
                icons.add(SERVICE_STOPPING_ICON);
            } else {
                icons.add(SERVICE_STOPPED_ICON);
            }
        } else {
            if (si.isRunning(testOnly)) {
                icons.add(SERVICE_RUNNING_ICON);
            } else {
                icons.add(SERVICE_STARTED_ICON);
            }
        }
        if (!si.isManaged(testOnly) || si.getService().isOrphaned()) {
            icons.add(SERVICE_UNMANAGED_ICON);
        }
        if (si.getMigratedTo(testOnly) != null
            || si.getMigratedFrom(testOnly) != null) {
            icons.add(SERVICE_MIGRATED_ICON);
        }
        return icons;
    }

    /** Returns whether to show an edge arrow. */
    @Override
    protected boolean showEdgeArrow(final Edge e) {
        if (edgeIsOrderList.contains(e)) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
            mHbConnectionReadLock.unlock();
            return hbci != null && !hbci.isOrderTwoDirections();
        }
        return false;
    }

    /** Returns whether to show a hollow arrow. */
    @Override
    protected boolean showHollowArrow(final Edge e) {
        if (edgeIsOrderList.contains(e)) {
            mHbConnectionReadLock.lock();
            final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
            mHbConnectionReadLock.unlock();
            return hbci != null && hbci.isOrdScoreNull(null, null);
        }
        return false;
    }

    /** Reloads popup menus for all services. */
    public void reloadServiceMenus() {
        lockGraph();
        for (final Vertex v : getGraph().getVertices()) {
            final JMenu existingServiceMenuItem =
                                vertexToAddExistingServiceMap.get(v);
            reloadAddExistingServicePopup(existingServiceMenuItem, v);
        }
        unlockGraph();
    }

    /** Is called after an edge was pressed. */
    @Override
    protected void oneEdgePressed(final Edge e) {
        mHbConnectionReadLock.lock();
        final HbConnectionInfo hbci = edgeToHbconnectionMap.get(e);
        mHbConnectionReadLock.unlock();
        if (hbci != null) {
            getClusterBrowser().setRightComponentInView(hbci);
        }
    }

    /** Removes the connection, the order, the colocation or both. */
    public void removeConnection(final HbConnectionInfo hbci,
                                 final Host dcHost,
                                 final boolean testOnly) {
        final ServiceInfo siP = hbci.getLastServiceInfoParent();
        final ServiceInfo siC = hbci.getLastServiceInfoChild();
        mHbConnectionReadLock.lock();
        final Edge edge = hbconnectionToEdgeMap.get(hbci);
        mHbConnectionReadLock.unlock();
        if (edgeIsOrderList.contains(edge)) {
            siC.removeOrder(siP, dcHost, testOnly);
        }
        final ServiceInfo siRsc = hbci.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc = hbci.getLastServiceInfoWithRsc();
        if (edgeIsColocationList.contains(edge)) {
            if (!testOnly) {
                edgeIsOrderList.remove(edge);
                edgeIsColocationList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
            siRsc.removeColocation(siWithRsc, dcHost, testOnly);
        } else {
            if (!testOnly) {
                edgeIsOrderList.remove(edge);
                edgeIsColocationList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
        }
        if (testOnly) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeEdge(edge, testOnly);
                    }
                });
            }
        }
    }

    /** Removes order. */
    public void removeOrder(final HbConnectionInfo hbci,
                            final Host dcHost,
                            final boolean testOnly) {
        if (hbci == null) {
            return;
        }
        final ServiceInfo siP = hbci.getLastServiceInfoParent();
        final ServiceInfo siC = hbci.getLastServiceInfoChild();
        mHbConnectionReadLock.lock();
        final Edge edge = hbconnectionToEdgeMap.get(hbci);
        mHbConnectionReadLock.unlock();
        if (edgeIsOrderList.contains(edge)) {
            if (!testOnly) {
                edgeIsOrderList.remove(edge);
                keepEdgeIsOrderList.remove(edge);
            }
            siC.removeOrder(siP, dcHost, testOnly);
        }
        if (testOnly) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeEdge(edge, testOnly);
                    }
                });
            }
        }
    }

    /** Adds order. */
    public void addOrder(final HbConnectionInfo hbConnectionInfo,
                         final Host dcHost,
                         final boolean testOnly) {
        final ServiceInfo siRsc = hbConnectionInfo.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc =
                                  hbConnectionInfo.getLastServiceInfoWithRsc();
        if ((siWithRsc != null
             && siWithRsc.getService() != null
             && siWithRsc.getService().isNew())
            || (siRsc != null
                && siRsc.getService() != null
                && siRsc.getService().isNew())) {
            addOrder(null, siRsc, siWithRsc);
        } else {
            siWithRsc.addOrder(siRsc, dcHost, testOnly);
        }
        if (testOnly) {
            mHbConnectionReadLock.lock();
            final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
            mHbConnectionReadLock.unlock();
            addExistingTestEdge(edge);
        }
    }

    /** Removes colocation. */
    public void removeColocation(final HbConnectionInfo hbci,
                                 final Host dcHost,
                                 final boolean testOnly) {
        final ServiceInfo siRsc = hbci.getLastServiceInfoRsc();
        final ServiceInfo siWithRsc = hbci.getLastServiceInfoWithRsc();
        mHbConnectionReadLock.lock();
        final Edge edge = hbconnectionToEdgeMap.get(hbci);
        if (edge == null) {
            mHbConnectionReadLock.unlock();
            return;
        }
        mHbConnectionReadLock.unlock();
        if (edgeIsColocationList.contains(edge)) {
            if (!testOnly) {
                edgeIsColocationList.remove(edge);
                keepEdgeIsColocationList.remove(edge);
            }
            siRsc.removeColocation(siWithRsc, dcHost, testOnly);
        }
        if (testOnly) {
            addExistingTestEdge(edge);
        } else {
            if (hbci.isNew()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeEdge(edge, testOnly);
                    }
                });
            }
        }
    }

    /** Adds colocation. */
    public void addColocation(final HbConnectionInfo hbConnectionInfo,
                              final Host dcHost,
                              final boolean testOnly) {
        final ServiceInfo siP = hbConnectionInfo.getLastServiceInfoParent();
        final ServiceInfo siC = hbConnectionInfo.getLastServiceInfoChild();
        if (siC != null
            && siC.getService() != null
            && siC.getService().isNew()) {
            addColocation(null, siP, siC);
        } else {
            siP.addColocation(siC, dcHost, testOnly);
        }
        if (testOnly) {
            mHbConnectionReadLock.lock();
            final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
            mHbConnectionReadLock.unlock();
            addExistingTestEdge(edge);
        }
    }

    /** Returns whether this hb connection is order. */
    public boolean isOrder(final HbConnectionInfo hbConnectionInfo) {
        mHbConnectionReadLock.lock();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        mHbConnectionReadLock.unlock();
        return edgeIsOrderList.contains(edge);
    }

    /** Returns whether this hb connection is colocation. */
    public boolean isColocation(final HbConnectionInfo hbConnectionInfo) {
        mHbConnectionReadLock.lock();
        final Edge edge = hbconnectionToEdgeMap.get(hbConnectionInfo);
        mHbConnectionReadLock.unlock();
        return edgeIsColocationList.contains(edge);
    }

    /** Adds host to the graph. */
    void addHost(final HostInfo hostInfo) {
        Vertex v = super.getVertex(hostInfo);
        if (v == null) {
            /* add host vertex */
            v = new Vertex();
            lockGraph();
            getGraph().addVertex(v);
            unlockGraph();
            somethingChanged();
            putInfoToVertex(hostInfo, v);
            vertexToHostMap.put(v, hostInfo);
            hostToVertexMap.put(hostInfo, v);
            putVertexToInfo(v, (Info) hostInfo);
            Point2D hostPos = getSavedPosition(hostInfo);
            if (hostPos == null) {
                hostPos = new Point2D.Double(hostDefaultXPos, HOST_Y_POS);
                hostDefaultXPos += HOST_STEP_X;
            }
            getVertexLocations().put(v, hostPos);
            putVertexLocations();
        }
    }

    /** Small text that appears above the icon. */
    @Override
    protected String getIconText(final Vertex v, final boolean testOnly) {
        if (isTestOnlyAnimation()) {
            return "ptest...";
        }
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getIconTextForGraph(testOnly);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getIconTextForGraph(testOnly);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getIconTextForGraph(testOnly);
    }

    /** Small text that appears in the right corner. */
    @Override
    protected Subtext getRightCornerText(final Vertex v,
                                         final boolean testOnly) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = vertexToHostMap.get(v);
            return hi.getRightCornerTextForGraph(testOnly);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return null;
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si != null) {
            return si.getRightCornerTextForGraph(testOnly);
        }
        return null;
    }

    /** Small text that appears down. */
    @Override
    protected Subtext[] getSubtexts(final Vertex v, final boolean testOnly) {
        if (vertexToHostMap.containsKey(v)) {
            return vertexToHostMap.get(v).getSubtextsForGraph(testOnly);
        } else if (vertexToConstraintPHMap.containsKey(v)) {
            return vertexToConstraintPHMap.get(v).getSubtextsForGraph(testOnly);
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return null;
        }
        return si.getSubtextsForGraph(testOnly);
    }

    /**
     * Returns how much of the disk is used. Probably useful only for disks.
     * -1 for not used or not applicable.
     */
    protected int getUsed(final Vertex v) {
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            if (hi == null) {
                return 0;
            }
            return hi.getUsed();
        }
        final ServiceInfo si = (ServiceInfo) getInfo(v);
        if (si == null) {
            return 0;
        }
        return si.getUsed();
    }

    /**
     * This method draws how much of the vertex is used for something.
     * It draws more colors for verteces that have more background colors.
     */
    @Override
    protected void drawInside(final Vertex v,
                              final Graphics2D g2d,
                              final double x,
                              final double y,
                              final Shape shape) {
        final boolean tOnly = isTestOnly();
        final float height = (float) shape.getBounds().getHeight();
        final float width = (float) shape.getBounds().getWidth();
        if (vertexToHostMap.containsKey(v)) {
            final HostInfo hi = (HostInfo) getInfo(v);
            if (hi != null) {
                drawInsideVertex(g2d,
                                 v,
                                 hi.getHost().getPmColors(),
                                 x,
                                 y,
                                 height,
                                 width);
            }
        } else if (vertexToConstraintPHMap.containsKey(v)) {
        } else {
            final ServiceInfo si = (ServiceInfo) getInfo(v);
            if (si == null) {
                return;
            }
            final List<Color> colors = si.getHostColors(tOnly);
            final int number = colors.size();
            if (number > 1) {
                for (int i = 1; i < number; i++) {
                    final Paint p = new GradientPaint(
                                                (float) x + width / number,
                                                (float) y,
                                                getVertexFillSecondaryColor(v),
                                                (float) x + width / number,
                                                (float) y + height,
                                                colors.get(i),
                                                false);
                    g2d.setPaint(p);
                    final RoundRectangle2D s =
                       new RoundRectangle2D.Double(
                                    x + (width / number) * i - 5,
                                    y,
                                    width / number + (i < number - 1 ? 20 : 5),
                                    height,
                                    20,
                                    20);
                    g2d.fill(s);
                }
            }
            final double used = getUsed(v);
            if (used > 0) {
                /** Show how much is used. */
                final double freeWidth = width * (100 - used) / 100;
                final RoundRectangle2D freeShape =
                   new RoundRectangle2D.Double(x + width - freeWidth,
                                               y,
                                               freeWidth,
                                               height,
                                               20,
                                               20);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fill(freeShape);
            }
        }
        if (isPicked(v)) {
            if (tOnly) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(shape);
    }

    /** Returns all crm connections. */
    List<HbConnectionInfo> getAllHbConnections() {
        final List<HbConnectionInfo> allConnections =
                                            new ArrayList<HbConnectionInfo>();
        mHbConnectionReadLock.lock();
        for (final HbConnectionInfo hbci : hbconnectionToEdgeMap.keySet()) {
            allConnections.add(hbci);
        }
        mHbConnectionReadLock.unlock();
        return allConnections;
    }

    /**
     * Returns the vertex that represents the specified resource or its group.
     */
    @Override
    public Vertex getVertex(final Info i) {
        if (i == null) {
            return null;
        }
        if (hostToVertexMap.containsKey(i)) {
            return super.getVertex(i);
        }
        final GroupInfo gi = ((ServiceInfo) i).getGroupInfo();
        if (gi == null) {
            return super.getVertex(i);
        } else {
            return super.getVertex(gi);
        }
    }

    /** Adds placeholder that is used to create resource sets. */
    public void addConstraintPlaceholder(final ConstraintPHInfo rsoi,
                                         final Point2D pos,
                                         final boolean testOnly) {
        if (testOnly) {
            return;
        }
        final Vertex v = new Vertex();
        if (pos == null) {
            final Point2D newPos = getSavedPosition(rsoi);
            if (newPos == null) {
                final Point2D max = getLastPosition();
                final float maxYPos = (float) max.getY();
                getVertexLocations().put(
                                        v,
                                        new Point2D.Float(BD_X_POS,
                                                          maxYPos + 40));
                putVertexLocations();
            } else {
                getVertexLocations().put(v, newPos);
                putVertexLocations();
            }
        } else {
            getVertexLocations().put(v, pos);
            putVertexLocations();
        }

        lockGraph();
        getGraph().addVertex(v);
        unlockGraph();
        putInfoToVertex(rsoi, v);
        putVertexToInfo(v, (Info) rsoi);
        constraintPHToVertexMap.put(rsoi, v);
        vertexToConstraintPHMap.put(v, rsoi);
        somethingChanged();
    }

    /** Returns height of the vertex. */
    @Override
    protected int getDefaultVertexHeight(final Vertex v) {
        if (vertexToConstraintPHMap.containsKey(v)) {
            return 50;
        } else {
            return VERTEX_HEIGHT;
        }
    }


    /** Returns the width of the service vertex shape. */
    @Override
    protected int getDefaultVertexWidth(final Vertex v) {
        if (vertexToConstraintPHMap.containsKey(v)) {
            return 50;
        } else {
            return super.getDefaultVertexWidth(v);
        }
    }

    /** Sets the vertex width. */
    @Override
    protected void setVertexWidth(final Vertex v, final int size) {
        super.setVertexWidth(v, size);
    }

    /** Sets the vertex height. */
    @Override
    protected void setVertexHeight(final Vertex v, final int size) {
        super.setVertexHeight(v, size);
    }

    /** Select multiple services. */
    @Override
    protected void multiSelection() {
        final List<Info> selectedInfos = new ArrayList<Info>();
        final PickedState<Vertex> ps =
                getVisualizationViewer().getRenderContext()
                                                    .getPickedVertexState();
        for (final Vertex v : ps.getPicked()) {
            selectedInfos.add(getInfo(v));
        }
        multiSelectionInfo = new PcmkMultiSelectionInfo(selectedInfos,
                                                        getClusterBrowser());
        getClusterBrowser().setRightComponentInView(multiSelectionInfo);
    }
}
