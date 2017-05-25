package com.ly.fn.inf.lnk.core;

import com.ly.fn.inf.lnk.api.InvokerCommand;
import com.ly.fn.inf.lnk.api.exception.InvokerException;
import com.ly.fn.inf.lnk.api.exception.InvokerTimeoutException;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午12:55:54
 */
public interface Invoker {
    InvokerCommand invokeSync(final InvokerCommand command, final long timeoutMillis) throws InvokerException, InvokerTimeoutException;
    void invokeAsync(final InvokerCommand command, final long timeoutMillis, final InvokerCallback callback) throws InvokerException, InvokerTimeoutException;
    void invokeOneway(final InvokerCommand command) throws InvokerException, InvokerTimeoutException;
    void start();
    void shutdown();
}
