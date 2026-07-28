/**
 * Records one operator action (admit or serve) so the ActionStack
 * can reverse it with undoLastAction().
 *
 * actionType values used in this simulator:
 *   ADMIT_NORMAL      – request entered the normal circular queue
 *   ADMIT_URGENT      – request entered the priority queue
 *   ADMIT_CORRECTION_FRONT – urgent correction request entered the correction deque at the front
 *   ADMIT_CORRECTION_REAR  – routine correction request entered the correction deque at the rear
 *   SERVE_NORMAL      – request served from the circular queue
 *   SERVE_URGENT      – request served from the priority queue
 *   SERVE_CORRECTION  – request served from the correction deque
 *
 */
public class ActionRecord {
    public final String actionType;
    public final Request request;

    public ActionRecord(String actionType, Request request) {
        this.actionType = actionType;
        this.request    = request;
    }

    @Override
    public String toString() {
        return actionType + " -> " + request;
    }
}
