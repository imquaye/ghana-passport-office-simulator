/**
 * ActionStack – array-based LIFO stack recording every admit and serve action
 * performed by the passport office operator.
 *
 * Why a stack for undo?
 *   Undo always reverses the MOST RECENT action first. That is exactly what
 *   a stack provides: the last item pushed is the first item popped (LIFO).
 *   All core operations (push, pop, peek) are O(1).
 *
 * Capacity is set generously (300) to accommodate at least 35 requests × 2
 * actions each plus multiple undo calls without overflow.
 *
 */
public class ActionStack {
    private ActionRecord[] data;
    private int top;        // -1 means empty

    public ActionStack(int capacity) {
        data = new ActionRecord[capacity];
        top  = -1;
    }
// stack is empty when nothing has been pushed yet.
    public boolean isEmpty() {
        return top == -1;
    }
// push action; return false if the stack has no more room.
    public boolean push(ActionRecord action) {
        if (top == data.length - 1) return false;   // full
        data[++top] = action;
        return true;
    }
// remove and return the most recent action.
    public ActionRecord pop() {
        if (isEmpty()) return null;
        ActionRecord item = data[top];
        data[top--] = null;     // release reference
        return item;
    }
// inspect the top without removing it.
    public ActionRecord peek() {
        return isEmpty() ? null : data[top];
    }

    public int size() { return top + 1; }
}
