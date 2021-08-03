package com.axway.adi.tools.operatiions;

import com.axway.adi.tools.util.db.DiagnosticResult;

public interface OperationDriver {
    void addOperation(Operation operation);

    void addResult(DiagnosticResult result);
}
