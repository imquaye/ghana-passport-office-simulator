import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GhanaServiceSimulator – main entry point.
 *
 * The simulator processes requests.csv by arrivalOrder. No request is manually
 * moved ahead to create a trace. Instead, the service centre admits each
 * arriving applicant into the correct ADT and performs service work at fixed
 * service windows during the arrivals. This models a busy passport office where
 * new applicants keep arriving while counters are already serving earlier ones.
 *
 * Service-window policy used in this run:
 *   - after every 3rd arrival, one applicant is served;
 *   - after every 4th arrival, an additional service window is opened.
 *
 * Because this policy is based only on arrival count, the trace is still based
 * on the actual input order while naturally showing circular-queue reuse and
 * wrap-around when front slots become free.
 *
 * Current simulation configuration:
 *   normalCapacity = 7
 *   correctionCapacity = 11
 *   urgencyBonus = 2
 *   serviceSteps = 27
 *   traceLength = 12
 *
 */
public class GhanaServiceSimulator {

    static final int NORMAL_CAPACITY     =  7;
    static final int CORRECTION_CAPACITY = 11;
    static final int URGENCY_BONUS       =  2;
    static final int SERVICE_STEPS       = 27;
    static final int TRACE_LENGTH        = 12;

    private static int serviceStep = 0;
    private static int traceCount  = 0;
    private static boolean undoDone = false;
    private static final List<String> traceRows = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java GhanaServiceSimulator <path/to/requests.csv>");
            return;
        }

        GhanaServiceCentre centre = new GhanaServiceCentre(
            NORMAL_CAPACITY, CORRECTION_CAPACITY, URGENCY_BONUS
        );

        List<Request> all = loadRequests(args[0]);
        all.sort(Comparator.comparingInt(r -> r.arrivalOrder));

        System.out.println("============================================================");
        System.out.println("  GHANA IMMIGRATION SERVICE - PASSPORT OFFICE SIMULATOR");
        System.out.printf ("  Parameters: normalCap=%d  correctionCap=%d  urgencyBonus=%d%n",
            NORMAL_CAPACITY, CORRECTION_CAPACITY, URGENCY_BONUS);
        System.out.printf ("  Minimum service steps: %d   Trace length: %d%n",
            SERVICE_STEPS, TRACE_LENGTH);
        System.out.println("============================================================");
        System.out.println("Requests loaded: " + all.size());
        System.out.println("Processing policy: requests are admitted strictly by arrivalOrder.");
        System.out.println("Service windows: one service after every 3rd arrival and an extra service after every 4th arrival.");
        System.out.println();

        System.out.println("--- ARRIVAL-ORDER ADMISSION AND SERVICE RUN ---");
        int arrivalCount = 0;
        for (Request r : all) {
            arrivalCount++;
            centre.admitRequest(r);
            System.out.printf("Arrival %-2d: processed %-6s  [NQf=%d NQr=%d NQsz=%d DQsz=%d UQsz=%d STKsz=%d]%n",
                arrivalCount, r.requestId, centre.normalFront(), centre.normalRear(),
                centre.normalSize(), centre.correctionSize(), centre.urgentSize(), centre.stackSize());

            int serviceWindows = serviceWindowsForArrival(arrivalCount);
            for (int i = 0; i < serviceWindows; i++) {
                performServiceStep(centre);
            }
        }

        System.out.println();
        System.out.println("--- COMPLETING SERVICE FOR REMAINING APPLICANTS ---");
        while (performServiceStep(centre)) {
            // Continue until all queues are empty. SERVICE_STEPS is a minimum target,
            // not a cap; final report should reflect the completed run.
        }

        System.out.println();
        printTraceTable();

        System.out.println();
        centre.printReport();
    }

    private static int serviceWindowsForArrival(int arrivalCount) {
        int windows = 0;
        if (arrivalCount % 3 == 0) windows++;
        if (arrivalCount % 4 == 0) windows++;
        return windows;
    }

    private static boolean performServiceStep(GhanaServiceCentre centre) {
        serviceStep++;
        Request served = centre.serveNextRequest();
        if (served == null) {
            System.out.println("Service step " + serviceStep + ": All queues empty.");
            return false;
        }

        String actionType = centre.stackTop().split(":")[0];
        System.out.printf("Service step %-2d: served %-45s [NQf=%d NQr=%d NQsz=%d DQsz=%d UQsz=%d]%n",
            serviceStep, served, centre.normalFront(), centre.normalRear(),
            centre.normalSize(), centre.correctionSize(), centre.urgentSize());

        if (traceCount < TRACE_LENGTH) {
            traceCount++;
            traceRows.add(formatTraceRow(String.valueOf(traceCount), served.requestId,
                actionType, centre));
        }

        // Undo is demonstrated immediately after the fifth successful service action,
        // so it reverses a serve action rather than a later admission.
        if (serviceStep == 5 && !undoDone) {
            System.out.println();
            System.out.println("  *** UNDO DEMONSTRATION (operator error after service step " + serviceStep + ") ***");
            centre.undoLastAction();
            if (traceCount <= TRACE_LENGTH) {
                traceRows.add(formatTraceRow("-", served.requestId,
                    "UNDO: returned to queue", centre));
            }
            System.out.println();
            undoDone = true;
        }

        return true;
    }

    private static String formatTraceRow(String event, String requestId,
                                         String actionType, GhanaServiceCentre centre) {
        return String.format("%-4s %-8s %-24s %-5d %-5d %-5d %-5d %-5d %-5d",
            event, requestId, actionType, centre.normalFront(), centre.normalRear(),
            centre.normalSize(), centre.correctionSize(), centre.urgentSize(), centre.stackSize());
    }

    private static void printTraceTable() {
        System.out.println("--- TRACE TABLE (first " + TRACE_LENGTH + " service events from the actual run) ---");
        System.out.printf("%-4s %-8s %-24s %-5s %-5s %-5s %-5s %-5s %-5s%n",
            "Evt", "ReqID", "Action", "NQf", "NQr", "NQsz", "DQsz", "UQsz", "STKsz");
        System.out.println("-".repeat(82));
        for (String row : traceRows) System.out.println(row);
    }

    private static List<Request> loadRequests(String path) throws Exception {
        List<Request> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null)
                if (!line.trim().isEmpty()) list.add(Request.fromCsv(line));
        }
        return list;
    }
}
