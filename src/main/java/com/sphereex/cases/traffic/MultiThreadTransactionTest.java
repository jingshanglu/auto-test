package com.sphereex.cases.traffic;

import com.sphereex.core.AutoTest;
import com.sphereex.core.CaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@AutoTest
public class MultiThreadTransactionTest extends TrafficBaseTest {
    private final Logger logger = LoggerFactory.getLogger(MultiThreadTransactionTest.class);
    
    private final List<Connection> connections = new LinkedList<>();
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @Override
    public boolean run() throws Exception {
        for (int i = 0; i < 10; i++) {
            Connection connection = getDataSource().getConnection();
            connection.setAutoCommit(false);
            connections.add(connection);
        }
        List<Future<?>> futures = new LinkedList<>();
        Iterator<Connection> iterator = connections.iterator();
        for (int i = 0; i < 10; i++) {
            futures.add(executorService.submit(() -> execute(iterator)));
        }
        for (Future<?> each : futures) {
            try {
                each.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("execute error", e);
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void end() throws Exception {
        for (Connection each : connections) {
            each.close();
        }
        executorService.shutdown();
    }
    
    @Override
    public void initCaseInfo() {
        String name = "MultiThreadTransactionTest";
        String feature = "traffic-transaction";
        String tag = "conf/Traffic";
        String message = "Connection in transaction should be traffic to proxy.";
        CaseInfo caseInfo = new CaseInfo(name, feature, tag, message);
        setCaseInfo(caseInfo);
    }
    
    private void execute(final Iterator<Connection> iterator) {
        Connection connection = iterator.next();
        try {
            connection.createStatement().execute("select 1");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
