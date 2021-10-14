package com.axway.adi.tools.disturb.operatiions;

import com.axway.adi.tools.disturb.db.DiagnosticResult;

public interface OperationDriver {
    void addOperation(Operation operation);

    void addResult(DiagnosticResult result);
}
