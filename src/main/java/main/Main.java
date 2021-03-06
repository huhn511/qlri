package main;

import api.resp.general.ResponseAbstract;
import api.resp.general.ResponseError;
import commands.Command;
import tangle.TangleAPI;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class Main {

    public static final DateFormat DF = new SimpleDateFormat("YYYY/MMM/dd HH:mm:ss");
    private static final String VERSION = "0.3";
    private static final Scanner s = new Scanner(System.in);

    private static Persistence persistence;

    public static void main(String[] args) {

        println("");
        println("=== Welcome to QLRI v"+VERSION+" ===");
        println("");

        Configs cfg = Configs.getInstance();

        cfg.processArguments(args);
        persistence = new Persistence(cfg.isTestnet());
        if(cfg.isApiEnabled())
            new api.API(persistence, cfg.getHost(), cfg.getPort());

        println("");
        println("Type 'help' for more information.");
        println("");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                println("terminating ...");
                persistence.store();
            }
        });

        while(true) {
            String input = nextLine();
            String[] par = input.split(" ");

            String commandString = par[0];
            println("");
            performCommandFromTerminal(commandString, par);
            println("");
        }
    }

    /**
     * Performs a command by validating the parameters and calling the respective command if validation succeeded.
     * @param command the command to call the command
     * @param par     the parameters passed with the command call
     * */
    private static void performCommandFromTerminal(String command, String[] par) {

        Command c = Command.findCommand(command);

        if(c == null) {
            println("unknown command '"+command+"', maybe try 'help'");
            return;
        }

        try {

            String validation = c.getCallValidatorForTerminal().validate(par);
            if(validation == null) {
                executeCommandFromTerminal(c, par);
            } else {
                println(validation);
                println("for more information try: 'help "+c.getAlias()+"'");
            }

        } catch (Throwable t) {
            System.err.println("exception thrown while trying to perform command '" + c.getName() + "':");
            t.printStackTrace();
        }

    }

    /**
     * Executes a command.
     * @param c the command to execute
     * @param par the parameters (because of how it is generated from user input, the first parameter is the command name)
     * */
    public static void executeCommandFromTerminal(Command c, String[] par) {

        Map<String, Object> parMap = c.getCallValidatorForTerminal().genParMap(par);
        ResponseAbstract response = c.perform(persistence, parMap);

        if(response instanceof ResponseError) {
            println(((ResponseError) response).getError());
            return;
        }

        c.terminalPostPerformAction(response, persistence, par);
    }

    /**
     * Prints a string into the terminal into a seperate line.
     * @param s the string to print
     * */
    public static void println(String s) {
        s = s.replace("\n", "\n  ");
        System.out.println("  " + s);
    }

    /**
     * Similar to println() but exclusively used for printing error messages.
     * @param s the error message to print
     * */
    public static void err(String s) {
        s = s.replace("\n", "\n  ");
        System.err.println("  " + s);
    }

    /**
     * Waits for user input, giving user opportunity to execute any command.
     * @return user input
     * */
    private static String nextLine() {
        System.out.print("$ ");
        return s.nextLine();
    }

    /**
     * Forces user to give input before user can execute another command.
     * @return user input
     * */
    public static String input() {
        System.out.print("  > ");
        return s.nextLine();
    }
}