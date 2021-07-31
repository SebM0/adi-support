package com.axway.adi.tools.operatiions;

import java.util.*;
import java.util.concurrent.*;
import javafx.concurrent.Task;

public class OperationExecutor extends Task<Void> {
    private final List<CallbackOperation> pendingOperations = new LinkedList<>();
    private ExecutorService executorService = null;
    private int currentOperation = 0;
    private int totalOperations = 0;

    public void kill() {
        if (executorService != null)
            executorService.shutdownNow();
    }

    public void addOperation(Operation operation) {
        CallbackOperation op = new CallbackOperation(operation);
        if (executorService == null) {
            pendingOperations.add(op);
        } else {
            executorService.submit(op);
        }
        totalOperations++;
        updateProgress();
    }

    private void updateProgress() {
        this.updateProgress(currentOperation, totalOperations);
    }

    @Override
    protected Void call() throws Exception {
        executorService = Executors.newSingleThreadExecutor();
        while (!pendingOperations.isEmpty()) {
            executorService.submit(pendingOperations.remove(0));
        }
        return null;
    }

    private class CallbackOperation implements Runnable {
        private final Operation operation;
        private CallbackOperation(Operation operation) {
            this.operation = operation;
        }

        @Override
        public void run() {
            // starting operation
            OperationExecutor.this.updateMessage(operation.getFullName());

            // running operation
            operation.run();

            // operation completed
            currentOperation++;
            updateProgress();
        }
    }
}
