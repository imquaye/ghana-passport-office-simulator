/**
 * CircularQueue – fixed-capacity FIFO queue modelling the normal waiting
 * line at the Ghana Immigration Service Passport Office.
 *
 * Why circular?
 *   When an applicant at the front is served (dequeued), a naive array would
 *   require every remaining applicant to shift one position forward: O(n).
 *   Instead, we keep a 'front' index and advance it with modulo arithmetic so
 *   the slot is simply marked free and reused when rear wraps around. Both
 *   enqueue and dequeue are O(1) with no element movement.
 *
 * The default simulator configuration uses a capacity of 7.
 *
 * Internal convention:
 *   rear  starts at -1 and moves to (rear+1) % capacity before each insert.
 *   front starts at  0 and moves to (front+1) % capacity after each remove.
 *   size  is maintained explicitly to distinguish full from empty when
 *         front == rear + 1 (mod capacity) would be ambiguous.
 *
 */
public class CircularQueue {
    private Request[] data;
    private int front;
    private int rear;
    private int size;

    public CircularQueue(int capacity) {
        data  = new Request[capacity];
        front = 0;
        rear  = -1;
        size  = 0;
    }
// full when size fills the entire array.
    public boolean isFull() {
        return size == data.length;
    }
// empty when no applicant is waiting.
    public boolean isEmpty() {
        return size == 0;
    }
// 5: add applicant at rear using modulo wrap-around.
    public boolean enqueue(Request request) {
// reject if no seats remain.
        if (isFull()) return false;
// advance rear circularly, then store.
        rear = (rear + 1) % data.length;
        data[rear] = request;
// keep size accurate, signal success.
        size++;
        return true;
    }
// 7: remove and return the front applicant.
    public Request dequeue() {
// nothing to serve if empty.
        if (isEmpty()) return null;
// save item, free slot, advance front circularly, shrink size.
        Request item = data[front];
        data[front]  = null;                    // release reference; aids debugging
        front        = (front + 1) % data.length;
        size--;
        return item;
    }
// inspect without removing.
    public Request peek() {
        return isEmpty() ? null : data[front];
    }

    public int size()       { return size;  }
    public int frontIndex() { return front; }
    public int rearIndex()  { return rear;  }

    @Override
    public String toString() {
        if (isEmpty()) return "CircularQueue[] front=" + front + " rear=" + rear + " size=0";
        StringBuilder sb = new StringBuilder("CircularQueue[");
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
