package com.ads.apiseng.handlers;

import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;

public class RetryRecoveryCallback implements RecoveryCallback<Object> {
    
    @Override
    public Object recover(RetryContext context) throws Exception {
        System.out.println("[RETRY-RECOVERY] Max retries exceeded for: " + context.getAttribute("message"));
        // Send to dead letter queue or handle permanent failure
        return "RECOVERY_HANDLED";
    }
}