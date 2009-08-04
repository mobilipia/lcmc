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

package drbd.configs;

import java.util.Arrays;

/**
 * Here are commands for all fedoras.
 */
public class DistResource_fedora extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "fedora"},
        {"distribution", "redhat"},
        {"version:Fedora release 10*", "10"},
        {"version:Fedora release 11*", "11"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        {"DrbdInst.install",
         "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},

        {"HbPmInst.install.text.2",
         "the fedora way: possibly too old" },

        {"HbPmInst.install.2",
         "/usr/bin/yum -y install heartbeat"},
        /* at least fedora 10 and fedora11 in version 2.1.3 and 2.14 has different
           ocf path. */
        {"Heartbeat.2.1.4.getOCFParameters",
         "export OCF_RESKEY_vmxpath=a;export OCF_ROOT=/usr/share/ocf;"
         + "for prov in `ls -1 /usr/share/ocf/resource.d/`; do "
         + " for s in `ls -1 /usr/share/ocf/resource.d/$prov/ `; do "
         + " /usr/share/ocf/resource.d/$prov/$s meta-data 2>/dev/null; done;"
         + "done;"
         + "/usr/local/bin/drbd-gui-helper get-old-style-resources;"
         + "/usr/local/bin/drbd-gui-helper get-lsb-resources"},

        {"HbCheck.version",
         "/usr/local/bin/drbd-gui-helper get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Openais.removeHeartbeatAddOpenais",
         "/etc/init.d/heartbeat stop;/sbin/chkconfig --del heartbeat;"
         + "/etc/init.d/openais start && "
         + "/sbin/chkconfig --add openais"},

        {"Heartbeat.removeOpenaisAddHeartbeat",
         "/etc/init.d/openais stop;/sbin/chkconfig --del openais;"
         + "/etc/init.d/heartbeat start && "
         + "/sbin/chkconfig --add heartbeat"},

        {"Openais.addOpenaisToRc",
         "/sbin/chkconfig --add openais"},

        {"Heartbeat.addHeartbeatToRc",
         "/sbin/chkconfig --add heartbeat"},

        {"Openais.startOpenaisRc",
         "/etc/init.d/openais start;/sbin/chkconfig --add openais"},

        {"Heartbeat.startHeartbeatRc",
         "/etc/init.d/heartbeat start;/sbin/chkconfig --add heartbeat"},
    };
}
