import java.util.PriorityQueue;

/**
 * GhanaServiceCentre – core simulation logic for the Ghana Immigration Service
 * Passport Office queue management system.
 *
 * Three waiting structures:
 *   normalQueue     – CircularQueue   (capacity 7,  index rule: 5 + last digit 2)
 *   correctionDeque – CorrectionDeque (capacity 11, index rule: 4 + second-last 7)
 *   urgentQueue     – Java PriorityQueue with custom priority comparator
 *
 * Correction routing (fixed from v1):
 *   needsCorrection && urgencyLevel >= 4 → addFront (urgent correction jumps lane)
 *   needsCorrection && urgencyLevel <  4 → addRear  (routine correction waits)
 *
 * Admission undo (fixed from v1):
 *   ADMIT_NORMAL    → rebuild circular queue without that element (O(n), correct)
 *   ADMIT_CORRECTION → removeRear if it was a rear admission, addFront tracked
 *   ADMIT_URGENT    → remove from PriorityQueue directly (O(n) scan, correct)
 *
 */
public class GhanaServiceCentre {

    private final CircularQueue          normalQueue;
    private final CorrectionDeque        correctionDeque;
    private final PriorityQueue<Request> urgentQueue;
    private final ActionStack            actions;
    private final int                    urgencyBonus;

    // Counters for the final report
    private int overflowCount               = 0;
    private int servedCount                 = 0;
    private int urgentServed                = 0;
    private int normalServed                = 0;
    private int correctionServed            = 0;
    private int totalEstimatedMinutesServed = 0;

    public GhanaServiceCentre(int normalCapacity, int correctionCapacity, int urgencyBonus) {
        this.normalQueue     = new CircularQueue(normalCapacity);
        this.correctionDeque = new CorrectionDeque(correctionCapacity);
        this.urgencyBonus    = urgencyBonus;
        this.actions         = new ActionStack(300);

        // Higher computePriority score → served first (max-priority queue).
        this.urgentQueue = new PriorityQueue<>(
            (a, b) -> Integer.compare(computePriority(b), computePriority(a))
        );
    }

    // ---------------------------------------------------------------
// PRIORITY SCORE
    // ---------------------------------------------------------------
    /**
     * Computes a numeric priority score for urgent-queue ordering.
     *
     * Three urgency reasons at the passport office:
     *   TYPE A – Flight booked within 48 hours      (urgencyLevel = 4)
     *   TYPE B – Diplomatic / government travel     (urgencyLevel = 4)
     *   TYPE C – Medical emergency abroad           (urgencyLevel = 5)
     *
     * Formula used by the current configuration (urgencyBonus = 2):
     *   score = (urgencyLevel * 100)
     *           + urgencyBonus                  ← configurable bonus
     *           - (estimatedMinutes / 3)        ← shorter processing preferred
     *           - (arrivalOrder / 5)            ← earlier arrival earns slight edge
     *
     * urgencyLevel x 100 guarantees a medical emergency (U=5) always outranks
     * a flight-deadline case (U=4): worst-case Level 5 (5*100+2-10-7=485) still
     * exceeds best-case Level 4 (4*100+2-0-0=402), regardless of processing
     * time or arrival order.
     */
    public int computePriority(Request r) {
        return (r.urgencyLevel * 100)
               + urgencyBonus
               - (r.estimatedMinutes / 3)
               - (r.arrivalOrder / 5);
    }

    // ---------------------------------------------------------------
// ADMIT REQUEST
    // ---------------------------------------------------------------
    /**
     * Routes each arriving applicant into exactly one data structure.
     *
     * Routing rules (applied in this order):
     *   1a. needsCorrection && urgencyLevel >= 4 → correctionDeque.addFront()
     *       Urgent applicant with a birth-cert mismatch: they cannot be processed
     *       yet, but their time pressure means they jump to the front of the
     *       correction lane so they are resolved and served as fast as possible.
     *   1b. needsCorrection && urgencyLevel <  4 → correctionDeque.addRear()
     *       Routine correction: join the back of the correction lane.
     *   2.  urgencyLevel >= 4 (no correction needed) → urgentQueue.add()
     *       PriorityQueue is unbounded – never overflows.
     *   3.  otherwise → normalQueue.enqueue()
     *       Routine applicant: FIFO circular queue.
     *
     * Overflow: if a bounded structure is full, the request is not admitted and
     * overflowCount is incremented. In a real office the officer gives a return slip.
     */
    public void admitRequest(Request request) {
        if (request.needsCorrection) {
            if (correctionDeque.isFull()) {
                overflowCount++;
                System.out.println("[OVERFLOW] Correction lane full - "
                    + request.requestId + " cannot be admitted. Officer issues a return slip.");
                return;
            }
            if (request.urgencyLevel >= 4) {
                // Urgent correction: jump to the front of the correction lane
                correctionDeque.addFront(request);
                actions.push(new ActionRecord("ADMIT_CORRECTION_FRONT", request));
            } else {
                // Routine correction: join the rear of the correction lane
                correctionDeque.addRear(request);
                actions.push(new ActionRecord("ADMIT_CORRECTION_REAR", request));
            }

        } else if (request.urgencyLevel >= 4) {
            urgentQueue.add(request);
            actions.push(new ActionRecord("ADMIT_URGENT", request));

        } else {
            if (normalQueue.isFull()) {
                overflowCount++;
                System.out.println("[OVERFLOW] Normal queue full - "
                    + request.requestId + " cannot be admitted. Officer issues a return slip.");
                return;
            }
            normalQueue.enqueue(request);
            actions.push(new ActionRecord("ADMIT_NORMAL", request));
        }
    }

    // ---------------------------------------------------------------
// SERVE NEXT REQUEST
    // ---------------------------------------------------------------
    /**
     * Serves the highest-priority waiting applicant.
     * Service order: urgent → correction → normal.
     */
    public Request serveNextRequest() {
        Request served;
        String  actionType;

        if (!urgentQueue.isEmpty()) {
            served     = urgentQueue.poll();
            actionType = "SERVE_URGENT";
            urgentServed++;

        } else if (!correctionDeque.isEmpty()) {
            served     = correctionDeque.removeFront();
            actionType = "SERVE_CORRECTION";
            correctionServed++;

        } else if (!normalQueue.isEmpty()) {
            served     = normalQueue.dequeue();
            actionType = "SERVE_NORMAL";
            normalServed++;

        } else {
            return null;
        }

        servedCount++;
        totalEstimatedMinutesServed += served.estimatedMinutes;
        actions.push(new ActionRecord(actionType, served));
        return served;
    }

    // ---------------------------------------------------------------
// UNDO LAST ACTION (fully implemented)
    // ---------------------------------------------------------------
    /**
     * Reverses the most recent operator action by popping the ActionStack.
     *
     * All six action types are now fully handled:
     *
     *   SERVE_URGENT        → re-insert into urgentQueue              O(log n)
     *   SERVE_CORRECTION    → re-insert at front of correctionDeque   O(1)
     *   SERVE_NORMAL        → re-insert at front of normalQueue
     *                         (rebuilt by removing all, prepending, re-adding) O(n)
     *   ADMIT_URGENT        → remove from PriorityQueue by value      O(n) scan
     *   ADMIT_CORRECTION_REAR  → removeRear from correctionDeque      O(1)
     *   ADMIT_CORRECTION_FRONT → removeFront from correctionDeque     O(1)
     *   ADMIT_NORMAL        → rebuild circular queue without element   O(n)
     */
    public void undoLastAction() {
        ActionRecord last = actions.pop();
        if (last == null) {
            System.out.println("[UNDO] Nothing on the action stack to undo.");
            return;
        }
        System.out.println("[UNDO] Reversing: " + last);

        switch (last.actionType) {

            case "SERVE_URGENT":
                urgentQueue.add(last.request);
                servedCount--;  urgentServed--;
                totalEstimatedMinutesServed -= last.request.estimatedMinutes;
                System.out.println("[UNDO] " + last.request.requestId
                    + " returned to urgent priority queue.");
                break;

            case "SERVE_CORRECTION":
                correctionDeque.addFront(last.request);
                servedCount--;  correctionServed--;
                totalEstimatedMinutesServed -= last.request.estimatedMinutes;
                System.out.println("[UNDO] " + last.request.requestId
                    + " returned to front of correction lane.");
                break;

            case "SERVE_NORMAL":
                // Rebuild the circular queue with the returned request at the front.
                // Drain existing items, re-enqueue the undone item first, then the rest.
                servedCount--;  normalServed--;
                totalEstimatedMinutesServed -= last.request.estimatedMinutes;
                int currentSize = normalQueue.size();
                Request[] temp = new Request[currentSize];
                for (int i = 0; i < currentSize; i++) temp[i] = normalQueue.dequeue();
                normalQueue.enqueue(last.request);
                for (Request r : temp) normalQueue.enqueue(r);
                System.out.println("[UNDO] " + last.request.requestId
                    + " restored to front of normal queue.");
                break;

            case "ADMIT_URGENT":
                // Remove from PriorityQueue by value (O(n) scan – acceptable for undo).
                urgentQueue.remove(last.request);
                System.out.println("[UNDO] " + last.request.requestId
                    + " removed from urgent queue.");
                break;

            case "ADMIT_CORRECTION_REAR":
                // The most recently rear-admitted item is at the rear.
                correctionDeque.removeRear();
                System.out.println("[UNDO] " + last.request.requestId
                    + " removed from rear of correction lane.");
                break;

            case "ADMIT_CORRECTION_FRONT":
                // The most recently front-admitted item is at the front.
                correctionDeque.removeFront();
                System.out.println("[UNDO] " + last.request.requestId
                    + " removed from front of correction lane.");
                break;

            case "ADMIT_NORMAL":
                // Rebuild circular queue without this request (O(n)).
                int sz = normalQueue.size();
                Request[] buf = new Request[sz];
                for (int i = 0; i < sz; i++) buf[i] = normalQueue.dequeue();
                for (Request r : buf) {
                    if (!r.requestId.equals(last.request.requestId))
                        normalQueue.enqueue(r);
                }
                System.out.println("[UNDO] " + last.request.requestId
                    + " removed from normal queue.");
                break;

            default:
                System.out.println("[UNDO] Unknown action type: " + last.actionType);
        }
    }

    public void printReport() {
        System.out.println("\n========================================");
        System.out.println("   GHANA IMMIGRATION SERVICE - REPORT   ");
        System.out.println("   Passport Office, Accra (Simulated)   ");
        System.out.println("========================================");
        System.out.println("Served total          : " + servedCount);
        System.out.println("  Urgent served       : " + urgentServed);
        System.out.println("  Correction served   : " + correctionServed);
        System.out.println("  Normal served       : " + normalServed);
        System.out.println("Overflow (return slip): " + overflowCount);
        System.out.println("Remaining urgent      : " + urgentQueue.size());
        System.out.println("Remaining correction  : " + correctionDeque.size());
        System.out.println("Remaining normal      : " + normalQueue.size());
        double avg = servedCount == 0 ? 0.0
                     : (double) totalEstimatedMinutesServed / servedCount;
        System.out.printf("Avg service time      : %.2f min%n", avg);
        System.out.println("========================================");
    }

    // Accessors for trace table
    public int    normalFront()    { return normalQueue.frontIndex(); }
    public int    normalRear()     { return normalQueue.rearIndex();  }
    public int    normalSize()     { return normalQueue.size();       }
    public int    correctionSize() { return correctionDeque.size();   }
    public int    urgentSize()     { return urgentQueue.size();       }
    public int    stackSize()      { return actions.size();           }
    public String stackTop()       {
        ActionRecord r = actions.peek();
        return r == null ? "—" : r.actionType + ":" + r.request.requestId;
    }
}
