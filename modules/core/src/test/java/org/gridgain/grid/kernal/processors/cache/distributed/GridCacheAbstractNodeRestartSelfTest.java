/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheConfiguration.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;

/**
 * Test node restart.
 */
@SuppressWarnings({"PointlessArithmeticExpression"})
public abstract class GridCacheAbstractNodeRestartSelfTest extends GridCommonAbstractTest {
    /** Cache name. */
    protected static final String CACHE_NAME = "TEST_CACHE";

    /** */
    private static final long TEST_TIMEOUT = 5 * 60 * 1000;

    /** Default backups. */
    private static final int DFLT_BACKUPS = 1;

    /** Partitions. */
    private static final int DFLT_PARTITIONS = 521;

    /** Preload batch size. */
    private static final int DFLT_BATCH_SIZE = DFLT_PRELOAD_BATCH_SIZE;

    /** Number of key backups. Each test method can set this value as required. */
    protected int backups = DFLT_BACKUPS;

    /** */
    private static final int DFLT_NODE_CNT = 4;

    /** */
    private static final int DFLT_KEY_CNT = 100;

    /** */
    private static final int DFLT_RETRIES = 10;

    /** */
    private static final Random RAND = new Random();

    /** */
    private static volatile int idx = -1;

    /** Preload mode. */
    protected GridCachePreloadMode preloadMode = ASYNC;

    /** */
    protected int preloadBatchSize = DFLT_BATCH_SIZE;

    /** Number of partitions. */
    protected int partitions = DFLT_PARTITIONS;

    /** Node count. */
    protected int nodeCnt = DFLT_NODE_CNT;

    /** Key count. */
    protected int keyCnt = DFLT_KEY_CNT;

    /** Retries. */
    private int retries = DFLT_RETRIES;

    /** */
    private static final GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration c = super.getConfiguration(gridName);

        // Discovery.
        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        c.setDiscoverySpi(disco);

        return c;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        backups = DFLT_BACKUPS;
        partitions = DFLT_PARTITIONS;
        preloadMode = ASYNC;
        preloadBatchSize = DFLT_BATCH_SIZE;
        nodeCnt = DFLT_NODE_CNT;
        keyCnt = DFLT_KEY_CNT;
        retries = DFLT_RETRIES;
        idx = -1;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return TEST_TIMEOUT;
    }

    /**
     * @throws Exception If failed.
     */
    private void startGrids() throws  Exception {
        for (int i = 0; i < nodeCnt; i++) {
            startGrid(i);

            if (idx < 0)
                idx = i;
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestart() throws Exception {
        preloadMode = SYNC;
        partitions = 3;
        nodeCnt = 2;
        keyCnt = 10;
        retries = 3;

        info("*** STARTING TEST ***");

        startGrids();

        try {
            GridCache<Integer, String> c = grid(idx).cache(CACHE_NAME);

            for (int j = 0; j < retries; j++) {
                for (int i = 0; i < keyCnt; i++)
                    c.putx(i, Integer.toString(i));

                info("Stored items.");

                checkGet(c, j);

                info("Stopping node: " + idx);

                stopGrid(idx);

                info("Starting node: " + idx);

                Grid grid = startGrid(idx);

                c = grid.cache(CACHE_NAME);

                checkGet(c, j);
            }
        }
        finally {
            stopAllGrids(true);
        }
    }

    /**
     * @param c Cache.
     * @param attempt Attempt.
     * @throws Exception If failed.
     */
    private void checkGet(GridCache<Integer, String> c, int attempt) throws Exception {
        for (int i = 0; i < keyCnt; i++) {
            String v = c.get(i);

            if (v == null) {
                printFailureDetails(c, i, attempt);

                fail("Value is null [key=" + i + ", attempt=" + attempt + "]");
            }

            if (!Integer.toString(i).equals(v)) {
                printFailureDetails(c, i, attempt);

                fail("Wrong value for key [key=" +
                    i + ", actual value=" + v + ", expected value=" + Integer.toString(i) + "]");
            }
        }

        info("Read items.");
    }

    /**
     * @return Transaction concurrency to use in tests.
     */
    protected GridCacheTxConcurrency txConcurrency() {
        return PESSIMISTIC;
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutTwoNodesNoBackups() throws Throwable {
        backups = 0;
        nodeCnt = 2;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 30000;

        checkRestartWithPut(duration, 1, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxTwoNodesNoBackups() throws Throwable {
        backups = 0;
        nodeCnt = 2;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 30000;

        checkRestartWithTx(duration, 1, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutTwoNodesOneBackup() throws Throwable {
        backups = 1;
        nodeCnt = 2;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 30000;

        checkRestartWithPut(duration, 1, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxTwoNodesOneBackup() throws Throwable {
        backups = 1;
        nodeCnt = 2;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 30000;

        checkRestartWithTx(duration, 1, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutFourNodesNoBackups() throws Throwable {
        backups = 0;
        nodeCnt = 4;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 60000;

        checkRestartWithPut(duration, 2, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxFourNodesNoBackups() throws Throwable {
        backups = 0;
        nodeCnt = 4;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 60000;

        checkRestartWithTx(duration, 2, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutFourNodesOneBackups() throws Throwable {
        backups = 1;
        nodeCnt = 4;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 60000;

        checkRestartWithPut(duration, 2, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxFourNodesOneBackups() throws Throwable {
        backups = 1;
        nodeCnt = 4;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 60000;

        checkRestartWithTx(duration, 2, 2);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutSixNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 6;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithPut(duration, 3, 3);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxSixNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 6;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithTx(duration, 3, 3);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutEightNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 8;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithPut(duration, 4, 4);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxEightNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 8;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithTx(duration, 4, 4);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithPutTenNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 10;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithPut(duration, 5, 5);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxTenNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 10;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithTx(duration, 5, 5);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxPutAllTenNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 10;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithTxPutAll(duration, 5, 5);
    }

    /**
     * @throws Exception If failed.
     */
    public void testRestartWithTxPutAllFourNodesTwoBackups() throws Throwable {
        backups = 2;
        nodeCnt = 4;
        keyCnt = 10;
        partitions = 29;
        preloadMode = ASYNC;

        long duration = 90000;

        checkRestartWithTxPutAll(duration, 2, 2);
    }

    /**
     * @param duration Test duration.
     * @param putThreads Put threads count.
     * @param restartThreads Restart threads count.
     * @throws Exception If failed.
     */
    public void checkRestartWithPut(long duration, int putThreads, int restartThreads) throws Throwable {
        final long endTime = System.currentTimeMillis() + duration;

        final AtomicReference<Throwable> err = new AtomicReference<>();

        startGrids();

        Collection<Thread> threads = new LinkedList<>();

        try {
            final int logFreq = 20;

            final AtomicInteger putCntr = new AtomicInteger();

            final CyclicBarrier barrier = new CyclicBarrier(putThreads + restartThreads);

            for (int i = 0; i < putThreads; i++) {
                final int gridIdx = i;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting put thread...");

                            GridCache<Integer, String> cache = grid(gridIdx).cache(CACHE_NAME);

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                int key = RAND.nextInt(keyCnt);

                                try {
                                    cache.put(key, Integer.toString(key));
                                }
                                catch (GridCacheTxRollbackException | GridTopologyException ignored) {
                                    // It is ok if primary node leaves grid.
                                }

                                int c = putCntr.incrementAndGet();

                                if (c % logFreq == 0)
                                    info(">>> Put iteration [cnt=" + c + ", key=" + key + ']');
                            }
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to put value in cache.", e);
                        }
                    }
                }, "put-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (int i = 0; i < restartThreads; i++) {
                final int gridIdx = i + putThreads;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting restart thread...");

                            int cnt = 0;

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                log.info(">>>>>>> Stopping grid " + gridIdx);

                                stopGrid(gridIdx);

                                log.info(">>>>>>> Starting grid " + gridIdx);

                                startGrid(gridIdx);

                                int c = ++cnt;

                                if (c % logFreq == 0)
                                    info(">>> Restart iteration: " + c);
                            }
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to restart grid node.", e);
                        }
                    }
                }, "restart-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (Thread t : threads)
                t.join();

            if (err.get() != null)
                throw err.get();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param duration Test duration.
     * @param putThreads Put threads count.
     * @param restartThreads Restart threads count.
     * @throws Exception If failed.
     */
    public void checkRestartWithTx(long duration, int putThreads, int restartThreads) throws Throwable {
        final long endTime = System.currentTimeMillis() + duration;

        final AtomicReference<Throwable> err = new AtomicReference<>();

        startGrids();

        Collection<Thread> threads = new LinkedList<>();

        try {
            final int logFreq = 20;

            final AtomicInteger txCntr = new AtomicInteger();

            final CyclicBarrier barrier = new CyclicBarrier(putThreads + restartThreads);

            final int txKeys = 3;

            for (int i = 0; i < putThreads; i++) {
                final int gridIdx = i;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting put thread...");

                            Grid grid = grid(gridIdx);

                            UUID locNodeId = grid.localNode().id();

                            GridCache<Integer, String> cache = grid.cache(CACHE_NAME);

                            List<Integer> keys = new ArrayList<>(txKeys);

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                keys.clear();

                                for (int i = 0; i < txKeys; i++)
                                    keys.add(RAND.nextInt(keyCnt));

                                // Ensure lock order.
                                Collections.sort(keys);

                                int c = 0;

                                try {
                                    try (GridCacheTx tx = cache.txStart(txConcurrency(), REPEATABLE_READ)) {
                                        c = txCntr.incrementAndGet();

                                        if (c % logFreq == 0)
                                            info(">>> Tx iteration started [cnt=" + c + ", keys=" + keys + ", " +
                                                "locNodeId=" + locNodeId + ']');

                                        for (int key : keys) {
                                            int op = cacheOp();

                                            if (op == 1)
                                                cache.put(key, Integer.toString(key));
                                            else if (op == 2)
                                                cache.remove(key);
                                            else
                                                cache.get(key);
                                        }

                                        tx.commit();
                                    }
                                    catch (GridTopologyException ignored) {
                                        // It is ok if primary node leaves grid.
                                    }
                                }
                                catch (GridTopologyException ignored) {
                                    // It is ok if primary node leaves grid.
                                }

                                if (c % logFreq == 0)
                                    info(">>> Tx iteration finished [cnt=" + c + ", keys=" + keys + ", " +
                                        "locNodeId=" + locNodeId + ']');
                            }

                            info(">>> " + Thread.currentThread().getName() + " finished.");
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to put value in cache.", e);
                        }
                    }
                }, "put-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (int i = 0; i < restartThreads; i++) {
                final int gridIdx = i + putThreads;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting restart thread...");

                            int cnt = 0;

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                stopGrid(gridIdx);
                                startGrid(gridIdx);

                                int c = ++cnt;

                                if (c % logFreq == 0)
                                    info(">>> Restart iteration: " + c);
                            }

                            info(">>> " + Thread.currentThread().getName() + " finished.");
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to restart grid node.", e);
                        }
                    }
                }, "restart-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (Thread t : threads)
                t.join();

            if (err.get() != null)
                throw err.get();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @param duration Test duration.
     * @param putThreads Put threads count.
     * @param restartThreads Restart threads count.
     * @throws Exception If failed.
     */
    public void checkRestartWithTxPutAll(long duration, int putThreads, int restartThreads) throws Throwable {
        final long endTime = System.currentTimeMillis() + duration;

        final AtomicReference<Throwable> err = new AtomicReference<>();

        startGrids();

        Collection<Thread> threads = new LinkedList<>();

        try {
            final int logFreq = 20;

            final AtomicInteger txCntr = new AtomicInteger();

            final CyclicBarrier barrier = new CyclicBarrier(putThreads + restartThreads);

            final int txKeys = 3;

            for (int i = 0; i < putThreads; i++) {
                final int gridIdx = i;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting put thread...");

                            Grid grid = grid(gridIdx);

                            UUID locNodeId = grid.localNode().id();

                            GridCache<Integer, String> cache = grid.cache(CACHE_NAME);

                            List<Integer> keys = new ArrayList<>(txKeys);

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                keys.clear();

                                for (int i = 0; i < txKeys; i++)
                                    keys.add(RAND.nextInt(keyCnt));

                                // Ensure lock order.
                                Collections.sort(keys);

                                int c = 0;

                                try (GridCacheTx tx = cache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                                    c = txCntr.incrementAndGet();

                                    if (c % logFreq == 0)
                                        info(">>> Tx iteration started [cnt=" + c + ", keys=" + keys + ", " +
                                            "locNodeId=" + locNodeId + ']');

                                    Map<Integer, String> batch = new LinkedHashMap<>();

                                    for (int key : keys)
                                        batch.put(key, String.valueOf(key));

                                    cache.putAll(batch);

                                    tx.commit();
                                }
                                catch (GridTopologyException ignored) {
                                    // It is ok if primary node leaves grid.
                                }

                                if (c % logFreq == 0)
                                    info(">>> Tx iteration finished [cnt=" + c + ", keys=" + keys + ", " +
                                        "locNodeId=" + locNodeId + ']');
                            }
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to put value in cache.", e);
                        }
                    }
                }, "put-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (int i = 0; i < restartThreads; i++) {
                final int gridIdx = i + putThreads;

                Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        try {
                            barrier.await();

                            info("Starting restart thread...");

                            int cnt = 0;

                            while (System.currentTimeMillis() < endTime && err.get() == null) {
                                stopGrid(gridIdx);
                                startGrid(gridIdx);

                                int c = ++cnt;

                                if (c % logFreq == 0)
                                    info(">>> Restart iteration: " + c);
                            }
                        }
                        catch (Exception e) {
                            err.compareAndSet(null, e);

                            error("Failed to restart grid node.", e);
                        }
                    }
                }, "restart-worker-" + i);

                t.start();

                threads.add(t);
            }

            for (Thread t : threads)
                t.join();

            if (err.get() != null)
                throw err.get();
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * @return Cache operation.
     */
    private int cacheOp() {
        return RAND.nextInt(3) + 1;
    }

    /**
     * @param c Cache projection.
     * @param key Key.
     * @param attempt Attempt.
     */
    private void printFailureDetails(GridCache<Integer, String> c, int key, int attempt) {
        error("*** Failure details ***");
        error("Key: " + key);
        error("Partition: " + c.configuration().getAffinity().partition(key));
        error("Attempt: " + attempt);
        error("Node: " + c.gridProjection().grid().localNode().id());
    }
}
