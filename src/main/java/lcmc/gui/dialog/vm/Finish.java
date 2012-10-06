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


package lcmc.gui.dialog.vm;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.gui.resources.VMSVirtualDomainInfo;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An implementation of a dialog where user can enter a new domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
final class Finish extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Input pane cache for back button. */
    private JComponent inputPane = null;

    /** Prepares a new <code>Finish</code> object. */
    Finish(final WizardDialog previousDialog,
           final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.vm.Domain.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Finish.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Finish.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(finishButton()).setEnabled(false);
            }
        });
    }

    /** Returns input pane where user can configure a vm. */
    @Override
    protected JComponent getInputPane() {
        final VMSVirtualDomainInfo vdi = getVMSVirtualDomainInfo();
        vdi.selectMyself();
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        final MyButton createConfigBtn = new MyButton("Create Config");
        createConfigBtn.setBackgroundColor(
                                 Tools.getDefaultColor("ConfigDialog.Button"));
        createConfigBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                createConfigBtn.setEnabled(false);
                            }
                        });
                        vdi.apply(false);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                buttonClass(finishButton()).setEnabled(true);
                            }
                        });
                    }
                });
                thread.start();
            }
        });
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        vdi.waitForInfoPanel();
        optionsPanel.add(vdi.getDefinedOnHostsPanel("wizard", createConfigBtn));

        optionsPanel.add(createConfigBtn);
        panel.add(optionsPanel);

        final JScrollPane sp = new JScrollPane(panel);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = sp;
        return sp;
    }
}
