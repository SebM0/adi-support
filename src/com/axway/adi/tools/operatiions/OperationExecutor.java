package com.axway.adi.tools.operatiions;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.axway.adi.tools.util.db.DiagnosticResult;
import javafx.concurrent.Task;

public class OperationExecutor extends Task<Void> implements OperationDriver {
    private final List<CallbackOperation> pendingOperations = new LinkedList<>();
    private final Consumer<DiagnosticResult> resultConsumer;
    private ExecutorService executorService = null;
    private Thread thread = null;
    private int currentOperation = 0;
    private int totalOperations = 0;

    public OperationExecutor(Consumer<DiagnosticResult> resultConsumer) {
        this.resultConsumer = resultConsumer;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void kill() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (thread != null) {
            try {
                thread.join(10_000);
            } catch (InterruptedException e) {
                // skip
            }
            thread = null;
        }
    }

    @Override
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

    @Override
    public void addResult(DiagnosticResult result) {
        resultConsumer.accept(result);
    }

    private void updateProgress() {
        this.updateProgress(currentOperation, totalOperations);
        if (currentOperation >= totalOperations) {
            // completed
            resultConsumer.accept(null);
        }
    }

    @Override
    protected Void call() {
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
            operation.setDriver(OperationExecutor.this);
        }

        @Override
        public void run() {
            // starting operation
            OperationExecutor.this.updateMessage(operation.getFullName());

            // running operation
            System.out.println("Running " + operation.getFullName());
            try {
                operation.run();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            // operation completed
            currentOperation++;
            updateProgress();
        }
    }
}
