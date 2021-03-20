package net.kibotu.android.deviceinfo.library.misc;

/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information (GPLv3) */

import android.util.Log;

import java.io.*;
import java.util.StringTokenizer;

public class ShellUtils {

    private static final String TAG = ShellUtils.class.getSimpleName();

    //various console cmds
    public final static String SHELL_CMD_CHMOD = "chmod";
    public final static String SHELL_CMD_KILL = "kill -9";
    public final static String SHELL_CMD_RM = "rm";
    public final static String SHELL_CMD_PS = "ps";
    public final static String SHELL_CMD_PIDOF = "pidof";
    public final static String CHMOD_EXE_VALUE = "700";

    public static boolean isRootPossible() {
        try {
            // Check if Superuser.apk exists
            File fileSU = new File("/system/app/Superuser.apk");
            if (fileSU.exists())
                return true;

            fileSU = new File("/system/bin/su");
            if (fileSU.exists())
                return true;

            //Check for 'su' binary
            String[] cmd = {"which su"};
            int exitCode = ShellUtils.doShellCommand(null, cmd, new ShellCallback() {

                @Override
                public void shellOut(String msg) {
                    //System.out.print(msg);
                }

                @Override
                public void processComplete(int exitValue) {
                }

            }, false, true).exitValue();

            if (exitCode == 0) {
                new IllegalAccessException("Can acquire root permissions").printStackTrace();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            //this means that there is no root to be had (normally)
        }
        return false;
    }


    public static int findProcessId(String command) {
        int procId = -1;

        try {
            procId = findProcessIdWithPidOf(command);

            if (procId == -1)
                procId = findProcessIdWithPS(command);
        } catch (Exception e) {
            try {
                procId = findProcessIdWithPS(command);
            } catch (Exception e2) {
                Log.e(TAG, "Unable to get proc id for: " + command, e2);
            }
        }

        return procId;
    }

    //use 'pidof' command
    public static int findProcessIdWithPidOf(String command) throws Exception {

        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs;

        String baseName = new File(command).getName();
        //fix contributed my mikos on 2010.12.10
        procPs = r.exec(new String[]{SHELL_CMD_PIDOF, baseName});
        //procPs = r.exec(SHELL_CMD_PIDOF);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {

            try {
                //this line should just be the process id
                procId = Integer.parseInt(line.trim());
                break;
            } catch (NumberFormatException e) {
                Log.e(TAG, "unable to parse process pid: " + line, e);
            }
        }


        return procId;

    }

    //use 'ps' command
    public static int findProcessIdWithPS(String command) throws Exception {

        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs = null;

        procPs = r.exec(SHELL_CMD_PS);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line = null;

        while ((line = reader.readLine()) != null) {
            if (!line.contains(' ' + command))
                continue;

            StringTokenizer st = new StringTokenizer(line, " ");
            st.nextToken(); //proc owner

            procId = Integer.parseInt(st.nextToken().trim());

            break;
        }

        return procId;
    }

    public static int doShellCommand(String[] cmds, ShellCallback sc, boolean runAsRoot, boolean waitFor) throws Exception {
        return doShellCommand(null, cmds, sc, runAsRoot, waitFor).exitValue();
    }

    public static Process doShellCommand(Process proc, String[] cmds, ShellCallback sc, boolean runAsRoot, boolean waitFor) throws Exception {
        if (proc == null) {
            if (runAsRoot)
                proc = Runtime.getRuntime().exec("su");
            else
                proc = Runtime.getRuntime().exec("sh");
        }

        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

        for (String cmd : cmds) {
            Log.v(TAG, "executing shell cmd: " + cmd + "; runAsRoot=" + runAsRoot + ";waitFor=" + waitFor);

            out.write(cmd);
            out.write("\n");
        }

        out.flush();
        out.write("exit\n");
        out.flush();

        if (waitFor) {

            final char buf[] = new char[20];

            // Consume the "stdout"
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            int read = 0;
            while ((read = reader.read(buf)) != -1) {
                if (sc != null) sc.shellOut(new String(buf));
            }

            // Consume the "stderr"
            reader = new InputStreamReader(proc.getErrorStream());
            read = 0;
            while ((read = reader.read(buf)) != -1) {
                if (sc != null) sc.shellOut(new String(buf));
            }

            proc.waitFor();

        }

        if (sc != null) {
            sc.processComplete(proc.exitValue());
        }

        return proc;
    }

    /**
     * Executes a shell command.
     *
     * @param command - Unix shell command.
     * @return <code>true</code> if shell command was successful.
     * @credits http://stackoverflow.com/a/15485210
     */
    public static boolean executeShellCommand(final String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            Log.v(TAG, "'" + command + "' successfully excecuted.");
            Log.v(TAG, "is rooted by su command");
            return true;
        } catch (final Exception e) {
            Log.e(TAG, "" + e.getMessage());
            return false;
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (final Exception e) {
                    Log.e(TAG, "" + e.getMessage());
                }
            }
        }
    }

    public static int readIntegerFile(final String filePath) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            final String line = reader.readLine();
            return Integer.parseInt(line);
        } catch (final Exception e) {
            try {
                Thread.currentThread().join();
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public interface ShellCallback {
        void shellOut(String shellLine);

        void processComplete(int exitValue);
    }
}
