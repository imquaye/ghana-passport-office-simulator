# Ghana Immigration Service Passport Office Simulator

A Java console application that simulates applicant flow at a Ghana Immigration Service passport office using custom data structures.

The project models normal applications, urgent cases and applications that require document corrections. It demonstrates how queues, deques, priority queues and stacks can work together in a realistic service-centre scenario.

## Features

- Processes normal applicants in FIFO arrival order while prioritising urgent and correction cases according to defined service rules
- Uses a circular queue for normal applicants
- Uses a double-ended queue for document-correction cases
- Uses a priority queue for urgent and emergency applications
- Uses a stack to support undo operations
- Loads 38 sample requests from a CSV file
- Detects queue overflow and records applicants who must return later
- Produces a trace table and final service report
- Simulates passport collections, renewals, new applications, diplomatic travel and emergency travel

## Data Structures

| Structure | Purpose |
|---|---|
| `CircularQueue` | Handles normal applicants in FIFO order while reusing array space |
| `CorrectionDeque` | Handles correction cases from either end based on urgency |
| `PriorityQueue<Request>` | Serves the most urgent eligible applicants first |
| `ActionStack` | Records recent actions for undo functionality |

## Service Rules

1. Applicants requiring corrections enter the correction deque.
2. Urgent correction cases are inserted at the front.
3. Routine correction cases are inserted at the rear.
4. Urgent applicants without corrections enter the priority queue.
5. Other applicants enter the normal circular queue.
6. Service order is urgent queue, correction deque, then normal queue.

## Priority Calculation

Urgent applicants are ordered using:

```text
priority = urgencyLevel × 100
         + urgencyBonus
         - estimatedMinutes ÷ 3
         - arrivalOrder ÷ 5
```

This ensures that medical emergencies outrank lower-urgency cases while still considering processing time and arrival order.

## Project Structure

```text
ghana-passport-office-simulator/
├── data/
│   └── requests.csv
├── docs/
│   └── sample-output.txt
├── src/
│   ├── ActionRecord.java
│   ├── ActionStack.java
│   ├── CircularQueue.java
│   ├── CorrectionDeque.java
│   ├── GhanaServiceCentre.java
│   ├── GhanaServiceSimulator.java
│   └── Request.java
├── .gitignore
└── README.md
```

## Requirements

- Java 17 or later
- No external libraries are required

## Compile and Run

From the repository root:

```bash
mkdir -p out
javac -d out src/*.java
java -cp out GhanaServiceSimulator data/requests.csv
```

On Windows Command Prompt:

```bat
mkdir out
javac -d out src\*.java
java -cp out GhanaServiceSimulator data\requests.csv
```

## Example Results

Using the included dataset, the simulator:

- Loads 38 passport service requests
- Completes 27 successful services
- Demonstrates an undo operation after the fifth service event
- Reports 11 overflow cases
- Finishes with all admitted queues empty
- Produces an average estimated service time of 19.22 minutes

A longer run excerpt is available in [`docs/sample-output.txt`](docs/sample-output.txt).

## Concepts Demonstrated

- FIFO queue processing
- Circular-array wrap-around
- Double-ended queue operations
- Custom priority comparison
- Stack-based undo
- CSV parsing
- Object-oriented design
- Time-complexity trade-offs

## Possible Improvements

- Add automated unit tests
- Add a graphical or web-based interface
- Store service results in a database
- Make queue capacities configurable from the command line
- Add separate service counters and simulated waiting times
- Export reports as CSV or JSON

## Author

**Isaac Morrison Quaye**  
Computer Science student at the University of Ghana
