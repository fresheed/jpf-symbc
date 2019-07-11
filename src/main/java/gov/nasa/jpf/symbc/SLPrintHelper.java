package gov.nasa.jpf.symbc;

import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.util.Pair;
import gov.nasa.jpf.vm.Types;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by fresheed on 11.07.19.
 */
public class SLPrintHelper {
        /*
     * The way this method works is specific to the format of the methodSummary data structure
     */

    private Map<String, SLPrintHelper.MethodSummary> allSummaries;

    public SLPrintHelper(Map<String, SLPrintHelper.MethodSummary> as){
        allSummaries = as;
    }

    // TODO: needs to be changed not to use String representations
    private void printMethodSummary(PrintWriter pw, MethodSummary methodSummary) {

        System.out.println("Inputs: " + methodSummary.getSymValues());
        Vector<Pair> pathConditions = methodSummary.getPathConditions();
        if (pathConditions.size() > 0) {
            Iterator it = pathConditions.iterator();
            String allTestCases = "";
            while (it.hasNext()) {
                String testCase = methodSummary.getMethodName() + "(";
                Pair pcPair = (Pair) it.next();
                String pc = (String) pcPair._1;
                String errorMessage = (String) pcPair._2;
                String symValues = methodSummary.getSymValues();
                String argValues = methodSummary.getArgValues();
                String argTypes = methodSummary.getArgTypes();

                StringTokenizer st = new StringTokenizer(symValues, ",");
                StringTokenizer st2 = new StringTokenizer(argValues, ",");
                StringTokenizer st3 = new StringTokenizer(argTypes, ",");
                if (!argTypes.isEmpty() && argValues.isEmpty()) {
                    continue;
                }
                while (st2.hasMoreTokens()) {
                    String token = "";
                    String actualValue = st2.nextToken();
                    byte actualType = Byte.parseByte(st3.nextToken());
                    if (st.hasMoreTokens())
                        token = st.nextToken();
                    if (pc.contains(token)) {
                        String temp = pc.substring(pc.indexOf(token));
                        if (temp.indexOf(']') < 0) {
                            continue;
                        }

                        String val = temp.substring(temp.indexOf("[") + 1, temp.indexOf("]"));

                        if (actualType == Types.T_INT || actualType == Types.T_FLOAT || actualType == Types.T_LONG
                                || actualType == Types.T_SHORT || actualType == Types.T_BYTE
                                || actualType == Types.T_CHAR || actualType == Types.T_DOUBLE) {
                            String suffix = "";
                            if (actualType == Types.T_LONG) {
                                suffix = "l";
                            } else if (actualType == Types.T_FLOAT) {
                                val = String.valueOf(Double.valueOf(val).floatValue());
                                suffix = "f";
                            }
                            if (val.endsWith("Infinity")) {
                                boolean isNegative = val.startsWith("-");
                                val = ((actualType == Types.T_DOUBLE) ? "Double" : "Float");
                                val += isNegative ? ".NEGATIVE_INFINITY" : ".POSITIVE_INFINITY";
                                suffix = "";
                            }
                            testCase = testCase + val + suffix + ",";
                        } else if (actualType == Types.T_BOOLEAN) { // translate boolean values represented as ints
                            // to "true" or "false"
                            if (val.equalsIgnoreCase("0"))
                                testCase = testCase + "false" + ",";
                            else
                                testCase = testCase + "true" + ",";
                        } else
                            throw new RuntimeException(
                                    "## Error: listener does not support type other than int, long, short, byte, float, double and boolean");
                        // TODO: to extend with arrays
                    } else {
                        // need to check if value is concrete
                        if (token.contains("CONCRETE"))
                            testCase = testCase + actualValue + ",";
                        else
                            testCase = testCase + SymbolicInteger.UNDEFINED + "(don't care),";// not correct in concolic
                                                                                              // mode
                    }
                }
                if (testCase.endsWith(","))
                    testCase = testCase.substring(0, testCase.length() - 1);
                testCase = testCase + ")";
                // process global information and append it to the output

                if (!errorMessage.equalsIgnoreCase(""))
                    testCase = testCase + "  --> " + errorMessage;
                // do not add duplicate test case
                if (!allTestCases.contains(testCase))
                    allTestCases = allTestCases + "\n" + testCase;
            }
            pw.println(allTestCases);
        } else {
            pw.println("No path conditions for " + methodSummary.getMethodName() + "(" + methodSummary.getArgValues()
                    + ")");
        }
    }

    private void printMethodSummaryHTML(PrintWriter pw, MethodSummary methodSummary) {
        pw.println("<h1>Test Cases Generated by Symbolic JavaPath Finder for " + methodSummary.getMethodName()
                + " (Path Coverage) </h1>");

        Vector<Pair> pathConditions = methodSummary.getPathConditions();
        if (pathConditions.size() > 0) {
            Iterator it = pathConditions.iterator();
            String allTestCases = "";
            String symValues = methodSummary.getSymValues();
            StringTokenizer st = new StringTokenizer(symValues, ",");
            while (st.hasMoreTokens())
                allTestCases = allTestCases + "<td>" + st.nextToken() + "</td>";
            allTestCases = "<tr>" + allTestCases + "<td>RETURN</td></tr>\n";
            while (it.hasNext()) {
                String testCase = "<tr>";
                Pair pcPair = (Pair) it.next();
                String pc = (String) pcPair._1;
                String errorMessage = (String) pcPair._2;
                // String symValues = methodSummary.getSymValues();
                String argValues = methodSummary.getArgValues();
                String argTypes = methodSummary.getArgTypes();
                // StringTokenizer
                st = new StringTokenizer(symValues, ",");
                StringTokenizer st2 = new StringTokenizer(argValues, ",");
                StringTokenizer st3 = new StringTokenizer(argTypes, ",");
                while (st2.hasMoreTokens()) {
                    String token = "";
                    String actualValue = st2.nextToken();
                    byte actualType = Byte.parseByte(st3.nextToken());
                    if (st.hasMoreTokens())
                        token = st.nextToken();
                    if (pc.contains(token)) {
                        String temp = pc.substring(pc.indexOf(token));
                        if (temp.indexOf(']') < 0) {
                            continue;
                        }

                        String val = temp.substring(temp.indexOf("[") + 1, temp.indexOf("]"));
                        if (actualType == Types.T_INT || actualType == Types.T_FLOAT || actualType == Types.T_LONG
                                || actualType == Types.T_SHORT || actualType == Types.T_BYTE
                                || actualType == Types.T_DOUBLE)
                            testCase = testCase + "<td>" + val + "</td>";
                        else if (actualType == Types.T_BOOLEAN) { // translate boolean values represented as ints
                            // to "true" or "false"
                            if (val.equalsIgnoreCase("0"))
                                testCase = testCase + "<td>false</td>";
                            else
                                testCase = testCase + "<td>true</td>";
                        } else
                            throw new RuntimeException(
                                    "## Error: listener does not support type other than int, long, short, byte, float, double and boolean");

                    } else {
                        // need to check if value is concrete
                        if (token.contains("CONCRETE"))
                            testCase = testCase + "<td>" + actualValue + "</td>";
                        else
                            testCase = testCase + "<td>" + SymbolicInteger.UNDEFINED + "(don't care)</td>"; // not
                                                                                                            // correct
                                                                                                            // in
                                                                                                            // concolic
                                                                                                            // mode
                    }
                }

                if (!errorMessage.equalsIgnoreCase(""))
                    testCase = testCase + "<td>" + errorMessage + "</td>";
                // do not add duplicate test case
                if (!allTestCases.contains(testCase))
                    allTestCases = allTestCases + testCase + "</tr>\n";
            }
            pw.println("<table border=1>");
            pw.print(allTestCases);
            pw.println("</table>");
        } else {
            pw.println("No path conditions for " + methodSummary.getMethodName() + "(" + methodSummary.getArgValues()
                    + ")");
        }

    }

    // -------- the publisher interface
    public void publishFinished(Publisher publisher) {
        String[] dp = SymbolicInstructionFactory.dp;
        if (dp[0].equalsIgnoreCase("no_solver") || dp[0].equalsIgnoreCase("cvc3bitvec"))
            return;

        PrintWriter pw = publisher.getOut();

        publisher.publishTopicStart("Method Summaries");
        Iterator it = allSummaries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            MethodSummary methodSummary = (MethodSummary) me.getValue();
            printMethodSummary(pw, methodSummary);
        }

        publisher.publishTopicStart("Method Summaries (HTML)");
        it = allSummaries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            MethodSummary methodSummary = (MethodSummary) me.getValue();
            printMethodSummaryHTML(pw, methodSummary);
        }
    }

    public static class MethodSummary {
        private String methodName = "";
        private String argTypes = "";
        private String argValues = "";
        private String symValues = "";
        private Vector<Pair> pathConditions;

        public MethodSummary() {
            pathConditions = new Vector<Pair>();
        }

        public void setMethodName(String mName) {
            this.methodName = mName;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public void setArgTypes(String args) {
            this.argTypes = args;
        }

        public String getArgTypes() {
            return this.argTypes;
        }

        public void setArgValues(String vals) {
            this.argValues = vals;
        }

        public String getArgValues() {
            return this.argValues;
        }

        public void setSymValues(String sym) {
            this.symValues = sym;
        }

        public String getSymValues() {
            return this.symValues;
        }

        public void addPathCondition(Pair pc) {
            pathConditions.add(pc);
        }

        public Vector<Pair> getPathConditions() {
            return this.pathConditions;
        }

    }

}
