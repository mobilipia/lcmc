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

package lcmc.utilities;

import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;

/**
 * This is a menu object that holds MyMenuItems.
 */
public class MyMenu extends JMenu implements UpdatableItem {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Position of the menu that can be stored and retrieved. */
    private Point2D pos = null;
    /** Access Type for this component to become enabled. */
    private final AccessMode enableAccessMode;
    /** Access Type for this component to become visible. */
    private final AccessMode visibleAccessMode; // TODO: not implemented

    /** Prepares a new <code>MyMenu</code> object. */
    public MyMenu(final String text,
                  final AccessMode enableAccessMode,
                  final AccessMode visibleAccessMode) {
        super(text);
        this.enableAccessMode = enableAccessMode;
        this.visibleAccessMode = visibleAccessMode;
        setOpaque(false);
        setEnabled(false);
    }

    /** Stores the position. */
    @Override
    public final void setPos(final Point2D pos) {
        this.pos = pos;
    }

    /** Gets the position. */
    protected final Point2D getPos() {
        return pos;
    }

    /** Predicate that can be used, but it is not. */
    boolean predicate() {
        return true;
    }

    /**
     * Returns whether the item should be enabled or not.
     * null if it should be enabled or some string that can be used as
     * tooltip if it should be disabled.
     */
    public String enablePredicate() {
        return null;
    }

    /** Returns whether the item should be visible or not. */
    boolean visiblePredicate() {
        return true;
    }

    /**
     * This function is usually overriden and is called when the menu and its
     * items are to be updated.
     */
    @Override
    public void update() {
        processAccessMode();
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Component> copy = new ArrayList<Component>();
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        for (final Component m : getMenuComponents()) {
                            copy.add(m);
                        }
                    }
                });
                for (final Component m : copy) {
                    if (m instanceof UpdatableItem) {
                        ((UpdatableItem) m).update();
                    }
                }
            }
        });
        t.start();
    }

    /** Sets this item enabled and visible according to its access type. */
    private void processAccessMode() {
        final boolean accessible =
                   Tools.getConfigData().isAccessible(enableAccessMode);
        final String disableTooltip = enablePredicate();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setEnabled(disableTooltip == null && accessible);
            }
        });
        if (isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!accessible && enableAccessMode.getAccessType()
                                       != ConfigData.AccessType.NEVER) {
                        String advanced = "";
                        if (enableAccessMode.isAdvancedMode()) {
                            advanced = "Advanced ";
                        }
                        setToolTipText("<html><b>"
                                       + getText()
                                       + " (disabled)</b><br>available in \""
                                       + advanced
                                       + ConfigData.OP_MODES_MAP.get(
                                              enableAccessMode.getAccessType())
                                       + "\" mode</html>");
                    } else if (disableTooltip != null) {
                        setToolTipText("<html><b>"
                                       + getText()
                                       + " (disabled)</b><br>"
                                       + disableTooltip
                                       + "</html>");
                    }
                }
            });
        }
    }

    /** Cleanup. */
    @Override
    public final void cleanup() {
        for (final java.awt.Component m : getMenuComponents()) {
            if (m instanceof UpdatableItem) {
                ((UpdatableItem) m).cleanup();
            } else if (m instanceof JScrollPane) {
                ((MyList) ((JScrollPane) m).getViewport().getView()).cleanup();
            }
        }
    }

    /** Remove all items. */
    @Override
    public final void removeAll() {
        for (int i = 0; i < getItemCount(); i++) {
            final JMenuItem item = getItem(i);
            if (item instanceof MyMenuItem) {
                ((MyMenuItem) item).cleanup();
            } else if (item instanceof MyMenu) {
                ((MyMenu) item).removeAll();
            }
        }
        super.removeAll();
    }
}
