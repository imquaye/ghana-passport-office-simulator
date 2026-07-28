/**
 * CorrectionDeque – fixed-capacity circular-array double-ended queue for
 * passport applicants whose birth certificate does not match their application.
 *
 * Passport-office context:
 *   When a data entry officer spots that an applicant's birth certificate name
 *   or date of birth differs from what was submitted on their form, the
 *   applicant is directed to the correction desk. After the discrepancy is
 *   resolved they rejoin:
 *     addRear  – routine correction (urgencyLevel < 4); applicant rejoins at
 *                the back of the correction lane (fair FIFO within the group).
 *     addFront – urgent correction (urgencyLevel >= 4, e.g. flight in 48 hrs
 *                with a birth cert mismatch); they jump to the front of the
 *                correction lane so they are served next from that lane.
 *
 * Both front and rear pointers wrap using modulo arithmetic, giving O(1) for
 * all four deque operations.
 *
 * Bug fix: addFront on an empty deque must set rear = front so that
 * removeRear() can locate the single element correctly.
 *
 * The default simulator configuration uses a capacity of 11.
 *
 */
public class CorrectionDeque {
    private Request[] data;
    private int front;
    private int rear;
    private int size;

    public CorrectionDeque(int capacity) {
        data  = new Request[capacity];
        front = 0;
        rear  = -1;
        size  = 0;
    }
// full when every slot is occupied.
    public boolean isFull() {
        return size == data.length;
    }
// empty when no applicant is in the correction lane.
    public boolean isEmpty() {
        return size == 0;
    }
// routine correction – join at the rear.
    public boolean addRear(Request request) {
        if (isFull()) return false;
        rear       = (rear + 1) % data.length;
        data[rear] = request;
        size++;
        return true;
    }
// urgent correction – jump to the front.
    // Step front one position backward (circularly) before inserting.
    // Bug fix: when deque is empty, rear must be synchronised to front
    // so removeRear() can find the element. Without this, rear stays -1
    // and removeRear() crashes with ArrayIndexOutOfBoundsException.
    public boolean addFront(Request request) {
        if (isFull()) return false;
        front       = (front - 1 + data.length) % data.length;
        data[front] = request;
        size++;
        if (size == 1) rear = front;   // FIX: sync rear when deque was empty
        return true;
    }
// serve the applicant who has been waiting longest in this lane.
    public Request removeFront() {
        if (isEmpty()) return null;
        Request item  = data[front];
        data[front]   = null;
        front         = (front + 1) % data.length;
        size--;
        return item;
    }
// remove the most recently added rear applicant (used by undo).
    public Request removeRear() {
        if (isEmpty()) return null;
        Request item = data[rear];
        data[rear]   = null;
        rear         = (rear - 1 + data.length) % data.length;
        size--;
        return item;
    }

    public int size()       { return size;  }
    public int frontIndex() { return front; }
    public int rearIndex()  { return rear;  }

    @Override
    public String toString() {
        if (isEmpty()) return "CorrectionDeque[] front=" + front + " rear=" + rear + " size=0";
        StringBuilder sb = new StringBuilder("CorrectionDeque[");
        for (int i = 0; i < size; i++) {
            int idx = (front + i) % data.length;
            sb.append(data[idx].requestId);
            if (i < size - 1) sb.append(", ");
        }
        return sb.append("] front=").append(front)
                 .append(" rear=").append(rear)
                 .append(" size=").append(size).toString();
    }
}
