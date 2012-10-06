package lcmc.utilities;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import java.net.InetAddress;
import lcmc.Exceptions;
import lcmc.data.Host;

public final class ToolsTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTestCluster();
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    @Test
    public void testCreateImageIcon() {
        assertNull("not existing", Tools.createImageIcon("notexisting"));
        assertFalse("".equals(TestSuite1.getStdout()));
        TestSuite1.clearStdout();
        assertNotNull("existing", Tools.createImageIcon("startpage_head.jpg"));
    }

    @Test
    public void testGetRelease() {
        final String release = Tools.getRelease();
        assertNotNull("not null", release);
        assertFalse("not empty", "".equals(release));
        TestSuite1.realPrintln("release: " + release);
    }

    @Test
    public void testInfo() {
        Tools.info("info a");
        assertEquals(TestSuite1.INFO_STRING + "info a\n",
                     TestSuite1.getStdout());
        TestSuite1.clearStdout();
    }

    @Test
    public void testSetDefaults() {
        Tools.setDefaults();
    }

    @Test
    public void testDebug() {
        Tools.setDebugLevel(1);
        Tools.debug(null, "test a");
        assertTrue(TestSuite1.getStdout().indexOf("test a\n") > 0);
        TestSuite1.clearStdout();
        Tools.setDebugLevel(0);
        Tools.decrementDebugLevel(); /* -1 */
        TestSuite1.clearStdout();
        Tools.debug(null, "test b");
        assertEquals("", TestSuite1.getStdout());
        Tools.incrementDebugLevel(); /* 0 */
        TestSuite1.clearStdout();
        Tools.debug(null, "test c");
        assertTrue(TestSuite1.getStdout().indexOf("test c\n") > 0);
        TestSuite1.clearStdout();
        Tools.setDebugLevel(1); /* 1 */
        TestSuite1.clearStdout();
        Tools.debug(new Object(), "test d2", 2);
        Tools.debug(new Object(), "test d1", 1);
        Tools.debug(new Object(), "test d0", 0);
        final Pattern p = Pattern.compile("^" + TestSuite1.DEBUG_STRING
                + "\\(1\\) \\[\\d+s\\] test d1 \\(java\\.lang\\.Object\\)\\s+"
                + TestSuite1.DEBUG_STRING + ".*"
                + "\\(0\\) \\[\\d+s\\] test d0 \\(java\\.lang\\.Object\\)\\s+");
        final Matcher m = p.matcher(TestSuite1.getStdout());
        assertTrue(m.matches());
        TestSuite1.clearStdout();
        Tools.setDebugLevel(-1);
    }

    @Test
    public void testError() {
        if (TestSuite1.INTERACTIVE) {
            Tools.error("test error a / just click ok");
            assertEquals(TestSuite1.ERROR_STRING
                         + "test error a / just click ok\n",
                         TestSuite1.getStdout());
            TestSuite1.clearStdout();
        }
    }

    @Test
    public void testSSHError() {
        for (final Host host : TestSuite1.getHosts()) {
            Tools.sshError(host, "cmd a", "ans a", "stack trace a", 2);
            assertTrue(
                TestSuite1.getStdout().indexOf("returned exit code 2") >= 0);
            TestSuite1.clearStdout();
        }
    }

    @Test
    public void testConfirmDialog() {
        if (TestSuite1.INTERACTIVE) {
            assertTrue(
              Tools.confirmDialog("title a", "click yes", "yes (click)", "no"));
            assertFalse(
              Tools.confirmDialog("title a", "click no", "yes", "no (click)"));
        }
    }

    @Test
    public void testExecCommandProgressIndicator() {
        for (int i = 0; i < 1 * TestSuite1.getFactor(); i++) {
            for (final Host host : TestSuite1.getHosts()) {
                Tools.execCommandProgressIndicator(host,
                                                   "uname -a",
                                                   null, /* ExecCallback */
                                                   true, /* outputVisible */
                                                   "text h",
                                                   1000); /* command timeout */
            }
        }
        TestSuite1.clearStdout();
    }

    @Test
    public void testAppWarning() {
        Tools.appWarning("warning a");
        if (Tools.getDefault("AppWarning").equals("y")) {
            assertEquals(TestSuite1.APPWARNING_STRING + "warning a\n",
                         TestSuite1.getStdout());
        }
        TestSuite1.clearStdout();
    }

    @Test
    public void testInfoDialog() {
        if (TestSuite1.INTERACTIVE) {
            Tools.infoDialog("info a", "info1 a", "info2 a / CLICK OK");
        }
    }

    @Test
    public void testIsIp() {
        assertTrue(Tools.isIp("127.0.0.1"));
        assertTrue(Tools.isIp("0.0.0.0"));
        assertTrue(Tools.isIp("0.0.0.1"));
        assertTrue(Tools.isIp("255.255.255.255"));
        assertTrue(Tools.isIp("254.255.255.255"));

        assertFalse(Tools.isIp("localhost"));
        assertFalse(Tools.isIp("127-0-0-1"));
        assertFalse(Tools.isIp("256.255.255.255"));
        assertFalse(Tools.isIp("255.256.255.255"));
        assertFalse(Tools.isIp("255.255.256.255"));
        assertFalse(Tools.isIp("255.255.255.256"));

        assertFalse(Tools.isIp("255.255.255.1000"));
        assertFalse(Tools.isIp("255.255.255.-1"));
        assertFalse(Tools.isIp("255.255.255"));
        assertFalse(Tools.isIp(""));
        assertFalse(Tools.isIp("255.255.255.255.255"));

        assertFalse(Tools.isIp("127.0.0.false"));
        assertFalse(Tools.isIp("127.0.false.1"));
        assertFalse(Tools.isIp("127.false.0.1"));
        assertFalse(Tools.isIp("false.0.0.1"));

        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(Tools.isIp(host.getIp()));
            assertFalse(Tools.isIp(host.getHostname()));
        }
    }

    @Test
    public void testPrintStackTrace() {
        Tools.printStackTrace();
        assertFalse("".equals(TestSuite1.getStdout()));
        TestSuite1.clearStdout();
        Tools.printStackTrace("stack trace test");
        assertTrue(TestSuite1.getStdout().startsWith("stack trace test"));
        TestSuite1.clearStdout();
        assertFalse("".equals(Tools.getStackTrace()));
    }

    @Test
    public void testLoadFile() {
        for (float i = 0; i < 0.01 * TestSuite1.getFactor(); i++) {
            assertNull(Tools.loadFile("JUNIT_TEST_FILE_CLICK_OK",
                                      TestSuite1.INTERACTIVE));
            final String testFile = "/tmp/lcmc-test-file";
            Tools.save(testFile, false);
            final String file = Tools.loadFile(testFile,
                                               TestSuite1.INTERACTIVE);
            assertNotNull(file);
            TestSuite1.clearStdout();
            assertFalse("".equals(file));
        }
    }

    @Test
    public void testRemoveEverything() {
        //TODO:
        //Tools.removeEverything();
    }

    @Test
    public void testGetDefault() {
        if (TestSuite1.INTERACTIVE) {
            assertEquals("JUNIT TEST JUNIT TEST, click ok",
                         Tools.getDefault("JUNIT TEST JUNIT TEST, click ok"));
            assertTrue(TestSuite1.getStdout().indexOf("unresolved") >= 0);
            TestSuite1.clearStdout();
        }
        assertEquals("", Tools.getDefault("SSH.PublicKey"));
        assertEquals("", Tools.getDefault("SSH.PublicKey"));
        assertEquals("22", Tools.getDefault("SSH.Port"));
    }

    @Test
    public void testGetDefaultColor() {
        if (TestSuite1.INTERACTIVE) {
            assertEquals(
                  java.awt.Color.WHITE,
                  Tools.getDefaultColor("JUNIT TEST unknown color, click ok"));
            TestSuite1.clearStdout();
        }
        assertEquals(java.awt.Color.BLACK,
                     Tools.getDefaultColor("TerminalPanel.Background"));
    }

    @Test
    public void testGetDefaultInt() {
        if (TestSuite1.INTERACTIVE) {
            assertEquals(
                      0,
                      Tools.getDefaultInt("JUNIT TEST unknown int, click ok"));
            TestSuite1.clearStdout();
        }
        assertEquals(100000,
                     Tools.getDefaultInt("Score.Infinity"));
    }

    @Test
    public void testGetString() {
        final String testString = "JUNIT TEST unknown string, click ok";
        if (TestSuite1.INTERACTIVE) {
            assertEquals(testString, Tools.getString(testString));
            TestSuite1.clearStdout();
        }
        assertEquals("Linux Cluster Management Console",
                     Tools.getString("DrbdMC.Title"));
    }

    @Test
    public void testGetErrorString() {
        final String errorString = "the same string";
        assertEquals(errorString, errorString);
    }

    @Test
    public void testGetDistString() {
        /* text, dist, version, arch */
        assertNull(Tools.getDistString("none",
                                       "none",
                                       "none",
                                       "none"));
        assertEquals("no", Tools.getDistString("Support",
                                               "none",
                                               "none",
                                               "none"));
        assertEquals("no", Tools.getDistString("Support",
                                               null,
                                               null,
                                               null));
        assertEquals("debian", Tools.getDistString("Support",
                                                   "debian",
                                                   null,
                                                   null));
        assertEquals("debian-SQUEEZE", Tools.getDistString("Support",
                                                           "debian",
                                                           "SQUEEZE",
                                                           null));
        assertEquals("debian-SQUEEZE", Tools.getDistString("Support",
                                                           "debian",
                                                           "SQUEEZE",
                                                           "a"));
        assertEquals("i586", Tools.getDistString("PmInst.install",
                                                 "suse",
                                                 null,
                                                 "i386"));
    }

    @Test
    public void testGetDistCommand() {
        /*
         String text
         String dist
         String version
         String arch
         ConvertCmdCallback convertCmdCallback
         boolean inBash
        */
        assertEquals("undefined",
                     Tools.getDistCommand("undefined",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          null,
                                          false));
        assertEquals(TestSuite1.APPWARNING_STRING
                     + "unknown command: undefined\n",
                     TestSuite1.getStdout());
        TestSuite1.clearStdout();

        assertEquals("undefined2;;;undefined3",
                     Tools.getDistCommand("undefined2;;;undefined3",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          null,
                                          false));
        assertEquals(TestSuite1.APPWARNING_STRING
                     + "unknown command: undefined2\n"
                     + TestSuite1.APPWARNING_STRING
                     + "unknown command: undefined3\n",
                     TestSuite1.getStdout());
        TestSuite1.clearStdout();
        final lcmc.utilities.ConvertCmdCallback ccc =
                                    new lcmc.utilities.ConvertCmdCallback() {
            @Override
            public String convert(final String command) {
                return command.replaceAll(lcmc.configs.DistResource.SUDO,
                                          "sudo ");
            }
        };
        assertEquals("sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals("sudo bash -c \"sudo /etc/init.d/corosync start\"",
                     Tools.getDistCommand("Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          true));
        assertEquals("sudo /etc/init.d/corosync start"
                     + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("Corosync.startCorosync;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals("undefined4"
                     + ";;;sudo /etc/init.d/corosync start",
                     Tools.getDistCommand("undefined4;;;"
                                          + "Corosync.startCorosync",
                                          "debian",
                                          "squeeze",
                                          "i386",
                                          ccc,
                                          false));
        assertEquals(TestSuite1.APPWARNING_STRING
                     + "unknown command: undefined4\n",
                     TestSuite1.getStdout());
        TestSuite1.clearStdout();
        assertNull(Tools.getDistCommand(null,
                                        "debian",
                                        "squeeze",
                                        "i386",
                                        ccc,
                                        false));
        assertNull(Tools.getDistCommand(null,
                                        "debian",
                                        "squeeze",
                                        "i386",
                                        ccc,
                                        true));
        assertNull(Tools.getDistCommand(null, null, null, null, null, true));
    }

    @Test
    public void testGetKernelDownloadDir() {
        /* String kernelVersion
           String dist
           String version
           String arch
         */
         assertNull(Tools.getKernelDownloadDir(null, null, null, null));
         assertEquals("2.6.32-28",
                      Tools.getKernelDownloadDir("2.6.32-28-server",
                                                 "ubuntu",
                                                 "lucid",
                                                 "x86_64"));
         assertEquals("2.6.32-28",
                      Tools.getKernelDownloadDir("2.6.32-28-server",
                                                 "ubuntu",
                                                 "lucid",
                                                 "i386"));
         assertEquals("2.6.24-28",
                      Tools.getKernelDownloadDir("2.6.24-28-server",
                                                 "ubuntu",
                                                 "hardy",
                                                 "x86_64"));
         assertEquals("2.6.32.27-0.2",
                      Tools.getKernelDownloadDir("2.6.32.27-0.2-default",
                                                 "suse",
                                                 "SLES11",
                                                 "x86_64"));
         assertEquals("2.6.16.60-0.60.1",
                      Tools.getKernelDownloadDir("2.6.16.60-0.60.1-default",
                                                 "suse",
                                                 "SLES10",
                                                 "x86_64"));
         assertEquals("2.6.18-194.8.1.el5",
                      Tools.getKernelDownloadDir("2.6.18-194.8.1.el5",
                                                 "redhatenterpriseserver",
                                                 "5",
                                                 "x86_64"));
         assertEquals("2.6.32-71.18.1.el6.x86_64",
                      Tools.getKernelDownloadDir("2.6.32-71.18.1.el6.x86_64",
                                                 "redhatenterpriseserver",
                                                 "6",
                                                 "x86_64"));
         assertEquals("2.6.26-2",
                      Tools.getKernelDownloadDir("2.6.26-2-amd64",
                                                 "debian",
                                                 "lenny",
                                                 "x86_64"));
         assertEquals("2.6.32-5",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "debian",
                                                 "squeeze",
                                                 "x86_64"));
         assertEquals("2.6.32-5",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "debian",
                                                 "unknown",
                                                 "x86_64"));
         assertEquals("2.6.32-5-amd64",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 "unknown",
                                                 "unknown",
                                                 "x86_64"));
         assertNull(Tools.getKernelDownloadDir(null,
                                               "unknown",
                                               "unknown",
                                               "x86_64"));
         assertEquals("2.6.32-5-amd64",
                      Tools.getKernelDownloadDir("2.6.32-5-amd64",
                                                 null,
                                                 null,
                                                 "x86_64"));
    }

    @Test
    public void testGetDistVersionString() {
        /* String dist
           String version */
        assertEquals("LENNY", Tools.getDistVersionString("debian", "5.0.8"));
        assertEquals("SQUEEZE", Tools.getDistVersionString("debian", "6.0"));
        assertEquals("12", Tools.getDistVersionString(
                                        "fedora",
                                        "Fedora release 12 (Constantine)"));
        assertEquals("13", Tools.getDistVersionString(
                                        "fedora",
                                        "Fedora release 13 (Goddard)"));
        assertEquals("14",
                     Tools.getDistVersionString(
                                        "fedora",
                                        "Fedora release 14 (Laughlin)"));
        assertEquals("5", Tools.getDistVersionString(
                                        "redhat",
                                        "CentOS release 5.5 (Final)"));
        assertEquals("5", Tools.getDistVersionString(
                                        "redhat",
                                        "CentOS release 5.5 (Final)"));
        assertEquals(
                "6",
                Tools.getDistVersionString(
                    "redhatenterpriseserver",
                    "Red Hat Enterprise Linux Server release 6.0 (Santiago)"));
        assertEquals(
                "5",
                Tools.getDistVersionString(
                    "redhatenterpriseserver",
                    "Red Hat Enterprise Linux Server release 5.5 (Tikanga)"));
        assertEquals("squeeze/sid/10.10", /* maverick */
                     Tools.getDistVersionString("ubuntu", "squeeze/sid/10.10"));
        assertEquals("KARMIC",
                     Tools.getDistVersionString("ubuntu", "squeeze/sid/9.10"));
        assertEquals("LUCID",
                     Tools.getDistVersionString("ubuntu", "squeeze/sid/10.04"));
        assertEquals("HARDY",
                     Tools.getDistVersionString("ubuntu", "lenny/sid/8.04"));
        assertEquals("SLES10",
                     Tools.getDistVersionString(
                                  "suse",
                                  "SUSE Linux Enterprise Server 10 (x86_64)"));
        assertEquals("SLES11",
                     Tools.getDistVersionString(
                                  "suse",
                                  "SUSE Linux Enterprise Server 11 (x86_64)"));
        assertEquals("OPENSUSE11_2",
                     Tools.getDistVersionString(
                                  "suse",
                                  "openSUSE 11.2 (x86_64)"));
        assertEquals("OPENSUSE11_3",
                     Tools.getDistVersionString(
                                  "suse",
                                  "openSUSE 11.3 (x86_64)"));
        assertEquals("OPENSUSE11_4",
                     Tools.getDistVersionString(
                                  "suse",
                                  "openSUSE 11.4 (x86_64)"));
        assertEquals("2", Tools.getDistVersionString("openfiler",
                                                     "Openfiler NSA 2.3"));
    }

    @Test
    public void testJoin() {
        assertEquals("a,b", Tools.join(",", new String[]{"a", "b"}));
        assertEquals("a", Tools.join(",", new String[]{"a"}));
        assertEquals("", Tools.join(",", new String[]{}));
        assertEquals("", Tools.join(",", (String[]) null));
        assertEquals("ab", Tools.join(null, new String[]{"a", "b"}));
        assertEquals("a,b,c", Tools.join(",", new String[]{"a", "b", "c"}));


        assertEquals("", Tools.join("-", (Collection<String>) null));
        assertEquals("a-b", Tools.join("-", Arrays.asList("a", "b")));
        assertEquals("ab", Tools.join(null, Arrays.asList("a", "b")));

        final List<String> bigArray = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            bigArray.add("x");
        }
        assertTrue(Tools.join(",", bigArray).length() == 2000 - 1);

        assertEquals("a,b", Tools.join(",", new String[]{"a", "b"}, 2));
        assertEquals("a,b", Tools.join(",", new String[]{"a", "b"}, 3));
        assertEquals("a", Tools.join(",", new String[]{"a", "b"}, 1));
        assertEquals("", Tools.join(",", new String[]{"a", "b"}, 0));
        assertEquals("", Tools.join(",", new String[]{"a", "b"}, -1));
        assertEquals("", Tools.join(",", null, 1));

        assertEquals("a", Tools.join(",", new String[]{"a"}, 1));
        assertEquals("", Tools.join(",", new String[]{}, 2));
        assertEquals("", Tools.join(",", (String[]) null, 1));
        assertEquals("a,b,c", Tools.join(",", new String[]{"a", "b", "c"}, 3));
        assertTrue(Tools.join(",", bigArray.toArray(
                    new String[bigArray.size()]), 500).length() == 1000 - 1);
    }

    @Test
    public void testUCFirst() {
        assertEquals("Rasto", Tools.ucfirst("rasto"));
        assertEquals("Rasto", Tools.ucfirst("Rasto"));
        assertEquals("RASTO", Tools.ucfirst("RASTO"));
        assertEquals("", Tools.ucfirst(""));
        assertNull(Tools.ucfirst(null));
    }

    @Test
    public void testEnumToStringArray() {
        assertNull(Tools.enumToStringArray(null));
        final String[] testString = Tools.enumToStringArray(
                new Vector<String>(Arrays.asList("a", "b", "c")).elements());
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, testString);
    }

    @Test
    public void testGetIntersection() {
        assertEquals(new HashSet<String>(Arrays.asList("b")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b")),
                            new HashSet<String>(Arrays.asList("b", "c"))));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b")),
                            new HashSet<String>(Arrays.asList("a", "b"))));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b")),
                            new HashSet<String>(Arrays.asList("b", "a"))));
        assertEquals(new HashSet<String>(),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b")),
                            new HashSet<String>(Arrays.asList("c", "d"))));
        assertEquals(new HashSet<String>(Arrays.asList("a")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "a")),
                            new HashSet<String>(Arrays.asList("a", "a"))));
        assertEquals(new HashSet<String>(Arrays.asList("a", "c")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b", "c")),
                            new HashSet<String>(Arrays.asList("a", "d", "c"))));
        assertEquals(null,
                     Tools.getIntersection(null, null));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c")),
                     Tools.getIntersection(
                            new HashSet<String>(Arrays.asList("a", "b", "c")),
                            null));
        assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c")),
                     Tools.getIntersection(
                            null,
                            new HashSet<String>(Arrays.asList("a", "b", "c"))));
    }

    @Test
    public void testHTML() {
        assertEquals("<html><p>test\n</html>", Tools.html("test"));
        assertEquals("<html><p>test<br>line2\n</html>",
                     Tools.html("test\nline2"));
        assertEquals("<html>\n</html>", Tools.html(null));
    }

    @Test
    public void testIsStringClass() {
        assertTrue(Tools.isStringClass("string"));
        assertTrue(Tools.isStringClass((String) null));
        assertTrue(Tools.isStringClass((Object) null));
        assertFalse(Tools.isStringClass(new Object()));
        assertFalse(Tools.isStringClass(new StringBuilder()));
        assertFalse(Tools.isStringClass(
                new lcmc.gui.resources.StringInfo("name", "string", null)));
    }

    @Test
    public void testIsStringInfoClass() {
        assertFalse(Tools.isStringInfoClass("string"));
        assertFalse(Tools.isStringInfoClass((String) null));
        assertFalse(Tools.isStringInfoClass((Object) null));
        assertFalse(Tools.isStringInfoClass(new Object()));
        assertFalse(Tools.isStringInfoClass(new StringBuilder()));
        assertTrue(Tools.isStringInfoClass(
                new lcmc.gui.resources.StringInfo("name", "string", null)));
    }

    @Test
    public void testEscapeConfig() {
        assertNull(Tools.escapeConfig(null));
        assertEquals("", Tools.escapeConfig(""));
        assertEquals("\"\\\"\"", Tools.escapeConfig("\""));
        assertEquals("text", Tools.escapeConfig("text"));
        assertEquals("\"text with space\"",
                     Tools.escapeConfig("text with space"));
        assertEquals("\"text with \\\"\"",
                     Tools.escapeConfig("text with \""));
        assertEquals("\"just\\\"\"",
                     Tools.escapeConfig("just\""));
    }

    @Test
    public void testStartProgressIndicator() {
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            Tools.startProgressIndicator(null);
            Tools.startProgressIndicator("test");
            Tools.startProgressIndicator("test2");
            Tools.startProgressIndicator("test3");
            Tools.startProgressIndicator(null, "test4");
            Tools.startProgressIndicator("name", "test4");
            Tools.startProgressIndicator("name2", "test4");
            Tools.startProgressIndicator("name2", null);
            Tools.startProgressIndicator(null, null);
            for (final Host host : TestSuite1.getHosts()) {
                Tools.startProgressIndicator(host.getName(), "test");
            }

            for (final Host host : TestSuite1.getHosts()) {
                Tools.stopProgressIndicator(host.getName(), "test");
            }
            Tools.stopProgressIndicator(null, null);
            Tools.stopProgressIndicator("name2", null);
            Tools.stopProgressIndicator("name2", "test4");
            Tools.stopProgressIndicator("name", "test4");
            Tools.stopProgressIndicator(null, "test4");
            Tools.stopProgressIndicator("test3");
            Tools.stopProgressIndicator("test2");
            Tools.stopProgressIndicator("test");
            Tools.stopProgressIndicator(null);
        }
    }

    @Test
    public void testStopProgressIndicator() {
    }

    @Test
    public void testProgressIndicatorFailed() {
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            Tools.progressIndicatorFailed(null, "fail3");
            Tools.progressIndicatorFailed("name", "fail2");
            Tools.progressIndicatorFailed("name", null);
            Tools.progressIndicatorFailed("fail1");
            Tools.progressIndicatorFailed(null);

            Tools.progressIndicatorFailed("fail two seconds", 2);
            for (final Host host : TestSuite1.getHosts()) {
                Tools.progressIndicatorFailed(host.getName(), "fail");
            }
        }
        TestSuite1.clearStdout();
    }

    @Test
    public void testSetSize() {
        final JPanel p = new JPanel();
        Tools.setSize(p, 20, 10);
        assertEquals(new Dimension(20, 10), p.getMaximumSize());
        assertEquals(new Dimension(20, 10), p.getMinimumSize());
        assertEquals(new Dimension(20, 10), p.getPreferredSize());
    }

    @Test
    public void testCompareVersions() {
        try {
            assertEquals(-1, Tools.compareVersions("2.1.3", "2.1.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("2.1.3", "3.1.2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("2.1.3", "2.2.2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1.3", "2.1.3.1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("2.1.3.1", "2.1.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2", "2.1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }

        try {
            assertEquals(0, Tools.compareVersions("2.1.3", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1", "2.1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2", "2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }

        try {
            assertEquals(1, Tools.compareVersions("2.1.4", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("3.1.2", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("2.2.2", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1.3.1", "2.1.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("2.1.4", "2.1.3.1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1.3", "2.1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1.3", "2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("2.1", "2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.3", "8.3.0"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }

        try {
            assertEquals(-100, Tools.compareVersions("", ""));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions(null, null));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("", "2.1.3"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("2.1.3", ""));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions(null, "2.1.3"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("2.1.3", null));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("2.1.3", "2.1.a"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("a.1.3", "2.1.3"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }

        try {
            assertEquals(-100, Tools.compareVersions("rc1", "8rc1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8rc1", "8rc"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8rc1", "8rc"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8rc", "8rc1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8rc1", "rc"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("rc", "8rc1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8r1", "8.3.1rc1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8.3.1", "8.3rc1.1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }
        try {
            assertEquals(-100, Tools.compareVersions("8.3rc1.1", "8.3.1"));
            assertFalse(true);
        } catch (Exceptions.IllegalVersionException e) {
        }

        try {
            assertEquals(1, Tools.compareVersions("8.3.10rc1", "8.3.9"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("8.3.10rc2", "8.3.10rc1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("8.3.10", "8.3.10rc2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("8.3.10",
                                                  "8.3.10rc99999999"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }

        try {
            assertEquals(0, Tools.compareVersions("8.3.10rc1", "8.3.10rc1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.3rc1", "8.3rc1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8rc1", "8rc1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3.9", "8.3.10rc1"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3.10rc1", "8.3.10rc2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3.10rc2", "8.3.10"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.3rc2", "8.3.0"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.3", "8.3.2"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.3.2", "8.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3", "8.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("8.4", "8.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.4", "8.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3", "8.4.5"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3.5", "8.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("8.3", "8.4rc3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.4", "8.4.0rc3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("8.4.0rc3", "8.4"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("8.4rc3", "8.3"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("1.1.7-2.fc16", "1.1.7"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("1.1.7-2.fc16", "1.1.8"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("1.1.7-2.fc16", "1.1.6"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("1.7.0_03", "1.7"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(-1, Tools.compareVersions("1.6.0_26", "1.7"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(1, Tools.compareVersions("1.7", "1.6.0_26"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
        try {
            assertEquals(0, Tools.compareVersions("1.6.0_26", "1.6.0"));
        } catch (Exceptions.IllegalVersionException e) {
            assertFalse(true);
        }
    }

    @Test
    public void testCharCount() {
        assertEquals(1, Tools.charCount("abcd", 'b'));
        assertEquals(0, Tools.charCount("abcd", 'e'));
        assertEquals(1, Tools.charCount("abcd", 'd'));
        assertEquals(1, Tools.charCount("abcd", 'a'));
        assertEquals(2, Tools.charCount("abcdb", 'b'));
        assertEquals(5, Tools.charCount("ccccc", 'c'));
        assertEquals(1, Tools.charCount("a", 'a'));
        assertEquals(0, Tools.charCount("a", 'b'));

        assertEquals(0, Tools.charCount("", 'b'));
        assertEquals(0, Tools.charCount(null, 'b'));
    }

    @Test
    public void testIsLinux() {
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertFalse(Tools.isLinux());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertTrue(Tools.isLinux());
        }
    }

    @Test
    public void testIsWindows() {
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertTrue(Tools.isWindows());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertFalse(Tools.isWindows());
        }
    }

    @Test
    public void testGetFile() {
        final String testFile = "/help-progs/lcmc-gui-helper";
        assertTrue(Tools.getFile(testFile).indexOf("#!") == 0);
        assertNull(Tools.getFile(null));
        assertNull(Tools.getFile("not_existing_file"));
    }

    @Test
    public void testParseAutoArgs() {
        Tools.parseAutoArgs(null);
        Tools.parseAutoArgs("test");
        TestSuite1.clearStdout();
    }

    @Test
    public void testSleep() {
        Tools.sleep(1);
        Tools.sleep(1.1f);
    }

    @Test
    public void testGetLatestVersion() {
        if (TestSuite1.CONNECT_LINBIT) {
            final String latestVersion = Tools.getLatestVersion();
            assertNotNull(latestVersion);
            try {
                assertEquals(-1, Tools.compareVersions(latestVersion,
                                                   Tools.getRelease()));
            } catch (Exceptions.IllegalVersionException e) {
                assertFalse(true);
            }
        }
    }

    @Test
    public void testOpenBrowser() {
        if (TestSuite1.INTERACTIVE) {
            Tools.openBrowser("http://www.google.com");
        }
    }

    @Test
    public void testHideMousePointer() {
        Tools.hideMousePointer(new JPanel());
    }

    @Test
    public void testIsNumber() {
        assertTrue(Tools.isNumber("1"));
        assertTrue(Tools.isNumber("-1"));
        assertTrue(Tools.isNumber("0"));
        assertTrue(Tools.isNumber("-0"));
        assertTrue(Tools.isNumber("1235"));
        assertTrue(Tools.isNumber("100000000000000000"));
        assertTrue(Tools.isNumber("-100000000000000000"));
        assertFalse(Tools.isNumber("0.1"));
        assertFalse(Tools.isNumber("1 1"));
        assertFalse(Tools.isNumber("-"));
        assertFalse(Tools.isNumber(""));
        assertFalse(Tools.isNumber("a"));
        assertFalse(Tools.isNumber(null));
        assertFalse(Tools.isNumber(".5"));
        assertFalse(Tools.isNumber("a1344"));
        assertFalse(Tools.isNumber("1344a"));
        assertFalse(Tools.isNumber("13x44"));
    }

    @Test
    public void testShellList() {
        assertEquals("{'a','b'}", Tools.shellList(new String[]{"a", "b"}));
        assertEquals("{'a','b','b'}",
                     Tools.shellList(new String[]{"a", "b", "b"}));
        assertEquals("a", Tools.shellList(new String[]{"a"}));
        assertNull(Tools.shellList(new String[]{}));
        assertNull(Tools.shellList(null));
    }

    @Test
    public void testAreEqual() {
        assertTrue(Tools.areEqual(null, null));
        assertTrue(Tools.areEqual("", ""));
        assertTrue(Tools.areEqual("x", "x"));

        assertFalse(Tools.areEqual("x", "a"));
        assertFalse(Tools.areEqual("x", ""));
        assertFalse(Tools.areEqual("", "x"));
        assertFalse(Tools.areEqual(null, "x"));
        assertFalse(Tools.areEqual("x", null));
    }

    @Test
    public void testExtractUnit() {
        Assert.assertArrayEquals(new Object[]{"10", "min"},
                                 Tools.extractUnit("10min"));
        Assert.assertArrayEquals(new Object[]{"0", "s"},
                                 Tools.extractUnit("0s"));
        Assert.assertArrayEquals(new Object[]{"0", ""},
                                 Tools.extractUnit("0"));
        Assert.assertArrayEquals(new Object[]{"5", ""},
                                 Tools.extractUnit("5"));
        Assert.assertArrayEquals(new Object[]{"", "s"},
                                 Tools.extractUnit("s"));
        Assert.assertArrayEquals(new Object[]{null, null},
                                 Tools.extractUnit(null));
    }

    @Test
    public void testGetRandomSecret() {
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            final String s = Tools.getRandomSecret(2000);
            assertTrue(s.length() == 2000);
            final int count = Tools.charCount(s, 'a');
            assertTrue(count > 2 && count < 500);
        }
    }

    @Test
    public void testIsLocalIp() {
        assertTrue(Tools.isLocalIp(null));
        assertTrue(Tools.isLocalIp("127.0.0.1"));
        assertTrue(Tools.isLocalIp("127.0.1.1"));
        assertFalse(Tools.isLocalIp("127.0.0"));
        assertFalse(Tools.isLocalIp("127.0.0.1.1"));
        assertFalse(Tools.isLocalIp("127.0.0.a"));
        assertFalse(Tools.isLocalIp("a"));
        assertFalse(Tools.isLocalIp("a"));
        try {
            assertTrue(Tools.isLocalIp(
                            InetAddress.getLocalHost().getHostAddress()));
        } catch (java.net.UnknownHostException e) {
            fail();
        }
        for (final Host host : TestSuite1.getHosts()) {
            assertFalse(Tools.isLocalIp(host.getIp()));
        }
    }

    @Test
    public void testConvertKilobytes() {
        assertEquals("wrong", "aa", Tools.convertKilobytes("aa"));
        assertEquals("negative", "-1000K", Tools.convertKilobytes("-1000"));
        assertEquals("2G", Tools.convertKilobytes("2G"));
        assertEquals("0K", Tools.convertKilobytes("0"));
        assertEquals("1K", Tools.convertKilobytes("1"));

        assertEquals("1023K", Tools.convertKilobytes("1023"));
        assertEquals("1M", Tools.convertKilobytes("1024"));
        assertEquals("1025K", Tools.convertKilobytes("1025"));

        assertEquals("2047K", Tools.convertKilobytes("2047"));
        assertEquals("2M", Tools.convertKilobytes("2048"));
        assertEquals("2049K", Tools.convertKilobytes("2049"));
        assertEquals("1048575K", Tools.convertKilobytes("1048575"));
        assertEquals("1G", Tools.convertKilobytes("1048576"));
        assertEquals("1023M", Tools.convertKilobytes("1047552"));
        assertEquals("1048577K", Tools.convertKilobytes("1048577"));
        assertEquals("1025M", Tools.convertKilobytes("1049600"));

        assertEquals("1073741825K", Tools.convertKilobytes("1073741825"));
        assertEquals("1023G", Tools.convertKilobytes("1072693248"));
        assertEquals("1T", Tools.convertKilobytes("1073741824"));
        assertEquals("1025G", Tools.convertKilobytes("1074790400"));
        assertEquals("1050625M", Tools.convertKilobytes("1075840000"));
        assertEquals("1073741827K", Tools.convertKilobytes("1073741827"));



        assertEquals("1P", Tools.convertKilobytes("1099511627776"));
        assertEquals("1024P", Tools.convertKilobytes("1125899906842624"));
        assertEquals("10000P", Tools.convertKilobytes("10995116277760000"));
    }

    @Test
    public void testConvertToKilobytes() {
        assertEquals(10, Tools.convertToKilobytes("10k"));
        assertEquals(5, Tools.convertToKilobytes("5K"));

        assertEquals(6144, Tools.convertToKilobytes("6M"));
        assertEquals(7168, Tools.convertToKilobytes("7m"));

        assertEquals(8388608, Tools.convertToKilobytes("8G"));
        assertEquals(9437184, Tools.convertToKilobytes("9g"));

        assertEquals(10737418240L, Tools.convertToKilobytes("10T"));
        assertEquals(11811160064L, Tools.convertToKilobytes("11t"));

        assertEquals(13194139533312L, Tools.convertToKilobytes("12P"));
        assertEquals(14293651161088L, Tools.convertToKilobytes("13p"));

        assertEquals(1099511627776000000L,
                     Tools.convertToKilobytes("1000000P"));
        //TODO:
        //assertEquals(10995116277760000000L,
        //             Tools.convertToKilobytes("10000000"));

        assertEquals(-1, Tools.convertToKilobytes("7"));

        assertEquals(-1, Tools.convertToKilobytes(null));
        assertEquals(-1, Tools.convertToKilobytes("P"));
        assertEquals(-1, Tools.convertToKilobytes("-3"));
    }

    @Test
    public void testGetUnixPath() {
        assertEquals("/bin", Tools.getUnixPath("/bin"));
        if (Tools.isWindows()) {
            assertEquals("/bin/dir/file",
                         Tools.getUnixPath("d:\\bin\\dir\\file"));
        }
    }

    @Test
    public void testTrimText() {
        assertNull(Tools.trimText(null));
        assertEquals("x", Tools.trimText("x"));
        final String x20 = " xxxxxxxxxxxxxxxxxxx";
        assertEquals(x20 + x20 + x20 + x20,
                     Tools.trimText(x20 + x20 + x20 + x20));
        assertEquals(x20 + x20 + x20 + x20 + "\n" + x20.trim(),
                     Tools.trimText(x20 + x20 + x20 + x20 + x20));
    }

    @Test
    public void testWaitForSwing() {
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            Tools.waitForSwing();
        }
    }

    @Test
    public void testGetDirectoryPart() {
        assertEquals("/usr/bin/", Tools.getDirectoryPart("/usr/bin/somefile"));
        assertEquals("/usr/bin/", Tools.getDirectoryPart("/usr/bin/"));
        assertEquals("somefile", Tools.getDirectoryPart("somefile"));
        assertEquals("", Tools.getDirectoryPart(""));
        assertNull(Tools.getDirectoryPart(null));
        assertEquals("/", Tools.getDirectoryPart("/"));
    }

    @Test
    public void testEscapeQuotes() {
        assertEquals("test", Tools.escapeQuotes("test", 0));
        assertEquals("test", Tools.escapeQuotes("test", -1));
        assertNull(Tools.escapeQuotes(null, -1));
        assertNull(Tools.escapeQuotes(null, 1));

        assertEquals("test", Tools.escapeQuotes("test", 1));
        assertEquals("test", Tools.escapeQuotes("test", 2));
        assertEquals("test", Tools.escapeQuotes("test", 100));

        assertEquals("\\\"\\$\\`test\\\\",
                     Tools.escapeQuotes("\"$`test\\", 1));
        assertEquals("\\\\\\\"\\\\\\$\\\\\\`test\\\\\\\\",
                     Tools.escapeQuotes("\"$`test\\", 2));
    }

    @Test
    public void testGetHostCheckBoxes() {
        for (final Host host : TestSuite1.getHosts()) {
            final Map<Host, JCheckBox> comps =
                                    Tools.getHostCheckBoxes(host.getCluster());
            assertNotNull(comps);
            assertTrue(comps.size() == TestSuite1.getHosts().size());
            assertTrue(comps.containsKey(host));
        }
    }

    @Test
    public void testVersionBeforePacemaker() {
        final Host host = new Host();
        host.setPacemakerVersion("1.1.5");
        host.setHeartbeatVersion(null);
        assertFalse(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion(null);
        host.setHeartbeatVersion("2.1.4");
        assertTrue(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion(null);
        host.setHeartbeatVersion("2.1.3");
        assertTrue(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion(null);
        host.setHeartbeatVersion(null);
        assertFalse(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion("1.0.9");
        host.setHeartbeatVersion("3.0.2");
        assertFalse(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion("1.0.9");
        host.setHeartbeatVersion("2.99.0");
        assertFalse(Tools.versionBeforePacemaker(host));

        host.setPacemakerVersion("1.0.9");
        host.setHeartbeatVersion(null);
        assertFalse(Tools.versionBeforePacemaker(host));
        for (final Host h : TestSuite1.getHosts()) {
            Tools.versionBeforePacemaker(h);
        }
    }

    private String ssb(final String s) {
        final StringBuffer sb = new StringBuffer(s);
        Tools.chomp(sb);
        return sb.toString();
    }

    @Test
    public void testComp() {
        assertEquals("",      ssb(""));
        assertEquals("\n",      ssb("\n\n\n"));
        assertEquals(" ",      ssb(" "));
        assertEquals("a",     ssb("a"));
        assertEquals("a\nb",  ssb("a\nb"));
        assertEquals(" a\n",    ssb(" a\n"));
        assertEquals(" a\n",    ssb(" a\n\n"));
        assertEquals(" a \n",    ssb(" a \n"));
    }
}
