/**
 * Represents a single passport service request at the Ghana Immigration
 * Service Passport Office, Accra.
 *
 * urgencyLevel scale used in this simulator:
 *   1 = Routine application (no deadline pressure)
 *   2 = Renewal with loose deadline (travel in 2+ weeks)
 *   3 = Moderate urgency (travel in under a week)
 *   4 = Flight booked within 48 hours OR diplomatic/government travel
 *   5 = Medical emergency requiring immediate international travel
 *
 * needsCorrection = true means the applicant's birth certificate name or date
 * does not match the passport application form. They must step aside, have the
 * discrepancy resolved at the records desk, then rejoin the queue.
 *
 */
public class Request {
    public final String requestId;
    public final int arrivalOrder;
    public final String location;
    public final String serviceType;
    public final int urgencyLevel;
    public final int estimatedMinutes;
    public final boolean needsCorrection;
    public final String notes;

    public Request(String requestId, int arrivalOrder, String location,
                   String serviceType, int urgencyLevel, int estimatedMinutes,
                   boolean needsCorrection, String notes) {
        this.requestId        = requestId;
        this.arrivalOrder     = arrivalOrder;
        this.location         = location;
        this.serviceType      = serviceType;
        this.urgencyLevel     = urgencyLevel;
        this.estimatedMinutes = estimatedMinutes;
        this.needsCorrection  = needsCorrection;
        this.notes            = notes;
    }

    public static Request fromCsv(String line) {
        String[] p = line.split(",", -1);
        return new Request(
            p[0].trim(),
            Integer.parseInt(p[1].trim()),
            p[2].trim(),
            p[3].trim(),
            Integer.parseInt(p[4].trim()),
            Integer.parseInt(p[5].trim()),
            Boolean.parseBoolean(p[6].trim()),
            p.length > 7 ? p[7].trim() : ""
        );
    }

    @Override
    public String toString() {
        return requestId + "[" + serviceType + ", U=" + urgencyLevel
               + ", T=" + estimatedMinutes + "min]";
    }
}
